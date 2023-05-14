package ru.nsu.ccfit.manager.service;

import com.mongodb.client.FindIterable;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.nsu.ccfit.manager.exception.RabbitUnavailable;
import ru.nsu.ccfit.manager.producer.ManagerMessageProducer;
import ru.nsu.ccfit.manager.models.Task;
import ru.nsu.ccfit.manager.models.TaskStatus;
import ru.nsu.ccfit.manager.exception.NoSuchTask;
import ru.nsu.ccfit.manager.exception.NotMD5Hash;
import ru.nsu.ccfit.manager.repository.ActiveRequestsRepository;
import ru.nsu.ccfit.manager.repository.TaskRepository;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ManagerService {
    private final ManagerMessageProducer messageProducer;
    private final ActiveRequestsRepository activeRequestsRepository;
    private final TaskRepository taskRepository;
    private static final Logger logger = LogManager.getLogger(ManagerService.class);
    private final CrackHashManagerRequest.Alphabet alphabet = new CrackHashManagerRequest.Alphabet();

    private static final String WORKERS_AMOUNT_ENV_VAR_NAME = "WORKERS_AMOUNT";
    private static final int DEFAULT_WORKERS_AMOUNT = 1;
    private int workersAmount;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    @Autowired
    public ManagerService(ManagerMessageProducer messageProducer, ActiveRequestsRepository activeRequestsRepository, TaskRepository taskRepository) {
        this.messageProducer = messageProducer;
        this.activeRequestsRepository = activeRequestsRepository;
        this.taskRepository = taskRepository;

        alphabet.getSymbols().addAll(Arrays.asList("0123456789abcdefghijklmnopqrstuvwxyz".split("")));

        try {
            var workersAmountString =  System.getenv(WORKERS_AMOUNT_ENV_VAR_NAME);

            if (workersAmountString == null) {
                throw new IllegalArgumentException(String.format("No %s env var", WORKERS_AMOUNT_ENV_VAR_NAME));
            }

            workersAmount = Integer.parseInt(workersAmountString);
        }
        catch (IllegalArgumentException e) {
            workersAmount = DEFAULT_WORKERS_AMOUNT;
            logger.error("Error getting {}, manager will set it as {}", WORKERS_AMOUNT_ENV_VAR_NAME, workersAmount);
            logger.error(e);
        }

        try {
            var context = JAXBContext.newInstance(CrackHashManagerRequest.class);

            var xmlMarshaller = context.createMarshaller();
            xmlMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            this.marshaller = xmlMarshaller;

            this.unmarshaller = context.createUnmarshaller();
        }
        catch (JAXBException e) {
            logger.error("Unable to create marshaller/unmarshaller to save/extract active requests to/from db");
            logger.error(e.getMessage());
        }
    }

    private boolean isMD5(String hash) {
        String pattern = "^[a-fA-F\\d]{32}$"; // pattern for MD5 hash
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(hash);
        return matcher.matches();
    }

    public Task getTaskStatus(String taskId) throws NoSuchTask {
        if (taskId == null || taskRepository.get(taskId) == null) {
            throw new NoSuchTask(String.format("Task with UUID %s is not found", taskId));
        }
        return taskRepository.get(taskId);
    }

    public String crackHash(String hash, int maxLength) throws NotMD5Hash {
        if (!isMD5(hash)) {
            throw new NotMD5Hash(String.format("%s is not valid MD5 hash", hash));
        }

        if (maxLength < 1) {
            throw new IllegalArgumentException(String.format("%d is not valid maximum length for target word", maxLength));
        }

        var requestId = UUID.randomUUID();
        var requestIdString = requestId.toString();

        for (int i = 0; i < workersAmount; i++) {
            var managerRequest = new CrackHashManagerRequest();
            managerRequest.setHash(hash);
            managerRequest.setMaxLength(maxLength);
            managerRequest.setAlphabet(alphabet);
            managerRequest.setRequestId(requestIdString);
            managerRequest.setPartCount(workersAmount);
            managerRequest.setPartNumber(i);

            try {
                messageProducer.sendRequest(managerRequest);
            }
            catch (RabbitUnavailable e) {
                saveActiveRequest(managerRequest);
            }
        }

        var task = new Task(TaskStatus.IN_PROGRESS);
        taskRepository.insert(requestIdString, task);

        return requestIdString;
    }

    public String crackHashManagerRequest2Xml(CrackHashManagerRequest request) throws JAXBException {
        var stringWriter = new StringWriter();
        marshaller.marshal(request, stringWriter);
        return stringWriter.toString();
    }

    public void saveActiveRequest(CrackHashManagerRequest request) {
        try {
            var xmlRequest = crackHashManagerRequest2Xml(request);
            activeRequestsRepository.save(xmlRequest);
        }
        catch (JAXBException e) {
            logger.error("Unable to convert manager request to xml for saving in db,{} requests will not be saved", request.getRequestId());
            logger.error(e.getMessage());
        }
    }

    public void processWorkerResponse(CrackHashWorkerResponse response) {
        logger.info("Got response from worker with task {}", response.getRequestId());
        logger.info("{} results:", response.getAnswers());
        logger.info(response.getAnswers().getWords());

        var task = taskRepository.get(response.getRequestId());

        if (!task.getFinishedParts().contains(response.getPartNumber())) {
            task.getFinishedParts().add(response.getPartNumber());
            task.getData().addAll(response.getAnswers().getWords());

            if (task.getFinishedParts().size() == workersAmount) {
                task.setStatus(TaskStatus.READY);
                logger.info("Finished task with UUID {}, results: {}", response.getRequestId(), task.getData());
            }

            taskRepository.insert(response.getRequestId(), task);
        }
        else {
            logger.warn("Duplicate task result for request {}, not adding it", response.getRequestId());
        }
    }

    @Scheduled(fixedDelay = 15000)
    public void trySendRequestsAgain() {
        FindIterable<Document> tasks = activeRequestsRepository.findAll();
        tasks.forEach(task -> {
            String xml = (String) task.get(ActiveRequestsRepository.REQUEST_PAYLOAD_FIELD_NAME);
            try {
                var reader = new StringReader(xml);
                CrackHashManagerRequest request = (CrackHashManagerRequest) unmarshaller.unmarshal(reader);

                logger.info("Trying send request {} with par number {} again", request.getRequestId(), request.getPartNumber());
                messageProducer.sendRequest(request);

                activeRequestsRepository.delete(task);
                logger.info("Successfully sent request");
            }
            catch (AmqpException e) {
                logger.error("Unable to send message again: {}", xml);
            }
            catch (JAXBException e) {
                logger.error("Unable to unmarshall xml from database to ManagerRequest format, " +
                        "this record will be deleted from database and task with this part number will be lost");
                activeRequestsRepository.delete(task);
            } catch (RabbitUnavailable e) {
                logger.error("Rabbit is still unavailable, will try later");
            }
        });
    }
}

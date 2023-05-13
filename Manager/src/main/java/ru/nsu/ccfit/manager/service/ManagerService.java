package ru.nsu.ccfit.manager.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nsu.ccfit.manager.producer.ManagerMessageProducer;
import ru.nsu.ccfit.manager.models.Task;
import ru.nsu.ccfit.manager.models.TaskStatus;
import ru.nsu.ccfit.manager.exception.NoSuchTask;
import ru.nsu.ccfit.manager.exception.NotMD5Hash;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ManagerService {
    @Autowired
    private ManagerMessageProducer messageProducer;
    private static final Logger logger = LogManager.getLogger(ManagerService.class);
    private final ConcurrentHashMap<String, Task> taskStatuses = new ConcurrentHashMap<>();
    private final CrackHashManagerRequest.Alphabet alphabet = new CrackHashManagerRequest.Alphabet();

    private static final String WORKERS_AMOUNT_ENV_VAR_NAME = "WORKERS_AMOUNT";
    private static final int DEFAULT_WORKERS_AMOUNT = 1;
    private int workersAmount;

    public ManagerService() {
        alphabet.getSymbols().addAll(Arrays.asList(
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
                "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                "u", "v", "w", "x", "y", "z"));

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
    }

    private boolean isMD5(String hash) {
        String pattern = "^[a-fA-F\\d]{32}$"; // pattern for MD5 hash
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(hash);
        return matcher.matches();
    }

    public Task getTaskStatus(String taskId) throws NoSuchTask {
        if (taskId == null || !taskStatuses.containsKey(taskId)) {
            throw new NoSuchTask(String.format("Task with UUID %s is not found", taskId));
        }
        return taskStatuses.get(taskId);
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

            messageProducer.sendMessage(managerRequest);
        }

        taskStatuses.put(requestIdString, new Task(TaskStatus.IN_PROGRESS));

        return requestIdString;
    }

    public void processWorkerResponse(CrackHashWorkerResponse response) {
        logger.info("Got response from worker with task {}", response.getRequestId());
        logger.info("{} results:", response.getAnswers());
        logger.info(response.getAnswers().getWords());

        // todo: task may be null
        var task = taskStatuses.get(response.getRequestId());

        if (!task.getFinishedParts().contains(response.getPartNumber())) {
            task.getFinishedParts().add(response.getPartNumber());
            task.getData().addAll(response.getAnswers().getWords());
        }
        else {
            logger.warn("Duplicate task result for request {}, not adding it", response.getRequestId());
        }

        if (task.getFinishedParts().size() == workersAmount) {
            task.setStatus(TaskStatus.READY);
            logger.info("Finished task with UUID {}, results: {}", response.getRequestId(), task.getData());
        }
    }
}

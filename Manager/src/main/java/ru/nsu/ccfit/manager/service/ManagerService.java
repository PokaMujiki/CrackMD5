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

    public ManagerService() {
        alphabet.getSymbols().addAll(Arrays.asList(
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
                "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                "u", "v", "w", "x", "y", "z"));
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

        var managerRequest = new CrackHashManagerRequest();
        managerRequest.setHash(hash);
        managerRequest.setMaxLength(maxLength);
        managerRequest.setAlphabet(alphabet);
        managerRequest.setRequestId(requestIdString);

        taskStatuses.put(requestIdString, new Task(TaskStatus.IN_PROGRESS));

        messageProducer.sendMessage(managerRequest);

        return requestIdString;
    }

    public void processWorkerResponse(CrackHashWorkerResponse response) {
        logger.info("Got response from worker with task {}", response.getRequestId());
        logger.info("{} results:", response.getRequestId());
        logger.info(response.getAnswers().getWords());
        var task = taskStatuses.get(response.getRequestId());
        task.setStatus(TaskStatus.READY);
        task.getData().addAll(response.getAnswers().getWords());
    }
}

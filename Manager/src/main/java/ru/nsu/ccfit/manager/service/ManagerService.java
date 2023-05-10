package ru.nsu.ccfit.manager.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.nsu.ccfit.manager.Task;
import ru.nsu.ccfit.manager.TaskStatus;
import ru.nsu.ccfit.manager.exception.NoSuchTask;
import ru.nsu.ccfit.manager.exception.NotMD5Hash;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ManagerService {
    private static final Logger logger = LogManager.getLogger(ManagerService.class);
    private static final int TIMEOUT = 20; // seconds
    private static final String WORKER_CRACK_ENDPOINT = "http://worker:8081/internal/api/worker/hash/crack/task";
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

        var requestId = UUID.nameUUIDFromBytes(hash.getBytes(StandardCharsets.UTF_8));
        var requestIdString = requestId.toString();

        if (taskStatuses.containsKey(requestIdString) && taskStatuses.get(requestIdString).getStatus() != TaskStatus.ERROR) {
            logger.info("Task with UUID {} is already executing and it's status is not ERROR", requestIdString);
            return requestIdString;
        }

        var managerRequest = new CrackHashManagerRequest();
        managerRequest.setHash(hash);
        managerRequest.setMaxLength(maxLength);
        managerRequest.setAlphabet(alphabet);
        managerRequest.setRequestId(requestIdString);

        var client = WebClient.create(WORKER_CRACK_ENDPOINT);
        client.post()
                .contentType(MediaType.APPLICATION_XML)
                .bodyValue(managerRequest)
                .retrieve()
                .bodyToMono(CrackHashWorkerResponse.class)
                .timeout(Duration.ofSeconds(TIMEOUT))
                .subscribe(
                        response -> {
                            logger.info("Got response from worker with task {}", requestIdString);
                            logger.info("{} results:", requestIdString);
                            logger.info(response.getAnswers().getWords());

                            var task = taskStatuses.get(requestIdString);
                            task.setStatus(TaskStatus.READY);
                            task.getData().addAll(response.getAnswers().getWords());
                        },
                        error -> {
                            logger.error("Error receiving response from worker {}", requestId);
                            logger.error(error);
                            taskStatuses.get(requestIdString).setStatus(TaskStatus.ERROR);
                        }
                );

        taskStatuses.put(requestIdString, new Task(TaskStatus.IN_PROGRESS));

        return requestIdString;
    }
}

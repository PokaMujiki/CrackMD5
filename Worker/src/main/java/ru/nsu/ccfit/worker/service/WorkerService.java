package ru.nsu.ccfit.worker.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.paukov.combinatorics3.Generator;
import org.springframework.stereotype.Service;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse;

import java.util.ArrayList;

@Service
public class WorkerService {
    private static final Logger logger = LogManager.getLogger(WorkerService.class);
    public CrackHashWorkerResponse crackHash(CrackHashManagerRequest taskInfo) {
        var words = new ArrayList<String>();

        for (int i = 1; i <= taskInfo.getMaxLength(); i++) {
            Generator.permutation(taskInfo.getAlphabet().getSymbols())
                    .withRepetitions(i)
                    .stream()
                    .forEach(combinationArray -> {
                        var combination = String.join("", combinationArray);
                        var hash = DigestUtils.md5Hex(combination);

                        if (hash.equals(taskInfo.getHash())) {
                            logger.info("For hash {} found word {}", taskInfo.getHash(), combination);
                            words.add(combination);
                        }
                    });
        }

        var response = new CrackHashWorkerResponse();
        var answers = new CrackHashWorkerResponse.Answers();
        response.setRequestId(taskInfo.getRequestId());

        answers.getWords().addAll(words);
        response.setAnswers(answers);

        return response;
    }
}

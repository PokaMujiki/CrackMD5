package ru.nsu.ccfit.worker.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.paukov.combinatorics3.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse;
import ru.nsu.ccfit.worker.producer.WorkerMessageProducer;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WorkerService {
    private static final Logger logger = LogManager.getLogger(WorkerService.class);

    @Autowired
    WorkerMessageProducer messageProducer;
    public CrackHashWorkerResponse crackHash(CrackHashManagerRequest taskInfo) {
        var words = new ArrayList<String>();

        AtomicInteger step = new AtomicInteger();
        var partNumber = taskInfo.getPartNumber();
        var partCount = taskInfo.getPartCount();

        for (int i = 1; i <= taskInfo.getMaxLength(); i++) {
            Generator.permutation(taskInfo.getAlphabet().getSymbols())
                    .withRepetitions(i)
                    .stream()
                    .forEach(combinationArray -> {
                        var currentStep = step.get();
                        if (currentStep != partNumber) {
                            if (currentStep == partCount - 1) {
                                step.set(0);
                            }
                            else {
                                step.getAndIncrement();
                            }
                            return;
                        }

                        var combination = String.join("", combinationArray);
                        var hash = DigestUtils.md5Hex(combination);

                        if (hash.equals(taskInfo.getHash())) {
                            logger.info("For hash {} found word {}", taskInfo.getHash(), combination);
                            words.add(combination);
                        }

                        if (currentStep == partCount - 1) {
                            step.set(0);
                        }
                        else {
                            step.getAndIncrement();
                        }
                    });
        }

        var response = new CrackHashWorkerResponse();
        var answers = new CrackHashWorkerResponse.Answers();
        response.setRequestId(taskInfo.getRequestId());
        response.setPartNumber(taskInfo.getPartNumber());

        answers.getWords().addAll(words);
        response.setAnswers(answers);

        return response;
    }
}

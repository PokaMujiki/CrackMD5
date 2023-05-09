package ru.nsu.ccfit.worker.controller;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse;
import ru.nsu.ccfit.worker.service.WorkerService;

import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@RestController
@RequestMapping("/internal/api/worker/hash/crack/task")
public class WorkerController {
    private static final Logger logger = LogManager.getLogger(WorkerController.class);
    @Autowired
    WorkerService workerService;

    @PostMapping(produces = APPLICATION_XML_VALUE)
    public ResponseEntity<CrackHashWorkerResponse> crackHash(@RequestBody CrackHashManagerRequest taskInfo) {
        try {
            logger.info("Received request for cracking following hash: {}", taskInfo.getHash());
            return ResponseEntity.ok(workerService.crackHash(taskInfo));
        }
        catch (Exception e) {
            logger.error(e);
            return ResponseEntity.badRequest().build();
        }
    }
}

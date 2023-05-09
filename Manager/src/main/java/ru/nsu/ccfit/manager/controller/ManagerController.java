package ru.nsu.ccfit.manager.controller;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nsu.ccfit.manager.HashInfo;
import ru.nsu.ccfit.manager.Task;
import ru.nsu.ccfit.manager.exception.NoSuchTask;
import ru.nsu.ccfit.manager.exception.NotMD5Hash;
import ru.nsu.ccfit.manager.service.ManagerService;

@RestController
@RequestMapping("/api/hash/")
public class ManagerController {

    private static final Logger logger = LogManager.getLogger(ManagerController.class);
    @Autowired
    ManagerService managerService;

    @PostMapping("/crack")
    public ResponseEntity<String> crackHash(@RequestBody HashInfo hash) {
        try {
            logger.info("Received request for following hash crack: {}", hash.getHash());
            return ResponseEntity.ok(managerService.crackHash(hash.getHash(), hash.getMaxLength()));
        }
        catch (NotMD5Hash | IllegalArgumentException e) {
            logger.warn("Given hash is not in MD-5 format or maxLength is not valid");
            logger.warn(e.getMessage());
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Task> getTaskStatus(@RequestParam String requestId) {
        try {
            logger.info("Requested status for {}", requestId);
            return ResponseEntity.ok(managerService.getTaskStatus(requestId));
        }
        catch (NoSuchTask e) {
            logger.warn("No task associated with this UUID: {}", requestId);
            logger.warn(e.getMessage());
            return ResponseEntity.notFound().build();
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

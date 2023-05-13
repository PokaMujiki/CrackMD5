package ru.nsu.ccfit.manager.consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nsu.ccfit.manager.service.ManagerService;
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse;

@Service
public class ManagerMessageConsumer {
    private static final Logger logger = LogManager.getLogger(ManagerMessageConsumer.class);

    @Autowired
    ManagerService managerService;

    @RabbitListener(queues={"${rabbitmq.worker.response.queue.name}"})
    public void consume(CrackHashWorkerResponse response) {
        logger.info("Received answer from worker: {}", response);
        managerService.processWorkerResponse(response);
    }
}
package ru.nsu.ccfit.worker.consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;
import ru.nsu.ccfit.worker.producer.WorkerMessageProducer;
import ru.nsu.ccfit.worker.service.WorkerService;

@Service
public class WorkerMessageConsumer {
    private static final Logger logger = LogManager.getLogger(WorkerMessageConsumer.class);
    @Autowired
    private WorkerService workerService;

    @Autowired
    private WorkerMessageProducer workerProducer;

    @RabbitListener(queues = {"${rabbitmq.manager.request.queue.name}"}, containerFactory = "rabbitListenerContainerFactory")
    public void consume(CrackHashManagerRequest request) {
        logger.info("Received message with request id: {}, part number {}", request.getRequestId(), request.getPartNumber());
        var result = workerService.crackHash(request);
        workerProducer.sendMessage(result);
    }
}

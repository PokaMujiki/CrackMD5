package ru.nsu.ccfit.worker.producer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse;


@Service
public class WorkerMessageProducer {
    private static final Logger logger = LogManager.getLogger(WorkerMessageProducer.class);
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.worker.response.routing.key}")
    private String routingKey;

    public void sendMessage(CrackHashWorkerResponse response) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, response);
        logger.info("Sent answer queue for manager, request id: {}, part number: {}",
                response.getRequestId(), response.getPartNumber());
    }
}

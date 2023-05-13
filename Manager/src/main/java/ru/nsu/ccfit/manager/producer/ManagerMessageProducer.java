package ru.nsu.ccfit.manager.producer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;

@Service
public class ManagerMessageProducer {
    private static final Logger logger = LogManager.getLogger(ManagerMessageProducer.class);
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.manager.request.routing.key}")
    private String routingKey;

    public void sendMessage(CrackHashManagerRequest request) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, request);
        logger.info("Sent message to RabbitMQ: {}", request);
    }
}

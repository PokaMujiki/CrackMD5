package ru.nsu.ccfit.manager.producer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.nsu.ccfit.manager.exception.RabbitUnavailable;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;

import java.nio.charset.StandardCharsets;

@Service
public class ManagerMessageProducer {
    private static final Logger logger = LogManager.getLogger(ManagerMessageProducer.class);
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.manager.request.routing.key}")
    private String routingKey;

    public void sendRequest(CrackHashManagerRequest request) throws RabbitUnavailable {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, request);
            logger.info("Sent message to RabbitMQ: id {}, part number: {}", request.getRequestId(), request.getPartNumber());
        }
        catch (AmqpException e) {
            logger.error("Unable to send message to RabbitMQ: id {}, part number {}", request.getRequestId(), request.getPartNumber());
            logger.warn("Request with id {} will be saved in db while RabbitMQ is down", request.getRequestId());
            throw new RabbitUnavailable();
        }
    }

    public void sendMessage(String xml) {
        rabbitTemplate.send(exchangeName, routingKey, new Message(xml.getBytes(StandardCharsets.UTF_8)));
    }
}

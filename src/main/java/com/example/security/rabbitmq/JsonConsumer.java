package com.example.security.rabbitmq;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.security.config.RabbitMqConfig;
import com.example.security.message.ArticleMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import com.rabbitmq.client.Channel;

@Service
@Slf4j
public class JsonConsumer {
    @Autowired
    ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.queue_json)
    public void consume(final Message message) throws IOException {
        ArticleMessage mes = objectMapper.readValue(message.getBody(), ArticleMessage.class);
        log.info("Received message [x]: {}", message);
        log.info("Event message: {}", mes.toString());

    }
}

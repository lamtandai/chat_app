package com.example.security.rabbitmq;

import java.util.Objects;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.security.config.RabbitMqConfig;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class JsonProducer {
    @Autowired
    private AmqpTemplate template;
    public void sendMessage(Object obj){
        if (Objects.isNull(obj)){
            return;
        }
        log.info("Sending to rabbitmq data: {}", obj.toString());
        template.convertAndSend(RabbitMqConfig.exchange_json, RabbitMqConfig.routing_key_json, obj);

    }
}

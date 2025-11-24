package com.example.security.rabbitmq;

import java.util.Objects;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.security.config.RabbitMqConfig;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class Producer {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendMessage(Object obj){
        if (Objects.isNull(obj)){
            return;
        }
        log.info("Sending to rabbitmq data: {}", obj);
        rabbitTemplate.convertAndSend(RabbitMqConfig.exchange_name, RabbitMqConfig.routing_key, obj);

    }
}

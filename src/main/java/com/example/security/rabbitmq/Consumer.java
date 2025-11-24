package com.example.security.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.example.security.config.RabbitMqConfig;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class Consumer {
    
    @RabbitListener(queues = RabbitMqConfig.queue_name)
    public void consume(Object object){
        log.info("Consume data: {}", object.toString());
        
    }
}

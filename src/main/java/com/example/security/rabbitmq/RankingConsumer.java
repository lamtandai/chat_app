package com.example.security.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.example.security.config.RabbitMqConfig;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RankingConsumer {
    @RabbitListener(queues = "/topic/rankings")
    public void consume(Object object){
        log.info("Consume data: {}", object.toString());
        
    }
}

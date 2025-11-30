package com.example.security.controller;


import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


import com.example.security.dto.SendingMessageRequest;
import com.example.security.service.ChatService;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController

public class TestController {
    private RabbitTemplate rabbitTemplate;
    private ChatService chatService;

    public TestController(
        RabbitTemplate rabbitTemplate, 
        ChatService chatService) {

        this.rabbitTemplate = rabbitTemplate;
        this.chatService = chatService;

    }

    @GetMapping("/test/normal")
    public ResponseEntity<Object> test() {
        rabbitTemplate.convertAndSend("exchange_1", "routing_key_1_to_q1", "test message");
        return ResponseEntity.ok("test message");
    }

    @PostMapping("/test/send-message")
    public void getMethodName(@RequestBody SendingMessageRequest mess) {
        chatService.addMessage(mess);
    }
}

package com.example.security.listener;

import com.example.security.event.UserRegistrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatQueueListener {

    private final AmqpAdmin amqpAdmin;

    @EventListener
    public void handleUserRegistration(UserRegistrationEvent event) {
        String userId = event.getUser().getId().toString();
        String queueName = "chat-user-" + userId;

        try {
            amqpAdmin.declareQueue(new Queue(queueName, true));
            log.info("✅ Created chat queue for user: {}", userId);
        } catch (Exception e) {
            log.error("❌ Failed to create chat queue for user: {}", userId, e);
        }
    }
}

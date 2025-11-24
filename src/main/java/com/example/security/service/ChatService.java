package com.example.security.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.security.dto.MessageRequest;
import com.example.security.dto.MessageResponse;
import com.example.security.model.Message;
import com.example.security.model.User;
import com.example.security.repository.ChatRepository;
import com.example.security.repository.UserRepository;
import java.util.UUID;
import java.util.Optional;

@Service
public class ChatService {
    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void addMessage(MessageRequest messString) {
        Message newMess = Message.builder()
                .content(messString.getContent())
                .sender(messString.getSender())
                .createdAt(Instant.now())
                .build();

        chatRepository.save(newMess);

        showMessage();
    }

    public void showMessage() {
        List<MessageResponse> messages = this.chatRepository
                .findAll()
                .stream()
                .map(mess -> {
                    String senderId = mess.getSender();
                    String senderName = "Unknown";
                    String senderAvatar = "";

                    if (senderId != null) {
                        try {
                            Optional<User> userOpt = userRepository.findById(UUID.fromString(senderId));
                            if (userOpt.isPresent()) {
                                User user = userOpt.get();
                                senderName = user.getName() != null ? user.getName() : user.getUsername();
                                senderAvatar = user.getPicture(); // Assuming User has a getPicture() method
                            }
                        } catch (IllegalArgumentException e) {
                            // Handle invalid UUID string if necessary
                        }
                    }

                    return new MessageResponse(mess.getContent(), senderId, senderName, senderAvatar);
                })
                .collect(Collectors.toList());

        messagingTemplate.convertAndSend("/topic/chat", messages);
    }

}

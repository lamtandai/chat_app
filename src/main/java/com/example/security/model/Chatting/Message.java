package com.example.security.model.Chatting;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "messages")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message implements Serializable {
    @Id
    private Long messageId; // Local sequence-based ID
    private Long channelId; // Identifies the conversation (1-on-1 or group)
    private Long sequenceNumber; // Sequence within the channel
    private String content;
    private String sender;
    private String receiver;
    private Instant createdAt;

}

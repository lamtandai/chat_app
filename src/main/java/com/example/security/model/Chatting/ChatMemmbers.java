package com.example.security.model.Chatting;

import java.io.Serializable;
import java.util.UUID;

import com.example.security.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_members")
@IdClass(ChatMemberId.class)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMemmbers implements Serializable {
    @Id
    @Column(name = "channel_id")
    private long channel;

    @Id
    @Column(name = "user_id")
    private UUID user;
}

package com.example.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageResponse {
    private String content;
    private String sender;
    private String senderName;
    private String senderAvatar;
}

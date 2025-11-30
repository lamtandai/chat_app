package com.example.security.model.Chatting;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMemberId implements Serializable {
    private long channel;
    private UUID user;
}

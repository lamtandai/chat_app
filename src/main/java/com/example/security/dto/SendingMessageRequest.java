package com.example.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendingMessageRequest {
    private Sender sender;
    private Receiver receiver;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Sender {
        private String userId;
        private String content;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Receiver {
        private String userId;
        private Long channelId;
    }
}

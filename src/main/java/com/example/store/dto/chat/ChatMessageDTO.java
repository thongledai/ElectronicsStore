package com.example.store.dto.chat;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.store.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {

    private UUID id;
    private UUID roomId;
    private UUID senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private MessageType messageType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

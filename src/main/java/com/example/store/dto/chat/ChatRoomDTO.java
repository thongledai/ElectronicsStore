package com.example.store.dto.chat;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.store.enums.ChatRoomStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDTO {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String customerAvatar;
    private UUID employeeId;
    private String employeeName;
    private ChatRoomStatus status;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
    private LocalDateTime createdAt;
}

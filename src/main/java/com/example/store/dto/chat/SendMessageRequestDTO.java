package com.example.store.dto.chat;

import java.util.UUID;

import com.example.store.enums.MessageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequestDTO {

    @NotNull(message = "CHAT_ROOM_ID_REQUIRED")
    private UUID roomId;

    @NotBlank(message = "CHAT_CONTENT_REQUIRED")
    @Size(max = 1000, message = "CHAT_CONTENT_MAX_LENGTH")
    private String content;

    @Builder.Default
    private MessageType messageType = MessageType.TEXT;
}

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

    @NotNull(message = "ID phòng chat không được để trống")
    private UUID roomId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Size(max = 1000, message = "Tin nhắn tối đa 1000 ký tự")
    private String content;

    @Builder.Default
    private MessageType messageType = MessageType.TEXT;
}

package com.example.store.service.chat;

import java.util.List;
import java.util.UUID;

import com.example.store.dto.chat.ChatMessageDTO;
import com.example.store.dto.chat.ChatRoomDTO;
import com.example.store.dto.chat.SendMessageRequestDTO;

public interface IChatService {

    ChatRoomDTO getOrCreateRoomForCustomer(UUID customerId);

    ChatMessageDTO sendMessage(UUID senderId, SendMessageRequestDTO request);

    List<ChatMessageDTO> getRoomMessages(UUID roomId, UUID requesterId);

    List<ChatRoomDTO> getOpenRoomsForEmployee(UUID employeeId);

    ChatRoomDTO assignEmployee(UUID roomId, UUID employeeId);

    ChatRoomDTO closeRoom(UUID roomId, UUID employeeId);

    long getOpenRoomsCount();
}

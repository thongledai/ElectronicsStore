package com.example.store.service.chat.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.chat.ChatMessageDTO;
import com.example.store.dto.chat.ChatRoomDTO;
import com.example.store.dto.chat.SendMessageRequestDTO;
import com.example.store.entity.ChatMessage;
import com.example.store.entity.ChatRoom;
import com.example.store.entity.User;
import com.example.store.enums.ChatRoomStatus;
import com.example.store.enums.MessageType;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.repository.ChatMessageRepository;
import com.example.store.repository.ChatRoomRepository;
import com.example.store.repository.UserRepository;
import com.example.store.service.chat.IChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements IChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatRoomDTO getOrCreateRoomForCustomer(UUID customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        // Luôn tìm cuộc hội thoại gần nhất của khách hàng (giữ lại 100% lịch sử tin nhắn cũ)
        ChatRoom room = chatRoomRepository.findFirstByCustomerIdOrderByUpdatedAtDesc(customerId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .customer(customer)
                            .status(ChatRoomStatus.OPEN)
                            .build();
                    return chatRoomRepository.save(newRoom);
                });

        if (room.getStatus() == ChatRoomStatus.CLOSED) {
            room.setStatus(ChatRoomStatus.OPEN);
            chatRoomRepository.save(room);
        }

        return mapToRoomDTO(room, customerId);
    }

    @Override
    @Transactional
    public ChatMessageDTO sendMessage(UUID senderId, SendMessageRequestDTO request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người gửi."));

        ChatRoom room = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chat."));

        // Tự động gán nhân viên nếu phòng chưa có ai tiếp nhận và người gửi là nhân viên
        boolean isEmployee = sender.getRole() != null &&
                ("EMPLOYEE".equalsIgnoreCase(sender.getRole().getName()) ||
                 "ROLE_EMPLOYEE".equalsIgnoreCase(sender.getRole().getName()) ||
                 "MANAGER".equalsIgnoreCase(sender.getRole().getName()) ||
                 "ROLE_MANAGER".equalsIgnoreCase(sender.getRole().getName()));

        // Nếu phòng trước đó đã đóng (CLOSED), khi khách gửi tin nhắn mới -> tự động mở lại phiên (OPEN) để nhân viên tiếp nhận
        if (room.getStatus() == ChatRoomStatus.CLOSED) {
            room.setStatus(ChatRoomStatus.OPEN);
            room.setEmployee(null); // Reset người tiếp nhận để nhân viên ca trực mới có thể nhận
            chatRoomRepository.save(room);
        }

        if (isEmployee && room.getEmployee() == null) {
            room.setEmployee(sender);
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.getContent().trim())
                .messageType(request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT)
                .isRead(false)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Cập nhật thời gian phòng chat
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        ChatMessageDTO messageDTO = mapToMessageDTO(savedMessage);

        // Gửi qua WebSocket realtime
        try {
            // 1. Broadcast tới channel riêng của phòng chat
            messagingTemplate.convertAndSend("/topic/chat/" + room.getId().toString(), (Object) messageDTO);

            // 2. Nếu khách hàng gửi, thông báo tới channel nhân viên để nhân viên biết có tin nhắn mới
            if (!isEmployee) {
                messagingTemplate.convertAndSend("/topic/employee/chat-notifications", (Object) java.util.Map.of(
                        "type", "NEW_MESSAGE",
                        "roomId", room.getId().toString(),
                        "customerName", sender.getFullName() != null ? sender.getFullName() : sender.getEmail(),
                        "content", savedMessage.getContent(),
                        "time", savedMessage.getCreatedAt().toString()
                ));
            }
        } catch (Exception e) {
            log.warn("Không thể gửi tin nhắn WebSocket: {}", e.getMessage());
        }

        return messageDTO;
    }

    @Override
    @Transactional
    public List<ChatMessageDTO> getRoomMessages(UUID roomId, UUID requesterId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chat."));

        // Đánh dấu các tin nhắn chưa đọc của người khác thành đã đọc
        chatMessageRepository.markMessagesAsRead(roomId, requesterId);

        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId);
        return messages.stream().map(this::mapToMessageDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getOpenRoomsForEmployee(UUID employeeId) {
        List<ChatRoom> rooms = chatRoomRepository.findByStatusOrderByUpdatedAtDesc(ChatRoomStatus.OPEN);
        return rooms.stream().map(r -> mapToRoomDTO(r, employeeId)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatRoomDTO assignEmployee(UUID roomId, UUID employeeId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chat."));

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên."));

        room.setEmployee(employee);
        ChatRoom updated = chatRoomRepository.save(room);

        ChatRoomDTO dto = mapToRoomDTO(updated, employeeId);

        // Thông báo qua WebSocket cho khách hàng biết nhân viên đã tham gia (ẩn danh tính: Nhân Viên CSKH)
        try {
            messagingTemplate.convertAndSend("/topic/chat/" + room.getId().toString(), (Object) java.util.Map.of(
                    "type", "EMPLOYEE_JOINED",
                    "employeeName", "Nhân Viên CSKH",
                    "message", "Nhân Viên CSKH đã tham gia đoạn chat để hỗ trợ bạn."
            ));
        } catch (Exception e) {
            log.warn("Lỗi gửi thông báo WebSocket: {}", e.getMessage());
        }

        return dto;
    }

    @Override
    @Transactional
    public ChatRoomDTO closeRoom(UUID roomId, UUID employeeId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chat."));

        room.setStatus(ChatRoomStatus.CLOSED);
        ChatRoom updated = chatRoomRepository.save(room);

        // Thông báo đóng chat qua WebSocket
        try {
            messagingTemplate.convertAndSend("/topic/chat/" + room.getId().toString(), (Object) java.util.Map.of(
                    "type", "ROOM_CLOSED",
                    "message", "Cuộc hội thoại đã kết thúc. Cảm ơn quý khách đã liên hệ với TechNova!"
            ));
        } catch (Exception e) {
            log.warn("Lỗi gửi thông báo WebSocket: {}", e.getMessage());
        }

        return mapToRoomDTO(updated, employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getOpenRoomsCount() {
        return chatRoomRepository.countByStatus(ChatRoomStatus.OPEN);
    }

    private ChatRoomDTO mapToRoomDTO(ChatRoom room, UUID currentUserId) {
        String lastMsg = null;
        LocalDateTime lastTime = null;
        if (room.getMessages() != null && !room.getMessages().isEmpty()) {
            ChatMessage last = room.getMessages().get(room.getMessages().size() - 1);
            lastMsg = last.getContent();
            lastTime = last.getCreatedAt();
        }

        long unread = (currentUserId != null)
                ? chatMessageRepository.countByChatRoomIdAndIsReadFalseAndSenderIdNot(room.getId(), currentUserId)
                : 0;

        User customer = room.getCustomer();
        User employee = room.getEmployee();

        // Tên nhân viên: nếu người xem là khách hàng thì luôn hiển thị là "Nhân Viên CSKH" để bảo mật danh tính
        String employeeDisplayName = null;
        if (employee != null) {
            if (currentUserId != null && customer != null && customer.getId().equals(currentUserId)) {
                employeeDisplayName = "Nhân Viên CSKH";
            } else {
                employeeDisplayName = employee.getFullName() != null ? employee.getFullName() : employee.getEmail();
            }
        }

        return ChatRoomDTO.builder()
                .id(room.getId())
                .customerId(customer != null ? customer.getId() : null)
                .customerName(customer != null && customer.getFullName() != null ? customer.getFullName() : (customer != null ? customer.getEmail() : "Khách hàng"))
                .customerEmail(customer != null ? customer.getEmail() : "")
                .customerAvatar(customer != null ? customer.getAvatar() : null)
                .employeeId(employee != null ? employee.getId() : null)
                .employeeName(employeeDisplayName)
                .status(room.getStatus())
                .lastMessage(lastMsg)
                .lastMessageTime(lastTime)
                .unreadCount(unread)
                .createdAt(room.getCreatedAt())
                .build();
    }

    private ChatMessageDTO mapToMessageDTO(ChatMessage m) {
        User sender = m.getSender();
        String roleName = (sender != null && sender.getRole() != null) ? sender.getRole().getName() : "CUSTOMER";
        String senderName = "Người dùng";
        if (sender != null) {
            if ("CUSTOMER".equalsIgnoreCase(roleName) || "ROLE_CUSTOMER".equalsIgnoreCase(roleName)) {
                senderName = sender.getFullName() != null && !sender.getFullName().trim().isEmpty()
                        ? sender.getFullName()
                        : sender.getEmail();
            } else {
                // TÊN NHÂN VIÊN TUYỆT ĐỐI KHÔNG ĐƯỢC LỘ TRONG ĐOẠN CHAT:
                // Mặc định hiển thị là "Nhân Viên CSKH"
                senderName = "Nhân Viên CSKH";
            }
        }

        return ChatMessageDTO.builder()
                .id(m.getId())
                .roomId(m.getChatRoom() != null ? m.getChatRoom().getId() : null)
                .senderId(sender != null ? sender.getId() : null)
                .senderName(senderName)
                .senderRole(roleName)
                .content(m.getContent())
                .messageType(m.getMessageType())
                .isRead(m.getIsRead())
                .createdAt(m.getCreatedAt())
                .build();
    }
}

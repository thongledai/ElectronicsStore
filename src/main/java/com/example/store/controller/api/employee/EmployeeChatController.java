package com.example.store.controller.api.employee;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.dto.chat.ChatMessageDTO;
import com.example.store.dto.chat.ChatRoomDTO;
import com.example.store.dto.chat.SendMessageRequestDTO;
import com.example.store.dto.common.ApiResponse;
import com.example.store.enums.ApiMessage;
import com.example.store.security.CustomUserDetails;
import com.example.store.service.chat.IChatService;
import com.example.store.service.common.ICloudinaryUploadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employee/chat")
@RequiredArgsConstructor
public class EmployeeChatController {

    private final IChatService chatService;
    private final ICloudinaryUploadService cloudinaryUploadService;

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomDTO>>> getOpenRooms(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ApiMessage.LOGIN_REQUIRED));
        }
        List<ChatRoomDTO> rooms = chatService.getOpenRoomsForEmployee(user.getId());
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CHAT_EMPLOYEE_ROOMS_SUCCESS, rooms));
    }

    @GetMapping("/room-by-customer/{customerId}")
    public ResponseEntity<ApiResponse<ChatRoomDTO>> getOrCreateRoomByCustomer(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID customerId
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ApiMessage.LOGIN_REQUIRED));
        }
        ChatRoomDTO room = chatService.getOrCreateRoomForCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CHAT_ROOM_BY_CUSTOMER_SUCCESS, room));
    }

    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageDTO>>> getRoomMessages(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID roomId
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ApiMessage.LOGIN_REQUIRED));
        }
        List<ChatMessageDTO> messages = chatService.getRoomMessages(roomId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CHAT_MESSAGES_GET_SUCCESS, messages));
    }

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatMessageDTO>> sendMessage(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody SendMessageRequestDTO request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ApiMessage.LOGIN_REQUIRED));
        }
        ChatMessageDTO message = chatService.sendMessage(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CHAT_MESSAGE_SENT_SUCCESS, message));
    }

    @PutMapping("/room/{roomId}/assign")
    public ResponseEntity<ApiResponse<ChatRoomDTO>> assignRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID roomId
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ApiMessage.LOGIN_REQUIRED));
        }
        ChatRoomDTO room = chatService.assignEmployee(roomId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CHAT_ROOM_ASSIGN_SUCCESS, room));
    }

    @PutMapping("/room/{roomId}/close")
    public ResponseEntity<ApiResponse<ChatRoomDTO>> closeRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID roomId
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ApiMessage.LOGIN_REQUIRED));
        }
        ChatRoomDTO room = chatService.closeRoom(roomId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CHAT_ROOM_CLOSED_SUCCESS, room));
    }

    @GetMapping("/count-open")
    public ResponseEntity<ApiResponse<Long>> getOpenRoomsCount() {
        long count = chatService.getOpenRoomsCount();
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CHAT_OPEN_ROOMS_COUNT_SUCCESS, count));
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadChatImage(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("file") MultipartFile file
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ApiMessage.LOGIN_REQUIRED));
        }
        try {
            String url = cloudinaryUploadService.uploadImage(file, "chat");
            return ResponseEntity.ok(ApiResponse.success(ApiMessage.IMAGE_UPLOAD_SUCCESS, url));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ApiMessage.IMAGE_UPLOAD_FAILED));
        }
    }
}

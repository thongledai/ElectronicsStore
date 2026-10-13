package com.example.store.controller.api.customer;

import java.security.Principal;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.common.annotation.AuditAction;
import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.user.UserResponseDTO;
import com.example.store.dto.customer.CustomerProfileUpdateRequestDTO;
import com.example.store.enums.ApiMessage;
import com.example.store.service.user.IUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
public class CustomerProfileControllerAPI {

    private final IUserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponseDTO>> getProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(ApiMessage.AUTH_LOGIN_REQUIRED));
        }
        UserResponseDTO profile = userService.getCustomerProfile(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.USER_DETAIL_SUCCESS, profile));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditAction("Cập nhật thông tin cá nhân (kèm avatar)")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateProfileMultipart(
            Principal principal,
            @Valid @ModelAttribute CustomerProfileUpdateRequestDTO requestDTO,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(ApiMessage.AUTH_LOGIN_REQUIRED));
        }
        UserResponseDTO updated = userService.updateCustomerProfile(principal.getName(), requestDTO, avatarFile);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.USER_UPDATE_SUCCESS, updated));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction("Cập nhật thông tin cá nhân")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateProfileJson(
            Principal principal,
            @Valid @RequestBody CustomerProfileUpdateRequestDTO requestDTO) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(ApiMessage.AUTH_LOGIN_REQUIRED));
        }
        UserResponseDTO updated = userService.updateCustomerProfile(principal.getName(), requestDTO, null);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.USER_UPDATE_SUCCESS, updated));
    }
}

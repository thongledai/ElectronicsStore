package com.example.store.controller.api.manager;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.common.annotation.AuditAction;
import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.common.PageResponse;
import com.example.store.dto.user.UserResponseDTO;
import com.example.store.dto.user.UserCreateRequestDTO;
import com.example.store.dto.user.UserUpdateRequestDTO;
import com.example.store.enums.ApiMessage;
import com.example.store.service.user.IUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGER','ROLE_MANAGER')")
public class UserManagerControllerAPI {

    private final IUserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDTO>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<UserResponseDTO> result = userService.getAllUsers(search, role, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.USER_LIST_SUCCESS, result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable UUID id) {
        UserResponseDTO result = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.USER_DETAIL_SUCCESS, result));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<String>>> getAllRoles() {
        List<String> roles = userService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.ROLE_LIST_SUCCESS, roles));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditAction("Tạo mới người dùng (kèm avatar)")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUserMultipart(
            @Valid @ModelAttribute UserCreateRequestDTO requestDTO,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {
        UserResponseDTO result = userService.createUser(requestDTO, avatarFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiMessage.USER_CREATE_SUCCESS, result));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction("Tạo mới người dùng")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUserJson(
            @Valid @RequestBody UserCreateRequestDTO requestDTO) {
        UserResponseDTO result = userService.createUser(requestDTO, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiMessage.USER_CREATE_SUCCESS, result));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditAction("Cập nhật người dùng (kèm avatar)")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserMultipart(
            @PathVariable UUID id,
            @Valid @ModelAttribute UserUpdateRequestDTO requestDTO,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {
        UserResponseDTO result = userService.updateUser(id, requestDTO, avatarFile);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.USER_UPDATE_SUCCESS, result));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction("Cập nhật người dùng")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserJson(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequestDTO requestDTO) {
        UserResponseDTO result = userService.updateUser(id, requestDTO, null);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.USER_UPDATE_SUCCESS, result));
    }

    @DeleteMapping("/{id}")
    @AuditAction("Xóa người dùng")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID id,
            Principal principal) {
        userService.deleteUser(id, principal);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.USER_DELETE_SUCCESS));
    }
}

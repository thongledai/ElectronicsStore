package com.example.store.service.user;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.dto.common.PageResponse;
import com.example.store.dto.user.UserResponseDTO;
import com.example.store.dto.customer.CustomerProfileUpdateRequestDTO;
import com.example.store.dto.user.UserCreateRequestDTO;
import com.example.store.dto.user.UserUpdateRequestDTO;

public interface IUserService {
    PageResponse<UserResponseDTO> getAllUsers(String search, String role, Boolean isActive, Pageable pageable);
    UserResponseDTO getUserById(UUID id);
    UserResponseDTO createUser(UserCreateRequestDTO requestDTO);
    UserResponseDTO createUser(UserCreateRequestDTO requestDTO, MultipartFile avatarFile);
    UserResponseDTO updateUser(UUID id, UserUpdateRequestDTO requestDTO);
    UserResponseDTO updateUser(UUID id, UserUpdateRequestDTO requestDTO, MultipartFile avatarFile);
    void deleteUser(UUID id, Principal principal);
    List<String> getAllRoles();

    UserResponseDTO getCustomerProfile(String email);
    UserResponseDTO updateCustomerProfile(String email, CustomerProfileUpdateRequestDTO requestDTO, MultipartFile avatarFile);
}

package com.example.store.service.user.impl;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import com.example.store.dto.user.ProfileUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.common.PageResponse;
import com.example.store.dto.user.UserResponseDTO;
import com.example.store.dto.user.UserCreateRequestDTO;
import com.example.store.dto.user.UserUpdateRequestDTO;
import com.example.store.entity.Role;
import com.example.store.entity.User;
import com.example.store.enums.BusinessMessage;
import com.example.store.exception.BusinessException;
import com.example.store.exception.DuplicateResourceException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.UserMapper;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;
import com.example.store.service.user.IUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final com.example.store.common.service.ICloudinaryService cloudinaryService;

    private static final String CLOUDINARY_FOLDER = "electronics-store/users/avatars";

    private void validateImageFile(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException(com.example.store.enums.ValidationMessage.IMAGE_INVALID.getMessage());
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(com.example.store.enums.ApiMessage.FILE_TOO_LARGE.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponseDTO> getAllUsers(String search, String role, Boolean isActive, Pageable pageable) {
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String trimmedRole = (role != null && !role.isBlank()) ? role.trim() : null;

        Page<User> page = userRepository.searchUsers(trimmedSearch, trimmedRole, isActive, pageable);
        List<UserResponseDTO> dtoList = page.getContent().stream()
                .map(userMapper::toUserResponseDTO)
                .toList();

        return PageResponse.of(page, dtoList);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.USER_NOT_FOUND_ID, id));
        return userMapper.toUserResponseDTO(user);
    }

    @Override
    @Transactional
    public UserResponseDTO createUser(UserCreateRequestDTO requestDTO) {
        return createUser(requestDTO, null);
    }

    @Override
    @Transactional
    public UserResponseDTO createUser(UserCreateRequestDTO requestDTO, org.springframework.web.multipart.MultipartFile avatarFile) {
        String normalizedEmail = requestDTO.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException(BusinessMessage.USER_EMAIL_EXISTS, normalizedEmail);
        }

        String phone = (requestDTO.getPhone() != null && !requestDTO.getPhone().isBlank())
                ? requestDTO.getPhone().trim()
                : null;

        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException(BusinessMessage.USER_PHONE_EXISTS, phone);
        }

        Role role = roleRepository.findByName(requestDTO.getRole().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.USER_ROLE_NOT_FOUND, requestDTO.getRole()));

        validateImageFile(avatarFile);
        String avatarUrl = null;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            avatarUrl = cloudinaryService.uploadImage(avatarFile, CLOUDINARY_FOLDER);
        } else if (requestDTO.getAvatar() != null && !requestDTO.getAvatar().isBlank()) {
            avatarUrl = requestDTO.getAvatar().trim();
        }

        User user = User.builder()
                .fullName(requestDTO.getFullName().trim())
                .email(normalizedEmail)
                .phone(phone)
                .avatar(avatarUrl)
                .hashedPassword(passwordEncoder.encode(requestDTO.getPassword()))
                .role(role)
                .isEmailActive(Boolean.TRUE.equals(requestDTO.getIsActive()))
                .isPhoneActive(Boolean.TRUE.equals(requestDTO.getIsActive()))
                .build();

        User saved = userRepository.save(user);
        log.info("Tạo mới người dùng thành công: id={}, email={}", saved.getId(), saved.getEmail());
        return userMapper.toUserResponseDTO(saved);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(UUID id, UserUpdateRequestDTO requestDTO) {
        return updateUser(id, requestDTO, null);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(UUID id, UserUpdateRequestDTO requestDTO, org.springframework.web.multipart.MultipartFile avatarFile) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.USER_NOT_FOUND_ID, id));

        String normalizedEmail = requestDTO.getEmail().toLowerCase().trim();
        if (!normalizedEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new DuplicateResourceException(BusinessMessage.USER_EMAIL_EXISTS, normalizedEmail);
        }

        String phone = (requestDTO.getPhone() != null && !requestDTO.getPhone().isBlank())
                ? requestDTO.getPhone().trim()
                : null;

        if (phone != null && userRepository.existsByPhoneAndIdNot(phone, id)) {
            throw new DuplicateResourceException(BusinessMessage.USER_PHONE_EXISTS, phone);
        }

        Role role = roleRepository.findByName(requestDTO.getRole().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.USER_ROLE_NOT_FOUND, requestDTO.getRole()));

        validateImageFile(avatarFile);
        if (avatarFile != null && !avatarFile.isEmpty()) {
            if (user.getAvatar() != null && user.getAvatar().contains("cloudinary.com")) {
                cloudinaryService.deleteImageByUrl(user.getAvatar());
            }
            user.setAvatar(cloudinaryService.uploadImage(avatarFile, CLOUDINARY_FOLDER));
        } else if (requestDTO.getAvatar() != null) {
            user.setAvatar(requestDTO.getAvatar().trim().isEmpty() ? null : requestDTO.getAvatar().trim());
        }

        user.setFullName(requestDTO.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(phone);
        user.setRole(role);
        user.setIsEmailActive(Boolean.TRUE.equals(requestDTO.getIsActive()));
        user.setIsPhoneActive(Boolean.TRUE.equals(requestDTO.getIsActive()));

        if (requestDTO.getNewPassword() != null && !requestDTO.getNewPassword().isBlank()) {
            user.setHashedPassword(passwordEncoder.encode(requestDTO.getNewPassword().trim()));
        }

        User updated = userRepository.save(user);
        log.info("Cập nhật người dùng thành công: id={}, email={}", updated.getId(), updated.getEmail());
        return userMapper.toUserResponseDTO(updated);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id, Principal principal) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.USER_NOT_FOUND_ID, id));

        if (principal != null && user.getEmail().equalsIgnoreCase(principal.getName().trim())) {
            throw new BusinessException(BusinessMessage.USER_CANNOT_DELETE_SELF);
        }

        if (userRepository.existsOrderReference(id)) {
            throw new BusinessException(BusinessMessage.USER_HAS_ORDERS_DELETE);
        }

        userRepository.delete(user);
        log.info("Xóa vĩnh viễn người dùng thành công: id={}, email={}", id, user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(Role::getName)
                .sorted()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getProfile(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.USER_NOT_FOUND_EMAIL, email));
        return userMapper.toUserResponseDTO(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateProfile(String email, ProfileUpdateRequestDTO requestDTO, org.springframework.web.multipart.MultipartFile avatarFile) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.USER_NOT_FOUND_EMAIL, email));

        String phone = (requestDTO.getPhone() != null && !requestDTO.getPhone().isBlank())
                ? requestDTO.getPhone().trim()
                : null;

        if (phone != null && userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
            throw new DuplicateResourceException(BusinessMessage.USER_PHONE_EXISTS, phone);
        }

        validateImageFile(avatarFile);
        if (avatarFile != null && !avatarFile.isEmpty()) {
            if (user.getAvatar() != null && user.getAvatar().contains("cloudinary.com")) {
                cloudinaryService.deleteImageByUrl(user.getAvatar());
            }
            user.setAvatar(cloudinaryService.uploadImage(avatarFile, CLOUDINARY_FOLDER));
        } else if (requestDTO.getAvatar() != null && !requestDTO.getAvatar().isBlank()) {
            user.setAvatar(requestDTO.getAvatar().trim());
        }

        user.setFullName(requestDTO.getFullName().trim());
        user.setPhone(phone);
        if (requestDTO.getAddresses() != null) {
            user.setAddresses(requestDTO.getAddresses().trim());
        }

        User updated = userRepository.save(user);
        log.info("Cập nhật thông tin profile thành công: id={}, email={}", updated.getId(), updated.getEmail());
        return userMapper.toUserResponseDTO(updated);
    }
}

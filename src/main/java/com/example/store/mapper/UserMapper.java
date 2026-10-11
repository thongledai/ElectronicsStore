package com.example.store.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.example.store.dto.auth.RegisterDTO;
import com.example.store.dto.common.UserResponseDTO;
import com.example.store.entity.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    // MapStruct tự động map trực tiếp: fullName, email, phone (không cần viết gì thêm)
    User toUser(RegisterDTO dto);

    // Chỉ viết @Mapping khi có logic đặc biệt (khác tên hoặc tự tính toán biểu thức)
    @Mapping(target = "role", source = "role.name")
    @Mapping(target = "hasPassword", expression = "java(user.getHashedPassword() != null && !user.getHashedPassword().isBlank())")
    UserResponseDTO toUserResponseDTO(User user);
}
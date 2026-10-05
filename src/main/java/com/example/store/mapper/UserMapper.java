package com.example.store.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.example.store.dto.RegisterDTO;
import com.example.store.dto.UserResponseDTO;
import com.example.store.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "idCard", ignore = true)
    @Mapping(target = "isEmailActive", constant = "false")
    @Mapping(target = "isPhoneActive", constant = "false")
    @Mapping(target = "hashedPassword", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "point", constant = "0")
    @Mapping(target = "eWallet", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "phone", expression = "java(dto.getResolvedPhone())")
    User toUser(RegisterDTO dto);

    @Mapping(target = "role", source = "role.name")
    UserResponseDTO toUserResponseDTO(User user);
}

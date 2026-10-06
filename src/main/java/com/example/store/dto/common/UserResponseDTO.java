package com.example.store.dto.common;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String avatar;
    private Boolean isEmailActive;
}

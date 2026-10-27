package com.example.store.dto.user;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String userCode;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String avatar;
    private Boolean isEmailActive;
    private Boolean hasPassword;
    private Integer point;

    @JsonProperty("eWallet")
    private java.math.BigDecimal eWallet;

    private String addresses;
    private java.time.LocalDateTime createdAt;
}

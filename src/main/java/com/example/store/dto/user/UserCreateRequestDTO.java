package com.example.store.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequestDTO {

    @NotBlank(message = "FULL_NAME_REQUIRED")
    @Size(max = 200, message = "FULL_NAME_MAX_LENGTH")
    private String fullName;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    private String email;

    @NotBlank(message = "PHONE_REQUIRED")
    @Pattern(regexp = "^(0|\\+84)[35789][0-9]{8}$", message = "PHONE_INVALID")
    private String phone;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 6, message = "PASSWORD_MIN_LENGTH")
    private String password;

    @NotBlank(message = "ROLE_REQUIRED")
    private String role;

    @Builder.Default
    private Boolean isActive = true;

    private String avatar;
}

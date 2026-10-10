package com.example.store.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordDTO {
    @NotBlank(message = "CURRENT_PASSWORD_REQUIRED")
    private String oldPassword;

    @NotBlank(message = "NEW_PASSWORD_REQUIRED")
    @Size(min = 6, message = "PASSWORD_MIN_LENGTH")
    private String newPassword;

    @NotBlank(message = "CONFIRM_NEW_PASSWORD_REQUIRED")
    private String confirmNewPassword;
}

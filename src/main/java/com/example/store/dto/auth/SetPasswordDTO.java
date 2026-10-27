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
public class SetPasswordDTO {

    @NotBlank(message = "NEW_PASSWORD_REQUIRED")
    @Size(min = 6, message = "PASSWORD_MIN_LENGTH")
    private String newPassword;

    @NotBlank(message = "SET_PASSWORD_CONFIRM_REQUIRED")
    private String confirmNewPassword;
}

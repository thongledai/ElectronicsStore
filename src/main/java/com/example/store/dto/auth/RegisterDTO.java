package com.example.store.dto.auth;

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
public class RegisterDTO {
    @NotBlank(message = "FULL_NAME_REQUIRED")
    private String fullName;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    private String email;

    @NotBlank(message = "PHONE_REQUIRED")
    @Pattern(regexp = "^(0|\\+84)[35789][0-9]{8}$", message = "PHONE_INVALID")
    private String phone;

    private String mobileNumber;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 6, message = "PASSWORD_MIN_LENGTH")
    private String password;

    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
    private String confirmPassword;

    public String getResolvedPhone() {
        if (phone != null && !phone.isBlank()) {
            return phone;
        }
        return mobileNumber;
    }
}

package com.example.store.dto.user;

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
public class UserUpdateRequestDTO {

    @NotBlank(message = "FULL_NAME_REQUIRED")
    @Size(max = 200, message = "FULL_NAME_MAX_LENGTH")
    private String fullName;

    @NotBlank(message = "EMAIL_REQUIRED")
    @jakarta.validation.constraints.Email(message = "EMAIL_INVALID")
    private String email;

    @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$|^$", message = "PHONE_INVALID")
    private String phone;

    @NotBlank(message = "ROLE_REQUIRED")
    private String role;

    @Builder.Default
    private Boolean isActive = true;

    // Optional: Only updated if provided and non-empty
    @Pattern(regexp = "^$|.{6,}", message = "PASSWORD_MIN_LENGTH")
    private String newPassword;

    private String avatar;
}

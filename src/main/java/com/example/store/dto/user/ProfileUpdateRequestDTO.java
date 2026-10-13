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
public class ProfileUpdateRequestDTO {

    @NotBlank(message = "FULL_NAME_REQUIRED")
    @Size(max = 200, message = "FULL_NAME_MAX_LENGTH")
    private String fullName;

    @Pattern(regexp = "^(0|\\+84)[35789][0-9]{8}$|^$", message = "PHONE_INVALID")
    private String phone;

    @Size(max = 1200, message = "ADDRESS_MAX_LENGTH")
    private String addresses;

    private String avatar;
}

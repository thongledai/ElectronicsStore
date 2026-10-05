package com.example.store.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VerifyOtpDTO {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "OTP không được để trống")
    @Size(min = 6, max = 6, message = "OTP phải đúng 6 chữ số")
    private String otp;
}

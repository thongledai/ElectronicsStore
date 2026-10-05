package com.example.store.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "Username không được để trống")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)(3[2-9]|5[6|8|9]|7[0|6-9]|8[1-5]|9[0-4|6-9])[0-9]{7}$", message = "Số điện thoại không hợp lệ")
    private String mobileNumber;
    @NotBlank(message = "Xác nhận mật khẩu")
    private String confirmPassword;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
}

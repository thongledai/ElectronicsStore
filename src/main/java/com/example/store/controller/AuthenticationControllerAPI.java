package com.example.store.controller;

import java.security.Principal;

import com.example.store.dto.auth.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.common.ApiResponse;
import com.example.store.service.auth.IAuthenticationService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationControllerAPI {

    private final IAuthenticationService authenticationService;

    /**
     * Đăng ký tài khoản mới (sinh OTP và gửi email xác thực)
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody RegisterDTO registerDTO) {
        ApiResponse<?> response = authenticationService.register(registerDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Hỗ trợ alias /signup tương đương /register
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<?>> signup(@Valid @RequestBody RegisterDTO registerDTO) {
        return register(registerDTO);
    }

    /**
     * Đăng nhập hệ thống (xác thực email/mật khẩu, trả về JWT Token và role redirect)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginDTO loginDTO,
            HttpServletResponse httpResponse) {
        AuthResponseDTO authResponse = authenticationService.login(loginDTO, httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Login successful!", authResponse));
    }

    /**
     * Quên mật khẩu: gửi mã OTP về email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@Valid @RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        ApiResponse<?> response = authenticationService.forgotPassword(forgotPasswordDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Gửi lại mã OTP (khi hết hạn hoặc người dùng yêu cầu)
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<?>> resendOtp(@Valid @RequestBody ResendOtpDTO resendOtpDTO) {
        ApiResponse<?> response = authenticationService.resendOtp(resendOtpDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Xác thực OTP (kích hoạt đăng ký hoặc đặt lại mật khẩu quên)
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<?>> verifyOtp(@Valid @RequestBody VerifyOtpDTO verifyOtpDTO) {
        ApiResponse<?> response = authenticationService.verifyOtp(verifyOtpDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Đổi mật khẩu khi đã đăng nhập (cần mật khẩu cũ, mật khẩu mới, xác nhận mật khẩu mới)
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(
            @Valid @RequestBody ResetPasswordDTO resetPasswordDTO,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Please log in to change your password!"));
        }
        ApiResponse<?> response = authenticationService.resetPassword(principal.getName(), resetPasswordDTO);
        return ResponseEntity.ok(response);
    }
}

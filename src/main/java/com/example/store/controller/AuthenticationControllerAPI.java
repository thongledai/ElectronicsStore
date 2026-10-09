package com.example.store.controller;

import java.security.Principal;

import com.example.store.dto.auth.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.common.ApiResponse;
import com.example.store.enums.ApiMessage;
import com.example.store.service.auth.GoogleAuthService;
import com.example.store.service.auth.IAuthenticationService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationControllerAPI {

    private final IAuthenticationService authenticationService;
    private final GoogleAuthService googleAuthService;


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
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.AUTH_LOGIN_SUCCESS, authResponse));
    }

    /**
     * Đăng nhập / Đăng ký nhanh bằng Google ID Token
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> loginWithGoogle(
            @Valid @RequestBody GoogleLoginDTO googleLoginDTO,
            HttpServletResponse httpResponse) {
        AuthResponseDTO authResponse = authenticationService.loginWithGoogle(googleLoginDTO, httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Google login successful!", authResponse));
    }

    /**
     * Lấy Google Client ID công khai cho frontend SDK
     */
    @GetMapping("/google/client-id")
    public ResponseEntity<ApiResponse<String>> getGoogleClientId() {
        return ResponseEntity.ok(ApiResponse.success("Google Client ID", googleAuthService.getGoogleClientId()));
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
            return ResponseEntity.status(401).body(ApiResponse.error(ApiMessage.AUTH_LOGIN_REQUIRED));
        }
        ApiResponse<?> response = authenticationService.resetPassword(principal.getName(), resetPasswordDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Tạo mật khẩu ban đầu cho tài khoản Google chưa có mật khẩu
     */
    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<?>> setPassword(
            @Valid @RequestBody SetPasswordDTO setPasswordDTO,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Please log in to set your password!"));
        }
        ApiResponse<?> response = authenticationService.setPassword(principal.getName(), setPasswordDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Kiểm tra người dùng hiện tại đã có mật khẩu thủ công chưa (hay là tài khoản Google)
     */
    @GetMapping("/has-password")
    public ResponseEntity<ApiResponse<Boolean>> hasPassword(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        boolean hasPassword = authenticationService.hasPassword(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Has password status", hasPassword));
    }
}



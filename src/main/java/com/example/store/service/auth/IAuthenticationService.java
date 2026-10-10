package com.example.store.service.auth;

import com.example.store.dto.auth.AuthResponseDTO;
import com.example.store.dto.auth.ForgotPasswordDTO;
import com.example.store.dto.auth.LoginDTO;
import com.example.store.dto.auth.RegisterDTO;
import com.example.store.dto.auth.ResetPasswordDTO;
import com.example.store.dto.auth.VerifyOtpDTO;
import com.example.store.dto.common.ApiResponse;

import com.example.store.dto.auth.GoogleLoginDTO;
import jakarta.servlet.http.HttpServletResponse;

public interface IAuthenticationService {
    ApiResponse<?> register(RegisterDTO registerDTO);
    AuthResponseDTO login(LoginDTO loginDTO, HttpServletResponse response);
    AuthResponseDTO loginWithGoogle(GoogleLoginDTO googleLoginDTO, HttpServletResponse response);
    ApiResponse<?> forgotPassword(ForgotPasswordDTO forgotPasswordDTO);
    ApiResponse<?> resendOtp(com.example.store.dto.auth.ResendOtpDTO resendOtpDTO);
    ApiResponse<?> verifyOtp(VerifyOtpDTO verifyOtpDTO);
    ApiResponse<?> resetPassword(String currentUserEmail, ResetPasswordDTO resetPasswordDTO);
    ApiResponse<?> setPassword(String currentUserEmail, com.example.store.dto.auth.SetPasswordDTO setPasswordDTO);
    boolean hasPassword(String email);
    String getRedirectUrlForRole(String roleName);
}





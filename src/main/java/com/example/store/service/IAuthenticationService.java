package com.example.store.service;

import com.example.store.dto.ApiResponse;
import com.example.store.dto.AuthResponseDTO;
import com.example.store.dto.ForgotPasswordDTO;
import com.example.store.dto.LoginDTO;
import com.example.store.dto.RegisterDTO;
import com.example.store.dto.ResetPasswordDTO;
import com.example.store.dto.VerifyOtpDTO;
import jakarta.servlet.http.HttpServletResponse;

public interface IAuthenticationService {
    ApiResponse<?> register(RegisterDTO registerDTO);
    AuthResponseDTO login(LoginDTO loginDTO, HttpServletResponse response);
    ApiResponse<?> forgotPassword(ForgotPasswordDTO forgotPasswordDTO);
    ApiResponse<?> resendOtp(com.example.store.dto.ResendOtpDTO resendOtpDTO);
    ApiResponse<?> verifyOtp(VerifyOtpDTO verifyOtpDTO);
    ApiResponse<?> resetPassword(String currentUserEmail, ResetPasswordDTO resetPasswordDTO);
    String getRedirectUrlForRole(String roleName);
}

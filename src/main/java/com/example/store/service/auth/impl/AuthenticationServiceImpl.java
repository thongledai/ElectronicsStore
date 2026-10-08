package com.example.store.service.auth.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.auth.AuthResponseDTO;
import com.example.store.dto.auth.ForgotPasswordDTO;
import com.example.store.dto.auth.LoginDTO;
import com.example.store.dto.auth.RegisterDTO;
import com.example.store.dto.auth.ResetPasswordDTO;
import com.example.store.dto.auth.VerifyOtpDTO;
import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.common.UserResponseDTO;
import com.example.store.entity.Role;
import com.example.store.entity.User;
import com.example.store.enums.OtpType;
import com.example.store.mapper.UserMapper;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;
import com.example.store.service.auth.IAuthenticationService;
import com.example.store.service.auth.IJwtService;
import com.example.store.service.auth.IOtpService;
import com.example.store.service.common.IEmailService;

import java.util.Optional;
import com.example.store.exception.AccountNotActivatedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements IAuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IJwtService jwtService;
    private final IOtpService otpService;
    private final IEmailService emailService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public ApiResponse<?> register(RegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match!");
        }

        String normalizedEmail = registerDTO.getEmail().toLowerCase().trim();
        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (Boolean.TRUE.equals(existingUser.getIsEmailActive())) {
                throw new RuntimeException("This email is already in use! Please sign in.");
            } else {
                // DO NOT overwrite existing user data or credentials!
                // Signal client to show confirmation modal to verify account
                throw new AccountNotActivatedException(normalizedEmail);
            }
        }

        String phone = registerDTO.getResolvedPhone();
        if (phone != null && !phone.isBlank() && userRepository.existsByPhone(phone.trim())) {
            throw new RuntimeException("This phone number is already in use!");
        }

        User user = userMapper.toUser(registerDTO);
        user.setEmail(normalizedEmail);
        user.setHashedPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setIsEmailActive(false);
        user.setIsPhoneActive(false);

        Role customerRole = roleRepository.findByName("CUSTOMER").orElseGet(() ->
                roleRepository.save(Role.builder().name("CUSTOMER").build())
        );
        user.setRole(customerRole);

        userRepository.save(user);

        // Generate OTP and send email
        String otp = otpService.generateAndSaveOtp(user.getEmail(), OtpType.REGISTER);
        emailService.sendOtpEmail(user.getEmail(), otp, "Activate your TechNova Account", "account activation");

        return ApiResponse.success("Registration successful! Please enter the OTP code sent to your email.", user.getEmail());
    }

    @Override
    public AuthResponseDTO login(LoginDTO loginDTO, HttpServletResponse response) {
        String email = loginDTO.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password!"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getHashedPassword())) {
            throw new RuntimeException("Invalid email or password!");
        }

        if (Boolean.FALSE.equals(user.getIsEmailActive())) {
            // Do NOT auto-send OTP yet; user must confirm via popup modal
            throw new AccountNotActivatedException(user.getEmail());
        }

        long expiration = loginDTO.isRememberMe() ? (7L * 24 * 60 * 60 * 1000) : jwtService.getExpirationTime();
        String token = jwtService.generateToken(user, expiration);

        if (response != null) {
            Cookie jwtCookie = new Cookie("JWT_TOKEN", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge((int) (expiration / 1000));
            response.addCookie(jwtCookie);
        }

        String roleName = user.getRole() != null ? user.getRole().getName() : "CUSTOMER";
        String redirectUrl = getRedirectUrlForRole(roleName);
        UserResponseDTO userResponse = userMapper.toUserResponseDTO(user);

        return AuthResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiration)
                .redirectUrl(redirectUrl)
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<?> forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        String email = forgotPasswordDTO.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email address!"));

        String otp = otpService.generateAndSaveOtp(user.getEmail(), OtpType.FORGOT_PASSWORD);
        emailService.sendOtpEmail(user.getEmail(), otp, "TechNova Password Recovery OTP", "password reset");

        return ApiResponse.success("An OTP code has been sent to your email.", user.getEmail());
    }

    @Override
    @Transactional
    public ApiResponse<?> resendOtp(com.example.store.dto.auth.ResendOtpDTO resendOtpDTO) {
        String email = resendOtpDTO.getEmail().toLowerCase().trim();
        OtpType type = resendOtpDTO.getType() != null ? resendOtpDTO.getType() : OtpType.REGISTER;

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email address!"));

        String subject = type == OtpType.FORGOT_PASSWORD ? "TechNova Password Recovery OTP" : "Activate your TechNova Account";
        String purpose = type == OtpType.FORGOT_PASSWORD ? "password reset" : "account activation";

        String otp = otpService.generateAndSaveOtp(user.getEmail(), type);
        emailService.sendOtpEmail(user.getEmail(), otp, subject, purpose);

        return ApiResponse.success("A new OTP code has been sent to your email (valid for 5 minutes).", user.getEmail());
    }

    @Override
    @Transactional
    public ApiResponse<?> verifyOtp(VerifyOtpDTO verifyOtpDTO) {
        String email = verifyOtpDTO.getEmail().toLowerCase().trim();
        OtpType type = verifyOtpDTO.getType();
        if (type == null) {
            type = (verifyOtpDTO.getNewPassword() != null && !verifyOtpDTO.getNewPassword().isBlank())
                    ? OtpType.FORGOT_PASSWORD
                    : OtpType.REGISTER;
        }

        boolean isValid = otpService.verifyOtp(email, verifyOtpDTO.getOtp(), type);
        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP code!");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User account not found!"));

        if (type == OtpType.REGISTER) {
            user.setIsEmailActive(true);
            userRepository.save(user);
            return ApiResponse.success("Account activated successfully! You can now sign in.");
        } else {
            if (verifyOtpDTO.getNewPassword() == null || verifyOtpDTO.getNewPassword().isBlank()) {
                throw new RuntimeException("Please enter a new password!");
            }
            if (!verifyOtpDTO.getNewPassword().equals(verifyOtpDTO.getConfirmPassword())) {
                throw new RuntimeException("Confirm password does not match!");
            }
            if (verifyOtpDTO.getNewPassword().length() < 6) {
                throw new RuntimeException("New password must be at least 6 characters!");
            }

            user.setHashedPassword(passwordEncoder.encode(verifyOtpDTO.getNewPassword()));
            userRepository.save(user);
            return ApiResponse.success("Password reset successfully! You can now sign in with your new password.");
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> resetPassword(String currentUserEmail, ResetPasswordDTO resetPasswordDTO) {
        if (currentUserEmail == null || currentUserEmail.isBlank()) {
            throw new RuntimeException("User is not authenticated!");
        }

        User user = userRepository.findByEmail(currentUserEmail.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("User account not found!"));

        if (!passwordEncoder.matches(resetPasswordDTO.getOldPassword(), user.getHashedPassword())) {
            throw new RuntimeException("Current password is incorrect!");
        }

        if (!resetPasswordDTO.getNewPassword().equals(resetPasswordDTO.getConfirmNewPassword())) {
            throw new RuntimeException("Confirm new password does not match!");
        }

        if (passwordEncoder.matches(resetPasswordDTO.getNewPassword(), user.getHashedPassword())) {
            throw new RuntimeException("New password cannot be the same as your current password!");
        }

        user.setHashedPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.success("Password changed successfully!");
    }

    @Override
    public String getRedirectUrlForRole(String roleName) {
        if (roleName == null) return "/customer/index";
        return switch (roleName.toUpperCase()) {
            case "MANAGER", "ROLE_MANAGER" -> "/manager/index";
            case "EMPLOYEE", "ROLE_EMPLOYEE" -> "/employee/index";
            case "SHIPPER", "ROLE_SHIPPER" -> "/shipper/index";
            case "CUSTOMER", "ROLE_CUSTOMER" -> "/customer/index";
            default -> "/customer/index";
        };
    }
}

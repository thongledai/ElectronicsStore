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
import com.example.store.dto.user.UserResponseDTO;
import com.example.store.entity.Role;
import com.example.store.entity.User;
import com.example.store.enums.OtpType;
import com.example.store.enums.ApiMessage;
import com.example.store.mapper.UserMapper;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;
import com.example.store.service.auth.IAuthenticationService;
import com.example.store.service.auth.IJwtService;
import com.example.store.service.auth.IOtpService;
import com.example.store.service.common.IEmailService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import com.example.store.dto.auth.GoogleLoginDTO;
import com.example.store.dto.auth.GoogleUserInfo;
import com.example.store.service.auth.GoogleAuthService;
import com.example.store.enums.BusinessMessage;
import com.example.store.exception.AccountNotActivatedException;
import com.example.store.exception.BusinessException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.entity.RefreshToken;
import com.example.store.service.auth.IRefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
    private final IRefreshTokenService refreshTokenService;
    private final IOtpService otpService;
    private final IEmailService emailService;
    private final UserMapper userMapper;
    private final GoogleAuthService googleAuthService;


    @Override
    @Transactional
    public ApiResponse<?> register(RegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(BusinessMessage.AUTH_PASSWORDS_DO_NOT_MATCH);
        }

        String normalizedEmail = registerDTO.getEmail().toLowerCase().trim();
        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (Boolean.TRUE.equals(existingUser.getIsEmailActive())) {
                throw new BusinessException(BusinessMessage.AUTH_EMAIL_ALREADY_EXISTS);
            } else {
                throw new AccountNotActivatedException(normalizedEmail);
            }
        }

        String phone = registerDTO.getPhone();
        if (phone != null && !phone.isBlank() && userRepository.existsByPhone(phone.trim())) {
            throw new BusinessException(BusinessMessage.AUTH_PHONE_ALREADY_EXISTS);
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

        return ApiResponse.success(ApiMessage.AUTH_REGISTER_SUCCESS, user.getEmail());
    }

    @Override
    public AuthResponseDTO login(LoginDTO loginDTO, HttpServletResponse response) {
        String email = loginDTO.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(BusinessMessage.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getHashedPassword())) {
            throw new BusinessException(BusinessMessage.AUTH_INVALID_CREDENTIALS);
        }

        if (Boolean.FALSE.equals(user.getIsEmailActive())) {
            // Do NOT auto-send OTP yet; user must confirm via popup modal
            throw new AccountNotActivatedException(user.getEmail());
        }

        long expiration = jwtService.getExpirationTime(); // 45 phút Access Token
        String token = jwtService.generateToken(user, expiration);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, loginDTO.isRememberMe());

        if (response != null) {
            Cookie jwtCookie = new Cookie("JWT_TOKEN", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(45 * 60); // 45 phút (2700s)
            response.addCookie(jwtCookie);

            int refreshMaxAge = (int) (refreshTokenService.getRefreshTokenDuration(loginDTO.isRememberMe()) / 1000);
            Cookie refreshCookie = new Cookie("REFRESH_TOKEN", refreshToken.getToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(refreshMaxAge);
            response.addCookie(refreshCookie);
        }

        String roleName = user.getRole() != null ? user.getRole().getName() : "CUSTOMER";
        String redirectUrl = getRedirectUrlForRole(roleName);
        UserResponseDTO userResponse = userMapper.toUserResponseDTO(user);

        return AuthResponseDTO.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(expiration)
                .redirectUrl(redirectUrl)
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO loginWithGoogle(GoogleLoginDTO googleLoginDTO, HttpServletResponse response) {
        GoogleUserInfo googleUser = googleAuthService.verifyToken(googleLoginDTO.getIdToken(), googleLoginDTO.getAccessToken());

        String email = googleUser.getEmail().toLowerCase().trim();

        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            // Automatically activate email since it is verified by Google
            if (Boolean.FALSE.equals(user.getIsEmailActive())) {
                user.setIsEmailActive(true);
            }
            // Update profile avatar if missing and provided by Google
            if ((user.getAvatar() == null || user.getAvatar().isBlank()) && googleUser.getPicture() != null) {
                user.setAvatar(googleUser.getPicture());
            }
            user = userRepository.save(user);
        } else {
            // New user registered via Google
            Role customerRole = roleRepository.findByName("CUSTOMER").orElseGet(() ->
                    roleRepository.save(Role.builder().name("CUSTOMER").build())
            );

            user = User.builder()
                    .fullName(googleUser.getName())
                    .email(email)
                    .slug(UUID.randomUUID().toString())
                    .hashedPassword(null)
                    .isEmailActive(true)


                    .isPhoneActive(false)
                    .avatar(googleUser.getPicture())
                    .role(customerRole)
                    .point(0)
                    .eWallet(BigDecimal.ZERO)
                    .build();

            user = userRepository.save(user);
        }

        long expiration = jwtService.getExpirationTime(); // 45 phút Access Token
        String token = jwtService.generateToken(user, expiration);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, true); // 30 ngày cho Google

        if (response != null) {
            Cookie jwtCookie = new Cookie("JWT_TOKEN", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(45 * 60); // 45 phút (2700s)
            response.addCookie(jwtCookie);

            int refreshMaxAge = (int) (refreshTokenService.getRefreshTokenDuration(true) / 1000);
            Cookie refreshCookie = new Cookie("REFRESH_TOKEN", refreshToken.getToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(refreshMaxAge);
            response.addCookie(refreshCookie);
        }

        String roleName = user.getRole() != null ? user.getRole().getName() : "CUSTOMER";
        String redirectUrl = getRedirectUrlForRole(roleName);
        UserResponseDTO userResponse = userMapper.toUserResponseDTO(user);

        return AuthResponseDTO.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
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
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.AUTH_NO_ACCOUNT_FOUND_EMAIL));

        String otp = otpService.generateAndSaveOtp(user.getEmail(), OtpType.FORGOT_PASSWORD);
        emailService.sendOtpEmail(user.getEmail(), otp, "TechNova Password Recovery OTP", "password reset");

        return ApiResponse.success(ApiMessage.AUTH_OTP_SENT, user.getEmail());
    }

    @Override
    @Transactional
    public ApiResponse<?> resendOtp(com.example.store.dto.auth.ResendOtpDTO resendOtpDTO) {
        String email = resendOtpDTO.getEmail().toLowerCase().trim();
        OtpType type = resendOtpDTO.getType() != null ? resendOtpDTO.getType() : OtpType.REGISTER;

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.AUTH_NO_ACCOUNT_FOUND_EMAIL));

        String subject = type == OtpType.FORGOT_PASSWORD ? "TechNova Password Recovery OTP" : "Activate your TechNova Account";
        String purpose = type == OtpType.FORGOT_PASSWORD ? "password reset" : "account activation";

        String otp = otpService.generateAndSaveOtp(user.getEmail(), type);
        emailService.sendOtpEmail(user.getEmail(), otp, subject, purpose);

        return ApiResponse.success(ApiMessage.AUTH_OTP_RESENT, user.getEmail());
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
            throw new BusinessException(BusinessMessage.AUTH_INVALID_OR_EXPIRED_OTP);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.USER_ACCOUNT_NOT_FOUND));

        if (type == OtpType.REGISTER) {
            user.setIsEmailActive(true);
            userRepository.save(user);
            return ApiResponse.success(ApiMessage.AUTH_ACCOUNT_ACTIVATED);
        } else {
            if (verifyOtpDTO.getNewPassword() == null || verifyOtpDTO.getNewPassword().isBlank()) {
                throw new BusinessException(BusinessMessage.AUTH_NEW_PASSWORD_REQUIRED);
            }
            if (!verifyOtpDTO.getNewPassword().equals(verifyOtpDTO.getConfirmPassword())) {
                throw new BusinessException(BusinessMessage.AUTH_CONFIRM_PASSWORD_DOES_NOT_MATCH);
            }
            if (verifyOtpDTO.getNewPassword().length() < 6) {
                throw new BusinessException(BusinessMessage.AUTH_NEW_PASSWORD_MIN_LENGTH);
            }

            user.setHashedPassword(passwordEncoder.encode(verifyOtpDTO.getNewPassword()));
            userRepository.save(user);
            return ApiResponse.success(ApiMessage.AUTH_PASSWORD_RESET);
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> resetPassword(String currentUserEmail, ResetPasswordDTO resetPasswordDTO) {
        if (currentUserEmail == null || currentUserEmail.isBlank()) {
            throw new BusinessException(BusinessMessage.AUTH_USER_NOT_AUTHENTICATED);
        }

        User user = userRepository.findByEmail(currentUserEmail.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.USER_ACCOUNT_NOT_FOUND));

        if (user.getHashedPassword() == null || user.getHashedPassword().isBlank()) {
            throw new BusinessException(BusinessMessage.AUTH_ACCOUNT_NO_PASSWORD_YET);
        }

        if (!passwordEncoder.matches(resetPasswordDTO.getOldPassword(), user.getHashedPassword())) {
            throw new BusinessException(BusinessMessage.AUTH_CURRENT_PASSWORD_INCORRECT);
        }

        if (!resetPasswordDTO.getNewPassword().equals(resetPasswordDTO.getConfirmNewPassword())) {
            throw new BusinessException(BusinessMessage.AUTH_CONFIRM_NEW_PASSWORD_DOES_NOT_MATCH);
        }

        if (passwordEncoder.matches(resetPasswordDTO.getNewPassword(), user.getHashedPassword())) {
            throw new BusinessException(BusinessMessage.AUTH_NEW_PASSWORD_SAME_AS_CURRENT);
        }

        user.setHashedPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
        userRepository.save(user);


        return ApiResponse.success(ApiMessage.AUTH_PASSWORD_CHANGED);
    }

    @Override
    @Transactional
    public ApiResponse<?> setPassword(String currentUserEmail, com.example.store.dto.auth.SetPasswordDTO setPasswordDTO) {
        if (currentUserEmail == null || currentUserEmail.isBlank()) {
            throw new BusinessException(BusinessMessage.AUTH_USER_NOT_AUTHENTICATED);
        }

        User user = userRepository.findByEmail(currentUserEmail.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.USER_ACCOUNT_NOT_FOUND));

        if (user.getHashedPassword() != null && !user.getHashedPassword().isBlank()) {
            throw new BusinessException(BusinessMessage.AUTH_ACCOUNT_ALREADY_HAS_PASSWORD);
        }

        if (!setPasswordDTO.getNewPassword().equals(setPasswordDTO.getConfirmNewPassword())) {
            throw new BusinessException(BusinessMessage.AUTH_CONFIRM_NEW_PASSWORD_DOES_NOT_MATCH);
        }

        user.setHashedPassword(passwordEncoder.encode(setPasswordDTO.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.success(ApiMessage.AUTH_PASSWORD_SET_SUCCESS);
    }

    @Override
    public boolean hasPassword(String email) {
        if (email == null || email.isBlank()) return true;
        return userRepository.findByEmail(email.toLowerCase().trim())
                .map(u -> u.getHashedPassword() != null && !u.getHashedPassword().isBlank())
                .orElse(true);
    }



    @Override
    public String getRedirectUrlForRole(String roleName) {
        if (roleName == null) return "/customer/index";
        return switch (roleName.toUpperCase()) {
            case "MANAGER", "ROLE_MANAGER" -> "/manager/index";
            case "EMPLOYEE", "ROLE_EMPLOYEE" -> "/employee/orders";
            case "SHIPPER", "ROLE_SHIPPER" -> "/shipper/index";
            case "CUSTOMER", "ROLE_CUSTOMER" -> "/customer/index";
            default -> "/customer/index";
        };
    }

    @Override
    @Transactional
    public AuthResponseDTO refreshToken(String refreshTokenStr, HttpServletRequest request, HttpServletResponse response) {
        String tokenToVerify = refreshTokenStr;

        if (tokenToVerify == null || tokenToVerify.isBlank()) {
            if (request != null && request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("REFRESH_TOKEN".equals(cookie.getName())) {
                        tokenToVerify = cookie.getValue();
                        break;
                    }
                }
            }
        }

        if (tokenToVerify == null || tokenToVerify.isBlank()) {
            throw new BusinessException(BusinessMessage.AUTH_REFRESH_TOKEN_NOT_FOUND);
        }

        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(tokenToVerify);
        User user = newRefreshToken.getUser();

        long accessExpiration = jwtService.getExpirationTime();
        String newAccessToken = jwtService.generateToken(user, accessExpiration);

        if (response != null) {
            Cookie jwtCookie = new Cookie("JWT_TOKEN", newAccessToken);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(45 * 60);
            response.addCookie(jwtCookie);

            long remainingSeconds = newRefreshToken.getExpiryDate().getEpochSecond() - java.time.Instant.now().getEpochSecond();
            int refreshMaxAge = Math.max(0, (int) remainingSeconds);
            Cookie refreshCookie = new Cookie("REFRESH_TOKEN", newRefreshToken.getToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(refreshMaxAge);
            response.addCookie(refreshCookie);
        }

        String roleName = user.getRole() != null ? user.getRole().getName() : "CUSTOMER";
        String redirectUrl = getRedirectUrlForRole(roleName);
        UserResponseDTO userResponse = userMapper.toUserResponseDTO(user);

        return AuthResponseDTO.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(accessExpiration)
                .redirectUrl(redirectUrl)
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String tokenToRevoke = null;
        if (request != null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("REFRESH_TOKEN".equals(cookie.getName())) {
                    tokenToRevoke = cookie.getValue();
                    break;
                }
            }
        }

        if (tokenToRevoke != null && !tokenToRevoke.isBlank()) {
            refreshTokenService.revokeToken(tokenToRevoke);
        }

        if (response != null) {
            Cookie jwtCookie = new Cookie("JWT_TOKEN", "");
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(0);
            response.addCookie(jwtCookie);

            Cookie refreshCookie = new Cookie("REFRESH_TOKEN", "");
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(0);
            response.addCookie(refreshCookie);
        }
    }
}

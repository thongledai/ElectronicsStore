package com.example.store.filter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.store.entity.RefreshToken;
import com.example.store.entity.User;
import com.example.store.repository.RefreshTokenRepository;
import com.example.store.service.auth.IJwtService;
import com.example.store.service.auth.IRefreshTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final IJwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final IRefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (isStaticResource(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        boolean isAuthenticated = false;

        // 1. Kiểm tra Access Token hiện có
        if (token != null && jwtService.validateToken(token)) {
            String userEmail = jwtService.extractEmail(token);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    isAuthenticated = true;
                } catch (Exception e) {
                    log.error("Cannot set user authentication: {}", e.getMessage());
                }
            } else if (SecurityContextHolder.getContext().getAuthentication() != null) {
                isAuthenticated = true;
            }

            // Gia hạn Cookie JWT_TOKEN khi còn đang truy cập web
            Cookie jwtCookie = new Cookie("JWT_TOKEN", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(45 * 60); // 45 phút (2700 giây)
            response.addCookie(jwtCookie);
        }

        // 2. Tự động phục hồi phiên (Server-side Silent Refresh) nếu Access Token hết hạn nhưng có Cookie REFRESH_TOKEN
        if (!isAuthenticated && SecurityContextHolder.getContext().getAuthentication() == null) {
            String refreshTokenStr = resolveRefreshToken(request);
            if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
                try {
                    Optional<RefreshToken> rtOpt = refreshTokenRepository.findByToken(refreshTokenStr.trim());
                    if (rtOpt.isPresent()) {
                        RefreshToken rt = rtOpt.get();
                        if (!rt.isRevoked() && rt.getExpiryDate().isAfter(Instant.now())) {
                            User user = rt.getUser();
                            if (user != null && Boolean.TRUE.equals(user.getIsEmailActive())) {
                                // Sinh Access Token mới cho User
                                String newAccessToken = jwtService.generateToken(user);

                                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authToken);

                                // Ghi đè Cookie JWT_TOKEN mới về trình duyệt
                                Cookie newJwtCookie = new Cookie("JWT_TOKEN", newAccessToken);
                                newJwtCookie.setHttpOnly(true);
                                newJwtCookie.setPath("/");
                                newJwtCookie.setMaxAge(45 * 60);
                                response.addCookie(newJwtCookie);

                                // Gửi header kèm theo để frontend AJAX có thể đồng bộ vào localStorage
                                response.setHeader("X-New-Access-Token", newAccessToken);
                                response.setHeader("Access-Control-Expose-Headers", "X-New-Access-Token");

                                // Gia hạn thời hạn trượt (sliding window) cho Refresh Token nếu hợp lệ
                                boolean isRememberMe = rt.getExpiryDate().isAfter(Instant.now().plusSeconds(2 * 24 * 3600));
                                long duration = refreshTokenService.getRefreshTokenDuration(isRememberMe);
                                rt.setExpiryDate(Instant.now().plusMillis(duration));
                                refreshTokenRepository.save(rt);

                                isAuthenticated = true;
                                log.debug("Server-side silent refresh successful for user: {}", user.getEmail());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Silent refresh via REFRESH_TOKEN cookie in filter failed: {}", e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isStaticResource(String uri) {
        if (uri == null) return false;
        return uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".png") ||
               uri.endsWith(".jpg") || uri.endsWith(".jpeg") || uri.endsWith(".svg") ||
               uri.endsWith(".ico") || uri.endsWith(".woff") || uri.endsWith(".woff2") ||
               uri.endsWith(".ttf") || uri.startsWith("/webjars/");
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("REFRESH_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

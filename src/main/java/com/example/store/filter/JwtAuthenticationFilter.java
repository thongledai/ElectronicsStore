package com.example.store.filter;

import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.store.service.auth.IJwtService;

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

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

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
                } catch (Exception e) {
                    log.error("Cannot set user authentication: {}", e.getMessage());
                }
            }

            // Sliding window 120 giây (2 phút) cho Cookie JWT_TOKEN khi còn đang truy cập web
            String uri = request.getRequestURI();
            if (!isStaticResource(uri)) {
                Cookie jwtCookie = new Cookie("JWT_TOKEN", token);
                jwtCookie.setHttpOnly(true);
                jwtCookie.setPath("/");
                jwtCookie.setMaxAge(120); // 2 phút
                response.addCookie(jwtCookie);
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
}

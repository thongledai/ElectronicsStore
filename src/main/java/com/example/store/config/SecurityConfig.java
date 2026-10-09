package com.example.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.example.store.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Static resources
                .requestMatchers(
                    "/css/**", "/js/**", "/images/**", "/fonts/**", 
                    "/webjars/**", "/favicon.ico", "/error"
                ).permitAll()

                // Public HTML pages
                .requestMatchers(
                    "/", "/login", "/register", "/forgot-password", "/verify-otp",
                    "/customer/index", "/customer/products", "/customer/product-details",
                    "/pages/**"
                ).permitAll()

                // Public Authentication REST APIs
                .requestMatchers(
                    "/auth/login", "/auth/register", "/auth/signup", "/auth/forgot-password", "/auth/resend-otp", "/auth/verify-otp",
                    "/auth/google", "/auth/google/**"
                ).permitAll()


                // Reset & Set Password & Status APIs (must be authenticated)
                .requestMatchers("/auth/reset-password", "/auth/set-password", "/auth/has-password").authenticated()


                // Role-based reset password pages
                .requestMatchers("/manager/reset-password").hasAnyAuthority("MANAGER", "ROLE_MANAGER")
                .requestMatchers("/employee/reset-password").hasAnyAuthority("EMPLOYEE", "ROLE_EMPLOYEE")
                .requestMatchers("/shipper/reset-password").hasAnyAuthority("SHIPPER", "ROLE_SHIPPER")
                .requestMatchers("/customer/reset-password", "/customer/set-password").hasAnyAuthority("CUSTOMER", "ROLE_CUSTOMER")

                // Role-based dashboards
                .requestMatchers("/manager/**").hasAnyAuthority("MANAGER", "ROLE_MANAGER")
                .requestMatchers("/employee/**").hasAnyAuthority("EMPLOYEE", "ROLE_EMPLOYEE")
                .requestMatchers("/shipper/**").hasAnyAuthority("SHIPPER", "ROLE_SHIPPER")
                .requestMatchers("/customer/cart", "/customer/wishlist").hasAnyAuthority("CUSTOMER", "ROLE_CUSTOMER")

                // Any other request
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    String accept = request.getHeader("Accept");
                    if (uri.startsWith("/auth/") || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE))) {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Yêu cầu đăng nhập để truy cập tài nguyên này!\"}");
                    } else {
                        response.sendRedirect("/login?error=unauthorized");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String uri = request.getRequestURI();
                    String accept = request.getHeader("Accept");
                    if (uri.startsWith("/auth/") || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE))) {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Bạn không có quyền truy cập vào tài nguyên này!\"}");
                    } else {
                        response.sendRedirect("/login?error=forbidden");
                    }
                })
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "JWT_TOKEN")
                .logoutSuccessHandler((request, response, authentication) -> {
                    Cookie cookie = new Cookie("JWT_TOKEN", "");
                    cookie.setPath("/");
                    cookie.setHttpOnly(true);
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);

                    Cookie jsession = new Cookie("JSESSIONID", "");
                    jsession.setPath("/");
                    jsession.setMaxAge(0);
                    response.addCookie(jsession);

                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                    response.sendRedirect("/login?logout=true");
                })
                .permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

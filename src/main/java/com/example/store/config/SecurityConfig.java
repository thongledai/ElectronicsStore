package com.example.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Cho phép truy cập không cần đăng nhập vào toàn bộ tài nguyên tĩnh
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                // Cho phép truy cập xem giao diện Khách hàng, Trang tĩnh, và Đăng nhập
                .requestMatchers("/", "/customer/**", "/pages/**", "/auth/**").permitAll()
                // Cho phép xem trước các trang Dashboard (Quản lý, Nhân viên, Shipper) trong quá trình phát triển
                .requestMatchers("/manager/**", "/employee/**", "/shipper/**").permitAll()
                // Mọi request còn lại
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .defaultSuccessUrl("/customer/index", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/customer/index")
                .permitAll()
            );

        return http.build();
    }
}

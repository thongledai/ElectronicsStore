package com.example.store.service.auth.impl;

import java.util.List;

import com.example.store.security.CustomUserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.example.store.entity.User;
import com.example.store.enums.BusinessMessage;
import com.example.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException(String.format(
                        BusinessMessage.USER_NOT_FOUND_EMAIL.getMessage(), email)));

        String role = user.getRole() != null ? user.getRole().getName() : "CUSTOMER";
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(role),
                new SimpleGrantedAuthority(roleWithPrefix)
        );

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getHashedPassword(),
                Boolean.TRUE.equals(user.getIsEmailActive()),
                authorities
        );
    }
}

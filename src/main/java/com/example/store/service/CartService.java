package com.example.store.service;

//import com.example.store.dto.respone.CartRespone;
import com.example.store.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartService {
    private final UserRepository userRepository;

//    public getCurrentCart(UUID userId) {
//
//    }
}

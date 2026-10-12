package com.example.store.controller.user;

import com.example.store.dto.respone.CartItemDTO;
import com.example.store.service.cart.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {
    private final ICartService cartService;

    @GetMapping
    public List<CartItemDTO> getCart(@CookieValue(name = "JWT_TOKEN", required = true) String token) {
        return cartService.getCart(currentUser.getUser(token));
    }
}

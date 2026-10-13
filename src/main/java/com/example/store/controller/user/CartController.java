package com.example.store.controller.user;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.request.AddToCartRequest;
import com.example.store.dto.request.RemoveCartItemsRequest;
import com.example.store.dto.request.UpdateCartItemRequest;
import com.example.store.dto.respone.CartItemDTO;
import com.example.store.security.CustomUserDetails;
import com.example.store.service.cart.ICartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {
    private final ICartService cartService;

    @GetMapping
    public ApiResponse<List<CartItemDTO>> getCart(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.success("Lấy giỏ hàng thành công", cartService.getCart(user.getId()));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartItemDTO> addToCart(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody AddToCartRequest request) {
        return ApiResponse.success("Đã thêm vào giỏ hàng",
                cartService.addToCart(user.getId(), request));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<CartItemDTO> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success("Đã cập nhật số lượng",
                cartService.updateQuantity(user.getId(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> removeItem(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID itemId) {
        cartService.removeItem(user.getId(), itemId);
        return ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng");
    }

    @DeleteMapping("/items")
    public ApiResponse<Void> removeItems(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody RemoveCartItemsRequest request) {
        cartService.removeItems(user.getId(), request.itemIds());
        return ApiResponse.success("Đã xóa các sản phẩm đã chọn");
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart(@AuthenticationPrincipal CustomUserDetails user) {
        cartService.clearCart(user.getId());
        return ApiResponse.success("Đã xóa toàn bộ giỏ hàng");
    }
}

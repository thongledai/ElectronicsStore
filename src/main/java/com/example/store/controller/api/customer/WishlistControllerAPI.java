package com.example.store.controller.api.customer;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.wishlist.WishlistItemDTO;
import com.example.store.dto.wishlist.WishlistToggleResponseDTO;
import com.example.store.security.CustomUserDetails;
import com.example.store.service.wishlist.IWishlistService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistControllerAPI {

    private final IWishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItemDTO>>> getWishlist(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Yêu cầu đăng nhập."));
        }
        List<WishlistItemDTO> items = wishlistService.getWishlist(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu thích thành công.", items));
    }

    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<List<UUID>>> getWishlistProductIds(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.success("Chưa đăng nhập.", List.of()));
        }
        List<UUID> productIds = wishlistService.getWishlistProductIds(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ID yêu thích thành công.", productIds));
    }

    @PostMapping("/toggle/{productId}")
    public ResponseEntity<ApiResponse<WishlistToggleResponseDTO>> toggleWishlist(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID productId) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Yêu cầu đăng nhập."));
        }
        WishlistToggleResponseDTO response = wishlistService.toggleWishlist(user.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID productId) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Yêu cầu đăng nhập."));
        }
        wishlistService.removeFromWishlist(user.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm khỏi danh sách yêu thích."));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearWishlist(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Yêu cầu đăng nhập."));
        }
        wishlistService.clearWishlist(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ sản phẩm khỏi danh sách yêu thích."));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getWishlistCount(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.success("Số lượng sản phẩm yêu thích.", 0L));
        }
        long count = wishlistService.getWishlistCount(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Số lượng sản phẩm yêu thích.", count));
    }
}

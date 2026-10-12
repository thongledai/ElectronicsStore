package com.example.store.repository;

import com.example.store.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByCartIdAndVariantId(UUID cartId, UUID variantId);
    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);   // dùng để kiểm tra quyền sở hữu
    List<CartItem> findByCartId(UUID cartId);
}

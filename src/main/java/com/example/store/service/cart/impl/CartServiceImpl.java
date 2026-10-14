package com.example.store.service.cart.impl;

import com.example.store.dto.cart.AddToCartRequest;
import com.example.store.dto.cart.UpdateCartItemRequest;
import com.example.store.dto.cart.CartItemDTO;
import com.example.store.entity.Cart;
import com.example.store.entity.CartItem;
import com.example.store.entity.Product;
import com.example.store.entity.ProductVariant;
import com.example.store.enums.error.CartErrorCode;
import com.example.store.enums.error.ProductErrorCode;
import com.example.store.exception.AppException;
import com.example.store.mapper.CartItemMapper;
import com.example.store.repository.CartItemRepository;
import com.example.store.repository.CartRepository;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.repository.UserRepository;
import com.example.store.service.cart.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements ICartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartItemMapper cartItemMapper;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CartItemDTO> getCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> cartItemRepository.findByCartId(cart.getId())
                .stream().map(cartItemMapper::toCartItemDTO).toList())
                .orElse(List.of());
    }

    @Override
    public CartItemDTO addToCart(UUID userId, AddToCartRequest request) {
        // Kiem tra san pham co ton tai hay khong
        ProductVariant variant = variantRepository.findById(request.productVariantId())
                .orElseThrow(() -> new AppException(ProductErrorCode.VARIANT_NOT_FOUND));

        // Kiem tra san pham co dang ban hay khong
        if (!variant.isActive() || !variant.isSelling()) {
            throw new AppException(ProductErrorCode.VARIANT_NOT_FOUND);
        }

        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository
                .findByCartIdAndVariantId(cart.getId(), variant.getId())
                .orElseGet(() -> {
                    CartItem created = new CartItem();
                    created.setCart(cart);
                    created.setVariant(variant);
                    created.setCount(0);
                    return created;
                });

        int newCount = item.getCount() + request.count();   // cộng dồn, không ghi đè

        if (variant.getQuantity() == null || newCount > variant.getQuantity()) {
            throw new AppException(ProductErrorCode.INSUFFICIENT_STOCK);
        }

        item.setCount(newCount);
        return cartItemMapper.toCartItemDTO(cartItemRepository.save(item));
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(userRepository.getReferenceById(userId));
            return cartRepository.save(cart);
        });
    }


    private CartItem findOwnedItem(UUID userId, UUID cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(CartErrorCode.CART_ITEM_NOT_FOUND));
        return cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new AppException(CartErrorCode.CART_ITEM_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getOwnedItems(UUID userId, List<UUID> itemIds) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(CartErrorCode.CART_ITEM_NOT_FOUND));

        List<CartItem> items = cartItemRepository.findByIdInAndCartId(itemIds, cart.getId());

        // Có id lạ (không tồn tại hoặc của người khác) thì từ chối cả lô
        if (items.size() != new HashSet<>(itemIds).size()) {
            throw new AppException(CartErrorCode.CART_ITEM_NOT_FOUND);
        }
        return items;
    }

    private void validateQuantity(ProductVariant variant, int quantity) {
        Product product = variant.getProduct();
        if (!product.isActive() || !product.isSelling()) {
            throw new AppException(ProductErrorCode.PRODUCT_NOT_AVAILABLE);
        }

        if (variant.getQuantity() == null || quantity > variant.getQuantity()) {
            throw new AppException(ProductErrorCode.INSUFFICIENT_STOCK);
        }
    }

    @Override
    @Transactional
    public CartItemDTO updateQuantity(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        CartItem item = findOwnedItem(userId, itemId);

        validateQuantity(item.getVariant(), request.count());
        item.setCount(request.count());     // đặt lại, không cộng dồn

        return cartItemMapper.toCartItemDTO(cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public void removeItem(UUID userId, UUID itemId) {
        cartItemRepository.delete(findOwnedItem(userId, itemId));
    }

    @Override
    @Transactional
    public void removeItems(UUID userId, List<UUID> itemIds) {
        cartItemRepository.deleteAll(getOwnedItems(userId, itemIds));
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        cartRepository.findByUserId(userId)
                .ifPresent(cart -> cartItemRepository.deleteAllByCartId(cart.getId()));
    }

    @Override
    public long countItems(UUID userId) {
        return cartItemRepository.countByUserId(userId);
    }
}

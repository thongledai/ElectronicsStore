//package com.example.store.service.cart.impl;
//
//import com.example.store.dto.request.AddToCartRequest;
//import com.example.store.dto.request.UpdateCartItemRequest;
//import com.example.store.dto.respone.CartItemDTO;
//import com.example.store.entity.Cart;
//import com.example.store.entity.CartItem;
//import com.example.store.mapper.CartItemMapper;
//import com.example.store.repository.CartItemRepository;
//import com.example.store.repository.CartRepository;
//import com.example.store.service.cart.ICartService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.UUID;
//import java.util.function.Supplier;
//
//@Service
//@RequiredArgsConstructor
//public class CartServiceImpl implements ICartService {
//    private final CartRepository cartRepository;
//    private final CartItemRepository cartItemRepository;
//    private final CartItemMapper cartItemMapper;
//
//    @Transactional(readOnly = true)
//    public List<CartItemDTO> getCart(UUID userId) {
//        return cartRepository.findByUserId(userId).map(cart -> cartItemRepository.findByCartId(cart.getId())
//                .stream().map(cartItemMapper::toCartItemDTO).toList())
//                .orElseThrow();
//    }
//
//    @Transactional
//    public CartItemDTO addToCart(UUID userId, AddToCartRequest request) {
//        Cart cart = getOrCreateCart(userId);
//
//        ProductVariant variant = variantRepository.findById(request.productVariantId())
//                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
//        validateSellable(variant);
//
//        CartItem item = cartItemRepository
//                .findByCartIdAndVariantId(cart.getId(), variant.getId())
//                .orElseGet(() -> {
//                    CartItem newItem = new CartItem();
//                    newItem.setCart(cart);
//                    newItem.setVariant(variant);
//                    newItem.setCount(0);
//                    return newItem;
//                });
//
//        int newCount = item.getCount() + request.count();
//        validateStock(variant, newCount);
//        item.setCount(newCount);
//
//        return cartItemMapper.toCartItemDTO(cartItemRepository.save(item));
//    }
//
//    @Transactional
//    public CartItemDTO updateQuantity(UUID userId, UUID cartItemId, UpdateCartItemRequest request) {
//        CartItem item = findOwnedItem(userId, cartItemId);
//
//        validateSellable(item.getVariant());
//        validateStock(item.getVariant(), request.count());
//        item.setCount(request.count());   // đặt lại số lượng, không cộng dồn
//
//        return cartItemMapper.toCartItemDTO(cartItemRepository.save(item));
//    }
//
//    @Transactional
//    public void removeItem(UUID userId, UUID cartItemId) {
//        cartItemRepository.delete(findOwnedItem(userId, cartItemId));
//    }
//
//    @Transactional
//    public void clearCart(UUID userId) {
//        cartRepository.findByUserId(userId).ifPresent(cart ->
//                cartItemRepository.deleteAll(cartItemRepository.findByCartId(cart.getId())));
//    }
//
//    // ---------- helper ----------
//
//    private Cart getOrCreateCart(UUID userId) {
//        return cartRepository.findByUserId(userId).orElseGet(() -> {
//            Cart cart = new Cart();
//            cart.setUser(userRepository.getReferenceById(userId)); // inject thêm UserRepository
//            return cartRepository.save(cart);
//        });
//    }
//
//    private CartItem findOwnedItem(UUID userId, UUID cartItemId) {
//        Cart cart = cartRepository.findByUserId(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng trống"));
//        return cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
//                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));
//    }
//
//    private void validateSellable(ProductVariant variant) {
//        Product product = variant.getProduct();
//        if (!product.isActive() || !product.isSelling()) {
//            throw new BadRequestException("Sản phẩm hiện không được bán");
//        }
//    }
//
//    private void validateStock(ProductVariant variant, int requested) {
//        if (variant.getQuantity() == null || requested > variant.getQuantity()) {
//            throw new BadRequestException("Số lượng vượt quá tồn kho");
//        }
//    }
//
//
//}

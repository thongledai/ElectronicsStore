package com.example.store.service.wishlist.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.wishlist.WishlistItemDTO;
import com.example.store.dto.wishlist.WishlistToggleResponseDTO;
import com.example.store.entity.Product;
import com.example.store.entity.ProductVariant;
import com.example.store.entity.ProductVariantImage;
import com.example.store.entity.User;
import com.example.store.entity.UserFollowProduct;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.repository.ProductRepository;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.repository.UserFollowProductRepository;
import com.example.store.repository.UserRepository;
import com.example.store.service.wishlist.IWishlistService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements IWishlistService {

    private final UserFollowProductRepository followRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDTO> getWishlist(UUID userId) {
        List<UserFollowProduct> follows = followRepository.findAllWithProductByUserId(userId);
        if (follows.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> productIds = follows.stream()
                .map(f -> f.getProduct().getId())
                .distinct()
                .toList();

        List<ProductVariant> variants = productVariantRepository.findAllWithImagesByProductIdIn(productIds);
        Map<UUID, List<ProductVariant>> variantsByProduct = variants.stream()
                .filter(v -> v.isActive() && v.isSelling())
                .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

        return follows.stream()
                .map(follow -> toWishlistItemDTO(follow, variantsByProduct.getOrDefault(follow.getProduct().getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getWishlistProductIds(UUID userId) {
        return followRepository.findProductIdsByUserId(userId);
    }

    @Override
    @Transactional
    public WishlistToggleResponseDTO toggleWishlist(UUID userId, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm."));

        var existing = followRepository.findByUserIdAndProductId(userId, productId);
        boolean wishlisted;
        String message;

        if (existing.isPresent()) {
            followRepository.delete(existing.get());
            wishlisted = false;
            message = "Đã xóa sản phẩm khỏi danh sách yêu thích.";
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

            UserFollowProduct newFollow = UserFollowProduct.builder()
                    .user(user)
                    .product(product)
                    .build();
            followRepository.save(newFollow);
            wishlisted = true;
            message = "Đã thêm sản phẩm vào danh sách yêu thích.";
        }

        long totalCount = followRepository.countByUserId(userId);
        return WishlistToggleResponseDTO.builder()
                .productId(productId)
                .wishlisted(wishlisted)
                .totalCount(totalCount)
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public void removeFromWishlist(UUID userId, UUID productId) {
        followRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional
    public void clearWishlist(UUID userId) {
        followRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getWishlistCount(UUID userId) {
        return followRepository.countByUserId(userId);
    }

    private WishlistItemDTO toWishlistItemDTO(UserFollowProduct follow, List<ProductVariant> variants) {
        Product p = follow.getProduct();

        int totalStock = variants.stream()
                .mapToInt(v -> v.getQuantity() != null ? v.getQuantity() : 0)
                .sum();

        BigDecimal minPrice = null;
        BigDecimal minPromoPrice = null;
        BigDecimal effectiveMin = null;
        String thumbnail = null;
        UUID defaultVariantId = null;

        for (ProductVariant v : variants) {
            BigDecimal price = v.getPrice();
            BigDecimal promo = (v.getPromotionalPrice() != null
                    && v.getPromotionalPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? v.getPromotionalPrice()
                    : null;
            BigDecimal effective = promo != null ? promo : price;

            if (minPrice == null || (price != null && price.compareTo(minPrice) < 0)) {
                minPrice = price;
            }
            if (promo != null && (minPromoPrice == null || promo.compareTo(minPromoPrice) < 0)) {
                minPromoPrice = promo;
            }
            if (effectiveMin == null || (effective != null && effective.compareTo(effectiveMin) < 0)) {
                effectiveMin = effective;
                if (v.getQuantity() != null && v.getQuantity() > 0) {
                    defaultVariantId = v.getId();
                }
                if (v.getImages() != null && !v.getImages().isEmpty()) {
                    thumbnail = v.getImages().stream()
                            .map(ProductVariantImage::getImageUrl)
                            .sorted()
                            .findFirst()
                            .orElse(null);
                }
            }
        }

        // Fallback default variant id if cheapest had no stock
        if (defaultVariantId == null && !variants.isEmpty()) {
            for (ProductVariant v : variants) {
                if (v.getQuantity() != null && v.getQuantity() > 0) {
                    defaultVariantId = v.getId();
                    break;
                }
            }
            if (defaultVariantId == null) {
                defaultVariantId = variants.get(0).getId();
            }
        }

        if (thumbnail == null) {
            for (ProductVariant v : variants) {
                if (v.getImages() != null && !v.getImages().isEmpty()) {
                    thumbnail = v.getImages().stream()
                            .map(ProductVariantImage::getImageUrl)
                            .sorted()
                            .findFirst()
                            .orElse(null);
                    break;
                }
            }
        }

        return WishlistItemDTO.builder()
                .productId(p.getId())
                .defaultVariantId(defaultVariantId)
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .thumbnailUrl(thumbnail)
                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .rating(p.getRating())
                .originalPrice(minPrice != null ? minPrice : BigDecimal.ZERO)
                .promotionalPrice(minPromoPrice)
                .price(effectiveMin != null ? effectiveMin : (minPrice != null ? minPrice : BigDecimal.ZERO))
                .stock(totalStock)
                .isSelling(p.isSelling())
                .isActive(p.isActive())
                .addedAt(follow.getCreatedAt())
                .build();
    }
}

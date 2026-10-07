package com.example.store.service.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.example.store.dto.common.PageResponse;
import com.example.store.dto.product.ProductDetailResponseDTO;
import com.example.store.dto.product.ProductFilterDTO;
import com.example.store.dto.product.ProductRequestDTO;
import com.example.store.dto.product.ProductResponseDTO;

public interface IProductService {

    PageResponse<ProductResponseDTO> getPublicProducts(ProductFilterDTO filterDTO);

    ProductDetailResponseDTO getPublicProductDetailBySlug(String slug);

    List<ProductResponseDTO> getRelatedProducts(UUID productId, UUID categoryId, Long brandId, int limit);

    BigDecimal getMaxEffectivePrice();

    PageResponse<ProductResponseDTO> getManagerProducts(
            String search,
            UUID categoryId,
            Long brandId,
            Boolean isActive,
            Boolean isSelling,
            Pageable pageable);

    ProductResponseDTO getProductById(UUID id);

    ProductDetailResponseDTO getProductDetailById(UUID id);

    ProductDetailResponseDTO createProduct(ProductRequestDTO requestDTO);

    ProductDetailResponseDTO updateProduct(UUID id, ProductRequestDTO requestDTO);

    void deleteProduct(UUID id);

    void restoreProduct(UUID id);
}

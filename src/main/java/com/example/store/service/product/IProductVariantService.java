package com.example.store.service.product;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.example.store.dto.common.PageResponse;
import com.example.store.dto.product.ProductVariantRequestDTO;
import com.example.store.dto.product.ProductVariantResponseDTO;

public interface IProductVariantService {

    PageResponse<ProductVariantResponseDTO> getVariantsByProductId(UUID productId, Pageable pageable);

    List<ProductVariantResponseDTO> getAllVariantsByProductId(UUID productId);

    ProductVariantResponseDTO getVariantById(UUID id);

    ProductVariantResponseDTO createVariant(UUID productId, ProductVariantRequestDTO requestDTO);

    ProductVariantResponseDTO updateVariant(UUID productId, UUID variantId, ProductVariantRequestDTO requestDTO);

    void deleteVariant(UUID productId, UUID variantId);

    void restoreVariant(UUID productId, UUID variantId);
}

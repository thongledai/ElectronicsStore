package com.example.store.controller.api.manager;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.common.annotation.AuditAction;
import com.example.store.dto.common.ApiResponse;
import com.example.store.enums.ApiMessage;
import com.example.store.dto.common.PageResponse;
import com.example.store.dto.product.ProductVariantRequestDTO;
import com.example.store.dto.product.ProductVariantResponseDTO;
import com.example.store.service.product.IProductVariantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager/products/{productId}/variants")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGER','ROLE_MANAGER')")
public class ProductVariantManagerControllerAPI {

    private final IProductVariantService productVariantService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductVariantResponseDTO>>> getVariantsByProductId(
            @PathVariable UUID productId) {
        List<ProductVariantResponseDTO> result = productVariantService.getAllVariantsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.PRODUCT_VARIANT_LIST_SUCCESS, result));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ProductVariantResponseDTO>>> getVariantsPage(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "price"));
        PageResponse<ProductVariantResponseDTO> result = productVariantService.getVariantsByProductId(productId,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.PRODUCT_VARIANT_PAGE_SUCCESS, result));
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantResponseDTO>> getVariantById(
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        ProductVariantResponseDTO result = productVariantService.getVariantById(variantId);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.PRODUCT_VARIANT_DETAIL_SUCCESS, result));
    }

    @PostMapping
    @AuditAction("Tạo mới biến thể sản phẩm")
    public ResponseEntity<ApiResponse<ProductVariantResponseDTO>> createVariant(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductVariantRequestDTO requestDTO) {
        ProductVariantResponseDTO result = productVariantService.createVariant(productId, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiMessage.PRODUCT_VARIANT_CREATE_SUCCESS, result));
    }

    @PutMapping("/{variantId}")
    @AuditAction("Cập nhật biến thể sản phẩm")
    public ResponseEntity<ApiResponse<ProductVariantResponseDTO>> updateVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody ProductVariantRequestDTO requestDTO) {
        ProductVariantResponseDTO result = productVariantService.updateVariant(productId, variantId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.PRODUCT_VARIANT_UPDATE_SUCCESS, result));
    }

    @DeleteMapping("/{variantId}")
    @AuditAction("Xóa vĩnh viễn biến thể sản phẩm")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        productVariantService.deleteVariant(productId, variantId);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.PRODUCT_VARIANT_DELETE_SUCCESS));
    }

    @PostMapping("/{variantId}/restore")
    @AuditAction("Khôi phục biến thể sản phẩm")
    public ResponseEntity<ApiResponse<Void>> restoreVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        productVariantService.restoreVariant(productId, variantId);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.PRODUCT_VARIANT_RESTORE_SUCCESS));
    }
}

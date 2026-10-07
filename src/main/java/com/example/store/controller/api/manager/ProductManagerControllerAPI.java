package com.example.store.controller.api.manager;

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
import com.example.store.dto.common.PageResponse;
import com.example.store.dto.product.ProductDetailResponseDTO;
import com.example.store.dto.product.ProductRequestDTO;
import com.example.store.dto.product.ProductResponseDTO;
import com.example.store.service.product.IProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGER','ROLE_MANAGER')")
public class ProductManagerControllerAPI {

    private final IProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDTO>>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isSelling,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<ProductResponseDTO> result = productService.getManagerProducts(
                search, categoryId, brandId, isActive, isSelling, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> getProductById(@PathVariable UUID id) {
        ProductDetailResponseDTO result = productService.getProductDetailById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin sản phẩm thành công", result));
    }

    @PostMapping
    @AuditAction("Tạo mới sản phẩm")
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> createProduct(
            @Valid @RequestBody ProductRequestDTO requestDTO) {
        ProductDetailResponseDTO result = productService.createProduct(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mới sản phẩm thành công", result));
    }

    @PutMapping("/{id}")
    @AuditAction("Cập nhật thông tin sản phẩm")
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequestDTO requestDTO) {
        ProductDetailResponseDTO result = productService.updateProduct(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", result));
    }

    @DeleteMapping("/{id}")
    @AuditAction("Xóa mềm sản phẩm")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Vô hiệu hóa sản phẩm thành công"));
    }

    @PostMapping("/{id}/restore")
    @AuditAction("Khôi phục sản phẩm")
    public ResponseEntity<ApiResponse<Void>> restoreProduct(@PathVariable UUID id) {
        productService.restoreProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục sản phẩm thành công"));
    }
}

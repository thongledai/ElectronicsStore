package com.example.store.controller.api.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.common.PageResponse;
import com.example.store.dto.product.ProductDetailResponseDTO;
import com.example.store.dto.product.ProductFilterDTO;
import com.example.store.dto.product.ProductResponseDTO;
import com.example.store.service.product.IProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductControllerAPI {

    private final IProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDTO>>> getPublicProducts(
            @Valid @ModelAttribute ProductFilterDTO filterDTO) {
        PageResponse<ProductResponseDTO> result = productService.getPublicProducts(filterDTO);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", result));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> getPublicProductDetail(
            @PathVariable String slug) {
        ProductDetailResponseDTO result = productService.getPublicProductDetailBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết sản phẩm thành công", result));
    }
}

package com.example.store.controller.api.manager;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.common.annotation.AuditAction;
import com.example.store.dto.common.ApiResponse;
import com.example.store.service.product.IProductVariantImageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager/products/{productId}/variants/{variantId}/images")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGER','ROLE_MANAGER')")
public class ProductVariantImageManagerControllerAPI {

    private final IProductVariantImageService imageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getVariantImages(
            @PathVariable UUID productId,
            @PathVariable UUID variantId
    ) {
        List<String> urls = imageService.getVariantImageUrls(variantId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ảnh thành công", urls));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditAction("Tải lên hình ảnh biến thể")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @RequestParam("files") List<MultipartFile> files
    ) {
        List<String> uploadedUrls = imageService.uploadVariantImages(productId, variantId, files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tải ảnh lên thành công", uploadedUrls));
    }

    @DeleteMapping
    @AuditAction("Xóa hình ảnh biến thể")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @RequestParam("url") String imageUrl
    ) {
        imageService.deleteVariantImage(productId, variantId, imageUrl);
        return ResponseEntity.ok(ApiResponse.success("Xóa ảnh thành công"));
    }
}

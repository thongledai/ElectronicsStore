package com.example.store.controller.api.manager;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.common.annotation.AuditAction;
import com.example.store.dto.brand.BrandOptionDTO;
import com.example.store.dto.brand.BrandRequestDTO;
import com.example.store.dto.brand.BrandResponseDTO;
import com.example.store.dto.common.ApiResponse;
import com.example.store.enums.ApiMessage;
import com.example.store.dto.common.PageResponse;
import com.example.store.service.brand.IBrandService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager/brands")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGER','ROLE_MANAGER')")
public class BrandManagerControllerAPI {

    private final IBrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BrandResponseDTO>>> getAllBrands(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<BrandResponseDTO> result = brandService.getAllBrands(search, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.BRAND_LIST_SUCCESS, result));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<BrandOptionDTO>>> getBrandOptions() {
        List<BrandOptionDTO> result = brandService.getBrandOptions();
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.BRAND_OPTIONS_SUCCESS, result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponseDTO>> getBrandById(@PathVariable Long id) {
        BrandResponseDTO result = brandService.getBrandById(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.BRAND_DETAIL_SUCCESS, result));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditAction("Tạo mới thương hiệu (kèm logo)")
    public ResponseEntity<ApiResponse<BrandResponseDTO>> createBrandMultipart(
            @Valid @ModelAttribute BrandRequestDTO requestDTO,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile) {
        BrandResponseDTO result = brandService.createBrand(requestDTO, logoFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiMessage.BRAND_CREATE_SUCCESS, result));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction("Tạo mới thương hiệu")
    public ResponseEntity<ApiResponse<BrandResponseDTO>> createBrandJson(
            @Valid @RequestBody BrandRequestDTO requestDTO) {
        BrandResponseDTO result = brandService.createBrand(requestDTO, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiMessage.BRAND_CREATE_SUCCESS, result));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditAction("Cập nhật thương hiệu (kèm logo)")
    public ResponseEntity<ApiResponse<BrandResponseDTO>> updateBrandMultipart(
            @PathVariable Long id,
            @Valid @ModelAttribute BrandRequestDTO requestDTO,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile) {
        BrandResponseDTO result = brandService.updateBrand(id, requestDTO, logoFile);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.BRAND_UPDATE_SUCCESS, result));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction("Cập nhật thương hiệu")
    public ResponseEntity<ApiResponse<BrandResponseDTO>> updateBrandJson(
            @PathVariable Long id,
            @Valid @RequestBody BrandRequestDTO requestDTO) {
        BrandResponseDTO result = brandService.updateBrand(id, requestDTO, null);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.BRAND_UPDATE_SUCCESS, result));
    }

    @DeleteMapping("/{id}")
    @AuditAction("Xóa vĩnh viễn thương hiệu")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.BRAND_DELETE_SUCCESS));
    }

    @PostMapping("/{id}/restore")
    @AuditAction("Kích hoạt lại thương hiệu")
    public ResponseEntity<ApiResponse<Void>> restoreBrand(@PathVariable Long id) {
        brandService.restoreBrand(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.BRAND_RESTORE_SUCCESS));
    }
}

package com.example.store.controller.api.manager;

import java.util.List;
import java.util.UUID;

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
import com.example.store.dto.category.CategoryOptionDTO;
import com.example.store.dto.category.CategoryRequestDTO;
import com.example.store.dto.category.CategoryResponseDTO;
import com.example.store.dto.common.ApiResponse;
import com.example.store.enums.ApiMessage;
import com.example.store.dto.common.PageResponse;
import com.example.store.service.category.ICategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGER','ROLE_MANAGER')")
public class CategoryManagerControllerAPI {

    private final ICategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponseDTO>>> getAllCategories(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<CategoryResponseDTO> result = categoryService.getAllCategories(search, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CATEGORY_LIST_SUCCESS, result));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<CategoryOptionDTO>>> getCategoryOptions() {
        List<CategoryOptionDTO> result = categoryService.getCategoryOptions();
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CATEGORY_OPTIONS_SUCCESS, result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getCategoryById(@PathVariable UUID id) {
        CategoryResponseDTO result = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CATEGORY_DETAIL_SUCCESS, result));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditAction("Tạo mới danh mục (kèm ảnh)")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> createCategoryMultipart(
            @Valid @ModelAttribute CategoryRequestDTO requestDTO,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        CategoryResponseDTO result = categoryService.createCategory(requestDTO, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiMessage.CATEGORY_CREATE_SUCCESS, result));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction("Tạo mới danh mục")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> createCategoryJson(
            @Valid @RequestBody CategoryRequestDTO requestDTO) {
        CategoryResponseDTO result = categoryService.createCategory(requestDTO, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiMessage.CATEGORY_CREATE_SUCCESS, result));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditAction("Cập nhật danh mục (kèm ảnh)")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> updateCategoryMultipart(
            @PathVariable UUID id,
            @Valid @ModelAttribute CategoryRequestDTO requestDTO,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        CategoryResponseDTO result = categoryService.updateCategory(id, requestDTO, imageFile);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CATEGORY_UPDATE_SUCCESS, result));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction("Cập nhật danh mục")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> updateCategoryJson(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequestDTO requestDTO) {
        CategoryResponseDTO result = categoryService.updateCategory(id, requestDTO, null);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CATEGORY_UPDATE_SUCCESS, result));
    }

    @DeleteMapping("/{id}")
    @AuditAction("Xóa vĩnh viễn danh mục")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CATEGORY_DELETE_SUCCESS));
    }

    @PostMapping("/{id}/restore")
    @AuditAction("Khôi phục danh mục")
    public ResponseEntity<ApiResponse<Void>> restoreCategory(@PathVariable UUID id) {
        categoryService.restoreCategory(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CATEGORY_RESTORE_SUCCESS));
    }
}

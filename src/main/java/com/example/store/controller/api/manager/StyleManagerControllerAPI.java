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
import com.example.store.dto.common.PageResponse;
import com.example.store.dto.style.StyleOptionDTO;
import com.example.store.dto.style.StyleRequestDTO;
import com.example.store.dto.style.StyleResponseDTO;
import com.example.store.service.style.IStyleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager/styles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGER','ROLE_MANAGER')")
public class StyleManagerControllerAPI {

    private final IStyleService styleService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StyleResponseDTO>>> getAllStyles(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<StyleResponseDTO> result = styleService.getAllStyles(search, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kiểu thuộc tính thành công", result));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<StyleOptionDTO>>> getStyleOptions() {
        List<StyleOptionDTO> result = styleService.getStyleOptions();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lựa chọn kiểu thuộc tính thành công", result));
    }

    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<ApiResponse<List<StyleResponseDTO>>> getStylesByCategoryId(@PathVariable UUID categoryId) {
        List<StyleResponseDTO> result = styleService.getStylesByCategoryId(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Lấy kiểu thuộc tính theo danh mục thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StyleResponseDTO>> getStyleById(@PathVariable UUID id) {
        StyleResponseDTO result = styleService.getStyleById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin kiểu thuộc tính thành công", result));
    }

    @PostMapping
    @AuditAction("Tạo mới kiểu thuộc tính")
    public ResponseEntity<ApiResponse<StyleResponseDTO>> createStyle(
            @Valid @RequestBody StyleRequestDTO requestDTO) {
        StyleResponseDTO result = styleService.createStyle(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mới kiểu thuộc tính thành công", result));
    }

    @PutMapping("/{id}")
    @AuditAction("Cập nhật kiểu thuộc tính")
    public ResponseEntity<ApiResponse<StyleResponseDTO>> updateStyle(
            @PathVariable UUID id,
            @Valid @RequestBody StyleRequestDTO requestDTO) {
        StyleResponseDTO result = styleService.updateStyle(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật kiểu thuộc tính thành công", result));
    }

    @DeleteMapping("/{id}")
    @AuditAction("Xóa vĩnh viễn kiểu thuộc tính")
    public ResponseEntity<ApiResponse<Void>> deleteStyle(@PathVariable UUID id) {
        styleService.deleteStyle(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa vĩnh viễn kiểu thuộc tính thành công"));
    }

    @PostMapping("/{id}/restore")
    @AuditAction("Khôi phục kiểu thuộc tính")
    public ResponseEntity<ApiResponse<Void>> restoreStyle(@PathVariable UUID id) {
        styleService.restoreStyle(id);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục kiểu thuộc tính thành công"));
    }
}

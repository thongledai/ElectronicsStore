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
import com.example.store.dto.style.StyleValueOptionDTO;
import com.example.store.dto.style.StyleValueRequestDTO;
import com.example.store.dto.style.StyleValueResponseDTO;
import com.example.store.service.style.IStyleValueService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager/style-values")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGER','ROLE_MANAGER')")
public class StyleValueManagerControllerAPI {

    private final IStyleValueService styleValueService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StyleValueResponseDTO>>> getAllStyleValues(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID styleId,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<StyleValueResponseDTO> result = styleValueService.getAllStyleValues(search, styleId, isDeleted,
                pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách giá trị thuộc tính thành công", result));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<StyleValueOptionDTO>>> getStyleValueOptions(
            @RequestParam(required = false) UUID styleId,
            @RequestParam(required = false) UUID categoryId) {

        List<StyleValueOptionDTO> result;

        if (categoryId != null) {
            result = styleValueService.getStyleValueOptionsByCategoryId(categoryId);
        } else {
            result = styleValueService.getStyleValueOptions(styleId);
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy danh sách lựa chọn giá trị thuộc tính thành công",
                        result
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StyleValueResponseDTO>> getStyleValueById(@PathVariable UUID id) {
        StyleValueResponseDTO result = styleValueService.getStyleValueById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin giá trị thuộc tính thành công", result));
    }

    @PostMapping
    @AuditAction("Tạo mới giá trị thuộc tính")
    public ResponseEntity<ApiResponse<StyleValueResponseDTO>> createStyleValue(
            @Valid @RequestBody StyleValueRequestDTO requestDTO) {
        StyleValueResponseDTO result = styleValueService.createStyleValue(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mới giá trị thuộc tính thành công", result));
    }

    @PutMapping("/{id}")
    @AuditAction("Cập nhật giá trị thuộc tính")
    public ResponseEntity<ApiResponse<StyleValueResponseDTO>> updateStyleValue(
            @PathVariable UUID id,
            @Valid @RequestBody StyleValueRequestDTO requestDTO) {
        StyleValueResponseDTO result = styleValueService.updateStyleValue(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giá trị thuộc tính thành công", result));
    }

    @DeleteMapping("/{id}")
    @AuditAction("Xóa mềm giá trị thuộc tính")
    public ResponseEntity<ApiResponse<Void>> deleteStyleValue(@PathVariable UUID id) {
        styleValueService.deleteStyleValue(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa mềm giá trị thuộc tính thành công"));
    }

    @PostMapping("/{id}/restore")
    @AuditAction("Khôi phục giá trị thuộc tính")
    public ResponseEntity<ApiResponse<Void>> restoreStyleValue(@PathVariable UUID id) {
        styleValueService.restoreStyleValue(id);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục giá trị thuộc tính thành công"));
    }
}

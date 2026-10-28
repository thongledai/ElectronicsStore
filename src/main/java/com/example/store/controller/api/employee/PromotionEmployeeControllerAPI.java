package com.example.store.controller.api.employee;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.common.PageResponse;
import com.example.store.dto.promotion.PromotionRequestDTO;
import com.example.store.dto.promotion.PromotionResponseDTO;
import com.example.store.enums.ApiMessage;
import com.example.store.enums.PromotionType;
import com.example.store.service.promotion.IPromotionManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/employee/promotions", "/api/manager/promotions"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('EMPLOYEE', 'ROLE_EMPLOYEE', 'MANAGER', 'ROLE_MANAGER', 'ADMIN', 'ROLE_ADMIN')")
public class PromotionEmployeeControllerAPI {

    private final IPromotionManagementService promotionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PromotionResponseDTO>>> getPromotions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PromotionType type,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), sort);

        PageResponse<PromotionResponseDTO> result = promotionService.getPromotions(search, type, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.PROMOTION_LIST_SUCCESS, result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponseDTO>> getPromotionById(@PathVariable UUID id) {
        PromotionResponseDTO result = promotionService.getPromotionById(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.PROMOTION_DETAIL_SUCCESS, result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionResponseDTO>> createPromotion(
            @Valid @RequestBody PromotionRequestDTO requestDTO) {
        PromotionResponseDTO result = promotionService.createPromotion(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiMessage.PROMOTION_CREATE_SUCCESS, result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponseDTO>> updatePromotion(
            @PathVariable UUID id,
            @Valid @RequestBody PromotionRequestDTO requestDTO) {
        PromotionResponseDTO result = promotionService.updatePromotion(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.PROMOTION_UPDATE_SUCCESS, result));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<PromotionResponseDTO>> togglePromotionStatus(@PathVariable UUID id) {
        PromotionResponseDTO result = promotionService.togglePromotionStatus(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.PROMOTION_TOGGLE_SUCCESS, result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(@PathVariable UUID id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.PROMOTION_DELETE_SUCCESS));
    }
}

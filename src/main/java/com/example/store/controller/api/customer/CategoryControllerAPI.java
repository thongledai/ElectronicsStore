package com.example.store.controller.api.customer;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.category.CategoryResponseDTO;
import com.example.store.dto.common.ApiResponse;
import com.example.store.enums.ApiMessage;
import com.example.store.service.category.ICategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryControllerAPI {

    private final ICategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponseDTO>>> getActiveCategories() {
        List<CategoryResponseDTO> result = categoryService.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.CATEGORY_LIST_SUCCESS, result));
    }
}

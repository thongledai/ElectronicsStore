package com.example.store.service.category;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.dto.category.CategoryOptionDTO;
import com.example.store.dto.category.CategoryRequestDTO;
import com.example.store.dto.category.CategoryResponseDTO;
import com.example.store.dto.common.PageResponse;

public interface ICategoryService {

    PageResponse<CategoryResponseDTO> getAllCategories(String search, Boolean isDeleted, Pageable pageable);

    List<CategoryResponseDTO> getActiveCategories();

    List<CategoryOptionDTO> getCategoryOptions();

    CategoryResponseDTO getCategoryById(UUID id);

    CategoryResponseDTO getCategoryBySlug(String slug);

    CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO, MultipartFile imageFile);

    CategoryResponseDTO updateCategory(UUID id, CategoryRequestDTO requestDTO, MultipartFile imageFile);

    void deleteCategory(UUID id);

    void restoreCategory(UUID id);

    List<UUID> getAllSubCategoryIds(UUID parentId);
}

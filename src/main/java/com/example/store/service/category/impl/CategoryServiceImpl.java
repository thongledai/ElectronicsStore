package com.example.store.service.category.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.common.service.ICloudinaryService;
import com.example.store.common.util.SlugUtils;
import com.example.store.dto.category.CategoryOptionDTO;
import com.example.store.dto.category.CategoryRequestDTO;
import com.example.store.dto.category.CategoryResponseDTO;
import com.example.store.dto.common.PageResponse;
import com.example.store.dto.style.StyleValueOptionDTO;
import com.example.store.entity.Category;
import com.example.store.exception.BusinessException;
import com.example.store.exception.DuplicateResourceException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.CategoryMapper;
import com.example.store.repository.CategoryRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.service.category.ICategoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements ICategoryService {

    private static final String CLOUDINARY_FOLDER = "electronics-store/categories/";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final ICloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponseDTO> getAllCategories(String search, Boolean isDeleted, Pageable pageable) {
        Page<Category> page = categoryRepository.search(search, isDeleted, pageable);
        List<CategoryResponseDTO> dtoList = page.getContent().stream()
                .map(categoryMapper::toResponseDTO)
                .toList();
        return PageResponse.of(page, dtoList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getActiveCategories() {
        return categoryRepository.findByIsDeletedFalseOrderByCreatedAtDesc().stream()
                .map(categoryMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryOptionDTO> getCategoryOptions() {
        return categoryRepository.findByIsDeletedFalse().stream()
                .map(c -> CategoryOptionDTO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .slug(c.getSlug())
                        .parentId(c.getParent() != null ? c.getParent().getId() : null)
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));
        return categoryMapper.toResponseDTO(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với slug: " + slug));
        return categoryMapper.toResponseDTO(category);
    }

    @Override
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO, MultipartFile imageFile) {
        String slug = generateAndValidateSlug(requestDTO.getName(), null);

        if (categoryRepository.existsByName(requestDTO.getName())) {
            throw new DuplicateResourceException("Tên danh mục đã tồn tại: " + requestDTO.getName());
        }

        Category parent = null;
        if (requestDTO.getParentId() != null) {
            parent = categoryRepository.findById(requestDTO.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy danh mục cha với ID: " + requestDTO.getParentId()));
            if (Boolean.TRUE.equals(parent.getIsDeleted())) {
                throw new BusinessException("Không thể chọn danh mục cha đã bị xóa!");
            }
        }

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = cloudinaryService.uploadImage(imageFile, CLOUDINARY_FOLDER);
        } else if (requestDTO.getImage() != null && !requestDTO.getImage().trim().isEmpty()) {
            imageUrl = requestDTO.getImage().trim();
        }

        Category category = Category.builder()
                .name(requestDTO.getName().trim())
                .slug(slug)
                .parent(parent)
                .image(imageUrl)
                .isDeleted(requestDTO.getIsDeleted() != null ? requestDTO.getIsDeleted() : false)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Tạo mới danh mục thành công: id={}, name={}", saved.getId(), saved.getName());
        return categoryMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(UUID id, CategoryRequestDTO requestDTO, MultipartFile imageFile) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        if (categoryRepository.existsByNameAndIdNot(requestDTO.getName(), id)) {
            throw new DuplicateResourceException("Tên danh mục đã tồn tại: " + requestDTO.getName());
        }

        // Chỉ tạo lại slug khi đổi tên, tránh làm hỏng đường dẫn cũ
        String slug = (requestDTO.getName().trim().equals(category.getName())
                && category.getSlug() != null && !category.getSlug().isBlank())
                        ? category.getSlug()
                        : generateAndValidateSlug(requestDTO.getName(), id);

        Category parent = null;
        if (requestDTO.getParentId() != null) {
            if (requestDTO.getParentId().equals(id)) {
                throw new BusinessException("Danh mục không thể là cha của chính nó!");
            }
            // Chống vòng lặp cha-con
            if (isDescendant(requestDTO.getParentId(), id)) {
                throw new BusinessException("Không thể chọn danh mục con làm danh mục cha (gây vòng lặp)!");
            }
            parent = categoryRepository.findById(requestDTO.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy danh mục cha với ID: " + requestDTO.getParentId()));
            if (Boolean.TRUE.equals(parent.getIsDeleted())) {
                throw new BusinessException("Không thể chọn danh mục cha đã bị xóa!");
            }
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            // Xóa ảnh cũ trên Cloudinary nếu có
            if (category.getImage() != null) {
                cloudinaryService.deleteImageByUrl(category.getImage());
            }
            category.setImage(cloudinaryService.uploadImage(imageFile, CLOUDINARY_FOLDER));
        } else if (requestDTO.getImage() != null) {
            category.setImage(requestDTO.getImage().trim());
        }

        category.setName(requestDTO.getName().trim());
        category.setSlug(slug);
        category.setParent(parent);
        if (requestDTO.getIsDeleted() != null) {
            category.setIsDeleted(requestDTO.getIsDeleted());
        }

        Category updated = categoryRepository.save(category);
        log.info("Cập nhật danh mục thành công: id={}, name={}", updated.getId(), updated.getName());
        return categoryMapper.toResponseDTO(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        // Ràng buộc: chỉ xóa mềm khi không còn Product chưa xóa và không còn category
        // con chưa xóa
        if (productRepository.existsByCategoryIdAndIsActiveTrue(id)) {
            throw new BusinessException("Không thể xóa danh mục vì vẫn còn sản phẩm đang hoạt động!");
        }

        if (categoryRepository.existsByParentIdAndIsDeletedFalse(id)) {
            throw new BusinessException("Không thể xóa danh mục vì vẫn còn danh mục con đang hoạt động!");
        }

        category.setIsDeleted(true);
        categoryRepository.save(category);
        log.info("Xóa mềm danh mục id={}", id);
    }

    @Override
    @Transactional
    public void restoreCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        // Khôi phục: chỉ khi category cha chưa bị xóa
        if (category.getParent() != null && Boolean.TRUE.equals(category.getParent().getIsDeleted())) {
            throw new BusinessException("Không thể khôi phục danh mục vì danh mục cha đang bị xóa!");
        }

        category.setIsDeleted(false);
        categoryRepository.save(category);
        log.info("Khôi phục danh mục id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getAllSubCategoryIds(UUID parentId) {
        List<UUID> result = new ArrayList<>();
        result.add(parentId);
        collectSubCategories(parentId, result);
        return result;
    }

    private void collectSubCategories(UUID currentId, List<UUID> accumulator) {
        List<Category> children = categoryRepository.findByParentIdAndIsDeletedFalse(currentId);
        for (Category child : children) {
            if (!accumulator.contains(child.getId())) {
                accumulator.add(child.getId());
                collectSubCategories(child.getId(), accumulator);
            }
        }
    }

    private boolean isDescendant(UUID targetParentId, UUID currentId) {
        Set<UUID> visited = new HashSet<>();
        UUID checkingId = targetParentId;
        while (checkingId != null) {
            if (checkingId.equals(currentId)) {
                return true;
            }
            if (!visited.add(checkingId)) {
                break;
            }
            Category cat = categoryRepository.findById(checkingId).orElse(null);
            checkingId = (cat != null && cat.getParent() != null) ? cat.getParent().getId() : null;
        }
        return false;
    }

    // Slug luôn được tự tạo từ tên (không còn nhập tay). Trùng thì thêm hậu tố -1,
    // -2...
    private String generateAndValidateSlug(String name, UUID currentId) {
        String baseSlug = SlugUtils.toSlug(name);
        if (baseSlug.isEmpty()) {
            baseSlug = "item";
        }

        String slug = baseSlug;
        int count = 1;
        while (currentId == null ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndIdNot(slug, currentId)) {
            slug = baseSlug + "-" + count++;
        }
        return slug;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StyleValueOptionDTO> getStyleValueOptionsByCategoryId(UUID categoryId) {
        return styleValueRepository.findActiveByCategoryId(categoryId).stream()
                .map(sv -> StyleValueOptionDTO.builder()
                        .id(sv.getId())
                        .styleId(sv.getStyle().getId())
                        .styleName(sv.getStyle().getName())
                        .name(sv.getName())
                        .build())
                .toList();
    }
}

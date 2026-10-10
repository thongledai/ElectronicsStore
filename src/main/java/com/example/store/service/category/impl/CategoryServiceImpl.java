package com.example.store.service.category.impl;

import java.util.ArrayList;
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
import com.example.store.enums.BusinessMessage;
import com.example.store.exception.BusinessException;
import com.example.store.exception.DuplicateResourceException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.CategoryMapper;
import com.example.store.repository.CategoryRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.repository.StyleRepository;
import com.example.store.repository.StyleValueRepository;
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
	private final StyleRepository styleRepository;
	private final StyleValueRepository styleValueRepository;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<CategoryResponseDTO> getAllCategories(String search, Boolean isActive, Pageable pageable) {
		Page<Category> page = categoryRepository.search(search, isActive, pageable);
		List<CategoryResponseDTO> dtoList = page.getContent().stream().map(categoryMapper::toResponseDTO).toList();
		return PageResponse.of(page, dtoList);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CategoryResponseDTO> getActiveCategories() {
		return categoryRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream().map(categoryMapper::toResponseDTO)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CategoryOptionDTO> getCategoryOptions() {
		return categoryRepository
				.findByIsActiveTrue().stream().map(c -> CategoryOptionDTO.builder().id(c.getId()).name(c.getName())
						.slug(c.getSlug()).parentId(c.getParent() != null ? c.getParent().getId() : null).build())
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public CategoryResponseDTO getCategoryById(UUID id) {
		Category category = categoryRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException(String.format(
				BusinessMessage.CATEGORY_NOT_FOUND_ID.getMessage(), id)));
		return categoryMapper.toResponseDTO(category);
	}

	@Override
	@Transactional(readOnly = true)
	public CategoryResponseDTO getCategoryBySlug(String slug) {
		Category category = categoryRepository.findBySlug(slug)
			.orElseThrow(() -> new ResourceNotFoundException(String.format(
				BusinessMessage.CATEGORY_NOT_FOUND_SLUG.getMessage(), slug)));
		return categoryMapper.toResponseDTO(category);
	}

	@Override
	@Transactional
	public CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO, MultipartFile imageFile) {
		String slug = generateAndValidateSlug(requestDTO.getName(), null);

		if (categoryRepository.existsByName(requestDTO.getName())) {
			    throw new DuplicateResourceException(String.format(
				    BusinessMessage.CATEGORY_NAME_EXISTS.getMessage(), requestDTO.getName()));
		}

		Category parent = null;
		if (requestDTO.getParentId() != null) {
			parent = categoryRepository.findById(requestDTO.getParentId())
				    .orElseThrow(() -> new ResourceNotFoundException(String.format(
					    BusinessMessage.CATEGORY_PARENT_NOT_FOUND.getMessage(), requestDTO.getParentId())));
			if (!Boolean.TRUE.equals(parent.getIsActive())) {
				throw new BusinessException(BusinessMessage.CATEGORY_PARENT_INACTIVE.getMessage());
			}
		}

		String imageUrl = null;
		if (imageFile != null && !imageFile.isEmpty()) {
			imageUrl = cloudinaryService.uploadImage(imageFile, CLOUDINARY_FOLDER);
		} else if (requestDTO.getImage() != null && !requestDTO.getImage().trim().isEmpty()) {
			imageUrl = requestDTO.getImage().trim();
		}

		Category category = Category.builder().name(requestDTO.getName().trim()).slug(slug).parent(parent)
				.image(imageUrl).isActive(requestDTO.getIsActive() != null ? requestDTO.getIsActive() : true)
				.build();

		Category saved = categoryRepository.save(category);
		log.info("Tạo mới danh mục thành công: id={}, name={}", saved.getId(), saved.getName());
		return categoryMapper.toResponseDTO(saved);
	}

	@Override
	@Transactional
	public CategoryResponseDTO updateCategory(UUID id, CategoryRequestDTO requestDTO, MultipartFile imageFile) {
		Category category = categoryRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException(String.format(
				BusinessMessage.CATEGORY_NOT_FOUND_ID.getMessage(), id)));

		if (categoryRepository.existsByNameAndIdNot(requestDTO.getName(), id)) {
			    throw new DuplicateResourceException(String.format(
				    BusinessMessage.CATEGORY_NAME_EXISTS.getMessage(), requestDTO.getName()));
		}

		// Chỉ tạo lại slug khi đổi tên, tránh làm hỏng đường dẫn cũ
		String slug = (requestDTO.getName().trim().equals(category.getName()) && category.getSlug() != null
				&& !category.getSlug().isBlank()) ? category.getSlug()
						: generateAndValidateSlug(requestDTO.getName(), id);

		Category parent = null;
		if (requestDTO.getParentId() != null) {
			if (requestDTO.getParentId().equals(id)) {
				throw new BusinessException(BusinessMessage.CATEGORY_PARENT_SELF.getMessage());
			}
			// Chống vòng lặp cha-con
			if (isDescendant(requestDTO.getParentId(), id)) {
				throw new BusinessException(BusinessMessage.CATEGORY_PARENT_DESCENDANT.getMessage());
			}
			parent = categoryRepository.findById(requestDTO.getParentId())
				    .orElseThrow(() -> new ResourceNotFoundException(String.format(
					    BusinessMessage.CATEGORY_PARENT_NOT_FOUND.getMessage(), requestDTO.getParentId())));
			if (!Boolean.TRUE.equals(parent.getIsActive())) {
				throw new BusinessException(BusinessMessage.CATEGORY_PARENT_INACTIVE.getMessage());
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
		if (requestDTO.getIsActive() != null) {
			if (!requestDTO.getIsActive() && Boolean.TRUE.equals(category.getIsActive())) {
				validateCanDeleteCategory(id);
			}
			if (requestDTO.getIsActive() && !Boolean.TRUE.equals(category.getIsActive())
					&& parent != null && !Boolean.TRUE.equals(parent.getIsActive())) {
				throw new BusinessException(BusinessMessage.CATEGORY_RESTORE_PARENT_INACTIVE.getMessage());
			}
			category.setIsActive(requestDTO.getIsActive());
		}

		Category updated = categoryRepository.save(category);
		log.info("Cập nhật danh mục thành công: id={}, name={}", updated.getId(), updated.getName());
		return categoryMapper.toResponseDTO(updated);
	}

	@Override
	@Transactional
	public void deleteCategory(UUID id) {
		Category category = categoryRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException(String.format(
				BusinessMessage.CATEGORY_NOT_FOUND_ID.getMessage(), id)));

		if (productRepository.existsByCategoryId(id)) {
			throw new BusinessException(BusinessMessage.CATEGORY_HAS_PRODUCTS_DELETE.getMessage());
		}
		if (categoryRepository.existsByParentId(id)) {
			throw new BusinessException(BusinessMessage.CATEGORY_HAS_CHILDREN_DELETE.getMessage());
		}
		if (styleRepository.existsByCategoriesId(id)) {
			throw new BusinessException(BusinessMessage.CATEGORY_HAS_STYLES_DELETE.getMessage());
		}

		categoryRepository.delete(category);
		log.info("Xóa vĩnh viễn danh mục id={}", id);
	}

	@Override
	@Transactional
	public void restoreCategory(UUID id) {
		Category category = categoryRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException(String.format(
				BusinessMessage.CATEGORY_NOT_FOUND_ID.getMessage(), id)));

		// Khôi phục: chỉ khi category cha chưa bị xóa
		if (category.getParent() != null && !Boolean.TRUE.equals(category.getParent().getIsActive())) {
			throw new BusinessException(BusinessMessage.CATEGORY_PARENT_RESTORE_INACTIVE.getMessage());
		}

		category.setIsActive(true);
		categoryRepository.save(category);
		log.info("Khôi phục danh mục id={}", id);
	}

	private void validateCanDeleteCategory(UUID id) {
		if (productRepository.existsByCategoryId(id)) {
			throw new BusinessException(BusinessMessage.CATEGORY_HAS_PRODUCTS_DEACTIVATE.getMessage());
		}
		if (categoryRepository.existsByParentIdAndIsActiveTrue(id)) {
			throw new BusinessException(BusinessMessage.CATEGORY_HAS_ACTIVE_CHILDREN.getMessage());
		}
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
		List<Category> children = categoryRepository.findByParentIdAndIsActiveTrue(currentId);
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
		return styleValueRepository
				.findActiveByCategoryId(categoryId).stream().map(sv -> StyleValueOptionDTO.builder().id(sv.getId())
						.styleId(sv.getStyle().getId()).styleName(sv.getStyle().getName()).name(sv.getName()).build())
				.toList();
	}
}

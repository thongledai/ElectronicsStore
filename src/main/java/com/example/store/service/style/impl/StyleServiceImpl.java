package com.example.store.service.style.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.common.PageResponse;
import com.example.store.dto.style.StyleOptionDTO;
import com.example.store.dto.style.StyleRequestDTO;
import com.example.store.dto.style.StyleResponseDTO;
import com.example.store.entity.Category;
import com.example.store.entity.Style;
import com.example.store.entity.StyleValue;
import com.example.store.enums.BusinessMessage;
import com.example.store.exception.BusinessException;
import com.example.store.exception.DuplicateResourceException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.StyleMapper;
import com.example.store.repository.CategoryRepository;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.repository.StyleRepository;
import com.example.store.repository.StyleValueRepository;
import com.example.store.service.style.IStyleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StyleServiceImpl implements IStyleService {

	private final StyleRepository styleRepository;
	private final StyleValueRepository styleValueRepository;
	private final CategoryRepository categoryRepository;
	private final ProductVariantRepository productVariantRepository;
	private final StyleMapper styleMapper;

	// Dùng thay cho null khi tạo mới (không có style nào bị loại trừ)
	private static final UUID NO_ID = new UUID(0L, 0L);

	@Override
	@Transactional(readOnly = true)
	public PageResponse<StyleResponseDTO> getAllStyles(String search, Boolean isActive, Pageable pageable) {
		Page<Style> page = styleRepository.search(search, isActive, pageable);
		List<StyleResponseDTO> dtoList = page.getContent().stream().map(styleMapper::toResponseDTO).toList();
		return PageResponse.of(page, dtoList);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StyleResponseDTO> getActiveStyles() {
		return styleRepository.findByIsActiveTrue().stream().map(styleMapper::toResponseDTO).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StyleOptionDTO> getStyleOptions() {
		return styleRepository.findByIsActiveTrue().stream()
				.map(s -> StyleOptionDTO.builder().id(s.getId()).name(s.getName()).build()).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StyleResponseDTO> getStylesByCategoryId(UUID categoryId) {
		return styleRepository.findAllActiveByCategoryId(categoryId).stream().map(styleMapper::toResponseDTO).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public StyleResponseDTO getStyleById(UUID id) {
		Style style = styleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.STYLE_NOT_FOUND_ID.getMessage(), id)));
		return styleMapper.toResponseDTO(style);
	}

	private void validateStyleNameUniqueness(String name, Set<UUID> categoryIds, UUID excludeId) {
		String trimmedName = name != null ? name.trim() : "";
		UUID excludeIdSafe = excludeId != null ? excludeId : NO_ID;
		if (categoryIds != null && !categoryIds.isEmpty()) {
			if (styleRepository.existsByNameAndCategoryIds(trimmedName, categoryIds, excludeIdSafe)) {
				throw new DuplicateResourceException(
						"Tên kiểu thuộc tính '" + trimmedName + "' đã tồn tại trong danh mục được chọn!");
			}
		} else {
			if (styleRepository.existsByNameAndCategoriesIsEmpty(trimmedName, excludeIdSafe)) {
				throw new DuplicateResourceException(
						"Tên kiểu thuộc tính '" + trimmedName + "' chưa gán danh mục đã tồn tại!");
			}
		}
	}

	@Override
	@Transactional
	public StyleResponseDTO createStyle(StyleRequestDTO requestDTO) {
		validateStyleNameUniqueness(requestDTO.getName(), requestDTO.getCategoryIds(), null);

		Set<Category> categories = new HashSet<>();
		if (requestDTO.getCategoryIds() != null && !requestDTO.getCategoryIds().isEmpty()) {
			categories.addAll(categoryRepository.findAllById(requestDTO.getCategoryIds()));
			validateCategories(requestDTO.getCategoryIds(), categories);
		}

		Style style = Style.builder().name(requestDTO.getName().trim()).categories(categories)
				.isActive(requestDTO.getIsActive() != null ? requestDTO.getIsActive() : true).build();

		Style saved = styleRepository.save(style);
		log.info("Tạo mới kiểu thuộc tính thành công: id={}, name={}", saved.getId(), saved.getName());
		return styleMapper.toResponseDTO(saved);
	}

	@Override
	@Transactional
	public StyleResponseDTO updateStyle(UUID id, StyleRequestDTO requestDTO) {
		Style style = styleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.STYLE_NOT_FOUND_ID.getMessage(), id)));

		validateStyleNameUniqueness(requestDTO.getName(), requestDTO.getCategoryIds(), id);

		Set<Category> categories = new HashSet<>();
		if (requestDTO.getCategoryIds() != null) {
			categories.addAll(categoryRepository.findAllById(requestDTO.getCategoryIds()));
			validateCategories(requestDTO.getCategoryIds(), categories);
		}

		style.setName(requestDTO.getName().trim());
		style.setCategories(categories);
		if (requestDTO.getIsActive() != null) {
			if (!requestDTO.getIsActive() && Boolean.TRUE.equals(style.getIsActive())) {
				ensureNoActiveValues(id);
			}
			style.setIsActive(requestDTO.getIsActive());
		}

		Style updated = styleRepository.save(style);
		log.info("Cập nhật kiểu thuộc tính thành công: id={}, name={}", updated.getId(), updated.getName());
		return styleMapper.toResponseDTO(updated);
	}

	@Override
	@Transactional
	public void deleteStyle(UUID id) {
		Style style = styleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.STYLE_NOT_FOUND_ID.getMessage(), id)));

		List<StyleValue> values = styleValueRepository.findByStyleId(id);
		for (StyleValue value : values) {
			if (productVariantRepository.existsByStyleValuesId(value.getId())) {
				throw new BusinessException(String.format(
						BusinessMessage.STYLE_VALUE_REFERENCED_DELETE.getMessage(), value.getName()));
			}
		}

		styleValueRepository.deleteAll(values);
		styleRepository.delete(style);
		log.info("Xóa vĩnh viễn kiểu thuộc tính id={}", id);
	}

	@Override
	@Transactional
	public void restoreStyle(UUID id) {
		Style style = styleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.STYLE_NOT_FOUND_ID.getMessage(), id)));

		style.setIsActive(true);
		styleRepository.save(style);

	}

	private void validateCategories(Set<UUID> requestedIds, Set<Category> found) {
		if (found.size() != requestedIds.size()) {
			throw new ResourceNotFoundException(BusinessMessage.STYLE_CATEGORIES_NOT_FOUND.getMessage());
		}
		if (found.stream().anyMatch(c -> !Boolean.TRUE.equals(c.getIsActive()))) {
			throw new BusinessException(BusinessMessage.STYLE_CATEGORY_INACTIVE.getMessage());
		}
	}

	private void ensureNoActiveValues(UUID styleId) {
		List<StyleValue> values = styleValueRepository.findByStyleId(styleId);
		for (StyleValue value : values) {
			if (productVariantRepository.existsByStyleValuesId(value.getId())) {
				throw new BusinessException(String.format(
						BusinessMessage.STYLE_VALUE_REFERENCED_DEACTIVATE.getMessage(), value.getName()));
			}
		}
		long activeValues = values.stream().filter(value -> Boolean.TRUE.equals(value.getIsActive())).count();
		if (activeValues > 0) {
			throw new BusinessException(String.format(
					BusinessMessage.STYLE_HAS_ACTIVE_VALUES.getMessage(), activeValues));
		}
	}
}

package com.example.store.service.style.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.common.PageResponse;
import com.example.store.dto.style.StyleValueOptionDTO;
import com.example.store.dto.style.StyleValueRequestDTO;
import com.example.store.dto.style.StyleValueResponseDTO;
import com.example.store.entity.Style;
import com.example.store.entity.StyleValue;
import com.example.store.enums.BusinessMessage;
import com.example.store.exception.BusinessException;
import com.example.store.exception.DuplicateResourceException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.StyleValueMapper;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.repository.StyleRepository;
import com.example.store.repository.StyleValueRepository;
import com.example.store.service.style.IStyleValueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StyleValueServiceImpl implements IStyleValueService {

	private final StyleValueRepository styleValueRepository;
	private final StyleRepository styleRepository;
	private final ProductVariantRepository productVariantRepository;
	private final StyleValueMapper styleValueMapper;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<StyleValueResponseDTO> getAllStyleValues(String search, UUID styleId, Boolean isActive,
			Pageable pageable) {
		Page<StyleValue> page = styleValueRepository.search(search, styleId, isActive, pageable);
		List<StyleValueResponseDTO> dtoList = page.getContent().stream().map(styleValueMapper::toResponseDTO).toList();
		return PageResponse.of(page, dtoList);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StyleValueResponseDTO> getActiveStyleValues() {
		return styleValueRepository.findByIsActiveTrue().stream().map(styleValueMapper::toResponseDTO).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StyleValueOptionDTO> getStyleValueOptions(UUID styleId) {
		List<StyleValue> list = (styleId != null) ? styleValueRepository.findByStyleIdAndIsActiveTrue(styleId)
				: styleValueRepository.findByIsActiveTrue();
		return list.stream().map(sv -> StyleValueOptionDTO.builder().id(sv.getId()).styleId(sv.getStyle().getId())
				.styleName(sv.getStyle().getName()).name(sv.getName()).build()).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StyleValueResponseDTO> getStyleValuesByStyleId(UUID styleId) {
		return styleValueRepository.findByStyleIdAndIsActiveTrue(styleId).stream()
				.map(styleValueMapper::toResponseDTO).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public StyleValueResponseDTO getStyleValueById(UUID id) {
		StyleValue styleValue = styleValueRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.STYLE_VALUE_NOT_FOUND_ID.getMessage(), id)));
		return styleValueMapper.toResponseDTO(styleValue);
	}

	@Override
	@Transactional
	public StyleValueResponseDTO createStyleValue(StyleValueRequestDTO requestDTO) {
		Style style = styleRepository.findById(requestDTO.getStyleId()).orElseThrow(() -> new ResourceNotFoundException(
				"Không tìm thấy kiểu thuộc tính với ID: " + requestDTO.getStyleId()));

		if (!Boolean.TRUE.equals(style.getIsActive())) {
			throw new BusinessException(BusinessMessage.STYLE_INACTIVE_CREATE_VALUE.getMessage());
		}

		if (styleValueRepository.existsByNameAndStyleId(requestDTO.getName().trim(), style.getId())) {
			throw new DuplicateResourceException(
					"Giá trị thuộc tính '" + requestDTO.getName() + "' đã tồn tại trong kiểu này!");
		}

		StyleValue styleValue = StyleValue.builder().name(requestDTO.getName().trim()).style(style)
				.isActive(requestDTO.getIsActive() != null ? requestDTO.getIsActive() : true).build();

		StyleValue saved = styleValueRepository.save(styleValue);
		log.info("Tạo mới giá trị thuộc tính thành công: id={}, name={}", saved.getId(), saved.getName());
		return styleValueMapper.toResponseDTO(saved);
	}

	@Override
	@Transactional
	public StyleValueResponseDTO updateStyleValue(UUID id, StyleValueRequestDTO requestDTO) {
		StyleValue styleValue = styleValueRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.STYLE_VALUE_NOT_FOUND_ID.getMessage(), id)));

		Style style = styleRepository.findById(requestDTO.getStyleId()).orElseThrow(() -> new ResourceNotFoundException(
				"Không tìm thấy kiểu thuộc tính với ID: " + requestDTO.getStyleId()));
		if (!Boolean.TRUE.equals(style.getIsActive())) {
			throw new BusinessException(BusinessMessage.STYLE_INACTIVE_ASSIGN_VALUE.getMessage());
		}

		if (styleValueRepository.existsByNameAndStyleIdAndIdNot(requestDTO.getName().trim(), style.getId(), id)) {
			throw new DuplicateResourceException(
					"Giá trị thuộc tính '" + requestDTO.getName() + "' đã tồn tại trong kiểu này!");
		}

		if (!styleValue.getStyle().getId().equals(style.getId())
				&& productVariantRepository.existsByStyleValuesId(id)) {
			throw new BusinessException(BusinessMessage.STYLE_VALUE_REFERENCED_UPDATE.getMessage());
		}
		styleValue.setName(requestDTO.getName().trim());
		styleValue.setStyle(style);
		if (requestDTO.getIsActive() != null) {
			if (!requestDTO.getIsActive() && Boolean.TRUE.equals(styleValue.getIsActive())
					&& productVariantRepository.existsByStyleValuesId(id)) {
				throw new BusinessException(BusinessMessage.STYLE_VALUE_IN_USE_DEACTIVATE.getMessage());
			}
			if (requestDTO.getIsActive() && !Boolean.TRUE.equals(styleValue.getIsActive())
					&& !Boolean.TRUE.equals(style.getIsActive())) {
				throw new BusinessException(BusinessMessage.STYLE_INACTIVE_RESTORE_VALUE.getMessage());
			}
			styleValue.setIsActive(requestDTO.getIsActive());
		}

		StyleValue updated = styleValueRepository.save(styleValue);
		log.info("Cập nhật giá trị thuộc tính thành công: id={}, name={}", updated.getId(), updated.getName());
		return styleValueMapper.toResponseDTO(updated);
	}

	@Override
	@Transactional
	public void deleteStyleValue(UUID id) {
		StyleValue styleValue = styleValueRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.STYLE_VALUE_NOT_FOUND_ID.getMessage(), id)));

		if (productVariantRepository.existsByStyleValuesId(id)) {
			throw new BusinessException(
					"Không thể xóa vĩnh viễn giá trị thuộc tính vì vẫn còn biến thể sản phẩm tham chiếu!");
		}

		styleValueRepository.delete(styleValue);
		log.info("Xóa vĩnh viễn giá trị thuộc tính id={}", id);
	}

	@Override
	@Transactional
	public void restoreStyleValue(UUID id) {
		StyleValue styleValue = styleValueRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.STYLE_VALUE_NOT_FOUND_ID.getMessage(), id)));

		if (!Boolean.TRUE.equals(styleValue.getStyle().getIsActive())) {
			throw new BusinessException(BusinessMessage.STYLE_INACTIVE_RESTORE_STYLE_VALUE.getMessage());
		}

		styleValue.setIsActive(true);
		styleValueRepository.save(styleValue);
		log.info("Khôi phục giá trị thuộc tính id={}", id);
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

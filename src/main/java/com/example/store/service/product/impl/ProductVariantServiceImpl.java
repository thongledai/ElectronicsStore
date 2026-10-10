package com.example.store.service.product.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.common.util.SkuUtils;
import com.example.store.dto.common.PageResponse;
import com.example.store.dto.product.ProductVariantRequestDTO;
import com.example.store.dto.product.ProductVariantResponseDTO;
import com.example.store.entity.Product;
import com.example.store.entity.ProductVariant;
import com.example.store.entity.StyleValue;
import com.example.store.enums.BusinessMessage;
import com.example.store.exception.BusinessException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.ProductVariantMapper;
import com.example.store.repository.CategoryRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.repository.ProductVariantImageRepository;
import com.example.store.repository.StyleValueRepository;
import com.example.store.service.product.IProductVariantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductVariantServiceImpl implements IProductVariantService {

	private final ProductRepository productRepository;
	private final ProductVariantRepository productVariantRepository;
	private final ProductVariantImageRepository productVariantImageRepository;
	private final StyleValueRepository styleValueRepository;
	private final CategoryRepository categoryRepository;
	private final ProductVariantMapper productVariantMapper;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProductVariantResponseDTO> getVariantsByProductId(UUID productId, Pageable pageable) {
		Page<ProductVariant> page = productVariantRepository.findByProductId(productId, pageable);
		List<ProductVariantResponseDTO> dtoList = page.getContent().stream().map(productVariantMapper::toResponseDTO)
				.toList();
		return PageResponse.of(page, dtoList);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductVariantResponseDTO> getAllVariantsByProductId(UUID productId) {
		List<ProductVariant> variants = productVariantRepository.findAllWithImagesByProductId(productId);
		List<ProductVariant> styleVariants = productVariantRepository.findAllWithStyleValuesByProductId(productId);

		Map<UUID, ProductVariant> variantMap = variants.stream()
				.collect(Collectors.toMap(ProductVariant::getId, Function.identity(), (v1, v2) -> v1));
		for (ProductVariant svVar : styleVariants) {
			ProductVariant existing = variantMap.get(svVar.getId());
			if (existing != null) {
				existing.setStyleValues(svVar.getStyleValues());
			}
		}

		return variants.stream().map(productVariantMapper::toResponseDTO).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ProductVariantResponseDTO getVariantById(UUID id) {
		ProductVariant variant = productVariantRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.VARIANT_NOT_FOUND_ID.getMessage(), id)));
		if (variant.getStyleValues() != null) {
			variant.getStyleValues().forEach(sv -> {
				if (sv.getStyle() != null) {
					sv.getStyle().getName();
				}
			});
		}
		if (variant.getImages() != null) {
			variant.getImages().size();
		}
		return productVariantMapper.toResponseDTO(variant);
	}

	@Override
	@Transactional
	public ProductVariantResponseDTO createVariant(UUID productId, ProductVariantRequestDTO requestDTO) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.PRODUCT_NOT_FOUND_ID.getMessage(), productId)));

		if (!product.isActive() || !product.isSelling() || !Boolean.TRUE.equals(product.getCategory().getIsActive())
				|| !Boolean.TRUE.equals(product.getBrand().getIsActive())) {
			throw new BusinessException(BusinessMessage.VARIANT_PRODUCT_INACTIVE_CREATE.getMessage());
		}

		validatePrices(requestDTO);

		Set<StyleValue> styleValues = validateAndGetStyleValues(product, requestDTO.getStyleValueIds());
		boolean selling = requestDTO.getIsSelling() == null || requestDTO.getIsSelling();
		if (selling && (!product.isSelling()
				|| !Boolean.TRUE.equals(product.getBrand().getIsActive())
				|| !Boolean.TRUE.equals(product.getCategory().getIsActive()))) {
			throw new BusinessException(BusinessMessage.VARIANT_DEPENDENCY_INVALID_CREATE_SELLING.getMessage());
		}

		ProductVariant variant = ProductVariant.builder().product(product).sku(generateUniqueSku(product, styleValues))
				.price(requestDTO.getPrice()).promotionalPrice(requestDTO.getPromotionalPrice())
				.quantity(requestDTO.getQuantity() != null ? requestDTO.getQuantity() : 0).sold(0)
				.isSelling(requestDTO.getIsSelling() != null ? requestDTO.getIsSelling() : true)
				.isActive(requestDTO.getIsActive() == null || requestDTO.getIsActive())
				.styleValues(styleValues).build();
		validateVariantState(product, variant, styleValues);

		ProductVariant saved = productVariantRepository.save(variant);
		log.info("Tạo mới biến thể thành công: id={}, sku={}", saved.getId(), saved.getSku());
		return productVariantMapper.toResponseDTO(saved);
	}

	@Override
	@Transactional
	public ProductVariantResponseDTO updateVariant(UUID productId, UUID variantId,
			ProductVariantRequestDTO requestDTO) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.PRODUCT_NOT_FOUND_ID.getMessage(), productId)));

		ProductVariant variant = productVariantRepository.findById(variantId)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.VARIANT_NOT_FOUND_ID.getMessage(), variantId)));

		if (!variant.getProduct().getId().equals(productId)) {
			throw new BusinessException(BusinessMessage.VARIANT_NOT_IN_PRODUCT.getMessage());
		}

		validatePrices(requestDTO);

		Set<StyleValue> styleValues = validateAndGetStyleValues(product, requestDTO.getStyleValueIds());
		boolean selling = requestDTO.getIsSelling() != null ? requestDTO.getIsSelling() : variant.isSelling();
		boolean active = requestDTO.getIsActive() != null ? requestDTO.getIsActive() : variant.isActive();
		if (!active && variant.isActive()) {
			validateVariantCanBeDeleted(variantId);
		}
		if (Boolean.TRUE.equals(requestDTO.getIsSelling()) && (!product.isSelling()
				|| !Boolean.TRUE.equals(product.getBrand().getIsActive())
				|| !Boolean.TRUE.equals(product.getCategory().getIsActive()))) {
			throw new BusinessException(BusinessMessage.VARIANT_DEPENDENCY_INVALID_UPDATE_SELLING.getMessage());
		}
		ProductVariant proposed = ProductVariant.builder().product(product).isSelling(selling).isActive(active).build();
		validateVariantState(product, proposed, styleValues);

		variant.setPrice(requestDTO.getPrice());
		variant.setPromotionalPrice(requestDTO.getPromotionalPrice());
		if (requestDTO.getQuantity() != null) {
			variant.setQuantity(requestDTO.getQuantity());
		}
		if (requestDTO.getIsSelling() != null) {
			variant.setSelling(requestDTO.getIsSelling());
		}
		variant.setActive(active);
		variant.setStyleValues(styleValues);

		ProductVariant updated = productVariantRepository.save(variant);
		log.info("Cập nhật biến thể thành công: id={}, sku={}", updated.getId(), updated.getSku());
		return productVariantMapper.toResponseDTO(updated);
	}

	@Override
	@Transactional
	public void deleteVariant(UUID productId, UUID variantId) {
		ProductVariant variant = productVariantRepository.findById(variantId)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.VARIANT_NOT_FOUND_ID.getMessage(), variantId)));
		if (!variant.getProduct().getId().equals(productId)) {
			throw new BusinessException(BusinessMessage.VARIANT_NOT_IN_PRODUCT.getMessage());
		}
		validateVariantCanBeDeleted(variantId);
		productVariantImageRepository.deleteByProductVariantId(variantId);
		productVariantRepository.deleteStyleLinks(variantId);
		productVariantRepository.delete(variant);
		log.info("Xóa vĩnh viễn biến thể id={}", variantId);
	}

	@Override
	@Transactional
	public void restoreVariant(UUID productId, UUID variantId) {
		ProductVariant variant = productVariantRepository.findById(variantId)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						BusinessMessage.VARIANT_NOT_FOUND_ID.getMessage(), variantId)));
		if (!variant.getProduct().getId().equals(productId)) {
			throw new BusinessException(BusinessMessage.VARIANT_NOT_IN_PRODUCT.getMessage());
		}
		if (!variant.getProduct().isActive()) {
			throw new BusinessException(BusinessMessage.VARIANT_PARENT_INACTIVE_RESTORE.getMessage());
		}
		variant.setActive(true);
		productVariantRepository.save(variant);
	}

	private void validateVariantCanBeDeleted(UUID variantId) {
		if (productVariantRepository.existsOrderItemReference(variantId)
				|| productVariantRepository.existsCartItemReference(variantId)) {
			throw new BusinessException(BusinessMessage.VARIANT_REFERENCED_DELETE.getMessage());
		}
	}

	private void validatePrices(ProductVariantRequestDTO dto) {
		if (dto.getPromotionalPrice() != null) {
			if (dto.getPrice() == null || dto.getPromotionalPrice().compareTo(dto.getPrice()) >= 0) {
				throw new BusinessException(BusinessMessage.VARIANT_PROMOTIONAL_PRICE_INVALID.getMessage());
			}
		}
	}

	private Set<StyleValue> validateAndGetStyleValues(Product product, Set<UUID> styleValueIds) {

		if (styleValueIds == null || styleValueIds.isEmpty()) {
			return new HashSet<>();
		}

		List<StyleValue> styleValues = styleValueRepository.findAllById(styleValueIds);

		if (styleValues.size() != styleValueIds.size()) {
			throw new ResourceNotFoundException(BusinessMessage.VARIANT_STYLE_VALUES_NOT_FOUND.getMessage());
		}

		UUID productCategoryId = product.getCategory().getId();

		Set<UUID> seenStyles = new HashSet<>();

		for (StyleValue sv : styleValues) {
			if (!Boolean.TRUE.equals(sv.getIsActive())) {
				throw new BusinessException(String.format(
						BusinessMessage.STYLE_VALUE_INACTIVE.getMessage(), sv.getName()));
			}

			if (!Boolean.TRUE.equals(sv.getStyle().getIsActive())) {
				throw new BusinessException(String.format(
						BusinessMessage.STYLE_INACTIVE.getMessage(), sv.getStyle().getName()));
			}

			boolean belongsToCategory = sv.getStyle().getCategories().stream()
					.anyMatch(category -> category.getId().equals(productCategoryId));

			if (!belongsToCategory) {
				throw new BusinessException(String.format(BusinessMessage.STYLE_VALUE_NOT_IN_CATEGORY.getMessage(),
						sv.getName(), product.getCategory().getName()));
			}

			if (!seenStyles.add(sv.getStyle().getId())) {
				throw new BusinessException(String.format(
						BusinessMessage.VARIANT_STYLE_VALUE_LIMIT.getMessage(), sv.getStyle().getName()));
			}
		}

		return new HashSet<>(styleValues);
	}

	private void validateVariantState(Product product, ProductVariant variant, Set<StyleValue> values) {
		if (variant.isActive() && !product.isActive()) {
			throw new BusinessException(BusinessMessage.VARIANT_PARENT_INACTIVE.getMessage());
		}
		if (variant.isSelling() && (!product.isSelling() || !product.isActive())) {
			throw new BusinessException(BusinessMessage.VARIANT_PARENT_NOT_SELLING.getMessage());
		}
		if (variant.isSelling()) {
			for (StyleValue value : values) {
				if (!Boolean.TRUE.equals(value.getIsActive()) || !Boolean.TRUE.equals(value.getStyle().getIsActive())) {
					throw new BusinessException(String.format(
							BusinessMessage.VARIANT_HAS_INACTIVE_STYLE_VALUE.getMessage(), value.getName()));
				}
			}
		}
	}

	// SKU luôn được tự tạo khi thêm biến thể và giữ nguyên khi cập nhật
	private String generateUniqueSku(Product product, Set<StyleValue> styleValues) {
		String baseSku = SkuUtils.generateSkuFromStyleValues(product.getName(), styleValues);
		String sku = baseSku;
		while (productVariantRepository.existsBySku(sku)) {
			sku = SkuUtils.limitLength(baseSku, 93) + "-" + SkuUtils.randomSuffix(6);
		}
		return sku;
	}
}

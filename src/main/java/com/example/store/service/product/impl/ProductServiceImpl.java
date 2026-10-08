package com.example.store.service.product.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.common.util.SlugUtils;
import com.example.store.dto.common.PageResponse;
import com.example.store.dto.product.ProductDetailResponseDTO;
import com.example.store.dto.product.ProductFilterDTO;
import com.example.store.dto.product.ProductRequestDTO;
import com.example.store.dto.product.ProductResponseDTO;
import com.example.store.dto.product.ProductVariantResponseDTO;
import com.example.store.entity.Brand;
import com.example.store.entity.Category;
import com.example.store.entity.Product;
import com.example.store.entity.ProductVariant;
import com.example.store.entity.ProductVariantImage;
import com.example.store.exception.BusinessException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.BrandMapper;
import com.example.store.mapper.CategoryMapper;
import com.example.store.mapper.ProductMapper;
import com.example.store.mapper.ProductVariantMapper;
import com.example.store.repository.BrandRepository;
import com.example.store.repository.CategoryRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.service.category.ICategoryService;
import com.example.store.service.product.IProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements IProductService {

	private final ProductRepository productRepository;
	private final ProductVariantRepository productVariantRepository;
	private final CategoryRepository categoryRepository;
	private final BrandRepository brandRepository;
	private final ICategoryService categoryService;
	private final ProductMapper productMapper;
	private final ProductVariantMapper productVariantMapper;
	private final CategoryMapper categoryMapper;
	private final BrandMapper brandMapper;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProductResponseDTO> getPublicProducts(ProductFilterDTO filterDTO) {
		// 1. Resolve Category IDs (including all sub-categories)
		Set<UUID> categoryIds = new HashSet<>();
		if (filterDTO.getCategoryId() != null) {
			categoryIds.addAll(categoryService.getAllSubCategoryIds(filterDTO.getCategoryId()));
		}
		if (filterDTO.getCategory() != null && !filterDTO.getCategory().isEmpty()) {
			for (String catParam : filterDTO.getCategory()) {
				if (catParam == null || catParam.trim().isEmpty())
					continue;
				try {
					UUID catUuid = UUID.fromString(catParam.trim());
					categoryIds.addAll(categoryService.getAllSubCategoryIds(catUuid));
				} catch (IllegalArgumentException e) {
					categoryRepository.findBySlugAndIsDeletedFalse(catParam.trim())
							.ifPresent(c -> categoryIds.addAll(categoryService.getAllSubCategoryIds(c.getId())));
				}
			}
		}

		// 2. Resolve Brand IDs
		Set<Long> brandIds = new HashSet<>();
		if (filterDTO.getBrandId() != null) {
			brandIds.add(filterDTO.getBrandId());
		}
		if (filterDTO.getBrand() != null && !filterDTO.getBrand().isEmpty()) {
			for (String brandParam : filterDTO.getBrand()) {
				if (brandParam == null || brandParam.trim().isEmpty())
					continue;
				try {
					Long bId = Long.parseLong(brandParam.trim());
					brandIds.add(bId);
				} catch (NumberFormatException e) {
					brandRepository.findBySlugAndIsActiveTrue(brandParam.trim())
							.ifPresent(b -> brandIds.add(b.getId()));
				}
			}
		}

		// 3. Resolve sorting and pagination
		int pageNum = filterDTO.getPage() != null ? Math.max(0, filterDTO.getPage()) : 0;
		int pageSize = filterDTO.getSize() != null ? Math.min(50, Math.max(1, filterDTO.getSize())) : 12;

		Sort sort = buildSort(filterDTO.getSort());
		Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

		Page<Product> productPage = productRepository.searchPublicProducts(filterDTO.getKeyword(),
				!categoryIds.isEmpty(), categoryIds.isEmpty() ? Set.of(new UUID(0L, 0L)) : categoryIds,
				!brandIds.isEmpty(), brandIds.isEmpty() ? Set.of(-1L) : brandIds, filterDTO.getMinPrice(),
				filterDTO.getMaxPrice(), filterDTO.getMinRating(), filterDTO.getInStock(), filterDTO.getOnSale(),
				pageable);

		List<Product> products = productPage.getContent();
		if (products.isEmpty()) {
			return PageResponse.of(productPage, Collections.emptyList());
		}

		// Load variant info for all fetched products
		List<UUID> productIds = products.stream().map(Product::getId).toList();
		List<ProductVariant> allVariants = loadVariants(productIds);
		Map<UUID, List<ProductVariant>> variantsByProduct = allVariants.stream()
				.filter(v -> productIds.contains(v.getProduct().getId()) && v.isActive() && v.isSelling())
				.collect(Collectors.groupingBy(v -> v.getProduct().getId()));

		List<ProductResponseDTO> dtoList = products.stream()
				.map(product -> enrichProductResponseDTO(product,
						variantsByProduct.getOrDefault(product.getId(), Collections.emptyList())))
				.collect(Collectors.toList());

		// In-memory sort fallback for complex sorts if necessary (e.g. popular,
		// discount)
		if ("popular".equalsIgnoreCase(filterDTO.getSort())) {
			dtoList.sort(Comparator.comparing(ProductResponseDTO::getTotalSold,
					Comparator.nullsLast(Comparator.reverseOrder())));
		} else if ("discount".equalsIgnoreCase(filterDTO.getSort())) {
			dtoList.sort(Comparator.comparing(p -> (p.getMinPrice() != null && p.getMinPromotionalPrice() != null)
					? p.getMinPrice().subtract(p.getMinPromotionalPrice())
					: BigDecimal.ZERO, Comparator.reverseOrder()));
		}

		// Sắp xếp theo giá trong phạm vi trang hiện tại
		if ("price-asc".equalsIgnoreCase(filterDTO.getSort())) {
			dtoList.sort(Comparator.comparing(ProductResponseDTO::getEffectivePrice,
					Comparator.nullsLast(Comparator.naturalOrder())));
		} else if ("price-desc".equalsIgnoreCase(filterDTO.getSort())) {
			dtoList.sort(Comparator.comparing(ProductResponseDTO::getEffectivePrice,
					Comparator.nullsLast(Comparator.reverseOrder())));
		}

		return PageResponse.of(productPage, dtoList);
	}

	@Override
	@Transactional(readOnly = true)
	public ProductDetailResponseDTO getPublicProductDetailBySlug(String slug) {
		Product product = productRepository.findPublicBySlug(slug).orElseThrow(
				() -> new ResourceNotFoundException("Không tìm thấy sản phẩm hoặc sản phẩm đã ngừng bán: " + slug));

		List<ProductVariant> variants = productVariantRepository.findAllWithImagesByProductId(product.getId());
		List<ProductVariant> styleVariants = productVariantRepository
				.findAllWithStyleValuesByProductId(product.getId());

		// Merge style values & images into variants
		Map<UUID, ProductVariant> variantMap = variants.stream()
				.collect(Collectors.toMap(ProductVariant::getId, Function.identity(), (v1, v2) -> v1));
		for (ProductVariant svVar : styleVariants) {
			ProductVariant existing = variantMap.get(svVar.getId());
			if (existing != null) {
				existing.setStyleValues(svVar.getStyleValues());
			}
		}

		List<ProductVariantResponseDTO> variantDTOs = variants.stream().filter(v -> v.isActive() && v.isSelling())
				.map(productVariantMapper::toResponseDTO)
				.sorted(Comparator.comparing(ProductVariantResponseDTO::getPrice)).toList();

		if (variantDTOs.isEmpty()) {
			throw new ResourceNotFoundException("Sản phẩm chưa có biến thể hoạt động!");
		}

		return ProductDetailResponseDTO.builder().id(product.getId()).name(product.getName()).slug(product.getSlug())
				.description(product.getDescription()).isActive(product.isActive()).isSelling(product.isSelling())
				.rating(product.getRating()).category(categoryMapper.toResponseDTO(product.getCategory()))
				.brand(brandMapper.toResponseDTO(product.getBrand())).variants(variantDTOs)
				.createdAt(product.getCreatedAt()).updatedAt(product.getUpdatedAt()).build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductResponseDTO> getRelatedProducts(UUID productId, UUID categoryId, Long brandId, int limit) {
		Pageable pageable = PageRequest.of(0, limit);
		List<Product> products = productRepository.findRelatedProducts(productId, categoryId, brandId, pageable);

		List<UUID> productIds = products.stream().map(Product::getId).toList();
		List<ProductVariant> allVariants = loadVariants(productIds);
		Map<UUID, List<ProductVariant>> variantsByProduct = allVariants.stream()
				.filter(v -> productIds.contains(v.getProduct().getId()) && v.isActive() && v.isSelling())
				.collect(Collectors.groupingBy(v -> v.getProduct().getId()));

		return products.stream().map(product -> enrichProductResponseDTO(product,
				variantsByProduct.getOrDefault(product.getId(), Collections.emptyList()))).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getMaxEffectivePrice() {
		BigDecimal maxPrice = productRepository.findMaxEffectivePrice();
		return maxPrice != null ? maxPrice : BigDecimal.valueOf(100000000); // 100M VND default cap
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProductResponseDTO> getManagerProducts(String search, UUID categoryId, Long brandId,
			Boolean isActive, Boolean isSelling, Pageable pageable) {
		Page<Product> page = productRepository.search(search, categoryId, brandId, isActive, isSelling, pageable);

		List<UUID> productIds = page.getContent().stream().map(Product::getId).toList();
		List<ProductVariant> allVariants = loadVariants(productIds);
		Map<UUID, List<ProductVariant>> variantsByProduct = allVariants.stream()
				.filter(v -> productIds.contains(v.getProduct().getId()))
				.collect(Collectors.groupingBy(v -> v.getProduct().getId()));

		List<ProductResponseDTO> dtoList = page.getContent().stream().map(product -> enrichProductResponseDTO(product,
				variantsByProduct.getOrDefault(product.getId(), Collections.emptyList()))).toList();

		return PageResponse.of(page, dtoList);
	}

	@Override
	@Transactional(readOnly = true)
	public ProductResponseDTO getProductById(UUID id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
		List<ProductVariant> variants = productVariantRepository.findByProductId(id);
		return enrichProductResponseDTO(product, variants);
	}

	@Override
	@Transactional(readOnly = true)
	public ProductDetailResponseDTO getProductDetailById(UUID id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

		List<ProductVariant> variants = productVariantRepository.findAllWithImagesByProductId(id);
		List<ProductVariant> styleVariants = productVariantRepository.findAllWithStyleValuesByProductId(id);

		Map<UUID, ProductVariant> variantMap = variants.stream()
				.collect(Collectors.toMap(ProductVariant::getId, Function.identity(), (v1, v2) -> v1));
		for (ProductVariant svVar : styleVariants) {
			ProductVariant existing = variantMap.get(svVar.getId());
			if (existing != null) {
				existing.setStyleValues(svVar.getStyleValues());
			}
		}

		List<ProductVariantResponseDTO> variantDTOs = variants.stream().map(productVariantMapper::toResponseDTO)
				.toList();

		return ProductDetailResponseDTO.builder().id(product.getId()).name(product.getName()).slug(product.getSlug())
				.description(product.getDescription()).isActive(product.isActive()).isSelling(product.isSelling())
				.rating(product.getRating()).category(categoryMapper.toResponseDTO(product.getCategory()))
				.brand(brandMapper.toResponseDTO(product.getBrand())).variants(variantDTOs)
				.createdAt(product.getCreatedAt()).updatedAt(product.getUpdatedAt()).build();
	}

	@Override
	@Transactional
	public ProductDetailResponseDTO createProduct(ProductRequestDTO requestDTO) {
		String slug = generateAndValidateSlug(requestDTO.getName(), null);

		Category category = categoryRepository.findById(requestDTO.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + requestDTO.getCategoryId()));
		if (Boolean.TRUE.equals(category.getIsDeleted())) {
			throw new BusinessException("Không thể gán sản phẩm vào danh mục đã bị xóa!");
		}

		Brand brand = brandRepository.findById(requestDTO.getBrandId()).orElseThrow(
				() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + requestDTO.getBrandId()));
		if (Boolean.FALSE.equals(brand.getIsActive())) {
			throw new BusinessException("Không thể gán sản phẩm vào thương hiệu đã ngừng hoạt động!");
		}

		Product product = Product.builder().name(requestDTO.getName().trim()).slug(slug)
				.description(requestDTO.getDescription().trim()).category(category).brand(brand)
				.isActive(requestDTO.getIsActive() != null ? requestDTO.getIsActive() : true)
				.isSelling(requestDTO.getIsSelling() != null ? requestDTO.getIsSelling() : true).rating(5.0).build();

		Product saved = productRepository.save(product);
		log.info("Tạo mới sản phẩm thành công: id={}, name={}", saved.getId(), saved.getName());
		return getProductDetailById(saved.getId());
	}

	@Override
	@Transactional
	public ProductDetailResponseDTO updateProduct(UUID id, ProductRequestDTO requestDTO) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

		// Chỉ tạo lại slug khi đổi tên, tránh làm hỏng đường dẫn cũ
		String slug = (requestDTO.getName().trim().equals(product.getName()) && product.getSlug() != null
				&& !product.getSlug().isBlank()) ? product.getSlug()
						: generateAndValidateSlug(requestDTO.getName(), id);

		Category category = categoryRepository.findById(requestDTO.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + requestDTO.getCategoryId()));
		if (Boolean.TRUE.equals(category.getIsDeleted())) {
			throw new BusinessException("Không thể gán sản phẩm vào danh mục đã bị xóa!");
		}

		Brand brand = brandRepository.findById(requestDTO.getBrandId()).orElseThrow(
				() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + requestDTO.getBrandId()));
		if (Boolean.FALSE.equals(brand.getIsActive())) {
			throw new BusinessException("Không thể gán sản phẩm vào thương hiệu đã ngừng hoạt động!");
		}

		product.setName(requestDTO.getName().trim());
		product.setSlug(slug);
		product.setDescription(requestDTO.getDescription().trim());
		product.setCategory(category);
		product.setBrand(brand);
		if (requestDTO.getIsActive() != null) {
			product.setActive(requestDTO.getIsActive());
		}
		if (requestDTO.getIsSelling() != null) {
			product.setSelling(requestDTO.getIsSelling());
		}

		Product updated = productRepository.save(product);
		log.info("Cập nhật sản phẩm thành công: id={}, name={}", updated.getId(), updated.getName());
		return getProductDetailById(updated.getId());
	}

	@Override
	@Transactional
	public void deleteProduct(UUID id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

		product.setActive(false);
		// Đặt isActive=false cho cả các variant của nó
		List<ProductVariant> variants = productVariantRepository.findByProductId(id);
		for (ProductVariant v : variants) {
			v.setActive(false);
			productVariantRepository.save(v);
		}
		productRepository.save(product);
		log.info("Vô hiệu hóa sản phẩm và các biến thể của id={}", id);
	}

	@Override
	@Transactional
	public void restoreProduct(UUID id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

		// Ràng buộc: chỉ khi category và brand của nó đang hợp lệ
		if (Boolean.TRUE.equals(product.getCategory().getIsDeleted())) {
			throw new BusinessException("Không thể khôi phục sản phẩm vì danh mục đang bị xóa!");
		}
		if (Boolean.FALSE.equals(product.getBrand().getIsActive())) {
			throw new BusinessException("Không thể khôi phục sản phẩm vì thương hiệu đang ngừng hoạt động!");
		}

		product.setActive(true);
		productRepository.save(product);

		List<ProductVariant> variants = productVariantRepository.findByProductId(id);

		variants.forEach(v -> v.setActive(true));

		productVariantRepository.saveAll(variants);

	}

	// Chỉ nạp biến thể của các sản phẩm đang hiển thị (thay cho findAll() toàn
	// bảng)
	private List<ProductVariant> loadVariants(List<UUID> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return Collections.emptyList();
		}
		return productVariantRepository.findAllWithImagesByProductIdIn(productIds);
	}

	private ProductResponseDTO enrichProductResponseDTO(Product product, List<ProductVariant> variants) {
		ProductResponseDTO dto = productMapper.toResponseDTO(product);

		if (variants == null || variants.isEmpty()) {
			dto.setVariantCount(0);
			dto.setTotalStock(0);
			dto.setTotalSold(0);
			dto.setMinPrice(BigDecimal.ZERO);
			dto.setEffectivePrice(BigDecimal.ZERO);
			return dto;
		}

		dto.setVariantCount(variants.size());

		int totalStock = variants.stream().mapToInt(v -> v.getQuantity() != null ? v.getQuantity() : 0).sum();
		int totalSold = variants.stream().mapToInt(v -> v.getSold() != null ? v.getSold() : 0).sum();
		dto.setTotalStock(totalStock);
		dto.setTotalSold(totalSold);

		// Calculate min price, promotional price, effective price, thumbnail
		BigDecimal minPrice = null;
		BigDecimal minPromoPrice = null;
		BigDecimal effectiveMin = null;
		BigDecimal maxPrice = null;
		String thumbnail = null;

		for (ProductVariant v : variants) {
			BigDecimal price = v.getPrice();
			BigDecimal promo = (v.getPromotionalPrice() != null
					&& v.getPromotionalPrice().compareTo(BigDecimal.ZERO) > 0) ? v.getPromotionalPrice() : null;
			BigDecimal effective = promo != null ? promo : price;

			if (minPrice == null || (price != null && price.compareTo(minPrice) < 0)) {
				minPrice = price;
			}
			if (maxPrice == null || (price != null && price.compareTo(maxPrice) > 0)) {
				maxPrice = price;
			}
			if (promo != null && (minPromoPrice == null || promo.compareTo(minPromoPrice) < 0)) {
				minPromoPrice = promo;
			}
			if (effectiveMin == null || (effective != null && effective.compareTo(effectiveMin) < 0)) {
				effectiveMin = effective;
				// Thumbnail is first image of cheapest variant
				if (v.getImages() != null && !v.getImages().isEmpty()) {
					thumbnail = v.getImages().stream().map(ProductVariantImage::getImageUrl).sorted().findFirst()
							.orElse(null);
				}
			}
		}

		if (thumbnail == null) {
			// Find any variant with images
			for (ProductVariant v : variants) {
				if (v.getImages() != null && !v.getImages().isEmpty()) {
					thumbnail = v.getImages().stream().map(ProductVariantImage::getImageUrl).sorted().findFirst()
							.orElse(null);
					break;
				}
			}
		}

		dto.setMinPrice(minPrice != null ? minPrice : BigDecimal.ZERO);
		dto.setMinPromotionalPrice(minPromoPrice);
		dto.setEffectivePrice(effectiveMin != null ? effectiveMin : minPrice);
		dto.setMaxPrice(maxPrice != null ? maxPrice : minPrice);
		dto.setThumbnailUrl(thumbnail);

		return dto;
	}

	private Sort buildSort(String sortParam) {
		if (sortParam == null || sortParam.trim().isEmpty()) {
			return Sort.by(Sort.Direction.DESC, "createdAt");
		}
		return switch (sortParam.toLowerCase()) {
		case "price-asc" -> Sort.by(Sort.Direction.ASC, "name"); // fallback JPA sort
		case "price-desc" -> Sort.by(Sort.Direction.DESC, "name");
		case "rating" -> Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
		case "name-asc" -> Sort.by(Sort.Direction.ASC, "name");
		case "name-desc" -> Sort.by(Sort.Direction.DESC, "name");
		case "popular", "discount" -> Sort.by(Sort.Direction.DESC, "createdAt");
		default -> Sort.by(Sort.Direction.DESC, "createdAt");
		};
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
		while (currentId == null ? productRepository.existsBySlug(slug)
				: productRepository.existsBySlugAndIdNot(slug, currentId)) {
			slug = baseSlug + "-" + count++;
		}
		return slug;
	}
}

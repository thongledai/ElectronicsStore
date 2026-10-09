package com.example.store.service.product.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.store.entity.Brand;
import com.example.store.entity.Category;
import com.example.store.entity.Product;
import com.example.store.exception.BusinessException;
import com.example.store.mapper.BrandMapper;
import com.example.store.mapper.CategoryMapper;
import com.example.store.mapper.ProductMapper;
import com.example.store.mapper.ProductVariantMapper;
import com.example.store.repository.BrandRepository;
import com.example.store.repository.CategoryRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.service.category.ICategoryService;

@ExtendWith(MockitoExtension.class)
class ProductServiceStateTest {

	@Mock private ProductRepository productRepository;
	@Mock private ProductVariantRepository productVariantRepository;
	@Mock private CategoryRepository categoryRepository;
	@Mock private BrandRepository brandRepository;
	@Mock private ICategoryService categoryService;
	@Mock private ProductMapper productMapper;
	@Mock private ProductVariantMapper productVariantMapper;
	@Mock private CategoryMapper categoryMapper;
	@Mock private BrandMapper brandMapper;

	@InjectMocks private ProductServiceImpl service;

	@Test
	void disablingProductWithActiveVariantIsRejectedWithoutChangingEitherRecord() {
		UUID productId = UUID.randomUUID();
		Product product = Product.builder().id(productId).isActive(true).isSelling(true).build();
		when(productRepository.findById(productId)).thenReturn(Optional.of(product));
		when(productVariantRepository.existsByProductIdAndIsActiveTrue(productId)).thenReturn(true);

		assertThrows(BusinessException.class, () -> service.deleteProduct(productId));

		verify(productRepository, never()).save(product);
		verify(productVariantRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyIterable());
	}

	@Test
	void restoringProductDoesNotRestoreItsVariantsOrChangeSellingState() {
		UUID productId = UUID.randomUUID();
		Category category = Category.builder().isDeleted(false).build();
		Brand brand = Brand.builder().isActive(true).build();
		Product product = Product.builder().id(productId).category(category).brand(brand)
				.isActive(false).isSelling(false).build();
		when(productRepository.findById(productId)).thenReturn(Optional.of(product));

		service.restoreProduct(productId);

		assertFalse(product.isSelling());
		verify(productRepository).save(product);
		verify(productVariantRepository, never()).findByProductId(productId);
		verify(productVariantRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyIterable());
	}
}

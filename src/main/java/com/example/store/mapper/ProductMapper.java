package com.example.store.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.store.dto.product.ProductDetailResponseDTO;
import com.example.store.dto.product.ProductRequestDTO;
import com.example.store.dto.product.ProductResponseDTO;
import com.example.store.entity.Product;

@Mapper(componentModel = "spring", uses = {
                CategoryMapper.class,
                BrandMapper.class,
                ProductVariantMapper.class
})
public interface ProductMapper {

        @Mapping(target = "id", ignore = true)
        @Mapping(target = "slug", ignore = true)
        @Mapping(target = "category", ignore = true)
        @Mapping(target = "brand", ignore = true)
        @Mapping(target = "variants", ignore = true)
        @Mapping(target = "rating", ignore = true)
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        @Mapping(target = "isSelling", source = "isSelling")
        Product toEntity(ProductRequestDTO dto);

        @Mapping(target = "categoryId", source = "category.id")
        @Mapping(target = "categoryName", source = "category.name")
        @Mapping(target = "categorySlug", source = "category.slug")
        @Mapping(target = "brandId", source = "brand.id")
        @Mapping(target = "brandName", source = "brand.name")
        @Mapping(target = "brandSlug", source = "brand.slug")
        @Mapping(target = "isSelling", source = "selling")
        @Mapping(target = "isActive", source = "active")
        @Mapping(target = "variantCount", expression = "java(product.getVariants() == null ? 0 : product.getVariants().size())")
        @Mapping(target = "minPrice", ignore = true)
        @Mapping(target = "minPromotionalPrice", ignore = true)
        @Mapping(target = "effectivePrice", ignore = true)
        @Mapping(target = "maxPrice", ignore = true)
        @Mapping(target = "totalStock", ignore = true)
        @Mapping(target = "totalSold", ignore = true)
        @Mapping(target = "thumbnailUrl", ignore = true)
        ProductResponseDTO toResponseDTO(Product product);

        @Mapping(target = "category", source = "category")
        @Mapping(target = "brand", source = "brand")
        @Mapping(target = "variants", source = "variants")
        @Mapping(target = "isSelling", source = "selling")
        @Mapping(target = "isActive", source = "active")
        ProductDetailResponseDTO toDetailResponseDTO(Product product);
}
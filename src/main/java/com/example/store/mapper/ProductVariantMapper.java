package com.example.store.mapper;

import java.util.Collections;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.store.dto.product.ProductVariantRequestDTO;
import com.example.store.dto.product.ProductVariantResponseDTO;
import com.example.store.entity.ProductVariant;
import com.example.store.entity.ProductVariantImage;

@Mapper(componentModel = "spring", uses = {
        StyleValueMapper.class
})
public interface ProductVariantMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "styleValues", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "sold", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "isSelling", source = "isSelling")
    ProductVariant toEntity(ProductVariantRequestDTO dto);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "imageUrls", source = "images")
    @Mapping(target = "styleValues", source = "styleValues")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "isSelling", source = "selling")
    ProductVariantResponseDTO toResponseDTO(ProductVariant variant);

    default List<String> mapImagesToUrls(List<ProductVariantImage> images) {
        if (images == null) {
            return Collections.emptyList();
        }
        return images.stream()
                .map(ProductVariantImage::getImageUrl)
                .sorted()
                .toList();
    }
}
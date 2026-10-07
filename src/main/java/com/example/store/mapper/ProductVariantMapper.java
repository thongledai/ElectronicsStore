package com.example.store.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.store.dto.product.ProductVariantRequestDTO;
import com.example.store.dto.product.ProductVariantResponseDTO;
import com.example.store.entity.ProductVariant;

@Mapper(
        componentModel = "spring",
        uses = {
                StyleValueMapper.class
        }
)
public interface ProductVariantMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "styleValues", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "sold", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", source = "isActive")
    @Mapping(target = "selling", source = "isSelling")
    ProductVariant toEntity(ProductVariantRequestDTO dto);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "imageUrls", source = "image")
    @Mapping(target = "styleValues", source = "styleValues")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "isSelling", source = "selling")
    ProductVariantResponseDTO toResponseDTO(ProductVariant variant);
}
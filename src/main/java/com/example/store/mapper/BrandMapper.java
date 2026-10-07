package com.example.store.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.store.dto.brand.BrandRequestDTO;
import com.example.store.dto.brand.BrandResponseDTO;
import com.example.store.entity.Brand;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Brand toEntity(BrandRequestDTO dto);

    BrandResponseDTO toResponseDTO(Brand brand);
}
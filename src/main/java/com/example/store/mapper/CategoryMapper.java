package com.example.store.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.store.dto.category.CategoryRequestDTO;
import com.example.store.dto.category.CategoryResponseDTO;
import com.example.store.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryRequestDTO dto);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    CategoryResponseDTO toResponseDTO(Category category);
}
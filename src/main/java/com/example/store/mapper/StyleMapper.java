package com.example.store.mapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.store.dto.style.StyleRequestDTO;
import com.example.store.dto.style.StyleResponseDTO;
import com.example.store.entity.Category;
import com.example.store.entity.Style;

@Mapper(componentModel = "spring")
public interface StyleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Style toEntity(StyleRequestDTO dto);

    @Mapping(target = "categoryIds", source = "categories")
    @Mapping(target = "categoryNames", source = "categories")
    StyleResponseDTO toResponseDTO(Style style);

    default UUID mapCategoryId(Category category) {
        return category == null ? null : category.getId();
    }

    default String mapCategoryName(Category category) {
        return category == null ? null : category.getName();
    }

    default List<UUID> mapCategoryIds(Set<Category> categories) {
        if (categories == null) {
            return null;
        }

        return categories.stream()
                .map(this::mapCategoryId)
                .toList();
    }

    default List<String> mapCategoryNames(Set<Category> categories) {
        if (categories == null) {
            return null;
        }

        return categories.stream()
                .map(this::mapCategoryName)
                .toList();
    }
}
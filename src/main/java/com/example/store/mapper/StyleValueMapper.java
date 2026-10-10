package com.example.store.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.store.dto.style.StyleValueRequestDTO;
import com.example.store.dto.style.StyleValueResponseDTO;
import com.example.store.entity.StyleValue;

@Mapper(componentModel = "spring")
public interface StyleValueMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "style", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StyleValue toEntity(StyleValueRequestDTO dto);

    @Mapping(target = "styleId", source = "style.id")
    @Mapping(target = "styleName", source = "style.name")
    StyleValueResponseDTO toResponseDTO(StyleValue styleValue);
}
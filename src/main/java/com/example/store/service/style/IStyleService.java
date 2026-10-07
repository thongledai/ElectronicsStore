package com.example.store.service.style;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.example.store.dto.common.PageResponse;
import com.example.store.dto.style.StyleOptionDTO;
import com.example.store.dto.style.StyleRequestDTO;
import com.example.store.dto.style.StyleResponseDTO;

public interface IStyleService {

    PageResponse<StyleResponseDTO> getAllStyles(String search, Boolean isDeleted, Pageable pageable);

    List<StyleResponseDTO> getActiveStyles();

    List<StyleOptionDTO> getStyleOptions();

    List<StyleResponseDTO> getStylesByCategoryId(UUID categoryId);

    StyleResponseDTO getStyleById(UUID id);

    StyleResponseDTO createStyle(StyleRequestDTO requestDTO);

    StyleResponseDTO updateStyle(UUID id, StyleRequestDTO requestDTO);

    void deleteStyle(UUID id);

    void restoreStyle(UUID id);
}

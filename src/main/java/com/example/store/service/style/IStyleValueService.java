package com.example.store.service.style;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.example.store.dto.common.PageResponse;
import com.example.store.dto.style.StyleValueOptionDTO;
import com.example.store.dto.style.StyleValueRequestDTO;
import com.example.store.dto.style.StyleValueResponseDTO;

public interface IStyleValueService {

    PageResponse<StyleValueResponseDTO> getAllStyleValues(String search, UUID styleId, Boolean isDeleted,
            Pageable pageable);

    List<StyleValueResponseDTO> getActiveStyleValues();

    List<StyleValueOptionDTO> getStyleValueOptions(UUID styleId);

    List<StyleValueResponseDTO> getStyleValuesByStyleId(UUID styleId);

    StyleValueResponseDTO getStyleValueById(UUID id);

    StyleValueResponseDTO createStyleValue(StyleValueRequestDTO requestDTO);

    StyleValueResponseDTO updateStyleValue(UUID id, StyleValueRequestDTO requestDTO);

    void deleteStyleValue(UUID id);

    void restoreStyleValue(UUID id);
}

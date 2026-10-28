package com.example.store.service.promotion;

import com.example.store.dto.common.PageResponse;
import com.example.store.dto.promotion.PromotionRequestDTO;
import com.example.store.dto.promotion.PromotionResponseDTO;
import com.example.store.enums.PromotionType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IPromotionManagementService {

    PageResponse<PromotionResponseDTO> getPromotions(String search, PromotionType type, Boolean isActive, Pageable pageable);

    PromotionResponseDTO getPromotionById(UUID id);

    PromotionResponseDTO createPromotion(PromotionRequestDTO req);

    PromotionResponseDTO updatePromotion(UUID id, PromotionRequestDTO req);

    PromotionResponseDTO togglePromotionStatus(UUID id);

    void deletePromotion(UUID id);
}

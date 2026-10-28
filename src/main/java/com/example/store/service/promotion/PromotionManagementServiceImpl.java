package com.example.store.service.promotion;

import com.example.store.dto.common.PageResponse;
import com.example.store.dto.promotion.PromotionRequestDTO;
import com.example.store.dto.promotion.PromotionResponseDTO;
import com.example.store.entity.Promotion;
import com.example.store.enums.PromotionType;
import com.example.store.enums.error.PromotionErrorCode;
import com.example.store.exception.AppException;
import com.example.store.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionManagementServiceImpl implements IPromotionManagementService {

    private final PromotionRepository promotionRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionResponseDTO> getPromotions(String search, PromotionType type, Boolean isActive, Pageable pageable) {
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;
        Page<Promotion> page = promotionRepository.findWithFilters(cleanSearch, type, isActive, pageable);
        List<PromotionResponseDTO> dtoList = page.getContent().stream().map(this::toResponseDTO).toList();
        return PageResponse.of(page, dtoList);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponseDTO getPromotionById(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(PromotionErrorCode.VOUCHER_NOT_FOUND));
        return toResponseDTO(promotion);
    }

    @Override
    @Transactional
    public PromotionResponseDTO createPromotion(PromotionRequestDTO req) {
        String code = req.getCode().trim().toUpperCase();

        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new AppException(PromotionErrorCode.VOUCHER_CODE_EXISTS);
        }

        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new AppException(PromotionErrorCode.VOUCHER_DATE_INVALID);
        }

        BigDecimal minOrder = req.getMinOrderValue() != null ? req.getMinOrderValue() : BigDecimal.ZERO;
        BigDecimal maxDiscount = (req.getMaxDiscountAmount() != null && req.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
                ? req.getMaxDiscountAmount()
                : null;

        Promotion promotion = Promotion.builder()
                .code(code)
                .description(StringUtils.hasText(req.getDescription()) ? req.getDescription().trim() : null)
                .type(req.getType())
                .discountPercent(req.getDiscountPercent())
                .maxDiscountAmount(maxDiscount)
                .minOrderValue(minOrder)
                .quantity(req.getQuantity())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .isEnabled(req.getIsEnabled() != null ? req.getIsEnabled() : true)
                .build();

        Promotion saved = promotionRepository.save(promotion);
        log.info("Nhân viên đã tạo voucher mới: {}", saved.getCode());
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public PromotionResponseDTO updatePromotion(UUID id, PromotionRequestDTO req) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(PromotionErrorCode.VOUCHER_NOT_FOUND));

        String code = req.getCode().trim().toUpperCase();

        if (promotionRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new AppException(PromotionErrorCode.VOUCHER_CODE_EXISTS);
        }

        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new AppException(PromotionErrorCode.VOUCHER_DATE_INVALID);
        }

        BigDecimal minOrder = req.getMinOrderValue() != null ? req.getMinOrderValue() : BigDecimal.ZERO;
        BigDecimal maxDiscount = (req.getMaxDiscountAmount() != null && req.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
                ? req.getMaxDiscountAmount()
                : null;

        promotion.setCode(code);
        promotion.setDescription(StringUtils.hasText(req.getDescription()) ? req.getDescription().trim() : null);
        promotion.setType(req.getType());
        promotion.setDiscountPercent(req.getDiscountPercent());
        promotion.setMaxDiscountAmount(maxDiscount);
        promotion.setMinOrderValue(minOrder);
        promotion.setQuantity(req.getQuantity());
        promotion.setStartDate(req.getStartDate());
        promotion.setEndDate(req.getEndDate());
        if (req.getIsActive() != null) {
            promotion.setIsActive(req.getIsActive());
        }
        if (req.getIsEnabled() != null) {
            promotion.setIsEnabled(req.getIsEnabled());
        }

        Promotion saved = promotionRepository.save(promotion);
        log.info("Nhân viên đã cập nhật voucher: {}", saved.getCode());
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public PromotionResponseDTO togglePromotionStatus(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(PromotionErrorCode.VOUCHER_NOT_FOUND));

        boolean newStatus = !Boolean.TRUE.equals(promotion.getIsActive());
        promotion.setIsActive(newStatus);
        promotion.setIsEnabled(newStatus);

        Promotion saved = promotionRepository.save(promotion);
        log.info("Đổi trạng thái voucher {} sang: {}", saved.getCode(), newStatus ? "Kích hoạt" : "Tạm ngưng");
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public void deletePromotion(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(PromotionErrorCode.VOUCHER_NOT_FOUND));

        long usedCount = promotionRepository.countOrdersUsingPromotion(id);
        if (usedCount > 0) {
            // Không xóa vật lý vì sẽ vi phạm ràng buộc khóa ngoại hoặc mất lịch sử đơn hàng
            promotion.setIsActive(false);
            promotion.setIsEnabled(false);
            promotionRepository.save(promotion);
            log.info("Voucher {} đã có {} đơn sử dụng, chuyển sang vô hiệu hóa thay vì xóa vật lý", promotion.getCode(), usedCount);
        } else {
            promotionRepository.delete(promotion);
            log.info("Đã xóa hoàn toàn voucher: {}", promotion.getCode());
        }
    }

    private PromotionResponseDTO toResponseDTO(Promotion p) {
        LocalDateTime now = LocalDateTime.now();
        String status;
        String statusText;

        if (!Boolean.TRUE.equals(p.getIsActive()) || !Boolean.TRUE.equals(p.getIsEnabled())) {
            status = "INACTIVE";
            statusText = "Tạm ngưng";
        } else if (p.getQuantity() != null && p.getQuantity() <= 0) {
            status = "OUT_OF_STOCK";
            statusText = "Hết lượt dùng";
        } else if (p.getStartDate() != null && now.isBefore(p.getStartDate())) {
            status = "UPCOMING";
            statusText = "Sắp diễn ra";
        } else if (p.getEndDate() != null && now.isAfter(p.getEndDate())) {
            status = "EXPIRED";
            statusText = "Đã kết thúc";
        } else {
            status = "ACTIVE";
            statusText = "Đang áp dụng";
        }

        String typeText = p.getType() == PromotionType.PRODUCT_PERCENT
                ? "Giảm tiền hàng"
                : "Giảm phí vận chuyển";

        long usedCount = promotionRepository.countOrdersUsingPromotion(p.getId());

        return PromotionResponseDTO.builder()
                .id(p.getId())
                .code(p.getCode())
                .description(p.getDescription())
                .type(p.getType())
                .typeText(typeText)
                .discountPercent(p.getDiscountPercent())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .minOrderValue(p.getMinOrderValue())
                .quantity(p.getQuantity())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .isActive(p.getIsActive())
                .isEnabled(p.getIsEnabled())
                .status(status)
                .statusText(statusText)
                .usedCount(usedCount)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

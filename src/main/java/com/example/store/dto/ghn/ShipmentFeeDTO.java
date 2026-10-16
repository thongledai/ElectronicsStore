package com.example.store.dto.ghn;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShipmentFeeDTO(
        BigDecimal fee,
        LocalDateTime expected
) {}

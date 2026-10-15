package com.example.store.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryDTO(UUID id, String name, String description, BigDecimal price) {}

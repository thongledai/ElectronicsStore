package com.example.store.service.delivery;

import java.util.UUID;

import com.example.store.dto.ghn.GhnShipmentCreateRequestDTO;
import com.example.store.dto.ghn.GhnShipmentResponseDTO;

public interface IGhnDeliveryService {
    void confirmOrder(UUID orderId);
    GhnShipmentResponseDTO createShipment(UUID orderId, GhnShipmentCreateRequestDTO request);
    GhnShipmentResponseDTO getShipment(UUID orderId);
}

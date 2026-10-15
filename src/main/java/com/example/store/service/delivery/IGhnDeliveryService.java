package com.example.store.service.delivery;

import java.util.UUID;

import com.example.store.dto.ghn.GhnShipmentCreateRequestDTO;
import com.example.store.dto.ghn.GhnShipmentResponseDTO;

import com.example.store.dto.ghn.GhnCreateOrderPreviewDTO;

public interface IGhnDeliveryService {
    void confirmOrder(UUID orderId);
    void saveAddressSelection(UUID orderId, GhnShipmentCreateRequestDTO request);
    GhnShipmentResponseDTO createShipment(UUID orderId, GhnShipmentCreateRequestDTO request);
    GhnShipmentResponseDTO getShipment(UUID orderId);
    
GhnCreateOrderPreviewDTO previewShipment(
        UUID orderId,
        GhnShipmentCreateRequestDTO request);

}

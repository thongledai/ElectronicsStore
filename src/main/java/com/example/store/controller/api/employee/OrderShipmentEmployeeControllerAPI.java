package com.example.store.controller.api.employee;

import java.util.UUID;

import com.example.store.service.delivery.IGhnDeliveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.common.ApiResponse;
import com.example.store.enums.DeliveryMessage;
import com.example.store.enums.EmployeeMessage;
import com.example.store.dto.ghn.GhnShipmentCreateRequestDTO;
import com.example.store.dto.ghn.GhnShipmentResponseDTO;
import com.example.store.service.delivery.IGhnAddressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employee/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('EMPLOYEE', 'ROLE_EMPLOYEE')")
public class OrderShipmentEmployeeControllerAPI {
    private final IGhnDeliveryService deliveryService;
    private final IGhnAddressService addressService;

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirm(@PathVariable UUID orderId) {
        deliveryService.confirmOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(EmployeeMessage.ORDER_CONFIRMED.getMessage()));
    }

    @PostMapping("/{orderId}/ghn-shipment")
    public ResponseEntity<ApiResponse<GhnShipmentResponseDTO>> createShipment(@PathVariable UUID orderId,
            @Valid @RequestBody GhnShipmentCreateRequestDTO request) {
        GhnShipmentResponseDTO result = deliveryService.createShipment(orderId, request);
        String message = result.isAlreadyCreated()
                ? DeliveryMessage.GHN_SHIPMENT_ALREADY_EXISTS.getMessage()
                : DeliveryMessage.GHN_SHIPMENT_CREATED.getMessage();
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @GetMapping("/{orderId}/ghn-shipment")
    public ResponseEntity<ApiResponse<GhnShipmentResponseDTO>> getShipment(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success(DeliveryMessage.GHN_SHIPMENT_LOADED.getMessage(), deliveryService.getShipment(orderId)));
    }
}

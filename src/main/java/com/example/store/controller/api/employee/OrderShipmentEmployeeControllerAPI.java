package com.example.store.controller.api.employee;

import java.util.List;
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
import com.example.store.dto.ghn.GhnAddressOptionDTO;
import com.example.store.dto.ghn.GhnShipmentCreateRequestDTO;
import com.example.store.dto.ghn.GhnShipmentResponseDTO;
import com.example.store.service.delivery.IGhnAddressService;
//import com.example.store.service.delivery.IGhnDeliveryService;
import com.example.store.dto.ghn.GhnCreateOrderPreviewDTO;

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

//    @GetMapping("/ghn-address/provinces")
//    public ResponseEntity<ApiResponse<List<GhnAddressOptionDTO>>> provinces() {
//        return ResponseEntity.ok(ApiResponse.success(DeliveryMessage.GHN_PROVINCES_LOADED.getMessage(), addressService.getProvinces()));
//    }
//
//    @GetMapping("/ghn-address/provinces/{provinceId}/wards")
//    public ResponseEntity<ApiResponse<List<GhnAddressOptionDTO>>> wards(@PathVariable Integer provinceId) {
//        return ResponseEntity.ok(ApiResponse.success(DeliveryMessage.GHN_WARDS_LOADED.getMessage(), addressService.getWards(provinceId)));
//    }
//
//    @PostMapping("/{orderId}/ghn-address")
//    public ResponseEntity<ApiResponse<Void>> saveAddress(@PathVariable UUID orderId,
//            @Valid @RequestBody GhnShipmentCreateRequestDTO request) {
//        deliveryService.saveAddressSelection(orderId, request);
//        return ResponseEntity.ok(ApiResponse.success(DeliveryMessage.GHN_ADDRESS_SAVED.getMessage()));
//    }

//    @PostMapping("/{orderId}/ghn-shipment/preview")
//    public ResponseEntity<ApiResponse<GhnCreateOrderPreviewDTO>> previewShipment(
//            @PathVariable UUID orderId,
//            @Valid @RequestBody GhnShipmentCreateRequestDTO request) {
//
//        GhnCreateOrderPreviewDTO result =
//                deliveryService.previewShipment(orderId, request);
//
//        return ResponseEntity.ok(
//                ApiResponse.success(DeliveryMessage.GHN_PREVIEW_CREATED.getMessage(), result));
//    }


    @PostMapping("/{orderId}/ghn-shipment")
    public ResponseEntity<ApiResponse<GhnShipmentResponseDTO>> createShipment(@PathVariable UUID orderId,
            @Valid @RequestBody GhnShipmentCreateRequestDTO request) {
        GhnShipmentResponseDTO result = deliveryService.createShipment(orderId, request);
        String message = result.isAlreadyCreated()
                ? DeliveryMessage.GHN_SHIPMENT_ALREADY_EXISTS.getMessage()
                : DeliveryMessage.GHN_SHIPMENT_CREATED.getMessage();
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

//    @GetMapping("/{orderId}/ghn-shipment")
//    public ResponseEntity<ApiResponse<GhnShipmentResponseDTO>> getShipment(@PathVariable UUID orderId) {
//        return ResponseEntity.ok(ApiResponse.success(DeliveryMessage.GHN_SHIPMENT_LOADED.getMessage(), deliveryService.getShipment(orderId)));
//    }
}

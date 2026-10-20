package com.example.store.controller.api.manager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.common.PageResponse;
import com.example.store.dto.manager.order.ManagerOrderDetailDTO;
import com.example.store.dto.manager.order.ManagerOrderSummaryDTO;
import com.example.store.dto.manager.order.OrderStatusUpdateDTO;
import com.example.store.entity.PaymentMethod;
import com.example.store.repository.PaymentMethodRepository;
import com.example.store.service.manager.IManagerOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGER','ROLE_MANAGER')")
public class OrderManagerControllerAPI {

    private final IManagerOrderService managerOrderService;
    private final PaymentMethodRepository paymentMethodRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ManagerOrderSummaryDTO>>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer paymentMethodId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ManagerOrderSummaryDTO> result = managerOrderService.getOrders(status, keyword, paymentMethodId, fromDate, toDate, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", PageResponse.of(result)));
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getOrderCounts() {
        Map<String, Long> counts = managerOrderService.getOrderCountsByStatus();
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng đơn hàng theo trạng thái thành công", counts));
    }

    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<List<PaymentMethod>>> getPaymentMethods() {
        List<PaymentMethod> list = paymentMethodRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phương thức thanh toán thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ManagerOrderDetailDTO>> getOrderDetail(@PathVariable UUID id) {
        ManagerOrderDetailDTO detail = managerOrderService.getOrderDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn hàng thành công", detail));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ManagerOrderDetailDTO>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusUpdateDTO dto) {
        ManagerOrderDetailDTO updated = managerOrderService.updateOrderStatus(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", updated));
    }
}

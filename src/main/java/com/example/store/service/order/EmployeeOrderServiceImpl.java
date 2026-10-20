package com.example.store.service.order;

import com.example.store.entity.Order;
import com.example.store.enums.DeliveryMessage;
import com.example.store.enums.OrderStatusName;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeOrderServiceImpl implements IEmployeeOrderService {
    private static final int PAGE_SIZE = 20;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Order> search(String status, String keyword, int page) {
        String statusFilter = normalizeStatus(status);   // null = tất cả
        String kw = normalizeKeyword(keyword);           // "" = không lọc

        // Chờ xác nhận: đơn cũ nhất lên đầu để không bị bỏ sót. Trạng thái khác: mới nhất lên đầu
        Sort sort = "NOT_PROCESSED".equals(statusFilter)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, sort);
        return orderRepository.search(statusFilter, kw, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getDetail(UUID orderId) {
        return orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(DeliveryMessage.ORDER_NOT_FOUND.getMessage()));
    }

    @Override
    public Map<UUID, String> orderCodes(Collection<Order> orders) {
        return orders.stream().collect(Collectors.toMap(
                Order::getId,
                o -> orderCode(o.getId()),
                (a, b) -> a));                 // phòng trùng khóa, tránh lỗi "Duplicate key"
    }

    public static String orderCode(UUID id) {
        if (id == null) return "ORD001";
        return "ORD" + String.format("%03d", Math.abs(id.hashCode() % 1000));
    }

    /** Chỉ nhận giá trị nằm trong enum; giá trị lạ hoặc rỗng coi như "tất cả". */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        String s = status.trim().toUpperCase();
        boolean valid = Arrays.stream(OrderStatusName.values()).anyMatch(v -> v.name().equals(s));
        return valid ? s : null;
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) return "";
        String k = keyword.trim();
        return k.length() > MAX_KEYWORD_LENGTH ? k.substring(0, MAX_KEYWORD_LENGTH) : k;
    }
}

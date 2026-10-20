package com.example.store.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.store.dto.ghn.GhnCreateOrderRequest;
import com.example.store.entity.Order;

@Component
public class GhnOrderMapper {
    private static final Integer PAYMENT_TYPE_ID = 1;
    private static final Integer SERVICE_TYPE_ID = 2;
    private static final Integer GHN_DEFAULT_ITEM_WEIGHT_GRAM = 500;
    private static final Integer GHN_DEFAULT_LENGTH_CM = 30;
    private static final Integer GHN_DEFAULT_WIDTH_CM = 20;
    private static final Integer GHN_DEFAULT_HEIGHT_CM = 10;
    private static final String GHN_DEFAULT_REQUIRE_NOTE = "KHONGCHOXEMHANG";

    public GhnCreateOrderRequest toCreateOrderRequest(Order order) {
        String toName = StringUtils.hasText(order.getToName()) ? order.getToName()
                : (order.getUser() != null && StringUtils.hasText(order.getUser().getFullName())
                        ? order.getUser().getFullName() : "Khách hàng");

        String toPhone = StringUtils.hasText(order.getPhone()) ? order.getPhone()
                : (order.getUser() != null ? order.getUser().getPhone() : "");
        if (toPhone != null) {
            toPhone = toPhone.replaceAll("[^0-9]", "");
        }

        String toAddress = order.getFullAddress();
        String toWardName = order.getWardName();
        String toProvinceName = order.getProvinceName();

        // Fallback trích xuất nếu thiếu tên phường/xã hoặc tỉnh/thành từ toAddress
        if ((!StringUtils.hasText(toWardName) || !StringUtils.hasText(toProvinceName)) && StringUtils.hasText(toAddress)) {
            String[] parts = toAddress.split(",");
            if (parts.length >= 2) {
                if (!StringUtils.hasText(toProvinceName)) {
                    toProvinceName = parts[parts.length - 1].trim();
                }
                if (!StringUtils.hasText(toWardName)) {
                    toWardName = parts[parts.length - 2].trim();
                }
            }
        }

        int cod = Boolean.TRUE.equals(order.getIsPaidBefore()) ? 0
                : (order.getTotal() != null ? order.getTotal().intValue() : 0);

        int orderVal = order.getSubtotal() != null && order.getSubtotal().intValue() > 0
                ? order.getSubtotal().intValue()
                : (order.getTotal() != null ? order.getTotal().intValue() : 0);

        return GhnCreateOrderRequest.builder()
                .paymentTypeId(PAYMENT_TYPE_ID)
                .requiredNote(GHN_DEFAULT_REQUIRE_NOTE)
                .clientOrderCode(order.getId() != null ? order.getId().toString() : "")
                .toName(toName)
                .toPhone(toPhone)
                .toAddress(toAddress)
                .toWardName(toWardName)
                .toProvinceName(toProvinceName)
                .isNewToAddress(true)
                .serviceTypeId(SERVICE_TYPE_ID)
                .weight(GHN_DEFAULT_ITEM_WEIGHT_GRAM)
                .length(GHN_DEFAULT_LENGTH_CM)
                .width(GHN_DEFAULT_WIDTH_CM)
                .height(GHN_DEFAULT_HEIGHT_CM)
                .codAmount(cod)
                .orderValue(orderVal)
                .items(this.createOrderItems(order))
                .build();
    }

    private List<GhnCreateOrderRequest.Item> createOrderItems(Order order) {
        List<GhnCreateOrderRequest.Item> orderItems = new ArrayList<>();

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (var o : order.getItems()) {
                String name = StringUtils.hasText(o.getProductName()) ? o.getProductName() : "Sản phẩm";
                int qty = o.getQuantity() != null && o.getQuantity() > 0 ? o.getQuantity() : 1;
                orderItems.add(GhnCreateOrderRequest.Item.builder().name(name).quantity(qty).build());
            }
        }

        if (orderItems.isEmpty()) {
            orderItems.add(GhnCreateOrderRequest.Item.builder().name("Sản phẩm điện tử").quantity(1).build());
        }

        return orderItems;
    }
}

package com.example.store.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.store.dto.ghn.GhnCreateOrderRequest;
import com.example.store.entity.Order;

@Component
public class GhnOrderMapper {
    public static final int SERVICE_TYPE_STANDARD = 2;
    public static final int MAX_WEIGHT_GRAM_SERVICE_2 = 19_999;
    public static final long MAX_COD_AMOUNT = 50_000_000L;
    public static final long MAX_INSURANCE_VALUE = 5_000_000L;

    private final int paymentTypeId;
    private final boolean newAddressModel;
    private final int itemWeight;
    private final int length;
    private final int width;
    private final int height;

    public GhnOrderMapper(
            @Value("${ghn.payment-type-id:1}") int paymentTypeId,
            @Value("${ghn.new-address-model:true}") boolean newAddressModel,
            @Value("${ghn.default-item-weight-gram:500}") int itemWeight,
            @Value("${ghn.default-length-cm:30}") int length,
            @Value("${ghn.default-width-cm:20}") int width,
            @Value("${ghn.default-height-cm:10}") int height) {
        this.paymentTypeId = paymentTypeId;
        this.newAddressModel = newAddressModel;
        this.itemWeight = itemWeight;
        this.length = length;
        this.width = width;
        this.height = height;
    }

    public boolean isNewAddressModel() { return newAddressModel; }
    public int getPaymentTypeId() { return paymentTypeId; }
    public int getDefaultItemWeightGram() { return itemWeight; }
    public int getDefaultLengthCm() { return length; }
    public int getDefaultWidthCm() { return width; }
    public int getDefaultHeightCm() { return height; }

    
public GhnCreateOrderRequest toCreateOrderRequest(Order order) {
    return toCreateOrderRequest(
            order,
            order.getToDetailAddress(),
            order.getToWardName(),
            order.getToProvinceName());
}

public GhnCreateOrderRequest toCreateOrderRequest(
        Order order,
        String toAddress,
        String toWardName,
        String toProvinceName) {

    long value = toVnd(order.getAmountFromUser());

    return GhnCreateOrderRequest.builder()
            .paymentTypeId(paymentTypeId)
            .requiredNote("CHOXEMHANGKHONGTHU")
            .clientOrderCode(order.getId().toString())
            .toName(order.getUser().getFullName().trim())
            .toPhone(normalizePhone(order.getPhone()))
            .toAddress(toAddress == null ? null : toAddress.trim())
            .toWardName(toWardName == null ? null : toWardName.trim())
            .toDistrictName(null)
            .toProvinceName(toProvinceName == null ? null : toProvinceName.trim())
            .isNewToAddress(newAddressModel)
            .serviceTypeId(SERVICE_TYPE_STANDARD)
            .weight(calculateTotalWeightGram(order))
            .length(length)
            .width(width)
            .height(height)
            .codAmount(Boolean.TRUE.equals(order.getIsPaidBefore()) ? 0L : value)
            .orderValue(value)
            .insuranceValue(Math.min(value, MAX_INSURANCE_VALUE))
            .items(order.getItems().stream()
                    .map(item -> GhnCreateOrderRequest.Item.builder()
                            .name(item.getVariant().getProduct().getName())
                            .code(item.getVariant().getSku())
                            .quantity(item.getCount())
                            .weight(itemWeight)
                            .length(length)
                            .width(width)
                            .height(height)
                            .build())
                    .toList())
            .build();
}


    public long calculateCodAmount(Order order) {
        return Boolean.TRUE.equals(order.getIsPaidBefore()) ? 0L : toVnd(order.getAmountFromUser());
    }

    public int calculateTotalWeightGram(Order order) {
        long total = order.getItems().stream().mapToLong(i -> (long) i.getCount() * itemWeight).sum();
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("84") && digits.length() == 11) digits = "0" + digits.substring(2);
        return digits.matches("^0\\d{9}$") ? digits : null;
    }

    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static long toVnd(BigDecimal amount) { return amount == null ? 0L : amount.longValue(); }
}

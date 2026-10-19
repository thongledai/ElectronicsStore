package com.example.store.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.store.dto.ghn.GhnCreateOrderRequest;
import com.example.store.entity.Order;

@Component
public class GhnOrderMapper {
    private final static Integer PAYMENT_TYPE_ID = 1;
    private final static Integer SERVICE_TYPE_ID = 2;
    private final static Integer GHN_DEFAULT_ITEM_WEIGHT_GRAM=500;
    private final static Integer GHN_DEFAULT_LENGTH_CM=30;
    private final static Integer GHN_DEFAULT_WIDTH_CM=20;
    private final static Integer GHN_DEFAULT_HEIGHT_CM=10;
    private final static String GHN_DEFAULT_REQUIRE_NOTE = "KHONGCHOXEMHANG";
//
//    private final int paymentTypeId;
//    private final int itemWeight;
//    private final int length;
//    private final int width;
//    private final int height;
//
//    public GhnOrderMapper(
//                    @Value("${ghn.payment-type-id:1}") int paymentTypeId,
//                    @Value("${ghn.new-address-model:true}") boolean newAddressModel,
//                    @Value("${ghn.default-item-weight-gram:500}") int itemWeight,
//                    @Value("${ghn.default-length-cm:30}") int length,
//                    @Value("${ghn.default-width-cm:20}") int width,
//                    @Value("${ghn.default-height-cm:10}") int height) {
//            this.paymentTypeId = paymentTypeId;
//            this.newAddressModel = newAddressModel;
//            this.itemWeight = itemWeight;
//            this.length = length;
//            this.width = width;
//            this.height = height;
//    }

    public GhnCreateOrderRequest toCreateOrderRequest(Order order) {

        return GhnCreateOrderRequest.builder()
                .paymentTypeId(PAYMENT_TYPE_ID)
                .requiredNote(GHN_DEFAULT_REQUIRE_NOTE)
                .clientOrderCode(order.getId().toString())
                .toName(order.getToName())
                .toPhone(order.getPhone())
                .toAddress(order.getFullAddress())
                .toWardName(order.getWardName())
                .toProvinceName(order.getProvinceName())
                .isNewToAddress(true)
                .serviceTypeId(SERVICE_TYPE_ID)
                .weight(GHN_DEFAULT_ITEM_WEIGHT_GRAM)
                .length(GHN_DEFAULT_LENGTH_CM)
                .width(GHN_DEFAULT_WIDTH_CM)
                .height(GHN_DEFAULT_HEIGHT_CM)
                .codAmount(order.getTotal().intValue())
                .orderValue(order.getSubtotal().intValue())
                .items(this.createOrderItems(order))
                        .build();
    }

    private List<GhnCreateOrderRequest.Item> createOrderItems(Order order) {
        List<GhnCreateOrderRequest.Item> orderItems = new ArrayList<>();

        for (var o : order.getItems()) {
            GhnCreateOrderRequest.Item item =
                    GhnCreateOrderRequest.Item.builder().name(o.getProductName()).quantity(o.getQuantity()).build();

            orderItems.add(item);
        }

        return orderItems;
    }
}

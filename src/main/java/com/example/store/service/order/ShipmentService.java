package com.example.store.service.order;

import com.example.store.dto.ghn.*;
import com.example.store.dto.order.OrderPreviewRequest;
import com.example.store.entity.Order;
import com.example.store.enums.error.ShippingErrorCode;
import com.example.store.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService implements IShipmentService {
    private final static Integer PAYMENT_TYPE_ID = 1;
    private final static Integer SERVICE_TYPE_ID = 2;
    private final static Integer GHN_DEFAULT_ITEM_WEIGHT_GRAM=500;
    private final static Integer GHN_DEFAULT_LENGTH_CM=30;
    private final static Integer GHN_DEFAULT_WIDTH_CM=20;
    private final static Integer GHN_DEFAULT_HEIGHT_CM=10;
    private final static String GHN_DEFAULT_REQUIRE_NOTE = "KHONGCHOXEMHANG";
    private final RestClient ghnRestClient;

    @Override
    public GhnCreateOrderResponse createOrderPreview(OrderPreviewRequest req) {
        String fullAddress = String.join(", ", req.detailAddress(), req.wardName(), req.provinceName());

        GhnCreateOrderRequest createOrderBody = GhnCreateOrderRequest.builder()
                .paymentTypeId(PAYMENT_TYPE_ID)
                .requiredNote(GHN_DEFAULT_REQUIRE_NOTE)
                .toName(req.toName())
                .toPhone(req.phone())
                .toAddress(fullAddress)
                .toWardName(req.wardName())
                .toProvinceName(req.provinceName())
                .isNewToAddress(true)
                .serviceTypeId(SERVICE_TYPE_ID)
                .weight(GHN_DEFAULT_ITEM_WEIGHT_GRAM)
                .length(GHN_DEFAULT_LENGTH_CM)
                .width(GHN_DEFAULT_WIDTH_CM)
                .height(GHN_DEFAULT_HEIGHT_CM)
                .content("Hang de vo")
                .build();

        GhnApiResponse<GhnCreateOrderResponse> response = null;

        try {
            response = executePreview(createOrderBody);
        } catch (HttpClientErrorException e) {
            log.warn("GHN từ chối địa chỉ lần 1: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            String responseBody = e.getResponseBodyAsString();
            if (responseBody.contains("TO_ADDRESS_CONFLICT") || responseBody.contains("address conflict")) {
                // GHN geocoder phát hiện mâu thuẫn giữa tên đường chi tiết và phường/xã.
                // Thử lại với địa chỉ chuẩn hóa cấp phường/xã để lấy cước phí và ngày giao chuẩn xác.
                log.info("Thử lại GHN preview với địa chỉ cấp phường/xã: {}, {}", req.wardName(), req.provinceName());
                try {
                    GhnCreateOrderRequest fallbackBody = createOrderBody.toBuilder()
                            .toAddress(String.join(", ", req.wardName(), req.provinceName()))
                            .build();
                    response = executePreview(fallbackBody);
                } catch (Exception retryEx) {
                    log.warn("GHN preview fallback cũng thất bại", retryEx);
                    handleClientError(e);
                }
            } else {
                handleClientError(e);
            }
        } catch (RestClientException e) {
            log.error("Lỗi khi gọi GHN", e);
            throw new AppException(ShippingErrorCode.GHN_SERVICE_ERROR);
        }

        if (response == null || response.getData() == null) {
            log.error("GHN trả dữ liệu không đọc được: {}", response);
            throw new AppException(ShippingErrorCode.GHN_SERVICE_ERROR);
        }

        return response.getData();
    }

    @Override
    public GhnCreateOrderResponse createOrder(OrderPreviewRequest req, BigDecimal codAmount, BigDecimal expectedDeliveryCost, Order order) {
        String fullAddress = String.join(", ", req.detailAddress(), req.wardName(), req.provinceName());
        GhnCreateOrderRequest createOrderBody = GhnCreateOrderRequest.builder()
                .paymentTypeId(PAYMENT_TYPE_ID)
                .requiredNote(req.note())
                .clientOrderCode(order.getId().toString())
                .toName(req.toName())
                .toPhone(req.phone())
                .toAddress(fullAddress)
                .toWardName(req.wardName())
                .toProvinceName(req.provinceName())
                .isNewToAddress(true)
                .serviceTypeId(SERVICE_TYPE_ID)
                .weight(GHN_DEFAULT_ITEM_WEIGHT_GRAM)
                .length(GHN_DEFAULT_LENGTH_CM)
                .width(GHN_DEFAULT_WIDTH_CM)
                .height(GHN_DEFAULT_HEIGHT_CM)
                .codAmount((codAmount.add(expectedDeliveryCost)).intValue())
                .orderValue(codAmount.intValue())
                .items(this.createOrderItems(order))
                .build();

        GhnCreateOrderResponse response = null;

        try {
            response = executeCreate(createOrderBody);
        } catch (HttpClientErrorException e) {
            log.warn("GHN từ chối địa chỉ khi tạo đơn: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            String responseBody = e.getResponseBodyAsString();
            if (responseBody.contains("TO_ADDRESS_CONFLICT") || responseBody.contains("address conflict")) {
                log.info("Thử lại GHN tạo đơn với địa chỉ cấp phường/xã: {}, {}", req.wardName(), req.provinceName());
                try {
                    GhnCreateOrderRequest fallbackBody = createOrderBody.toBuilder()
                            .toAddress(String.join(", ", req.wardName(), req.provinceName()))
                            .build();
                    response = executeCreate(fallbackBody);
                } catch (Exception retryEx) {
                    log.warn("GHN tạo đơn fallback cũng thất bại", retryEx);
                    handleClientError(e);
                }
            } else {
                handleClientError(e);
            }
        } catch (RestClientException e) {
            log.error("Lỗi khi gọi GHN", e);
            throw new AppException(ShippingErrorCode.GHN_SERVICE_ERROR);
        }

        if (response == null) {
            log.error("GHN trả dữ liệu không đọc được: {}", response);
            throw new AppException(ShippingErrorCode.GHN_SERVICE_ERROR);
        }

        return response;
    }

    private GhnApiResponse<GhnCreateOrderResponse> executePreview(GhnCreateOrderRequest body) {
        return ghnRestClient.post()
                .uri("/v2/shipping-order/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private GhnCreateOrderResponse executeCreate(GhnCreateOrderRequest body) {
        return ghnRestClient.post()
                .uri("/v2/shipping-order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GhnCreateOrderResponse.class);
    }

    private void handleClientError(HttpClientErrorException e) {
        if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
            log.error("Token hoặc ShopId GHN không hợp lệ");
            throw new AppException(ShippingErrorCode.GHN_SERVICE_ERROR);
        }
        throw new AppException(ShippingErrorCode.ADDRESS_INVALID);
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

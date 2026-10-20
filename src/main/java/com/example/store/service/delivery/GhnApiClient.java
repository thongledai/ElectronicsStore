package com.example.store.service.delivery;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.store.dto.ghn.GhnApiResponse;
import com.example.store.dto.ghn.GhnCreateOrderResponse;
import com.example.store.dto.ghn.GhnCreateOrderRequest;
import com.example.store.exception.BusinessException;
import com.example.store.enums.DeliveryMessage;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GhnApiClient {
    private static final ParameterizedTypeReference<GhnApiResponse<GhnCreateOrderResponse>> TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final String token;
    private final String shopId;
    private final String baseUrl;

    public GhnApiClient(@Qualifier("ghnRestClient") RestClient restClient,
            @Value("${ghn.token:}") String token,
            @Value("${ghn.shop-id:}") String shopId,
            @Value("${ghn.base-url:https://dev-online-gateway.ghn.vn}") String baseUrl) {
        this.restClient = restClient;
        this.token = token;
        this.shopId = shopId;
        this.baseUrl = baseUrl;
    }

    private String getCreateUri() {
        String base = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://dev-online-gateway.ghn.vn";
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/shiip/public-api")) {
            return base + "/v2/shipping-order/create";
        }
        return base + "/shiip/public-api/v2/shipping-order/create";
    }

    public GhnCreateOrderResponse createOrder(GhnCreateOrderRequest request) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(shopId)) {
            throw new BusinessException(DeliveryMessage.GHN_CONFIG_MISSING.getMessage());
        }
        if (request == null) {
            throw new BusinessException(DeliveryMessage.GHN_ORDER_DATA_REQUIRED.getMessage());
        }
        if (!Boolean.TRUE.equals(request.getIsNewToAddress())) {
            throw new BusinessException(DeliveryMessage.GHN_NEW_ADDRESS_REQUIRED.getMessage());
        }
        if (!StringUtils.hasText(request.getToWardName())
                || !StringUtils.hasText(request.getToProvinceName())
                || !StringUtils.hasText(request.getToAddress())) {
            throw new BusinessException(DeliveryMessage.GHN_ADDRESS_FIELDS_MISSING.getMessage());
        }

        final GhnApiResponse<GhnCreateOrderResponse> response;
        final int[] httpStatus = { 0 };
        try {
            response = restClient.post().uri(getCreateUri()).body(request).exchange((req, res) -> {
                httpStatus[0] = res.getStatusCode().value();
                try {
                    return res.bodyTo(TYPE);
                } catch (RestClientException ex) {
                    log.warn("GHN create response unreadable: httpStatus={}", httpStatus[0]);
                    return null;
                }
            });
        } catch (ResourceAccessException ex) {
            // Có thể GHN đã nhận request nhưng client timeout. Không đổi client_order_code
            // khi đối soát/retry.
            log.warn("GHN create connection/timeout error: {}", safeMessage(ex.getMessage()));
            throw new BusinessException(
                    DeliveryMessage.GHN_RESPONSE_UNCONFIRMED.getMessage());
        } catch (RestClientException ex) {
            log.warn("GHN create HTTP client error: {}", safeMessage(ex.getMessage()));
            throw new BusinessException(DeliveryMessage.GHN_RESPONSE_UNREADABLE.getMessage());
        }

        if (response == null || response.getCode() == null || response.getCode() != 200
                || response.getData() == null || !StringUtils.hasText(response.getData().getOrderCode())) {
            String code = response == null ? "" : safeMessage(response.getCodeMessage());
            String message = response == null ? DeliveryMessage.GHN_BODY_UNREADABLE.getMessage()
                    : safeMessage(response.getMessage());
            log.warn("GHN rejected create order: httpStatus={}, code={}, codeMessage={}, message={}",
                    httpStatus[0], safeMessage(response == null ? null : String.valueOf(response.getCode())),
                    code, message);
            throw new BusinessException(translateError(code, message, httpStatus[0]));
        }
        return response.getData();
    }

    private String translateError(String code, String message, int httpStatus) {
        String normalized = (code + " " + message).toLowerCase(Locale.ROOT)
                .replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ");
        if (normalized.contains("to address conflict") || normalized.contains("address conflict")) {
            return DeliveryMessage.GHN_ADDRESS_CONFLICT.getMessage();
        }
        if (normalized.contains("phone invalid")) {
            return DeliveryMessage.GHN_PHONE_INVALID.getMessage();
        }
        if (normalized.contains("route not found") || normalized.contains("service fee error")) {
            return DeliveryMessage.GHN_ROUTE_NOT_FOUND.getMessage();
        }
        String detail = StringUtils.hasText(message) ? message
                : (StringUtils.hasText(code) ? code : DeliveryMessage.GHN_UNKNOWN_REASON.getMessage());
        return String.format(DeliveryMessage.GHN_REJECTED_PREFIX.getMessage(), httpStatus,
                detail.substring(0, Math.min(detail.length(), 250)));
    }

    private String safeMessage(String value) {
        if (value == null)
            return "";
        return value.replaceAll("[\\r\\n\\t]", " ").substring(0, Math.min(value.length(), 250));
    }
}

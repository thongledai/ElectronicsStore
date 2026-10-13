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
import com.example.store.dto.ghn.GhnCreateOrderData;
import com.example.store.dto.ghn.GhnCreateOrderRequest;
import com.example.store.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GhnApiClient {
    private static final String CREATE_PATH = "/shiip/public-api/v2/shipping-order/create";
    private static final ParameterizedTypeReference<GhnApiResponse<GhnCreateOrderData>> TYPE = new ParameterizedTypeReference<>() {};
    private final RestClient restClient;
    private final String token;
    private final String shopId;

    public GhnApiClient(@Qualifier("ghnRestClient") RestClient restClient,
            @Value("${ghn.token:}") String token,
            @Value("${ghn.shop-id:}") String shopId) {
        this.restClient = restClient;
        this.token = token;
        this.shopId = shopId;
    }

    public GhnCreateOrderData createOrder(GhnCreateOrderRequest request) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(shopId)) {
            throw new BusinessException("Chưa cấu hình GHN_TOKEN hoặc GHN_SHOP_ID.");
        }
        if (request == null) throw new BusinessException("Dữ liệu tạo đơn GHN không được để trống.");

        log.info("Creating GHN shipment: ward='{}', district='{}', province='{}', newAddressModel={}",
                request.getToWardName(), request.getToDistrictName(), request.getToProvinceName(), request.getIsNewToAddress());
        final GhnApiResponse<GhnCreateOrderData> response;
        try {
            response = restClient.post().uri(CREATE_PATH).body(request).exchange((req, res) -> {
                try {
                    return res.bodyTo(TYPE);
                } catch (RestClientException ex) {
                    throw new BusinessException("GHN trả về phản hồi không đọc được (HTTP " + res.getStatusCode().value() + ").");
                }
            });
        } catch (ResourceAccessException ex) {
            log.error("GHN connection failed: {}", ex.getMessage());
            throw new BusinessException("Không kết nối được GHN hoặc GHN phản hồi quá chậm.");
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("GHN request failed: {}", ex.getMessage());
            throw new BusinessException("Có lỗi khi gửi yêu cầu tới GHN.");
        }

        if (response == null || response.getCode() == null || response.getCode() != 200
                || response.getData() == null || !StringUtils.hasText(response.getData().getOrderCode())) {
            String code = response == null ? "" : String.valueOf(response.getCodeMessage());
            String message = response == null ? "GHN không trả dữ liệu" : response.getMessage();
            log.warn("GHN rejected shipment: code={}, code_message={}, message={}",
                    response == null ? null : response.getCode(), code, message);
            throw new BusinessException(translateError(code, message));
        }
        return response.getData();
    }

    private String translateError(String code, String message) {
        String normalized = (code + " " + message).toLowerCase(Locale.ROOT)
                .replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ");
        if (normalized.contains("address conflict")) {
            return "GHN từ chối địa chỉ nhận (TO_ADDRESS_CONFLICT). Hãy chọn lại phường/xã và tỉnh/thành từ danh mục GHN, đồng thời kiểm tra địa chỉ chi tiết.";
        }
        if (code != null && code.equalsIgnoreCase("PHONE_INVALID")) return "Số điện thoại người nhận hoặc cửa hàng không hợp lệ theo GHN.";
        if (code != null && code.equalsIgnoreCase("ROUTE_NOT_FOUND_SERVICE")) return "GHN chưa hỗ trợ tuyến giao hàng này.";
        if (code != null && code.equalsIgnoreCase("SERVICE_NOT_FOUND_CONFIG_FEE")) return "GHN chưa cấu hình phí cho tuyến giao hàng này.";
        String detail = StringUtils.hasText(message) ? message : (StringUtils.hasText(code) ? code : "không rõ nguyên nhân");
        return "GHN từ chối tạo vận đơn: " + detail.substring(0, Math.min(detail.length(), 300));
    }
}

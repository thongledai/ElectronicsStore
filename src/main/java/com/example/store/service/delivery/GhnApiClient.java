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
    private static final ParameterizedTypeReference<GhnApiResponse<GhnCreateOrderData>> TYPE =
            new ParameterizedTypeReference<>() {};

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
        if (request == null) {
            throw new BusinessException("Dữ liệu tạo đơn GHN không được để trống.");
        }
        if (!Boolean.TRUE.equals(request.getIsNewToAddress())) {
            throw new BusinessException("Tạo vận đơn phải dùng mô hình địa chỉ mới của GHN.");
        }
        if (StringUtils.hasText(request.getToDistrictName())) {
            throw new BusinessException("Mô hình địa chỉ mới không được gửi to_district_name.");
        }
        if (!StringUtils.hasText(request.getToWardName())
                || !StringUtils.hasText(request.getToProvinceName())
                || !StringUtils.hasText(request.getToAddress())) {
            throw new BusinessException("Thiếu địa chỉ, tên phường/xã hoặc tên tỉnh/thành GHN.");
        }

        final GhnApiResponse<GhnCreateOrderData> response;
        final int[] httpStatus = {0};
        try {
            response = restClient.post().uri(CREATE_PATH).body(request).exchange((req, res) -> {
                httpStatus[0] = res.getStatusCode().value();
                try {
                    return res.bodyTo(TYPE);
                } catch (RestClientException ex) {
                    log.warn("GHN create response unreadable: httpStatus={}", httpStatus[0]);
                    return null;
                }
            });
        } catch (ResourceAccessException ex) {
            // Có thể GHN đã nhận request nhưng client timeout. Không đổi client_order_code khi đối soát/retry.
            log.warn("GHN create connection/timeout error: {}", safeMessage(ex.getMessage()));
            throw new BusinessException(
                    "Không nhận được phản hồi xác định từ GHN. Không đổi mã tham chiếu đơn; hãy kiểm tra vận đơn trước khi thử lại.");
        } catch (RestClientException ex) {
            log.warn("GHN create HTTP client error: {}", safeMessage(ex.getMessage()));
            throw new BusinessException("Không thể đọc phản hồi tạo vận đơn từ GHN. Kiểm tra log backend.");
        }

        if (response == null || response.getCode() == null || response.getCode() != 200
                || response.getData() == null || !StringUtils.hasText(response.getData().getOrderCode())) {
            String code = response == null ? "" : safeMessage(response.getCodeMessage());
            String message = response == null ? "Không đọc được body phản hồi GHN" : safeMessage(response.getMessage());
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
            return "GHN từ chối địa chỉ (TO_ADDRESS_CONFLICT). Đã ghi lại mã lỗi/message để chẩn đoán; hãy kiểm tra tên tỉnh/phường nguyên văn từ danh mục GHN và payload Preview, không tự đổi địa chỉ giao hàng.";
        }
        if (normalized.contains("phone invalid")) {
            return "Số điện thoại người nhận hoặc cửa hàng không hợp lệ theo GHN.";
        }
        if (normalized.contains("route not found") || normalized.contains("service fee error")) {
            return "GHN không tìm được tuyến/dịch vụ cho địa chỉ và cấu hình hiện tại.";
        }
        String detail = StringUtils.hasText(message) ? message : (StringUtils.hasText(code) ? code : "không rõ nguyên nhân");
        return "GHN từ chối tạo vận đơn (HTTP " + httpStatus + "): " + detail.substring(0, Math.min(detail.length(), 250));
    }

    private String safeMessage(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\r\\n\\t]", " ").substring(0, Math.min(value.length(), 250));
    }
}

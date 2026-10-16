package com.example.store.service.order;

import com.example.store.dto.ghn.*;
import com.example.store.enums.error.ShippingErrorCode;
import com.example.store.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService implements IShipmentService {
    private final RestClient ghnRestClient;
    private final GhnProperties props;

    @Override
    public ShipmentFeeDTO calculateFee(CalculateShipmentFeeRequest req) {
        GhnFeeBody body = new GhnFeeBody(
                req.serviceTypeId(),
                props.fromDistrictId(),
                props.fromWardCode(),
                req.weight());

        GhnFeeResponse response;
        try {
            response = ghnRestClient.post()
                    .uri("/v2/shipping-order/fee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GhnFeeResponse.class);

        } catch (HttpClientErrorException e) {
            // GHN trả 4xx: dữ liệu địa chỉ/dịch vụ không hợp lệ
            log.warn("GHN từ chối địa chỉ: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                // sai token/shopId là lỗi cấu hình của mình, không phải lỗi user
                log.error("Token hoặc ShopId GHN không hợp lệ");
                throw new AppException(ShippingErrorCode.GHN_SERVICE_ERROR);
            }
            throw new AppException(ShippingErrorCode.ADDRESS_INVALID);

        } catch (RestClientException e) {
            // 5xx, timeout, mất kết nối
            log.error("Lỗi khi gọi GHN", e);
            throw new AppException(ShippingErrorCode.GHN_SERVICE_ERROR);
        }

        if (response == null || response.data() == null || response.data().total() == null) {
            log.error("GHN trả dữ liệu không đọc được: {}", response);
            throw new AppException(ShippingErrorCode.GHN_SERVICE_ERROR);
        }

        BigDecimal totalFee = response.data().total();
        response.data().

        return new ShipmentFeeDTO(BigDecimal.valueOf(response.data().total()));
    }
}

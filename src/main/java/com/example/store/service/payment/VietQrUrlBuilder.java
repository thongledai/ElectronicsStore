package com.example.store.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.store.config.PaymentProperties;

import lombok.RequiredArgsConstructor;

/** Dùng chung cấu hình VietQR cho thanh toán đơn hàng và nạp ví. */
@Component
@RequiredArgsConstructor
public class VietQrUrlBuilder {
    private static final String QR_BASE = "https://img.vietqr.io/image/";

    private final PaymentProperties props;

    /**
     * Khi amount là null, QR không gắn sẵn số tiền để người chuyển tự nhập.
     * Mã tham chiếu vẫn được đưa vào nội dung chuyển khoản để webhook đối soát.
     */
    public String build(String reference, BigDecimal amount) {
        String path = QR_BASE + props.getBankBin() + "-" + props.getAccountNumber()
                + "-" + props.getQrTemplate() + ".png";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(path)
                .queryParam("addInfo", reference);
        if (amount != null) {
            builder.queryParam("amount", amount.setScale(0, RoundingMode.HALF_UP).longValue());
        }
        if (StringUtils.hasText(props.getAccountName())) {
            builder.queryParam("accountName", props.getAccountName());
        }
        return builder.build().encode().toUriString();
    }
}

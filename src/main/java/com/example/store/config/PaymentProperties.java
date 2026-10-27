package com.example.store.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/** Cấu hình nhận tiền chuyển khoản VietQR. Bí mật (API key) chỉ lấy từ biến môi trường. */
@Component
@ConfigurationProperties(prefix = "payment")
@Getter
@Setter
public class PaymentProperties {
    /** Mã BIN ngân hàng nhận tiền (MB Bank = 970422). */
    private String bankBin = "970422";
    private String bankName = "MB Bank";
    private String accountNumber = "0905928100";
    /** Tên chủ tài khoản hiển thị trên ảnh QR (không bắt buộc). */
    private String accountName = "";
    /** Template ảnh VietQR Quick Link: compact2, compact, qr_only... */
    private String qrTemplate = "compact2";
    /** Tiền tố mã tham chiếu trong nội dung chuyển khoản, phải khớp "Cấu trúc mã thanh toán" trên SePay. */
    private String referencePrefix = "ES";
    /** Thời gian giữ hàng chờ thanh toán (phút). */
    private int expireMinutes = 30;
    /** API key SePay gửi qua header "Authorization: Apikey ...". Rỗng = từ chối mọi webhook. */
    private String sepayApiKey = "";
    /**
     * true: khi backend xác minh đã nhận tiền thì tự chuyển đơn NOT_PROCESSED -> CONFIRMED.
     * false (mặc định): giữ bước nhân viên bấm xác nhận như quy trình hiện tại; đơn chưa PAID vẫn bị chặn xác nhận.
     */
    private boolean autoConfirmAfterPaid = false;
}

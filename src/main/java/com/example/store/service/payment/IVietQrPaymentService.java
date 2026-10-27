package com.example.store.service.payment;

import java.util.List;
import java.util.UUID;

import com.example.store.dto.payment.SepayWebhookPayload;
import com.example.store.dto.payment.VietQrPaymentDTO;
import com.example.store.entity.Order;

public interface IVietQrPaymentService {

    /** Kết quả xử lý một giao dịch tiền vào do SePay báo về. */
    enum TransferResult {
        PAID, DUPLICATE_EVENT, DUPLICATE_PAYMENT, AMOUNT_MISMATCH, LATE_PAYMENT,
        ORDER_NOT_FOUND, ACCOUNT_MISMATCH, NOT_INCOMING, NOT_PAYABLE
    }

    /** Gắn mã tham chiếu, trạng thái UNPAID và hạn thanh toán cho đơn mới (gọi trong transaction đặt hàng). */
    void initPayment(Order order);

    /** Thông tin QR/trạng thái thanh toán; chỉ chủ đơn mới xem được. */
    VietQrPaymentDTO getPayment(UUID userId, UUID orderId);

    /** Xử lý webhook tiền vào đã được xác thực nguồn gọi. Idempotent theo mã giao dịch của nhà cung cấp. */
    TransferResult handleSepayTransfer(SepayWebhookPayload payload);

    List<UUID> findExpiredOrderIds();

    /** Hủy đơn quá hạn chưa thanh toán và hoàn tồn kho; an toàn khi gọi lặp. */
    void expireOrder(UUID orderId);
}

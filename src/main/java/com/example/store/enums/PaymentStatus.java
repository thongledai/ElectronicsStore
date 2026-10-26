package com.example.store.enums;

/**
 * Trạng thái thanh toán của đơn chuyển khoản VietQR.
 * Tách biệt với trạng thái xử lý đơn (OrderStatusName) và trạng thái giao hàng GHN.
 * Đơn COD để paymentStatus = null.
 */
public enum PaymentStatus {
    /** Đã tạo đơn, đang chờ khách chuyển khoản. */
    UNPAID,
    /** Backend đã xác minh giao dịch tiền vào hợp lệ qua webhook của nhà cung cấp. */
    PAID,
    /** Quá hạn thanh toán, đơn đã bị hủy và tồn kho đã được hoàn lại. */
    EXPIRED,
    /** Khách/nhân viên hủy đơn khi chưa thanh toán. */
    CANCELLED,
    /** Có tiền vào nhưng không khớp (sai số tiền, đơn đã hủy/hết hạn...), cần nhân viên đối soát thủ công. */
    NEEDS_RECONCILIATION
}

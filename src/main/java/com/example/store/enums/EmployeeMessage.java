package com.example.store.enums;

/** Tin nhắn nghiệp vụ trả về từ các API dành cho nhân viên. */
public enum EmployeeMessage {
    ORDER_CONFIRMED("Đã xác nhận đơn hàng.");

    private final String message;

    EmployeeMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

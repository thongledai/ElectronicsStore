package com.example.store.enums;

/** Tin nhắn phản hồi cho các thao tác vận chuyển/GHN ở backend. */
public enum DeliveryMessage {
    GHN_PROVINCES_LOADED("Đã tải danh sách tỉnh/thành GHN."),
    GHN_WARDS_LOADED("Đã tải danh sách phường/xã GHN."),
    GHN_ADDRESS_SAVED("Đã lưu địa chỉ GHN cho đơn hàng."),
    GHN_PREVIEW_CREATED("Đã tạo bản xem trước dữ liệu gửi GHN."),
    GHN_SHIPMENT_ALREADY_EXISTS("Đơn hàng đã có vận đơn GHN, không tạo thêm."),
    GHN_SHIPMENT_CREATED("Tạo vận đơn GHN staging thành công."),
    GHN_SHIPMENT_LOADED("Đã tải thông tin vận chuyển GHN.");

    private final String message;

    DeliveryMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

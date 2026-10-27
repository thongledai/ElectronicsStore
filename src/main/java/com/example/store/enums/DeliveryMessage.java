package com.example.store.enums;

/** Tin nhắn phản hồi cho các thao tác vận chuyển/GHN ở backend. */
public enum DeliveryMessage {
    GHN_PROVINCES_LOADED("Đã tải danh sách tỉnh/thành GHN."),
    GHN_WARDS_LOADED("Đã tải danh sách phường/xã GHN."),
    GHN_ADDRESS_SAVED("Đã lưu địa chỉ GHN cho đơn hàng."),
    GHN_PREVIEW_CREATED("Đã tạo bản xem trước dữ liệu gửi GHN."),
    GHN_SHIPMENT_ALREADY_EXISTS("Đơn hàng đã có vận đơn GHN, không tạo thêm."),
    GHN_SHIPMENT_CREATED("Tạo vận đơn GHN staging thành công."),
    GHN_SHIPMENT_LOADED("Đã tải thông tin vận chuyển GHN."),
    ORDER_NOT_FOUND("Không tìm thấy đơn hàng."),
    ORDER_HAS_SHIPMENT_CONFIRM("Đơn đã có vận đơn, không thể xác nhận lại."),
    ORDER_CONFIRM_STATUS_INVALID("Chỉ xác nhận đơn đang ở trạng thái NOT_PROCESSED."),
    ORDER_HAS_SHIPMENT_ADDRESS_CHANGE("Đơn đã có vận đơn GHN; không thể đổi địa chỉ."),
    ORDER_HAS_SHIPMENT_PREVIEW("Đơn hàng đã có vận đơn GHN, không cần tạo Preview."),
    ORDER_MUST_BE_CONFIRMED("Chỉ thao tác vận đơn khi đơn đã được xác nhận."),
    ORDER_PAYMENT_NOT_CONFIRMED("Đơn chuyển khoản VietQR chưa được xác minh đã nhận tiền, không thể xác nhận hoặc tạo vận đơn."),
    NEW_ADDRESS_MODEL_REQUIRED("GHN_NEW_ADDRESS_MODEL phải là true để dùng danh mục địa chỉ 2 cấp."),
    ADDRESS_INPUT_REQUIRED("Thiếu thông tin địa chỉ giao hàng."),
    ADDRESS_DETAIL_REQUIRED("Nhập số nhà và tên đường trong địa chỉ chi tiết."),
    ADDRESS_DETAIL_TOO_LONG("Địa chỉ chi tiết tối đa 500 ký tự."),
    SHIPMENT_DATA_MISSING_PREFIX("Thiếu dữ liệu tạo vận đơn: %s."),
    SHIPMENT_WEIGHT_INVALID("Tổng khối lượng phải từ 1 đến 19.999 gram cho dịch vụ GHN đang cấu hình."),
    COD_LIMIT_EXCEEDED("Tiền thu hộ vượt giới hạn GHN."),
    SHIPMENT_DIMENSIONS_INVALID("Thông số kiện hàng không hợp lệ theo dịch vụ GHN."),
    PAYMENT_TYPE_INVALID("payment_type_id phải là 1 hoặc 2."),
    STATUS_ROW_MISSING("Thiếu trạng thái đơn hàng %s trong order_status."),
    ADDRESS_PROVINCE_REQUIRED("Chọn tỉnh/thành trước khi tải phường/xã."),
    ADDRESS_CATALOG_INVALID("GHN không trả về danh mục địa chỉ hợp lệ. Kiểm tra token, Shop ID và môi trường."),
    ADDRESS_CATALOG_LOAD_FAILED("Không tải được danh mục địa chỉ GHN. Kiểm tra log backend."),
    GHN_CONFIG_MISSING("Chưa cấu hình GHN_TOKEN hoặc GHN_SHOP_ID."),
    GHN_ORDER_DATA_REQUIRED("Dữ liệu tạo đơn GHN không được để trống."),
    GHN_NEW_ADDRESS_REQUIRED("Tạo vận đơn phải dùng mô hình địa chỉ mới của GHN."),
    GHN_DISTRICT_NOT_ALLOWED("Mô hình địa chỉ mới không được gửi to_district_name."),
    GHN_ADDRESS_FIELDS_MISSING("Thiếu địa chỉ, tên phường/xã hoặc tên tỉnh/thành GHN."),
    GHN_RESPONSE_UNCONFIRMED("Không nhận được phản hồi xác định từ GHN. Không đổi mã tham chiếu đơn; hãy kiểm tra vận đơn trước khi thử lại."),
    GHN_RESPONSE_UNREADABLE("Không thể đọc phản hồi tạo vận đơn từ GHN. Kiểm tra log backend."),
    GHN_ADDRESS_CONFLICT("GHN từ chối địa chỉ (TO_ADDRESS_CONFLICT). Đã ghi lại mã lỗi/message để chẩn đoán; hãy kiểm tra tên tỉnh/phường nguyên văn từ danh mục GHN và payload Preview, không tự đổi địa chỉ giao hàng."),
    GHN_PHONE_INVALID("Số điện thoại người nhận hoặc cửa hàng không hợp lệ theo GHN."),
    GHN_ROUTE_NOT_FOUND("GHN không tìm được tuyến/dịch vụ cho địa chỉ và cấu hình hiện tại."),
    GHN_REJECTED_PREFIX("GHN từ chối tạo vận đơn (HTTP %d): %s"),
    WEBHOOK_PAYLOAD_INVALID("Payload callback GHN thiếu OrderCode, Type hoặc Time."),
    WEBHOOK_SHOP_MISMATCH("ShopID callback không khớp cấu hình GHN."),
    WEBHOOK_TYPE_UNSUPPORTED_PREFIX("Loại callback GHN chưa được hỗ trợ: %s"),
    WEBHOOK_ORDER_NOT_FOUND("Không tìm thấy đơn nội bộ tương ứng callback GHN."),
    WEBHOOK_ORDER_CODE_CONFLICT("ClientOrderCode đã liên kết với một mã vận đơn GHN khác."),
    INVALID_RECIPIENT_NAME("tên người nhận"),
    INVALID_RECIPIENT_PHONE("số điện thoại 10 số"),
    INVALID_ADDRESS("địa chỉ"),
    INVALID_DETAIL_ADDRESS("địa chỉ chi tiết"),
    INVALID_WARD("phường/xã"),
    INVALID_PROVINCE("tỉnh/thành"),
    INVALID_ORDER_ITEMS("sản phẩm trong đơn"),
    GHN_BODY_UNREADABLE("Không đọc được body phản hồi GHN"),
    GHN_UNKNOWN_REASON("không rõ nguyên nhân"),
    ORDER_WALLET_PAYMENT_NOT_CONFIRMED("Đơn thanh toán bằng ví chưa được xác nhận đã trừ đủ tiền, không thể xác nhận hoặc tạo vận đơn.");

    private final String message;

    DeliveryMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

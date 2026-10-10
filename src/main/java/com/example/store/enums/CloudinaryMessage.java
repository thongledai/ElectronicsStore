package com.example.store.enums;

public enum CloudinaryMessage {
    UPLOAD_FAILED("Tải ảnh lên máy chủ thất bại."),
    IMAGE_FILE_LIST_EMPTY("Danh sách file tải lên không được rỗng!"),
    PRODUCT_NOT_FOUND("Không tìm thấy sản phẩm với ID: %s"),
    VARIANT_NOT_FOUND("Không tìm thấy biến thể với ID: %s"),
    VARIANT_NOT_IN_PRODUCT("Biến thể không thuộc về sản phẩm này!"),
    IMAGE_LIMIT_EXCEEDED("Mỗi biến thể chỉ được có tối đa %d hình ảnh (hiện có %d ảnh)!"),
    IMAGE_URL_EMPTY("URL hình ảnh không được để trống!"),
    VARIANT_IMAGE_NOT_FOUND("Không tìm thấy ảnh của biến thể với URL này!");

    private final String message;

    CloudinaryMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
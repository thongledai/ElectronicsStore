package com.example.store.enums;

public enum ValidationMessage {
    EMAIL_REQUIRED("Vui lòng nhập email"),
    EMAIL_INVALID("Địa chỉ email không đúng định dạng"),
    PASSWORD_REQUIRED("Vui lòng nhập mật khẩu"),
    FULL_NAME_REQUIRED("Vui lòng nhập họ và tên"),
    FULL_NAME_MAX_LENGTH("Họ và tên không được vượt quá 200 ký tự"),
    ROLE_REQUIRED("Vui lòng chọn vai trò"),
    PHONE_REQUIRED("Vui lòng nhập số điện thoại"),
    PHONE_INVALID("Số điện thoại Việt Nam phải gồm 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09"),
    PASSWORD_MIN_LENGTH("Mật khẩu phải có ít nhất 6 ký tự"),
    CONFIRM_PASSWORD_REQUIRED("Vui lòng xác nhận mật khẩu"),
    CURRENT_PASSWORD_REQUIRED("Vui lòng nhập mật khẩu hiện tại"),
    NEW_PASSWORD_REQUIRED("Vui lòng nhập mật khẩu mới"),
    CONFIRM_NEW_PASSWORD_REQUIRED("Vui lòng xác nhận mật khẩu mới"),
    OTP_REQUIRED("Vui lòng nhập mã OTP"),
    OTP_SIX_DIGITS("Mã OTP phải gồm 6 chữ số"),
    SET_PASSWORD_CONFIRM_REQUIRED("Vui lòng xác nhận mật khẩu mới"),
    SHIPMENT_WEIGHT_MIN("Khối lượng phải lớn hơn 0"),
    SHIPMENT_WEIGHT_MAX("Khối lượng không được vượt quá 19.999 g"),
    SHIPMENT_LENGTH_MIN("Chiều dài phải lớn hơn 0"),
    SHIPMENT_LENGTH_MAX("Chiều dài không được vượt quá 200 cm"),
    SHIPMENT_WIDTH_MIN("Chiều rộng phải lớn hơn 0"),
    SHIPMENT_WIDTH_MAX("Chiều rộng không được vượt quá 200 cm"),
    SHIPMENT_HEIGHT_MIN("Chiều cao phải lớn hơn 0"),
    SHIPMENT_HEIGHT_MAX("Chiều cao không được vượt quá 200 cm"),

    BRAND_NAME_REQUIRED("Tên thương hiệu không được để trống"),
    BRAND_NAME_MAX_LENGTH("Tên thương hiệu không được vượt quá 100 ký tự"),
    BRAND_LOGO_URL_MAX_LENGTH("Đường dẫn logo không được vượt quá 1000 ký tự"),
    DESCRIPTION_MAX_LENGTH("Mô tả không được vượt quá 1000 ký tự"),
    CATEGORY_NAME_REQUIRED("Tên danh mục không được để trống"),
    CATEGORY_NAME_MAX_LENGTH("Tên danh mục không được vượt quá 100 ký tự"),
    CATEGORY_IMAGE_URL_MAX_LENGTH("Đường dẫn hình ảnh không được vượt quá 1000 ký tự"),

    MIN_PRICE_NON_NEGATIVE("Giá thấp nhất không được nhỏ hơn 0"),
    MAX_PRICE_NON_NEGATIVE("Giá cao nhất không được nhỏ hơn 0"),
    MIN_RATING_NON_NEGATIVE("Đánh giá thấp nhất không được nhỏ hơn 0"),
    MIN_RATING_MAX_FIVE("Đánh giá thấp nhất không được lớn hơn 5"),
    PAGE_NON_NEGATIVE("Số trang không được nhỏ hơn 0"),
    PAGE_SIZE_POSITIVE("Kích thước trang phải lớn hơn 0"),
    PAGE_SIZE_MAX_FIFTY("Kích thước trang không được lớn hơn 50"),

    PRODUCT_NAME_REQUIRED("Tên sản phẩm không được để trống"),
    PRODUCT_NAME_MAX_LENGTH("Tên sản phẩm không được vượt quá 100 ký tự"),
    PRODUCT_DESCRIPTION_REQUIRED("Mô tả sản phẩm không được để trống"),
    PRODUCT_DESCRIPTION_MAX_LENGTH("Mô tả sản phẩm không được vượt quá 1000 ký tự"),
    PRODUCT_CATEGORY_REQUIRED("Danh mục sản phẩm không được để trống"),
    PRODUCT_BRAND_REQUIRED("Thương hiệu sản phẩm không được để trống"),
    PRODUCT_PRICE_REQUIRED("Giá sản phẩm không được để trống"),
    PRODUCT_PRICE_NON_NEGATIVE("Giá sản phẩm không được nhỏ hơn 0"),
    PRODUCT_PROMOTIONAL_PRICE_NON_NEGATIVE("Giá khuyến mãi không được nhỏ hơn 0"),
    PRODUCT_QUANTITY_REQUIRED("Số lượng sản phẩm không được để trống"),
    PRODUCT_QUANTITY_NON_NEGATIVE("Số lượng sản phẩm không được nhỏ hơn 0"),
    PRODUCT_PROMOTIONAL_PRICE_BELOW_PRICE("Giá khuyến mãi phải nhỏ hơn giá sản phẩm"),

    STYLE_NAME_REQUIRED("Tên kiểu thuộc tính không được để trống"),
    STYLE_NAME_MAX_LENGTH("Tên kiểu thuộc tính không được vượt quá 100 ký tự"),
    STYLE_VALUE_NAME_REQUIRED("Tên giá trị thuộc tính không được để trống"),
    STYLE_VALUE_NAME_MAX_LENGTH("Tên giá trị thuộc tính không được vượt quá 100 ký tự"),
    STYLE_VALUE_STYLE_REQUIRED("Vui lòng chọn kiểu thuộc tính"),
    ADDRESS_MAX_LENGTH("Địa chỉ không được vượt quá 1200 ký tự"),
    IMAGE_INVALID("File tải lên phải là hình ảnh (JPG, PNG, WEBP) và có dung lượng tối đa 10MB");

    private final String message;

    ValidationMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

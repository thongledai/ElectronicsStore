package com.example.store.enums;

public enum ValidationMessage {
    EMAIL_REQUIRED("Email is required"),
    EMAIL_INVALID("Invalid email format"),
    PASSWORD_REQUIRED("Password is required"),
    FULL_NAME_REQUIRED("Full name is required"),
    PHONE_REQUIRED("Phone number is required"),
    PHONE_INVALID("Invalid Vietnamese phone number (10 digits starting with 03, 05, 07, 08, 09)"),
    PASSWORD_MIN_LENGTH("Password must be at least 6 characters"),
    CONFIRM_PASSWORD_REQUIRED("Confirm password is required"),
    CURRENT_PASSWORD_REQUIRED("Current password is required"),
    NEW_PASSWORD_REQUIRED("New password is required"),
    CONFIRM_NEW_PASSWORD_REQUIRED("Confirm new password is required"),
    OTP_REQUIRED("OTP code is required"),
    OTP_SIX_DIGITS("OTP code must be 6 digits"),

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
    STYLE_VALUE_STYLE_REQUIRED("Style không được để trống"),
    IMAGE_INVALID("File tải lên phải là hình ảnh (JPG, PNG, WEBP) và có dung lượng tối đa 10MB");

    private final String message;

    ValidationMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

package com.example.store.enums;

public enum BusinessMessage {
    BRAND_NOT_FOUND_ID("Không tìm thấy thương hiệu với ID: %s"),
    BRAND_NOT_FOUND_SLUG("Không tìm thấy thương hiệu với slug: %s"),
    BRAND_NAME_EXISTS("Tên thương hiệu đã tồn tại: %s"),
    BRAND_HAS_PRODUCTS_DEACTIVATE("Không thể xóa mềm thương hiệu vì vẫn còn sản phẩm tham chiếu!"),
    BRAND_HAS_PRODUCTS_DELETE("Không thể xóa vĩnh viễn thương hiệu vì vẫn còn sản phẩm tham chiếu!"),
    USER_NOT_FOUND_EMAIL("User not found with email: %s"),

    CATEGORY_NOT_FOUND_ID("Không tìm thấy danh mục với ID: %s"),
    CATEGORY_NOT_FOUND_SLUG("Không tìm thấy danh mục với slug: %s"),
    CATEGORY_PARENT_NOT_FOUND("Không tìm thấy danh mục cha với ID: %s"),
    CATEGORY_NAME_EXISTS("Tên danh mục đã tồn tại: %s"),
    CATEGORY_PARENT_INACTIVE("Không thể chọn danh mục cha đã bị xóa!"),
    CATEGORY_PARENT_SELF("Danh mục không thể là cha của chính nó!"),
    CATEGORY_PARENT_DESCENDANT("Không thể chọn danh mục con làm danh mục cha (gây vòng lặp)!"),
    CATEGORY_RESTORE_PARENT_INACTIVE("Không thể khôi phục danh mục khi danh mục cha đang bị xóa."),
    CATEGORY_PARENT_RESTORE_INACTIVE("Không thể khôi phục danh mục vì danh mục cha đang bị xóa!"),
    CATEGORY_HAS_PRODUCTS_DELETE("Không thể xóa vĩnh viễn danh mục vì vẫn còn sản phẩm tham chiếu!"),
    CATEGORY_HAS_CHILDREN_DELETE("Không thể xóa vĩnh viễn danh mục vì vẫn còn danh mục con!"),
    CATEGORY_HAS_STYLES_DELETE("Hãy gỡ danh mục khỏi các kiểu thuộc tính trước khi xóa vĩnh viễn!"),
    CATEGORY_HAS_PRODUCTS_DEACTIVATE("Không thể xóa mềm danh mục vì vẫn còn sản phẩm tham chiếu!"),
    CATEGORY_HAS_ACTIVE_CHILDREN("Không thể xóa danh mục vì vẫn còn danh mục con đang hoạt động!"),

    PRODUCT_NOT_FOUND_ID("Không tìm thấy sản phẩm với ID: %s"),
    PRODUCT_NOT_FOUND_SLUG("Không tìm thấy sản phẩm hoặc sản phẩm đã ngừng bán: %s"),
    PRODUCT_NO_ACTIVE_VARIANTS("Sản phẩm chưa có biến thể hoạt động!"),
    PRODUCT_CATEGORY_INACTIVE("Không thể gán sản phẩm vào danh mục đã bị xóa!"),
    PRODUCT_BRAND_INACTIVE("Không thể gán sản phẩm vào thương hiệu đã ngừng hoạt động!"),
    PRODUCT_CATEGORY_INACTIVE_ACTIVATE("Không thể kích hoạt/bán sản phẩm thuộc danh mục đã xóa."),
    PRODUCT_BRAND_INACTIVE_ACTIVATE("Không thể kích hoạt/bán sản phẩm thuộc thương hiệu đang tắt."),
    PRODUCT_VARIANTS_MUST_STOP_SELLING("Hãy tắt trạng thái đang bán của tất cả biến thể trước khi tắt bán sản phẩm."),
    PRODUCT_HAS_REFERENCES_DELETE("Không thể xóa mềm hoặc xóa vĩnh viễn sản phẩm vì còn biến thể, đánh giá hoặc danh sách theo dõi tham chiếu!"),
    PRODUCT_DEPENDENCY_INACTIVE("Không thể kích hoạt/bán sản phẩm thuộc danh mục đã xóa hoặc thương hiệu đang tắt."),

    VARIANT_NOT_FOUND_ID("Không tìm thấy biến thể với ID: %s"),
    VARIANT_PRODUCT_NOT_FOUND("Không tìm thấy sản phẩm với ID: %s"),
    VARIANT_NOT_IN_PRODUCT("Biến thể không thuộc về sản phẩm này!"),
    VARIANT_PRODUCT_INACTIVE_CREATE("Không thể tạo biến thể cho sản phẩm đã ngừng hoạt động!"),
    VARIANT_DEPENDENCY_INVALID_CREATE_SELLING("Không thể tạo biến thể đang bán khi sản phẩm hoặc điều kiện phụ thuộc không hợp lệ."),
    VARIANT_DEPENDENCY_INVALID_UPDATE_SELLING("Không thể bật bán biến thể khi sản phẩm hoặc điều kiện phụ thuộc không hợp lệ."),
    VARIANT_PARENT_INACTIVE_RESTORE("Không thể kích hoạt biến thể khi sản phẩm cha đang bị xóa mềm."),
    VARIANT_REFERENCED_DELETE("Không thể xóa mềm hoặc xóa vĩnh viễn biến thể đang được đơn hàng hoặc giỏ hàng tham chiếu!"),
    VARIANT_PROMOTIONAL_PRICE_INVALID("Giá khuyến mãi phải nhỏ hơn giá gốc của sản phẩm!"),
    VARIANT_STYLE_VALUES_NOT_FOUND("Một số thuộc tính lựa chọn không tồn tại!"),
    STYLE_VALUE_INACTIVE("Giá trị thuộc tính '%s' đã bị xóa."),
    STYLE_INACTIVE("Kiểu thuộc tính '%s' đã bị xóa!"),
    STYLE_VALUE_NOT_IN_CATEGORY("Giá trị '%s' không thuộc danh mục '%s'!"),
    VARIANT_STYLE_VALUE_LIMIT("Mỗi biến thể chỉ được chọn tối đa 1 giá trị cho kiểu '%s'!"),
    VARIANT_PARENT_INACTIVE("Không thể bật biến thể khi sản phẩm cha đang tắt."),
    VARIANT_PARENT_NOT_SELLING("Không thể bật bán biến thể khi sản phẩm cha chưa hoạt động/bán."),
    VARIANT_HAS_INACTIVE_STYLE_VALUE("Biến thể tham chiếu giá trị/kiểu thuộc tính không hoạt động: %s"),

    STYLE_NOT_FOUND_ID("Không tìm thấy kiểu thuộc tính với ID: %s"),
    STYLE_NAME_EXISTS_IN_CATEGORY("Tên kiểu thuộc tính '%s' đã tồn tại trong danh mục được chọn!"),
    STYLE_NAME_EXISTS_WITHOUT_CATEGORY("Tên kiểu thuộc tính '%s' chưa gán danh mục đã tồn tại!"),
    STYLE_VALUE_REFERENCED_DELETE("Không thể xóa vĩnh viễn kiểu thuộc tính vì giá trị '%s' vẫn được biến thể sản phẩm tham chiếu!"),
    STYLE_CATEGORIES_NOT_FOUND("Một hoặc nhiều danh mục không tồn tại."),
    STYLE_CATEGORY_INACTIVE("Không thể gán kiểu thuộc tính vào danh mục đã xóa."),
    STYLE_VALUE_REFERENCED_DEACTIVATE("Không thể tắt kiểu thuộc tính vì giá trị '%s' vẫn được biến thể hoạt động sử dụng."),
    STYLE_HAS_ACTIVE_VALUES("Không thể tắt kiểu thuộc tính vì vẫn còn %d giá trị đang hoạt động. Hãy tắt từng giá trị trước."),

    STYLE_VALUE_NOT_FOUND_ID("Không tìm thấy giá trị thuộc tính với ID: %s"),
    STYLE_INACTIVE_CREATE_VALUE("Không thể tạo giá trị cho kiểu thuộc tính đã bị xóa!"),
    STYLE_INACTIVE_ASSIGN_VALUE("Không thể gán giá trị vào kiểu thuộc tính đã bị xóa!"),
    STYLE_VALUE_REFERENCED_UPDATE("Không thể chuyển giá trị sang kiểu khác khi biến thể đang tham chiếu giá trị này."),
    STYLE_VALUE_IN_USE_DEACTIVATE("Không thể tắt giá trị thuộc tính vì đang được biến thể hoạt động sử dụng. Hãy tắt biến thể trước."),
    STYLE_INACTIVE_RESTORE_VALUE("Không thể khôi phục giá trị khi kiểu thuộc tính đang tắt."),
    STYLE_INACTIVE_RESTORE_STYLE_VALUE("Không thể khôi phục giá trị thuộc tính khi kiểu thuộc tính đang bị xóa!");

    private final String message;

    BusinessMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

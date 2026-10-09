package com.example.store.common.util;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.example.store.entity.StyleValue;

/**
 * Utility class để chuẩn hóa và sinh mã SKU (Stock Keeping Unit) cho biến thể sản phẩm.
 * SKU chuẩn có dạng: [TEN-SAN-PHAM]-[THUOC-TINH-1]-[THUOC-TINH-2]-...
 * Ví dụ: IPHONE-17-PRO-MAX-DEN-8GB-256GB
 */
public final class SkuUtils {

    private static final Pattern DIACRITICAL_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]+");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-+");

    private static final String ALPHANUMERIC_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_SKU_LENGTH = 100;

    private SkuUtils() {
        // Utility class, không cho phép khởi tạo
    }

    /**
     * Chuẩn hóa một chuỗi thành định dạng token SKU (viết hoa, không dấu tiếng Việt, ngăn cách bởi dấu gạch ngang).
     * Ví dụ: "iPhone 17 Pro Max" -> "IPHONE-17-PRO-MAX", "Màu Xanh Đậm" -> "MAU-XANH-DAM", "8 GB" -> "8GB"
     *
     * @param text Chuỗi đầu vào
     * @return Chuỗi đã được chuẩn hóa chữ hoa và không dấu
     */
    public static String normalizeSegment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String normalized = text.trim();

        // Xử lý đơn vị dung lượng / cấu hình như "8 GB", "256 GB", "1 TB" để liền mạch đẹp hơn (VD: 8GB, 256GB)
        normalized = normalized.replaceAll("(?i)(\\d+)\\s*(GB|TB|MB|RAM|HZ|W|V|MAH)", "$1$2");

        // Thay thế chữ đ/Đ tiếng Việt trước khi khử dấu Unicode
        normalized = normalized.replace("đ", "d").replace("Đ", "D");

        // Tách dấu tiếng Việt
        String nfd = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        String noAccents = DIACRITICAL_MARKS.matcher(nfd).replaceAll("");

        // Thay thế ký tự không phải chữ/số thành dấu '-'
        String result = NON_ALPHANUMERIC.matcher(noAccents).replaceAll("-");

        // Gộp nhiều dấu '-' liên tiếp thành 1 dấu '-'
        result = MULTI_HYPHEN.matcher(result).replaceAll("-");

        // Xóa dấu '-' ở đầu và cuối
        if (result.startsWith("-")) {
            result = result.substring(1);
        }
        if (result.endsWith("-")) {
            result = result.substring(0, result.length() - 1);
        }

        return result.toUpperCase(Locale.ROOT);
    }

    /**
     * Tạo tiền tố SKU từ tên sản phẩm.
     *
     * @param productName Tên sản phẩm
     * @return Tiền tố SKU (VD: "IPHONE-17-PRO-MAX")
     */
    public static String prefixFrom(String productName) {
        String prefix = normalizeSegment(productName);
        return prefix.isEmpty() ? "PROD" : prefix;
    }

    /**
     * Sinh chuỗi ngẫu nhiên ký tự viết hoa và số có độ dài mong muốn.
     *
     * @param length Độ dài chuỗi
     * @return Chuỗi ngẫu nhiên (VD: "A7X92K")
     */
    public static String randomSuffix(int length) {
        if (length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALPHANUMERIC_CHARS.length());
            sb.append(ALPHANUMERIC_CHARS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Sinh mã SKU từ tên sản phẩm và danh sách các thuộc tính (màu sắc, cấu hình, bộ nhớ,...).
     * Ví dụ:
     * productName = "iPhone 17 Pro Max"
     * attributes = ["Đen", "8GB", "256GB"]
     * Output: "IPHONE-17-PRO-MAX-DEN-8GB-256GB"
     *
     * @param productName Tên sản phẩm
     * @param attributes Danh sách các thuộc tính / phân loại
     * @return Chuỗi SKU hoàn chỉnh
     */
    public static String generateSku(String productName, Collection<String> attributes) {
        List<String> segments = new ArrayList<>();

        String productPrefix = normalizeSegment(productName);
        if (!productPrefix.isEmpty()) {
            segments.add(productPrefix);
        } else {
            segments.add("PROD");
        }

        if (attributes != null) {
            for (String attr : attributes) {
                String normalizedAttr = normalizeSegment(attr);
                if (!normalizedAttr.isEmpty()) {
                    segments.add(normalizedAttr);
                }
            }
        }

        String sku = String.join("-", segments);
        return limitLength(sku, MAX_SKU_LENGTH);
    }

    /**
     * Sinh mã SKU từ tên sản phẩm và mảng thuộc tính (varargs).
     *
     * @param productName Tên sản phẩm
     * @param attributes Các thuộc tính
     * @return Chuỗi SKU hoàn chỉnh
     */
    public static String generateSku(String productName, String... attributes) {
        if (attributes == null) {
            return generateSku(productName, Collections.emptyList());
        }
        return generateSku(productName, Arrays.asList(attributes));
    }

    /**
     * Sinh mã SKU từ tên sản phẩm và tập hợp các thực thể {@link StyleValue}.
     * Tự động sắp xếp các giá trị thuộc tính để đảm bảo thứ tự thống nhất của SKU.
     *
     * @param productName Tên sản phẩm
     * @param styleValues Danh sách hoặc tập hợp StyleValue
     * @return Mã SKU được tạo ra (VD: "IPHONE-17-PRO-MAX-DEN-8GB-256GB")
     */
    public static String generateSkuFromStyleValues(String productName, Collection<StyleValue> styleValues) {
        if (styleValues == null || styleValues.isEmpty()) {
            return generateSku(productName, Collections.emptyList());
        }

        // Lấy danh sách tên thuộc tính và sắp xếp theo thứ tự nhất quán
        List<String> attrNames = styleValues.stream()
                .filter(Objects::nonNull)
                .map(StyleValue::getName)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        return generateSku(productName, attrNames);
    }

    /**
     * Giới hạn độ dài SKU tối đa cho phép (thường là 100 ký tự theo độ dài cột cơ sở dữ liệu).
     *
     * @param sku Chuỗi SKU
     * @param maxLength Độ dài tối đa
     * @return Chuỗi SKU đã được cắt ngắn an toàn
     */
    public static String limitLength(String sku, int maxLength) {
        if (sku == null) {
            return "";
        }
        if (sku.length() <= maxLength) {
            return sku;
        }
        String truncated = sku.substring(0, maxLength);
        if (truncated.endsWith("-")) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated;
    }
}

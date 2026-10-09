package com.example.store.common.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-+");

    private SlugUtils() {
    }

    // Chuyển đổi tên tiếng Việt / chuỗi bất kỳ sang dạng slug chuẩn SEO.
    public static String toSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        String normalized = input.trim();
        // Thay thế ký tự đ, Đ
        normalized = normalized.replace("đ", "d").replace("Đ", "D");
        // Chuẩn hóa Unicode loại bỏ dấu tiếng Việt
        String nfdNormalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccents = pattern.matcher(nfdNormalized).replaceAll("");

        String slug = WHITESPACE.matcher(noAccents).replaceAll("-");
        slug = NONLATIN.matcher(slug).replaceAll("");
        slug = MULTI_HYPHEN.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ROOT);
        if (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug;
    }

    // Cắt slug theo độ dài tối đa (ví dụ 40 ký tự khi tạo folder Cloudinary).
    public static String limitSlug(String slug, int maxLen) {
        if (slug == null)
            return "";
        if (slug.length() <= maxLen)
            return slug;
        String truncated = slug.substring(0, maxLen);
        if (truncated.endsWith("-")) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated;
    }

    // Trích xuất public_id từ URL Cloudinary.
    // Quy tắc: Bỏ phần trước /upload/, bỏ segment version (v123...), bỏ phần mở
    // rộng file.
    // Ví dụ:
    // https://res.cloudinary.com/demo/image/upload/v1612345678/electronics-store/products/variants/macbook-pro/17123-uuid.webp
    // -> electronics-store/products/variants/macbook-pro/17123-uuid

    public static String extractPublicId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        try {
            int uploadIdx = url.indexOf("/upload/");
            if (uploadIdx == -1) {
                // Không phải URL chuẩn Cloudinary /upload/, fallback lấy từ sau domain
                int lastSlash = url.lastIndexOf('/');
                int dotIdx = url.lastIndexOf('.');
                if (dotIdx > lastSlash) {
                    return url.substring(lastSlash + 1, dotIdx);
                }
                return url.substring(lastSlash + 1);
            }

            String afterUpload = url.substring(uploadIdx + "/upload/".length());
            // Bỏ transform nếu có hoặc version (vd: v12345678/)
            // Nếu bắt đầu bằng v\d+/ thì bỏ qua
            if (afterUpload.matches("^v\\d+/.*")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf('/') + 1);
            } else if (afterUpload.contains("/")) {
                // Kiểm tra có transformation flags trước version không
                String[] segments = afterUpload.split("/");
                int startIdx = 0;
                for (int i = 0; i < segments.length; i++) {
                    if (segments[i].matches("^v\\d+$")) {
                        startIdx = i + 1;
                        break;
                    }
                }
                if (startIdx > 0 && startIdx < segments.length) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = startIdx; i < segments.length; i++) {
                        if (sb.length() > 0)
                            sb.append("/");
                        sb.append(segments[i]);
                    }
                    afterUpload = sb.toString();
                }
            }

            // Bỏ đuôi mở rộng file (.jpg, .png, .webp...)
            int dotIdx = afterUpload.lastIndexOf('.');
            if (dotIdx != -1) {
                afterUpload = afterUpload.substring(0, dotIdx);
            }
            return afterUpload;
        } catch (Exception e) {
            return null;
        }
    }

    // Định dạng số tiền VNĐ: 24.990.000 ₫
    public static String formatVnd(BigDecimal amount) {
        if (amount == null)
            return "0 ₫";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,### ₫", symbols);
        return df.format(amount);
    }
}

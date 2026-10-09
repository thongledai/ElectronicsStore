package com.example.store.dto.respone;

import java.util.UUID;

public record StyleValueDTO(
        UUID id,
        String styleName,   // tên thuộc tính, ví dụ "Màu sắc"
        String value        // giá trị, ví dụ "Đỏ"
) {}

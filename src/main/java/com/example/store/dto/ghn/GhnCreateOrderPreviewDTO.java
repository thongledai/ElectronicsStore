
package com.example.store.dto.ghn;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GhnCreateOrderPreviewDTO {
    private String sourceAddress;
    private Integer provinceId;
    private Integer wardId;
    private String provinceName;
    private String wardName;
    private String addressSource;
    private boolean validated;
    private boolean districtFieldOmitted;
    private GhnCreateOrderRequest payload;
}

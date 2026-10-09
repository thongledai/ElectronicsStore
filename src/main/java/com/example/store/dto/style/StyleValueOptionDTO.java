package com.example.store.dto.style;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StyleValueOptionDTO {
    private UUID id;
    private UUID styleId;
    private String styleName;
    private String name;
}

package com.example.store.dto.category;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryOptionDTO {
    private UUID id;
    private String name;
    private String slug;
    private UUID parentId;
}

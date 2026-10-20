package com.example.store.dto.ghn;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GhnAddressOptionDTO {
    private Integer id;
    private String name;
    private List<String> extensionNames;
}

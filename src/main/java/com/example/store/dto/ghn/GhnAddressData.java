package com.example.store.dto.ghn;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GhnAddressData {
    @JsonProperty("_id") private Integer id;
    private String name;
    private Integer status;
    @JsonProperty("extension_names") private List<String> extensionNames;
}

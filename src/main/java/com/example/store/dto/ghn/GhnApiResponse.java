package com.example.store.dto.ghn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GhnApiResponse<T> {
    private Integer code;
    private String message;
    @JsonProperty("code_message") @JsonAlias("code_message_value") private String codeMessage;
    private T data;
}

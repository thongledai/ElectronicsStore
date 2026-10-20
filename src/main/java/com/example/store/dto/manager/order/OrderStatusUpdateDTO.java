package com.example.store.dto.manager.order;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateDTO {
    @NotBlank(message = "Trạng thái mới không được để trống")
    private String newStatus;
    private String note;
}

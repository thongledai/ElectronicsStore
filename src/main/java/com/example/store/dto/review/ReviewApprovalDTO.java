package com.example.store.dto.review;

import com.example.store.enums.ReviewStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewApprovalDTO {

    @NotNull(message = "Trạng thái không được để trống")
    private ReviewStatus status;

    private String adminReply;
}

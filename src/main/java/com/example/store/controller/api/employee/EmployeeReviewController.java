package com.example.store.controller.api.employee;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.review.ReviewApprovalDTO;
import com.example.store.dto.review.ReviewResponseDTO;
import com.example.store.enums.ApiMessage;
import com.example.store.enums.ReviewStatus;
import com.example.store.security.CustomUserDetails;
import com.example.store.service.review.IReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employee/reviews")
@RequiredArgsConstructor
public class EmployeeReviewController {

    private final IReviewService reviewService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<ReviewResponseDTO>>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponseDTO> reviews = reviewService.getPendingReviews(pageable);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.REVIEW_PENDING_LIST_SUCCESS, reviews));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<ReviewResponseDTO>>> getAllReviews(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponseDTO> reviews = reviewService.getAllReviews(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.REVIEW_ALL_LIST_SUCCESS, reviews));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> approveReview(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewApprovalDTO body
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ApiMessage.LOGIN_REQUIRED));
        }
        String adminReply = body != null ? body.getAdminReply() : null;
        ReviewResponseDTO approved = reviewService.approveReview(id, adminReply, user.getId());
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.REVIEW_APPROVED_SUCCESS, approved));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> rejectReview(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewApprovalDTO body
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ApiMessage.LOGIN_REQUIRED));
        }
        String reason = body != null ? body.getAdminReply() : "Nội dung không phù hợp chính sách đánh giá";
        ReviewResponseDTO rejected = reviewService.rejectReview(id, reason, user.getId());
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.REVIEW_REJECTED_SUCCESS, rejected));
    }

    @GetMapping("/count-pending")
    public ResponseEntity<ApiResponse<Long>> getPendingCount() {
        long count = reviewService.getPendingReviewsCount();
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.REVIEW_PENDING_COUNT_SUCCESS, count));
    }
}

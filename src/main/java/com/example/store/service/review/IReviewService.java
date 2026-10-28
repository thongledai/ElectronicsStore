package com.example.store.service.review;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.store.dto.review.ReviewRequestDTO;
import com.example.store.dto.review.ReviewResponseDTO;
import com.example.store.dto.review.ReviewSummaryDTO;
import com.example.store.enums.ReviewStatus;

public interface IReviewService {

    ReviewResponseDTO createReview(UUID customerId, ReviewRequestDTO request);

    boolean canReview(UUID customerId, UUID productId, UUID orderId);

    Page<ReviewResponseDTO> getProductApprovedReviews(UUID productId, Integer starFilter, Boolean hasImagesOnly, Pageable pageable);

    ReviewSummaryDTO getProductReviewSummary(UUID productId);

    Page<ReviewResponseDTO> getPendingReviews(Pageable pageable);

    Page<ReviewResponseDTO> getAllReviews(ReviewStatus status, Pageable pageable);

    ReviewResponseDTO approveReview(UUID reviewId, String adminReply, UUID employeeId);

    ReviewResponseDTO rejectReview(UUID reviewId, String reason, UUID employeeId);

    long getPendingReviewsCount();
}

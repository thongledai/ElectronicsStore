package com.example.store.controller.api.customer;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.review.ReviewRequestDTO;
import com.example.store.dto.review.ReviewResponseDTO;
import com.example.store.dto.review.ReviewSummaryDTO;
import com.example.store.security.CustomUserDetails;
import com.example.store.service.common.ICloudinaryUploadService;
import com.example.store.service.review.IReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/customer/reviews")
@RequiredArgsConstructor
public class CustomerReviewController {

    private final IReviewService reviewService;
    private final ICloudinaryUploadService cloudinaryUploadService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> submitReview(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ReviewRequestDTO request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để đánh giá."));
        }

        try {
            ReviewResponseDTO response = reviewService.createReview(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Gửi đánh giá thành công! Đánh giá sẽ hiển thị sau khi được duyệt.", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi gửi đánh giá: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống khi gửi đánh giá."));
        }
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadReviewImage(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("file") MultipartFile file
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập."));
        }

        try {
            String imageUrl = cloudinaryUploadService.uploadImage(file, "reviews");
            return ResponseEntity.ok(ApiResponse.success("Tải ảnh thành công", imageUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tải ảnh thất bại: " + e.getMessage()));
        }
    }

    @GetMapping("/can-review")
    public ResponseEntity<ApiResponse<Boolean>> checkCanReview(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam UUID productId,
            @RequestParam UUID orderId
    ) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.success("Chưa đăng nhập", false));
        }
        boolean can = reviewService.canReview(user.getId(), productId, orderId);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra quyền đánh giá", can));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponseDTO>>> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(required = false) Integer star,
            @RequestParam(required = false, defaultValue = "false") Boolean hasImages,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponseDTO> reviews = reviewService.getProductApprovedReviews(productId, star, hasImages, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", reviews));
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ApiResponse<ReviewSummaryDTO>> getProductReviewSummary(
            @PathVariable UUID productId
    ) {
        ReviewSummaryDTO summary = reviewService.getProductReviewSummary(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy tổng hợp đánh giá thành công", summary));
    }
}

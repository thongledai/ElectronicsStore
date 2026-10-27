package com.example.store.service.review.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.review.ReviewRequestDTO;
import com.example.store.dto.review.ReviewResponseDTO;
import com.example.store.dto.review.ReviewSummaryDTO;
import com.example.store.entity.Order;
import com.example.store.entity.OrderItem;
import com.example.store.entity.Product;
import com.example.store.entity.Review;
import com.example.store.entity.ReviewImage;
import com.example.store.entity.User;
import com.example.store.enums.BusinessMessage;
import com.example.store.enums.ReviewStatus;
import com.example.store.exception.BusinessException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.repository.ReviewImageRepository;
import com.example.store.repository.ReviewRepository;
import com.example.store.repository.UserRepository;
import com.example.store.service.review.IReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements IReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ReviewResponseDTO createReview(UUID customerId, ReviewRequestDTO request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.USER_ACCOUNT_NOT_FOUND));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.PRODUCT_NOT_FOUND_ID, request.getProductId()));

        // 1. Kiểm tra tài khoản này đã từng gửi đánh giá cho sản phẩm này chưa
        if (reviewRepository.existsByUserIdAndProductId(customerId, request.getProductId())) {
            throw new BusinessException(BusinessMessage.REVIEW_ALREADY_EXISTS);
        }

        // 2. Xác định đơn hàng gắn kèm (nếu có)
        Order linkedOrder = null;
        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.ORDER_NOT_FOUND));

            if (!order.getUser().getId().equals(customerId)) {
                throw new BusinessException(BusinessMessage.REVIEW_ORDER_NOT_BELONG_TO_USER);
            }

            String statusName = order.getStatus() != null ? order.getStatus().getName() : "";
            if (!"delivered".equalsIgnoreCase(statusName)) {
                throw new BusinessException(BusinessMessage.REVIEW_ORDER_NOT_DELIVERED);
            }

            boolean hasProduct = false;
            for (OrderItem item : order.getItems()) {
                if (item.getVariant() != null && item.getVariant().getProduct() != null) {
                    if (item.getVariant().getProduct().getId().equals(request.getProductId())) {
                        hasProduct = true;
                        break;
                    }
                }
            }
            if (!hasProduct) {
                throw new BusinessException(BusinessMessage.REVIEW_PRODUCT_NOT_IN_ORDER);
            }
            linkedOrder = order;
        } else {
            // Không truyền orderId (đánh giá trực tiếp từ trang chi tiết sản phẩm)
            // Tự động kiểm tra xem khách này đã từng mua sản phẩm này và đã DELIVERED chưa để gắn nhãn "Đã mua tại TechNova"
            List<Order> deliveredOrders = orderRepository.findByUserIdAndStatus_NameIgnoreCaseOrderByCreatedAtDesc(customerId, "delivered");
            for (Order o : deliveredOrders) {
                boolean hasProd = o.getItems().stream().anyMatch(item ->
                        item.getVariant() != null && item.getVariant().getProduct() != null
                                && item.getVariant().getProduct().getId().equals(request.getProductId())
                );
                if (hasProd) {
                    linkedOrder = o;
                    break;
                }
            }
        }

        // 3. Tạo Review (mặc định PENDING chờ nhân viên duyệt)
        Review review = Review.builder()
                .user(customer)
                .product(product)
                .order(linkedOrder)
                .content(request.getContent().trim())
                .stars(request.getStars())
                .status(ReviewStatus.PENDING)
                .build();

        // 4. Thêm ảnh nếu có (tối đa 5 ảnh TGDĐ style)
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (String imgUrl : request.getImageUrls()) {
                if (imgUrl != null && !imgUrl.trim().isEmpty()) {
                    ReviewImage reviewImage = ReviewImage.builder()
                            .review(review)
                            .imageUrl(imgUrl.trim())
                            .build();
                    review.addImage(reviewImage);
                }
            }
        }

        Review savedReview = reviewRepository.save(review);
        ReviewResponseDTO responseDTO = mapToResponseDTO(savedReview);

        // 5. Gửi thông báo realtime đến nhân viên qua WebSocket
        try {
            long pendingCount = reviewRepository.countByStatus(ReviewStatus.PENDING);
            messagingTemplate.convertAndSend("/topic/employee/reviews", (Object) java.util.Map.of(
                    "type", "NEW_REVIEW",
                    "message", "Có 1 đánh giá mới cho sản phẩm: " + product.getName(),
                    "reviewId", savedReview.getId().toString(),
                    "productName", product.getName(),
                    "customerName", customer.getFullName() != null ? customer.getFullName() : customer.getEmail(),
                    "stars", savedReview.getStars(),
                    "pendingCount", pendingCount
            ));
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo WebSocket tới nhân viên: {}", e.getMessage());
        }

        return responseDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canReview(UUID customerId, UUID productId, UUID orderId) {
        if (customerId == null || productId == null) {
            return false;
        }
        return !reviewRepository.existsByUserIdAndProductId(customerId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getProductApprovedReviews(UUID productId, Integer starFilter, Boolean hasImagesOnly, Pageable pageable) {
        Page<Review> page;
        if (starFilter != null && starFilter >= 1 && starFilter <= 5) {
            page = reviewRepository.findByProductIdAndStatusAndStarsOrderByCreatedAtDesc(productId, ReviewStatus.APPROVED, starFilter, pageable);
        } else if (Boolean.TRUE.equals(hasImagesOnly)) {
            page = reviewRepository.findByProductIdAndStatusAndImagesIsNotEmptyOrderByCreatedAtDesc(productId, ReviewStatus.APPROVED, pageable);
        } else {
            page = reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, ReviewStatus.APPROVED, pageable);
        }

        return page.map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryDTO getProductReviewSummary(UUID productId) {
        long totalReviews = reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.APPROVED);
        Double avgRating = reviewRepository.findAverageRatingByProductIdAndStatus(productId, ReviewStatus.APPROVED);
        double finalAvg = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 5.0;

        long count5 = reviewRepository.countByProductIdAndStatusAndStars(productId, ReviewStatus.APPROVED, 5);
        long count4 = reviewRepository.countByProductIdAndStatusAndStars(productId, ReviewStatus.APPROVED, 4);
        long count3 = reviewRepository.countByProductIdAndStatusAndStars(productId, ReviewStatus.APPROVED, 3);
        long count2 = reviewRepository.countByProductIdAndStatusAndStars(productId, ReviewStatus.APPROVED, 2);
        long count1 = reviewRepository.countByProductIdAndStatusAndStars(productId, ReviewStatus.APPROVED, 1);
        long countWithImages = reviewRepository.countByProductIdAndStatusAndImagesIsNotEmpty(productId, ReviewStatus.APPROVED);

        int pct5 = totalReviews > 0 ? (int) Math.round((double) count5 / totalReviews * 100) : 0;
        int pct4 = totalReviews > 0 ? (int) Math.round((double) count4 / totalReviews * 100) : 0;
        int pct3 = totalReviews > 0 ? (int) Math.round((double) count3 / totalReviews * 100) : 0;
        int pct2 = totalReviews > 0 ? (int) Math.round((double) count2 / totalReviews * 100) : 0;
        int pct1 = totalReviews > 0 ? (int) Math.round((double) count1 / totalReviews * 100) : 0;

        return ReviewSummaryDTO.builder()
                .productId(productId)
                .totalReviews(totalReviews)
                .averageRating(finalAvg)
                .count5Star(count5)
                .count4Star(count4)
                .count3Star(count3)
                .count2Star(count2)
                .count1Star(count1)
                .countWithImages(countWithImages)
                .percent5Star(pct5)
                .percent4Star(pct4)
                .percent3Star(pct3)
                .percent2Star(pct2)
                .percent1Star(pct1)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING, pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getAllReviews(ReviewStatus status, Pageable pageable) {
        Page<Review> page;
        if (status != null) {
            page = reviewRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            page = reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::mapToResponseDTO);
    }

    @Override
    @Transactional
    public ReviewResponseDTO approveReview(UUID reviewId, String adminReply, UUID employeeId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.REVIEW_NOT_FOUND_ID, reviewId));

        review.setStatus(ReviewStatus.APPROVED);
        if (adminReply != null && !adminReply.trim().isEmpty()) {
            review.setAdminReply(adminReply.trim());
            review.setRepliedAt(LocalDateTime.now());
            if (employeeId != null) {
                userRepository.findById(employeeId).ifPresent(review::setRepliedBy);
            }
        }

        Review saved = reviewRepository.save(review);
        updateProductRating(review.getProduct().getId());

        try {
            messagingTemplate.convertAndSend("/topic/reviews/product/" + review.getProduct().getId(), (Object) java.util.Map.of(
                    "type", "REVIEW_APPROVED",
                    "productId", review.getProduct().getId().toString(),
                    "reviewId", review.getId().toString()
            ));
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo WebSocket cập nhật review sản phẩm: {}", e.getMessage());
        }

        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public ReviewResponseDTO rejectReview(UUID reviewId, String reason, UUID employeeId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessMessage.REVIEW_NOT_FOUND_ID, reviewId));

        review.setStatus(ReviewStatus.REJECTED);
        if (reason != null && !reason.trim().isEmpty()) {
            review.setAdminReply("Lý do từ chối: " + reason.trim());
            review.setRepliedAt(LocalDateTime.now());
            if (employeeId != null) {
                userRepository.findById(employeeId).ifPresent(review::setRepliedBy);
            }
        }

        Review saved = reviewRepository.save(review);
        updateProductRating(review.getProduct().getId());

        return mapToResponseDTO(saved);
    }

    private void updateProductRating(UUID productId) {
        if (productId == null) return;
        try {
            Double avg = reviewRepository.findAverageRatingByProductIdAndStatus(productId, ReviewStatus.APPROVED);
            productRepository.findById(productId).ifPresent(prod -> {
                double newRating = avg != null ? Math.round(avg * 10.0) / 10.0 : 5.0;
                prod.setRating(newRating);
                productRepository.save(prod);
            });
        } catch (Exception e) {
            log.warn("Không thể cập nhật rating sản phẩm {}: {}", productId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getPendingReviewsCount() {
        return reviewRepository.countByStatus(ReviewStatus.PENDING);
    }

    private ReviewResponseDTO mapToResponseDTO(Review r) {
        List<String> imgUrls = r.getImages() != null
                ? r.getImages().stream().map(ReviewImage::getImageUrl).collect(Collectors.toList())
                : Collections.emptyList();

        String userName = "Khách hàng";
        String avatar = null;
        UUID userId = null;
        if (r.getUser() != null) {
            userId = r.getUser().getId();
            userName = r.getUser().getFullName() != null && !r.getUser().getFullName().trim().isEmpty()
                    ? r.getUser().getFullName()
                    : r.getUser().getEmail();
            avatar = r.getUser().getAvatar();
        }

        String repliedByName = null;
        if (r.getRepliedBy() != null) {
            repliedByName = r.getRepliedBy().getFullName() != null
                    ? r.getRepliedBy().getFullName()
                    : r.getRepliedBy().getEmail();
        }

        // Kiểm tra xem đây có phải là khách hàng đã mua sản phẩm này tại shop không (Badge "✓ Đã mua tại TechNova")
        boolean isPurchased = false;
        if (r.getOrder() != null) {
            String status = r.getOrder().getStatus() != null ? r.getOrder().getStatus().getName() : "";
            isPurchased = "delivered".equalsIgnoreCase(status);
        } else if (r.getUser() != null && r.getProduct() != null) {
            List<Order> deliveredOrders = orderRepository.findByUserIdAndStatus_NameIgnoreCaseOrderByCreatedAtDesc(r.getUser().getId(), "delivered");
            isPurchased = deliveredOrders.stream().anyMatch(o -> o.getItems().stream().anyMatch(item ->
                    item.getVariant() != null && item.getVariant().getProduct() != null
                            && item.getVariant().getProduct().getId().equals(r.getProduct().getId())
            ));
        }

        return ReviewResponseDTO.builder()
                .id(r.getId())
                .productId(r.getProduct() != null ? r.getProduct().getId() : null)
                .productName(r.getProduct() != null ? r.getProduct().getName() : "")
                .orderId(r.getOrder() != null ? r.getOrder().getId() : null)
                .userId(userId)
                .userName(userName)
                .userAvatar(avatar)
                .isPurchased(isPurchased)
                .stars(r.getStars())
                .content(r.getContent())
                .status(r.getStatus())
                .statusDescription(r.getStatus() != null ? r.getStatus().getDescription() : "")
                .imageUrls(imgUrls)
                .adminReply(r.getAdminReply())
                .repliedByName(repliedByName)
                .repliedAt(r.getRepliedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

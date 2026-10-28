package com.example.store.dto.review;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSummaryDTO {

    private UUID productId;
    private Double averageRating;
    private long totalReviews;
    private long count5Star;
    private long count4Star;
    private long count3Star;
    private long count2Star;
    private long count1Star;
    private long countWithImages;

    private int percent5Star;
    private int percent4Star;
    private int percent3Star;
    private int percent2Star;
    private int percent1Star;
}

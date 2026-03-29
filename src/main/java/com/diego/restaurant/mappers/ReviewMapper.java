package com.diego.restaurant.mappers;

import com.diego.restaurant.domain.entities.Review;
import com.diego.restaurant.domain.dtos.ReviewCreateUpdateRequestDto;

public class ReviewMapper {

    public static Review toEntity(ReviewCreateUpdateRequestDto dto) {
        return Review.builder()
                .rating(dto.getRating())
                .comment(dto.getComment())
                .build();
    }
}

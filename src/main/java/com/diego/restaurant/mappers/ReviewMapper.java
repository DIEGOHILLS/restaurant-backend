package com.diego.restaurant.mappers;

import com.diego.restaurant.domain.ReviewCreateUpdateRequest;
import com.diego.restaurant.domain.dtos.ReviewCreateUpdateRequestDto;
import com.diego.restaurant.domain.dtos.ReviewDto;
import com.diego.restaurant.domain.entities.Review;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReviewMapper {

    ReviewCreateUpdateRequest toReviewCreateUpdateRequest(ReviewCreateUpdateRequestDto dto);

    ReviewDto toDto(Review review);

}

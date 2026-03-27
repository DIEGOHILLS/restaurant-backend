package com.diego.restaurant.services.impl;

import com.diego.restaurant.domain.ReviewCreateUpdateRequest;
import com.diego.restaurant.domain.entities.Photo;
import com.diego.restaurant.domain.entities.Restaurant;
import com.diego.restaurant.domain.entities.Review;
import com.diego.restaurant.domain.entities.User;
import com.diego.restaurant.exceptions.RestaurantNotFoundException;
import com.diego.restaurant.exceptions.ReviewNotAllowedException;
import com.diego.restaurant.repositories.RestaurantRepository;
import com.diego.restaurant.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final RestaurantRepository restaurantRepository;

    @Override
    public Review createReview(User author, String restaurantId, ReviewCreateUpdateRequest review) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);

        List<Review> existingReviews = safeReviews(restaurant);

        boolean hasExistingReview = existingReviews.stream()
                .anyMatch(r ->
                        r != null &&
                                r.getWrittenBy() != null &&
                                r.getWrittenBy().getId() != null &&
                                r.getWrittenBy().getId().equals(author.getId())
                );

        if (hasExistingReview) {
            throw new ReviewNotAllowedException("User has already reviewed this restaurant");
        }

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        List<Photo> photos = safePhotoIds(review).stream()
                .map(url -> Photo.builder()
                        .url(url)
                        .uploadDate(now)
                        .build())
                .toList();

        String reviewId = UUID.randomUUID().toString();

        Review reviewToCreate = Review.builder()
                .id(reviewId)
                .content(review.getContent())
                .rating(review.getRating())
                .photos(photos)
                .datePosted(now)
                .lastEdited(now)
                .writtenBy(author)
                .build();

        existingReviews.add(reviewToCreate);
        restaurant.setReviews(existingReviews);

        updateRestaurantAverageRating(restaurant);

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        return getReviewFromRestaurant(reviewId, savedRestaurant)
                .orElseThrow(() -> new RuntimeException("Error retrieving created review"));
    }

    @Override
    public Page<Review> listReviews(String restaurantId, Pageable pageable) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);
        List<Review> reviews = new ArrayList<>(safeReviews(restaurant));

        Sort sort = pageable.getSort();

        if (sort.isSorted()) {
            Sort.Order order = sort.iterator().next();
            String property = order.getProperty();
            boolean isAscending = order.getDirection().isAscending();

            Comparator<Review> comparator = switch (property) {
                case "datePosted" -> Comparator.comparing(
                        Review::getDatePosted,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                case "rating" -> Comparator.comparing(
                        Review::getRating,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                default -> Comparator.comparing(
                        Review::getDatePosted,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
            };

            reviews.sort(isAscending ? comparator : comparator.reversed());
        } else {
            reviews.sort(
                    Comparator.comparing(
                            Review::getDatePosted,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    )
            );
        }

        int start = (int) pageable.getOffset();

        if (start >= reviews.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, reviews.size());
        }

        int end = Math.min(start + pageable.getPageSize(), reviews.size());

        return new PageImpl<>(reviews.subList(start, end), pageable, reviews.size());
    }

    @Override
    public Optional<Review> getReview(String restaurantId, String reviewId) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);
        return getReviewFromRestaurant(reviewId, restaurant);
    }

    private static Optional<Review> getReviewFromRestaurant(String reviewId, Restaurant restaurant) {
        return safeReviews(restaurant).stream()
                .filter(r -> r != null && reviewId.equals(r.getId()))
                .findFirst();
    }

    @Override
    public Review updateReview(User author, String restaurantId, String reviewId, ReviewCreateUpdateRequest review) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);

        String authorId = author.getId();
        Review existingReview = getReviewFromRestaurant(reviewId, restaurant)
                .orElseThrow(() -> new ReviewNotAllowedException("Review does not exist"));

        if (
                existingReview.getWrittenBy() == null ||
                        existingReview.getWrittenBy().getId() == null ||
                        !authorId.equals(existingReview.getWrittenBy().getId())
        ) {
            throw new ReviewNotAllowedException("Cannot update another user's review");
        }

        if (
                existingReview.getDatePosted() != null &&
                        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                                .isAfter(existingReview.getDatePosted().plusHours(48))
        ) {
            throw new ReviewNotAllowedException("Review can no longer be edited");
        }

        existingReview.setContent(review.getContent());
        existingReview.setRating(review.getRating());
        existingReview.setLastEdited(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        existingReview.setPhotos(
                safePhotoIds(review).stream()
                        .map(photoId -> Photo.builder()
                                .url(photoId)
                                .uploadDate(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                                .build())
                        .toList()
        );

        List<Review> updatedReviews = safeReviews(restaurant).stream()
                .filter(r -> r != null && !reviewId.equals(r.getId()))
                .collect(Collectors.toCollection(ArrayList::new));

        updatedReviews.add(existingReview);

        restaurant.setReviews(updatedReviews);

        updateRestaurantAverageRating(restaurant);

        restaurantRepository.save(restaurant);

        return existingReview;
    }

    @Override
    public void deleteReview(String restaurantId, String reviewId) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);

        List<Review> filteredReviews = safeReviews(restaurant).stream()
                .filter(r -> r != null && !reviewId.equals(r.getId()))
                .collect(Collectors.toCollection(ArrayList::new));

        restaurant.setReviews(filteredReviews);

        updateRestaurantAverageRating(restaurant);

        restaurantRepository.save(restaurant);
    }

    private Restaurant getRestaurantOrThrow(String restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException("Restaurant with id not found: " + restaurantId)
                );
    }

    private void updateRestaurantAverageRating(Restaurant restaurant) {
        List<Review> reviews = safeReviews(restaurant);

        if (reviews.isEmpty()) {
            restaurant.setAverageRating(0.0f);
            return;
        }

        double averageRating = reviews.stream()
                .filter(r -> r != null && r.getRating() != null)
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);

        restaurant.setAverageRating((float) averageRating);
    }

    private static List<Review> safeReviews(Restaurant restaurant) {
        if (restaurant.getReviews() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(restaurant.getReviews());
    }

    private static List<String> safePhotoIds(ReviewCreateUpdateRequest review) {
        if (review == null || review.getPhotoIds() == null) {
            return List.of();
        }
        return review.getPhotoIds();
    }
}
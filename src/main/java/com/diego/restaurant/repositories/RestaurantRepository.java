package com.diego.restaurant.repositories;

import com.diego.restaurant.domain.entities.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    List<Restaurant> findByNameContainingIgnoreCase(String name);

    List<Restaurant> findByCuisineContainingIgnoreCase(String cuisine);

}

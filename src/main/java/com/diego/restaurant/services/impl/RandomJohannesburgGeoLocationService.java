package com.diego.restaurant.services.impl;

import com.diego.restaurant.domain.GeoLocation;
import com.diego.restaurant.domain.entities.Address;
import com.diego.restaurant.services.GeoLocationService;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class RandomJohannesburgGeoLocationService implements GeoLocationService {

    private static final float MIN_LATITUDE = -26.320f;
    private static final float MAX_LATITUDE = -25.900f;
    private static final float MIN_LONGITUDE = 27.870f;
    private static final float MAX_LONGITUDE = 28.280f;

    @Override
    public GeoLocation geoLocate(Address address) {
        Random random = new Random();

        double latitude = MIN_LATITUDE + random.nextDouble() * (MAX_LATITUDE - MIN_LATITUDE);
        double longitude = MIN_LONGITUDE + random.nextDouble() * (MAX_LONGITUDE - MIN_LONGITUDE);

        return GeoLocation.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
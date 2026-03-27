package com.diego.restaurant.services;

import com.diego.restaurant.domain.GeoLocation;
import com.diego.restaurant.domain.entities.Address;

public interface GeoLocationService {
    GeoLocation geoLocate(Address address);
}

package com.diego.restaurant.domain.entities;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeRange {

    private LocalTime openTime;
    private LocalTime closeTime;
}

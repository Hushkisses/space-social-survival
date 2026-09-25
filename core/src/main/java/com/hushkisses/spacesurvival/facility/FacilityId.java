package com.hushkisses.spacesurvival.facility;

import java.util.Objects;

public record FacilityId(String value) {

    public FacilityId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("FacilityId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

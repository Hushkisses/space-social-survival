package com.hushkisses.spacesurvival.facility.action;

import java.util.Objects;

public record FacilityActionId(String value) {
    public FacilityActionId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("FacilityActionId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

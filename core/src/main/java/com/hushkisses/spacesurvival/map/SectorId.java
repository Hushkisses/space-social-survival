package com.hushkisses.spacesurvival.map;

import java.util.Objects;

public record SectorId(String value) {

    public SectorId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("SectorId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

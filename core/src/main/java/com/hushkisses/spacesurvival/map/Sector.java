package com.hushkisses.spacesurvival.map;

import java.util.Objects;

public record Sector(SectorId id, String displayName) {

    public Sector {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}

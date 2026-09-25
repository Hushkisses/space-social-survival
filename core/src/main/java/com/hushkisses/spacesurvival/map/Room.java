package com.hushkisses.spacesurvival.map;

import java.util.Objects;

public record Room(RoomId id, SectorId sectorId, String displayName) {

    public Room {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sectorId, "sectorId");
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}

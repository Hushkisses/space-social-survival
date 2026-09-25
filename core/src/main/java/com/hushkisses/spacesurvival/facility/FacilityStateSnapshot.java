package com.hushkisses.spacesurvival.facility;

import java.util.Objects;

public record FacilityStateSnapshot(
        FacilityId id,
        FacilityType type,
        String displayName,
        FacilityStatus status
) {

    public FacilityStateSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(status, "status");
    }
}

package com.hushkisses.spacesurvival.resource.node;

import com.hushkisses.spacesurvival.facility.FacilityId;
import java.util.Objects;

public record ResourceSpawnSlot(String id, FacilityId facilityId) {
    public ResourceSpawnSlot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(facilityId, "facilityId");
        if (id.isBlank()) throw new IllegalArgumentException("id");
    }
}

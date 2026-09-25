package com.hushkisses.spacesurvival.resource.node;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.resource.ResourceType;
import java.util.Objects;

public record ResourceNode(
        ResourceNodeId id,
        ResourceType resourceType,
        int quantity,
        String spawnSlotId,
        FacilityId facilityId
) {
    public ResourceNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(spawnSlotId, "spawnSlotId");
        Objects.requireNonNull(facilityId, "facilityId");
        if (quantity < 1) throw new IllegalArgumentException("quantity");
    }
}

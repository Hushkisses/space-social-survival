package com.hushkisses.spacesurvival.paper.facility;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.facility.action.FacilityActionId;

import java.util.Map;
import java.util.Objects;

public record FacilityMenuSession(
        FacilityId facilityId,
        Map<Integer, FacilityActionId> slotActions
) {
    public FacilityMenuSession {
        Objects.requireNonNull(facilityId, "facilityId");
        slotActions = Map.copyOf(slotActions);
    }
}

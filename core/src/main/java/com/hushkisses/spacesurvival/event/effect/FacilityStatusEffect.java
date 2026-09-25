package com.hushkisses.spacesurvival.event.effect;

import com.hushkisses.spacesurvival.event.GameEventContext;
import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.facility.FacilityStatus;

import java.util.Objects;

public record FacilityStatusEffect(
        FacilityId facilityId,
        FacilityStatus status
) implements EventEffect {
    public FacilityStatusEffect {
        Objects.requireNonNull(facilityId, "facilityId");
        Objects.requireNonNull(status, "status");
    }

    @Override
    public void apply(GameEventContext context) {
        context.facilities().require(facilityId).setStatus(status);
    }
}

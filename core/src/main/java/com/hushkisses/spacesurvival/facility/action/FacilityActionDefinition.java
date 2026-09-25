package com.hushkisses.spacesurvival.facility.action;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.role.RoleCapability;

import java.util.Objects;
import java.util.Optional;

public record FacilityActionDefinition(
        FacilityActionId id,
        FacilityId facilityId,
        String displayName,
        FacilityActionTier tier,
        RoleCapability requiredCapability
) {
    public FacilityActionDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(facilityId, "facilityId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(tier, "tier");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }

    public Optional<RoleCapability> requiredCapabilityOptional() {
        return Optional.ofNullable(requiredCapability);
    }
}

package com.hushkisses.spacesurvival.facility.action;

import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.role.RoleCapability;

import java.util.Objects;
import java.util.Set;

public final class FacilityActionAccessPolicy {

    public FacilityActionAccessDecision evaluate(
            FacilityStatus status,
            FacilityActionDefinition action,
            Set<RoleCapability> capabilities
    ) {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(capabilities, "capabilities");

        if (status == FacilityStatus.OFFLINE) {
            return FacilityActionAccessDecision.deny(
                    FacilityActionAccessDecision.DenialReason.FACILITY_OFFLINE);
        }
        if (status == FacilityStatus.QUARANTINED) {
            return FacilityActionAccessDecision.deny(
                    FacilityActionAccessDecision.DenialReason.FACILITY_QUARANTINED);
        }
        if (status == FacilityStatus.DAMAGED
                && action.tier() == FacilityActionTier.ADVANCED) {
            return FacilityActionAccessDecision.deny(
                    FacilityActionAccessDecision.DenialReason.FACILITY_DAMAGED_ADVANCED_UNAVAILABLE);
        }

        RoleCapability required = action.requiredCapability();
        if (required != null && !capabilities.contains(required)) {
            return FacilityActionAccessDecision.deny(
                    FacilityActionAccessDecision.DenialReason.MISSING_ROLE_CAPABILITY);
        }
        return FacilityActionAccessDecision.allow();
    }
}

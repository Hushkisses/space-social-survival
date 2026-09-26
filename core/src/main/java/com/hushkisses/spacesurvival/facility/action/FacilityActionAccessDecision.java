package com.hushkisses.spacesurvival.facility.action;

public record FacilityActionAccessDecision(boolean allowed, DenialReason denialReason) {
    public static FacilityActionAccessDecision allow() {
        return new FacilityActionAccessDecision(true, null);
    }

    public static FacilityActionAccessDecision deny(DenialReason reason) {
        return new FacilityActionAccessDecision(false, reason);
    }

    public enum DenialReason {
        FACILITY_OFFLINE,
        FACILITY_QUARANTINED,
        FACILITY_DAMAGED_ADVANCED_UNAVAILABLE,
        MISSING_ROLE_CAPABILITY,
        MISSING_REQUIRED_EQUIPMENT
    }
}

package com.hushkisses.spacesurvival.facility.engineering;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityRegistry;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.ship.ShipMetric;
import com.hushkisses.spacesurvival.ship.ShipState;

import java.util.Objects;

public final class EngineeringFacilityService {
    private final FacilityRegistry facilities;
    private final ShipState ship;

    public EngineeringFacilityService(FacilityRegistry facilities, ShipState ship) {
        this.facilities = Objects.requireNonNull(facilities);
        this.ship = Objects.requireNonNull(ship);
    }

    public EngineeringDiagnosis diagnose() {
        return new EngineeringDiagnosis(
                facilities.require(DefaultFacilityCatalog.ENGINEERING).status(),
                ship.snapshot()
        );
    }

    public int adjust(ShipMetric metric, int delta) {
        Objects.requireNonNull(metric);
        ensureOperational();
        if (metric == ShipMetric.OXYGEN) {
            throw new IllegalArgumentException("Engineering does not directly adjust oxygen");
        }
        int next = Math.max(0, Math.min(100, ship.value(metric) + delta));
        ship.set(metric, next);
        return next;
    }

    private void ensureOperational() {
        FacilityStatus status = facilities.require(DefaultFacilityCatalog.ENGINEERING).status();
        if (status == FacilityStatus.OFFLINE || status == FacilityStatus.QUARANTINED) {
            throw new IllegalStateException("Engineering facility is unavailable");
        }
    }
}

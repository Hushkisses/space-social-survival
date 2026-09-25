package com.hushkisses.spacesurvival.event;

import com.hushkisses.spacesurvival.facility.FacilityRegistry;
import com.hushkisses.spacesurvival.resource.ResourceLedger;
import com.hushkisses.spacesurvival.ship.ShipState;

import java.util.Objects;

public record GameEventContext(
        ShipState shipState,
        FacilityRegistry facilities,
        ResourceLedger resources,
        GameEventRuntimeState runtimeState
) {
    public GameEventContext {
        Objects.requireNonNull(shipState, "shipState");
        Objects.requireNonNull(facilities, "facilities");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(runtimeState, "runtimeState");
    }
}

package com.hushkisses.spacesurvival.event;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityRegistry;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.resource.ResourceLedger;
import com.hushkisses.spacesurvival.resource.ResourceType;
import com.hushkisses.spacesurvival.ship.ShipState;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class EventSystemsBatchTest {

    private static GameEventContext context() {
        ResourceLedger resources = new ResourceLedger();
        resources.shared().add(ResourceType.POWER_CELLS, 2);

        return new GameEventContext(
                ShipState.healthy(),
                DefaultFacilityCatalog.createRegistry(),
                resources,
                new GameEventRuntimeState()
        );
    }

    @Test
    void smallEventAppliesEffectAndRecordsHistory() {
        GameEventRegistry registry = DefaultGameEventCatalog.createRegistry();
        GameEventContext context = context();

        new GameEventEngine().trigger(
                registry.require(new GameEventId("local_oxygen_drop")),
                context
        );

        assertEquals(95, context.shipState().oxygen());
        assertEquals(1, context.runtimeState().history().size());
    }

    @Test
    void majorPowerFailureChangesShipAndFacility() {
        GameEventRegistry registry = DefaultGameEventCatalog.createRegistry();
        GameEventContext context = context();

        new GameEventEngine().trigger(
                registry.require(new GameEventId("total_power_failure")),
                context
        );

        assertEquals(0, context.shipState().power());
        assertEquals(
                FacilityStatus.OFFLINE,
                context.facilities().require(DefaultFacilityCatalog.ENGINEERING).status()
        );
        assertTrue(context.runtimeState().hasFlag("total_power_failure"));
    }

    @Test
    void infectionEventQuarantinesMedicalWithoutImplementingInfectionModelYet() {
        GameEventContext context = context();

        new GameEventEngine().trigger(
                DefaultGameEventCatalog.createRegistry()
                        .require(new GameEventId("mass_infection")),
                context
        );

        assertEquals(
                FacilityStatus.QUARANTINED,
                context.facilities().require(DefaultFacilityCatalog.MEDICAL).status()
        );
        assertTrue(context.runtimeState().hasFlag("mass_infection"));
    }

    @Test
    void selectorIsSeeded() {
        GameEventRegistry registry = DefaultGameEventCatalog.createRegistry();
        GameEventSelector selector = new GameEventSelector();

        assertEquals(
                selector.select(registry, GameEventScale.SMALL, new Random(55)).id(),
                selector.select(registry, GameEventScale.SMALL, new Random(55)).id()
        );
    }
}

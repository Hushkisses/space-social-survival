package com.hushkisses.spacesurvival.facility;

import com.hushkisses.spacesurvival.facility.action.*;
import com.hushkisses.spacesurvival.facility.engineering.EngineeringFacilityService;
import com.hushkisses.spacesurvival.facility.medical.MedicalCondition;
import com.hushkisses.spacesurvival.facility.medical.MedicalFacilityService;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.RoleCapability;
import com.hushkisses.spacesurvival.ship.ShipMetric;
import com.hushkisses.spacesurvival.ship.ShipState;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FacilitySystemsBatchTest {

    @Test
    void actionCatalogContainsAllSixFacilities() {
        FacilityActionRegistry registry = DefaultFacilityActionCatalog.createRegistry();

        assertTrue(registry.size() >= 30);
        assertFalse(registry.forFacility(DefaultFacilityCatalog.BRIDGE).isEmpty());
        assertFalse(registry.forFacility(DefaultFacilityCatalog.ENGINEERING).isEmpty());
        assertFalse(registry.forFacility(DefaultFacilityCatalog.MEDICAL).isEmpty());
        assertFalse(registry.forFacility(DefaultFacilityCatalog.RESEARCH).isEmpty());
        assertFalse(registry.forFacility(DefaultFacilityCatalog.CARGO).isEmpty());
        assertFalse(registry.forFacility(DefaultFacilityCatalog.HABITATION).isEmpty());
    }

    @Test
    void advancedActionNeedsCapabilityAndHealthyFacility() {
        FacilityActionAccessPolicy policy = new FacilityActionAccessPolicy();
        FacilityActionDefinition action = DefaultFacilityActionCatalog.createRegistry()
                .find(new FacilityActionId("engineering.diagnose"))
                .orElseThrow();

        assertFalse(policy.evaluate(
                FacilityStatus.NORMAL,
                action,
                Set.of()
        ).allowed());

        assertTrue(policy.evaluate(
                FacilityStatus.NORMAL,
                action,
                Set.of(RoleCapability.DIAGNOSE_FAULT)
        ).allowed());

        assertFalse(policy.evaluate(
                FacilityStatus.DAMAGED,
                action,
                Set.of(RoleCapability.DIAGNOSE_FAULT)
        ).allowed());
    }

    @Test
    void engineeringAdjustsShipMetricsWhenOperational() {
        FacilityRegistry facilities = DefaultFacilityCatalog.createRegistry();
        ShipState ship = new ShipState(40, 100, 50, 60);
        EngineeringFacilityService engineering = new EngineeringFacilityService(facilities, ship);

        assertEquals(55, engineering.adjust(ShipMetric.POWER, 15));
        assertEquals(70, engineering.adjust(ShipMetric.HULL, 20));
        assertThrows(IllegalArgumentException.class,
                () -> engineering.adjust(ShipMetric.OXYGEN, 5));

        facilities.require(DefaultFacilityCatalog.ENGINEERING)
                .setStatus(FacilityStatus.OFFLINE);
        assertThrows(IllegalStateException.class,
                () -> engineering.adjust(ShipMetric.REACTOR, 5));
    }

    @Test
    void medicalServiceTreatsAndClearsGenericConditions() {
        FacilityRegistry facilities = DefaultFacilityCatalog.createRegistry();
        MedicalFacilityService medical = new MedicalFacilityService(facilities);
        PlayerId player = PlayerId.of(UUID.randomUUID());

        medical.patient(player).setHealthPercent(50);
        medical.patient(player).addCondition(MedicalCondition.WOUNDED);

        assertEquals(70, medical.treat(player, 20));
        assertTrue(medical.clearCondition(player, MedicalCondition.WOUNDED));

        facilities.require(DefaultFacilityCatalog.MEDICAL)
                .setStatus(FacilityStatus.QUARANTINED);
        assertThrows(IllegalStateException.class, () -> medical.treat(player, 10));
    }
}

package com.hushkisses.spacesurvival.facility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FacilityFrameworkTest {

    @Test
    void defaultCatalogContainsSixFacilities() {
        FacilityRegistry registry = DefaultFacilityCatalog.createRegistry();
        assertEquals(6, registry.size());
        assertEquals(FacilityStatus.NORMAL,
                registry.require(DefaultFacilityCatalog.BRIDGE).status());
    }

    @Test
    void statusChangesAndResetRestoresNormal() {
        FacilityRegistry registry = DefaultFacilityCatalog.createRegistry();
        registry.require(DefaultFacilityCatalog.MEDICAL)
                .setStatus(FacilityStatus.QUARANTINED);
        assertEquals(FacilityStatus.QUARANTINED,
                registry.require(DefaultFacilityCatalog.MEDICAL).status());

        registry.resetAll();

        assertTrue(registry.snapshots().stream()
                .allMatch(snapshot -> snapshot.status() == FacilityStatus.NORMAL));
    }

    @Test
    void duplicateIdIsRejected() {
        FacilityRegistry registry = new FacilityRegistry();
        FacilityDefinition definition = DefaultFacilityCatalog.createDefinitions().getFirst();
        registry.register(definition);
        assertThrows(IllegalArgumentException.class, () -> registry.register(definition));
    }

    @Test
    void snapshotIsIndependent() {
        FacilityState state = new FacilityState(
                DefaultFacilityCatalog.createDefinitions().getFirst()
        );
        FacilityStateSnapshot snapshot = state.snapshot();
        state.setStatus(FacilityStatus.DAMAGED);

        assertEquals(FacilityStatus.NORMAL, snapshot.status());
        assertEquals(FacilityStatus.DAMAGED, state.status());
    }
}

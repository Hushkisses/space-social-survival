package com.hushkisses.spacesurvival.role;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoleFrameworkTest {

    @Test
    void defaultCatalogContainsSixRoles() {
        RoleRegistry registry = DefaultRoleCatalog.createRegistry();

        assertEquals(6, registry.size());
        assertEquals(
                List.of(
                        DefaultRoleCatalog.ENGINEER,
                        DefaultRoleCatalog.MEDIC,
                        DefaultRoleCatalog.SECURITY,
                        DefaultRoleCatalog.RESEARCHER,
                        DefaultRoleCatalog.NAV_COMMS,
                        DefaultRoleCatalog.CARGO_MAINTENANCE
                ),
                registry.all().stream().map(RoleDefinition::id).toList()
        );
    }

    @Test
    void engineerMetadataMatchesDesign() {
        RoleDefinition engineer = DefaultRoleCatalog.createRegistry()
                .require(DefaultRoleCatalog.ENGINEER);

        assertEquals("엔지니어", engineer.displayName());
        assertTrue(engineer.hasPassive(RolePassive.REPAIR_EFFICIENCY));
        assertTrue(engineer.hasCapability(RoleCapability.DIAGNOSE_FAULT));
        assertTrue(engineer.hasCapability(RoleCapability.ADVANCED_REPAIR));
        assertTrue(engineer.hasCapability(RoleCapability.REDISTRIBUTE_POWER));
    }

    @Test
    void medicMetadataMatchesDesign() {
        RoleDefinition medic = DefaultRoleCatalog.createRegistry()
                .require(DefaultRoleCatalog.MEDIC);

        assertTrue(medic.hasPassive(RolePassive.TREATMENT_EFFICIENCY));
        assertTrue(medic.hasCapability(RoleCapability.PRECISE_INFECTION_TEST));
        assertTrue(medic.hasCapability(RoleCapability.ADVANCED_TREATMENT));
        assertTrue(medic.hasCapability(RoleCapability.SUPPRESS_INFECTION));
    }

    @Test
    void duplicateRoleIdIsRejected() {
        RoleRegistry registry = new RoleRegistry();
        RoleDefinition role = DefaultRoleCatalog.createDefinitions().getFirst();

        registry.register(role);

        assertThrows(IllegalArgumentException.class, () -> registry.register(role));
    }

    @Test
    void invalidCopyLimitIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RoleDefinition(
                        new RoleId("invalid"),
                        "잘못된 직업",
                        "테스트",
                        0,
                        List.of(),
                        List.of()
                )
        );
    }

    @Test
    void allInitialRolesAllowTwoCopiesUntilSelectionBalanceIsFinalized() {
        assertTrue(
                DefaultRoleCatalog.createDefinitions().stream()
                        .allMatch(role -> role.maxCopies() == 2)
        );
    }
}

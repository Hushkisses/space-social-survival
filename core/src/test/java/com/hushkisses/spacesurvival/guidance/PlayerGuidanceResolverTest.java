package com.hushkisses.spacesurvival.guidance;

import com.hushkisses.spacesurvival.ending.ReturnStage;
import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityStateSnapshot;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.facility.FacilityType;
import com.hushkisses.spacesurvival.ship.ShipStateSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerGuidanceResolverTest {

    private final PlayerGuidanceResolver resolver = new PlayerGuidanceResolver();

    @Test
    void lowPowerRoutesPlayersToEngineeringWithPowerCells() {
        PublicProblem problem = resolver.resolve(
                ReturnStage.SURVIVAL_SYSTEMS,
                new ShipStateSnapshot(35, 80, 80, 80),
                normalFacilities()
        );

        assertEquals(DefaultFacilityCatalog.ENGINEERING, problem.targetFacility());
        assertTrue(problem.title().contains("전력"));
        assertTrue(problem.need().contains("전력 셀"));
    }

    @Test
    void unusableEngineeringTakesPriorityOverMetricRepair() {
        List<FacilityStateSnapshot> facilities = normalFacilities().stream()
                .map(facility -> facility.id().equals(DefaultFacilityCatalog.ENGINEERING)
                        ? new FacilityStateSnapshot(
                        facility.id(),
                        facility.type(),
                        facility.displayName(),
                        FacilityStatus.OFFLINE
                )
                        : facility)
                .toList();

        PublicProblem problem = resolver.resolve(
                ReturnStage.SURVIVAL_SYSTEMS,
                new ShipStateSnapshot(20, 80, 80, 80),
                facilities
        );

        assertEquals(DefaultFacilityCatalog.ENGINEERING, problem.targetFacility());
        assertTrue(problem.title().contains("사용할 수 없습니다"));
    }

    @Test
    void navigationStageRoutesTeamToBridgeWhenShipIsStable() {
        PublicProblem problem = resolver.resolve(
                ReturnStage.NAVIGATION,
                new ShipStateSnapshot(80, 80, 80, 80),
                normalFacilities()
        );

        assertEquals(DefaultFacilityCatalog.BRIDGE, problem.targetFacility());
        assertTrue(problem.title().contains("항법"));
    }


    @Test
    void lowOxygenNamesConcreteEngineeringRecoveryAction() {
        PublicProblem problem = resolver.resolve(
                ReturnStage.SURVIVAL_SYSTEMS,
                new ShipStateSnapshot(80, 35, 80, 80),
                normalFacilities()
        );

        assertEquals(DefaultFacilityCatalog.ENGINEERING, problem.targetFacility());
        assertTrue(problem.need().contains("수리 부품"));
        assertTrue(problem.nextAction().contains("산소 계통 복구"));
    }

    @Test
    void damagedCargoNamesConcreteRepairAction() {
        List<FacilityStateSnapshot> facilities = List.of(
                new FacilityStateSnapshot(
                        DefaultFacilityCatalog.BRIDGE,
                        FacilityType.BRIDGE,
                        "함교",
                        FacilityStatus.NORMAL
                ),
                new FacilityStateSnapshot(
                        DefaultFacilityCatalog.ENGINEERING,
                        FacilityType.ENGINEERING,
                        "기관실",
                        FacilityStatus.NORMAL
                ),
                new FacilityStateSnapshot(
                        DefaultFacilityCatalog.CARGO,
                        FacilityType.CARGO,
                        "화물실",
                        FacilityStatus.DAMAGED
                )
        );

        PublicProblem problem = resolver.resolve(
                ReturnStage.SURVIVAL_SYSTEMS,
                new ShipStateSnapshot(80, 80, 80, 80),
                facilities
        );

        assertEquals(DefaultFacilityCatalog.CARGO, problem.targetFacility());
        assertTrue(problem.need().contains("수리 부품"));
        assertTrue(problem.nextAction().contains("화물실 설비 복구"));
    }


    @Test
    void stableSurvivalStageNamesBridgeAdvanceAction() {
        PublicProblem problem = resolver.resolve(
                ReturnStage.SURVIVAL_SYSTEMS,
                new ShipStateSnapshot(80, 80, 80, 80),
                normalFacilities()
        );

        assertEquals(DefaultFacilityCatalog.BRIDGE, problem.targetFacility());
        assertTrue(problem.title().contains("완료"));
        assertTrue(problem.need().contains("함교"));
        assertTrue(problem.nextAction().contains("귀환 절차 시작"));
    }

    private static List<FacilityStateSnapshot> normalFacilities() {
        return List.of(
                new FacilityStateSnapshot(
                        DefaultFacilityCatalog.BRIDGE,
                        FacilityType.BRIDGE,
                        "함교",
                        FacilityStatus.NORMAL
                ),
                new FacilityStateSnapshot(
                        DefaultFacilityCatalog.ENGINEERING,
                        FacilityType.ENGINEERING,
                        "기관실",
                        FacilityStatus.NORMAL
                )
        );
    }
}

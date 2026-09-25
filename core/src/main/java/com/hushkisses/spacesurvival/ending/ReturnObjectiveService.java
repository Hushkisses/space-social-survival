package com.hushkisses.spacesurvival.ending;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityRegistry;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.ship.ShipState;
import com.hushkisses.spacesurvival.ship.ShipStateSnapshot;

import java.util.Objects;

public final class ReturnObjectiveService {

    private final ShipState shipState;
    private final FacilityRegistry facilities;
    private final ReturnRequirements requirements;

    private ReturnStage stage = ReturnStage.SURVIVAL_SYSTEMS;

    public ReturnObjectiveService(
            ShipState shipState,
            FacilityRegistry facilities,
            ReturnRequirements requirements
    ) {
        this.shipState = Objects.requireNonNull(shipState, "shipState");
        this.facilities = Objects.requireNonNull(facilities, "facilities");
        this.requirements = Objects.requireNonNull(requirements, "requirements");
    }

    public ReturnStage stage() {
        return stage;
    }

    public boolean survivalSystemsReady() {
        ShipStateSnapshot ship = shipState.snapshot();
        return ship.power() >= requirements.minPower()
                && ship.oxygen() >= requirements.minOxygen()
                && ship.hull() >= requirements.minHull()
                && ship.reactor() >= requirements.minReactor()
                && usable(DefaultFacilityCatalog.BRIDGE)
                && usable(DefaultFacilityCatalog.ENGINEERING);
    }

    public boolean advanceSurvivalSystems() {
        if (stage != ReturnStage.SURVIVAL_SYSTEMS || !survivalSystemsReady()) {
            return false;
        }
        stage = ReturnStage.NAVIGATION;
        return true;
    }

    public boolean markNavigationReady() {
        if (stage != ReturnStage.NAVIGATION) {
            return false;
        }
        stage = ReturnStage.RETURN_PREPARATION;
        return true;
    }

    public boolean markReturnPreparationReady() {
        if (stage != ReturnStage.RETURN_PREPARATION) {
            return false;
        }
        stage = ReturnStage.FINAL_HOLD;
        return true;
    }

    public boolean complete() {
        if (stage != ReturnStage.FINAL_HOLD) {
            return false;
        }
        stage = ReturnStage.COMPLETED;
        return true;
    }

    public void fail() {
        if (stage != ReturnStage.COMPLETED) {
            stage = ReturnStage.FAILED;
        }
    }

    public boolean isTerminal() {
        return stage == ReturnStage.COMPLETED || stage == ReturnStage.FAILED;
    }

    private boolean usable(com.hushkisses.spacesurvival.facility.FacilityId id) {
        FacilityStatus status = facilities.require(id).status();
        return status != FacilityStatus.OFFLINE && status != FacilityStatus.QUARANTINED;
    }
}

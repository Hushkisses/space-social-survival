package com.hushkisses.spacesurvival.facility.engineering;

import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.ship.ShipStateSnapshot;

public record EngineeringDiagnosis(
        FacilityStatus facilityStatus,
        ShipStateSnapshot shipState
) {
}

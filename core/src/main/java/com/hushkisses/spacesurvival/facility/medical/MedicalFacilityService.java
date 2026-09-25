package com.hushkisses.spacesurvival.facility.medical;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityRegistry;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class MedicalFacilityService {

    private final FacilityRegistry facilities;
    private final Map<PlayerId, PatientMedicalState> patients = new LinkedHashMap<>();

    public MedicalFacilityService(FacilityRegistry facilities) {
        this.facilities = Objects.requireNonNull(facilities, "facilities");
    }

    public PatientMedicalState patient(PlayerId playerId) {
        return patients.computeIfAbsent(
                Objects.requireNonNull(playerId, "playerId"),
                PatientMedicalState::new
        );
    }

    public int treat(PlayerId playerId, int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be positive");
        }
        ensureOperational();

        PatientMedicalState patient = patient(playerId);
        patient.setHealthPercent(Math.min(100, patient.healthPercent() + amount));
        return patient.healthPercent();
    }

    public boolean clearCondition(PlayerId playerId, MedicalCondition condition) {
        ensureOperational();
        return patient(playerId).removeCondition(condition);
    }

    private void ensureOperational() {
        FacilityStatus status = facilities.require(DefaultFacilityCatalog.MEDICAL).status();
        if (status == FacilityStatus.OFFLINE || status == FacilityStatus.QUARANTINED) {
            throw new IllegalStateException("Medical facility is unavailable");
        }
    }
}

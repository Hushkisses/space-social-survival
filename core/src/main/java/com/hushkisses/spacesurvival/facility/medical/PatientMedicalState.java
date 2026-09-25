package com.hushkisses.spacesurvival.facility.medical;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class PatientMedicalState {
    private final PlayerId playerId;
    private int healthPercent = 100;
    private final EnumSet<MedicalCondition> conditions = EnumSet.noneOf(MedicalCondition.class);

    public PatientMedicalState(PlayerId playerId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
    }

    public PlayerId playerId() { return playerId; }

    public int healthPercent() { return healthPercent; }

    public Set<MedicalCondition> conditions() {
        return Collections.unmodifiableSet(conditions);
    }

    public void setHealthPercent(int value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("healthPercent must be between 0 and 100");
        }
        healthPercent = value;
    }

    public void addCondition(MedicalCondition condition) {
        conditions.add(Objects.requireNonNull(condition, "condition"));
    }

    public boolean removeCondition(MedicalCondition condition) {
        return conditions.remove(Objects.requireNonNull(condition, "condition"));
    }
}

package com.hushkisses.spacesurvival.death;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.Objects;
import java.util.Set;

public record InfectedPlayerState(
        PlayerId playerId,
        PostDeathForm form,
        Set<InfectedPostDeathGoal> goals
) {
    public InfectedPlayerState {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(goals, "goals");
        goals = Set.copyOf(goals);
    }

    public boolean canSpeakToLiving() {
        return false;
    }

    public boolean canRepair() {
        return form != PostDeathForm.INFECTED;
    }

    public boolean canUseRadio() {
        return form != PostDeathForm.INFECTED;
    }

    public boolean canManipulateNormalDoors() {
        return form != PostDeathForm.INFECTED;
    }
}

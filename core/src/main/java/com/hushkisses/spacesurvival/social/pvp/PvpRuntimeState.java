package com.hushkisses.spacesurvival.social.pvp;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class PvpRuntimeState {
    private boolean emergencyDeclared;
    private boolean scenarioAllows;
    private boolean specialEventAllows;
    private final Set<PlayerId> securityAuthorized = new LinkedHashSet<>();
    private final Set<PlayerId> confirmedInfected = new LinkedHashSet<>();

    public boolean emergencyDeclared() { return emergencyDeclared; }
    public boolean scenarioAllows() { return scenarioAllows; }
    public boolean specialEventAllows() { return specialEventAllows; }

    public void setEmergencyDeclared(boolean value) { emergencyDeclared = value; }
    public void setScenarioAllows(boolean value) { scenarioAllows = value; }
    public void setSpecialEventAllows(boolean value) { specialEventAllows = value; }

    public void setSecurityAuthorized(PlayerId playerId, boolean value) {
        mutate(securityAuthorized, playerId, value);
    }

    public void setConfirmedInfected(PlayerId playerId, boolean value) {
        mutate(confirmedInfected, playerId, value);
    }

    public boolean isSecurityAuthorized(PlayerId playerId) {
        return securityAuthorized.contains(Objects.requireNonNull(playerId, "playerId"));
    }

    public boolean isConfirmedInfected(PlayerId playerId) {
        return confirmedInfected.contains(Objects.requireNonNull(playerId, "playerId"));
    }

    private static void mutate(Set<PlayerId> set, PlayerId playerId, boolean value) {
        Objects.requireNonNull(playerId, "playerId");
        if (value) set.add(playerId);
        else set.remove(playerId);
    }
}

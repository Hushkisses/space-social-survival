package com.hushkisses.spacesurvival.communication;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class RadioRuntimeState {

    private final Set<PlayerId> radioHolders = new LinkedHashSet<>();
    private boolean longRangeEnabled = true;
    private boolean communicationsOutage;

    public boolean hasRadio(PlayerId playerId) {
        return radioHolders.contains(Objects.requireNonNull(playerId, "playerId"));
    }

    public void setRadio(PlayerId playerId, boolean value) {
        Objects.requireNonNull(playerId, "playerId");
        if (value) radioHolders.add(playerId);
        else radioHolders.remove(playerId);
    }

    public boolean longRangeEnabled() {
        return longRangeEnabled;
    }

    public void setLongRangeEnabled(boolean value) {
        longRangeEnabled = value;
    }

    public boolean communicationsOutage() {
        return communicationsOutage;
    }

    public void setCommunicationsOutage(boolean value) {
        communicationsOutage = value;
    }

    public boolean longRangeAvailable() {
        return longRangeEnabled && !communicationsOutage;
    }

    public Set<PlayerId> radioHolders() {
        return Set.copyOf(radioHolders);
    }

    public void reset() {
        radioHolders.clear();
        longRangeEnabled = true;
        communicationsOutage = false;
    }
}

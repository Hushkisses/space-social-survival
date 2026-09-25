package com.hushkisses.spacesurvival.social.sanction;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SanctionStateRegistry {
    private final Map<PlayerId, PlayerSanctionState> states = new LinkedHashMap<>();

    public PlayerSanctionState state(PlayerId playerId) {
        return states.computeIfAbsent(
                Objects.requireNonNull(playerId, "playerId"),
                ignored -> new PlayerSanctionState()
        );
    }
}

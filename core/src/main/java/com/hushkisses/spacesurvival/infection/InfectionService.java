package com.hushkisses.spacesurvival.infection;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.*;

public final class InfectionService {

    private final Map<PlayerId, InfectionState> states = new LinkedHashMap<>();

    public InfectionState state(PlayerId playerId) {
        return states.computeIfAbsent(
                Objects.requireNonNull(playerId, "playerId"),
                ignored -> new InfectionState()
        );
    }

    public void expose(PlayerId playerId) {
        state(playerId).expose();
    }

    public void advance(PlayerId playerId, int amount) {
        state(playerId).advance(amount);
    }

    public void suppress(PlayerId playerId) {
        state(playerId).suppress();
    }

    public void resume(PlayerId playerId) {
        state(playerId).resume();
    }

    public void cure(PlayerId playerId) {
        state(playerId).cure();
    }

    public InfectionTestResult test(PlayerId playerId, boolean precise) {
        InfectionState state = state(playerId);

        if (!state.infected()) {
            return InfectionTestResult.NEGATIVE;
        }

        if (precise) {
            return InfectionTestResult.POSITIVE;
        }

        return switch (state.stage()) {
            case SYMPTOMATIC -> InfectionTestResult.POSITIVE;
            case SUPPRESSED, EXPOSED, LATENT -> InfectionTestResult.INCONCLUSIVE;
            case NONE -> InfectionTestResult.NEGATIVE;
        };
    }

    public Set<PlayerId> infectedPlayers() {
        LinkedHashSet<PlayerId> result = new LinkedHashSet<>();
        states.forEach((playerId, state) -> {
            if (state.infected()) result.add(playerId);
        });
        return Collections.unmodifiableSet(result);
    }
}

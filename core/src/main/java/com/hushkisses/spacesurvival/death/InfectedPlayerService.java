package com.hushkisses.spacesurvival.death;

import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.scenario.ScenarioRuntime;
import com.hushkisses.spacesurvival.scenario.ScenarioType;

import java.util.*;

public final class InfectedPlayerService {

    private final Map<PlayerId, InfectedPlayerState> states = new LinkedHashMap<>();

    public InfectedPlayerState onDeath(
            DeathRecord death,
            ScenarioRuntime scenario
    ) {
        Objects.requireNonNull(death, "death");

        boolean converts = death.infectedAtDeath()
                && scenario != null
                && scenario.definition().type() == ScenarioType.INFECTION;

        InfectedPlayerState state = converts
                ? new InfectedPlayerState(
                        death.playerId(),
                        PostDeathForm.INFECTED,
                        Set.of(
                                InfectedPostDeathGoal.INFECT_OTHERS,
                                InfectedPostDeathGoal.BREACH_RESTRICTED_ZONE,
                                InfectedPostDeathGoal.ATTACK_FACILITY
                        )
                )
                : new InfectedPlayerState(
                        death.playerId(),
                        PostDeathForm.DEAD,
                        Set.of()
                );

        states.put(death.playerId(), state);
        return state;
    }

    public Optional<InfectedPlayerState> state(PlayerId playerId) {
        return Optional.ofNullable(states.get(Objects.requireNonNull(playerId, "playerId")));
    }

    public boolean isInfectedForm(PlayerId playerId) {
        return state(playerId)
                .map(value -> value.form() == PostDeathForm.INFECTED)
                .orElse(false);
    }
}

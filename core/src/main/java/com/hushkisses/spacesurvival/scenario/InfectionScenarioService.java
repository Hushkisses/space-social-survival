package com.hushkisses.spacesurvival.scenario;

import com.hushkisses.spacesurvival.infection.InfectionService;
import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class InfectionScenarioService {

    public PlayerId beginOutbreak(
            ScenarioRuntime runtime,
            List<PlayerId> participants,
            InfectionService infectionService,
            RandomGenerator random
    ) {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(participants, "participants");
        Objects.requireNonNull(infectionService, "infectionService");
        Objects.requireNonNull(random, "random");

        if (runtime.definition().type() != ScenarioType.INFECTION) {
            throw new IllegalStateException("Active scenario is not INFECTION");
        }
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("participants");
        }

        List<PlayerId> eligible = participants.stream()
                .filter(playerId -> !infectionService.state(playerId).infected())
                .toList();

        if (eligible.isEmpty()) {
            throw new IllegalStateException("No uninfected participant available");
        }

        PlayerId selected = eligible.get(random.nextInt(eligible.size()));
        infectionService.expose(selected);
        return selected;
    }
}

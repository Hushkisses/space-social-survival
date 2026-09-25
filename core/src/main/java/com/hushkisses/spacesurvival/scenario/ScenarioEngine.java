package com.hushkisses.spacesurvival.scenario;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.*;
import java.util.random.RandomGenerator;

public final class ScenarioEngine {

    private ScenarioRuntime active;

    public ScenarioRuntime activate(
            ScenarioDefinition definition,
            List<PlayerId> participants,
            RandomGenerator random
    ) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(participants, "participants");
        Objects.requireNonNull(random, "random");

        if (active != null) {
            throw new IllegalStateException("Scenario already active");
        }
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("participants");
        }

        int upper = Math.min(definition.maxInitialHostiles(), participants.size());
        int lower = Math.min(definition.minInitialHostiles(), upper);
        int count = lower + random.nextInt(upper - lower + 1);

        ArrayList<PlayerId> shuffled = new ArrayList<>(participants);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            Collections.swap(shuffled, i, random.nextInt(i + 1));
        }

        LinkedHashSet<PlayerId> hostiles =
                new LinkedHashSet<>(shuffled.subList(0, count));

        active = new ScenarioRuntime(definition, hostiles);
        return active;
    }

    public Optional<ScenarioRuntime> active() {
        return Optional.ofNullable(active);
    }

    public void clear() {
        active = null;
    }
}

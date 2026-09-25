package com.hushkisses.spacesurvival.objective.secret;

import com.hushkisses.spacesurvival.objective.ObjectiveDefinition;
import com.hushkisses.spacesurvival.objective.ObjectiveEngine;
import com.hushkisses.spacesurvival.objective.ObjectiveId;
import com.hushkisses.spacesurvival.objective.ObjectiveSlot;
import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class SecretMissionService {

    private final ObjectiveEngine engine;

    public SecretMissionService(ObjectiveEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    public SecretMissionGrantResult grant(PlayerId playerId, ObjectiveDefinition definition) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(definition, "definition");

        if (engine.objective(playerId, ObjectiveSlot.SECRET).isPresent()) {
            return SecretMissionGrantResult.ALREADY_HAS_SECRET;
        }

        engine.assign(playerId, definition, ObjectiveSlot.SECRET);
        return SecretMissionGrantResult.GRANTED;
    }

    public SecretMissionGrantResult grantRandom(
            PlayerId playerId,
            List<ObjectiveDefinition> candidates,
            RandomGenerator random
    ) {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(random, "random");

        if (engine.objective(playerId, ObjectiveSlot.SECRET).isPresent()) {
            return SecretMissionGrantResult.ALREADY_HAS_SECRET;
        }

        ObjectiveId baseId = engine.objective(playerId, ObjectiveSlot.BASE)
                .map(instance -> instance.definition().id())
                .orElse(null);

        List<ObjectiveDefinition> eligible = candidates.stream()
                .filter(definition -> !definition.id().equals(baseId))
                .toList();

        if (eligible.isEmpty()) {
            return SecretMissionGrantResult.NO_ELIGIBLE_OBJECTIVE;
        }

        ObjectiveDefinition selected = eligible.get(random.nextInt(eligible.size()));
        engine.assign(playerId, selected, ObjectiveSlot.SECRET);
        return SecretMissionGrantResult.GRANTED;
    }
}

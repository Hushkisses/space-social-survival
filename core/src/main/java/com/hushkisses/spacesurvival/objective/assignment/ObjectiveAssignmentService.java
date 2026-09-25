package com.hushkisses.spacesurvival.objective.assignment;

import com.hushkisses.spacesurvival.objective.ObjectiveDefinition;
import com.hushkisses.spacesurvival.objective.ObjectiveEngine;
import com.hushkisses.spacesurvival.objective.ObjectiveId;
import com.hushkisses.spacesurvival.objective.ObjectiveInstance;
import com.hushkisses.spacesurvival.objective.ObjectiveRegistry;
import com.hushkisses.spacesurvival.objective.ObjectiveSlot;
import com.hushkisses.spacesurvival.objective.conflict.ConflictSetDefinition;
import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.*;
import java.util.random.RandomGenerator;

public final class ObjectiveAssignmentService {

    private final ObjectiveRegistry registry;

    public ObjectiveAssignmentService(ObjectiveRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public Map<PlayerId, ObjectiveInstance> assignBaseObjectives(
            List<PlayerId> players,
            List<ConflictSetDefinition> conflictSets,
            ObjectiveEngine engine,
            RandomGenerator random
    ) {
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(conflictSets, "conflictSets");
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(random, "random");

        if (players.isEmpty()) throw new IllegalArgumentException("players");
        if (players.stream().distinct().count() != players.size()) {
            throw new IllegalArgumentException("duplicate players");
        }

        LinkedHashSet<ObjectiveId> ids = new LinkedHashSet<>();
        conflictSets.forEach(set -> ids.addAll(set.objectiveIds()));
        if (ids.isEmpty()) {
            registry.all().forEach(definition -> ids.add(definition.id()));
        }

        List<ObjectiveDefinition> pool = ids.stream()
                .map(registry::require)
                .toList();
        if (pool.isEmpty()) throw new IllegalStateException("objective pool is empty");

        Map<ObjectiveId, Integer> usage = new HashMap<>();
        LinkedHashMap<PlayerId, ObjectiveInstance> result = new LinkedHashMap<>();

        for (PlayerId player : players) {
            int minimumUsage = pool.stream()
                    .mapToInt(def -> usage.getOrDefault(def.id(), 0))
                    .min()
                    .orElse(0);

            List<ObjectiveDefinition> leastUsed = pool.stream()
                    .filter(def -> usage.getOrDefault(def.id(), 0) == minimumUsage)
                    .toList();

            ObjectiveDefinition selected = leastUsed.get(random.nextInt(leastUsed.size()));
            usage.merge(selected.id(), 1, Integer::sum);
            result.put(player, engine.assign(player, selected, ObjectiveSlot.BASE));
        }

        return Collections.unmodifiableMap(result);
    }
}

package com.hushkisses.spacesurvival.objective.conflict;

import com.hushkisses.spacesurvival.objective.ObjectiveDefinition;
import com.hushkisses.spacesurvival.objective.ObjectiveId;
import com.hushkisses.spacesurvival.objective.ObjectiveRegistry;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DefaultConflictSetCatalog {

    private DefaultConflictSetCatalog() {
    }

    public static List<ConflictSetDefinition> create(ObjectiveRegistry registry) {
        Map<ConflictAxis, Set<ObjectiveId>> grouped = new EnumMap<>(ConflictAxis.class);
        for (ConflictAxis axis : ConflictAxis.values()) {
            grouped.put(axis, new LinkedHashSet<>());
        }

        for (ObjectiveDefinition objective : registry.all()) {
            for (ConflictAxis axis : ConflictAxis.values()) {
                if (objective.tags().contains(axis.tag())) {
                    grouped.get(axis).add(objective.id());
                }
            }
        }

        return grouped.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> new ConflictSetDefinition(entry.getKey(), entry.getValue()))
                .toList();
    }
}

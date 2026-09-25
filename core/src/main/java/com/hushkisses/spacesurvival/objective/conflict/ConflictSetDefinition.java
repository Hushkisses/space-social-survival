package com.hushkisses.spacesurvival.objective.conflict;

import com.hushkisses.spacesurvival.objective.ObjectiveId;

import java.util.Objects;
import java.util.Set;

public record ConflictSetDefinition(
        ConflictAxis axis,
        Set<ObjectiveId> objectiveIds
) {
    public ConflictSetDefinition {
        Objects.requireNonNull(axis, "axis");
        Objects.requireNonNull(objectiveIds, "objectiveIds");
        if (objectiveIds.isEmpty()) throw new IllegalArgumentException("objectiveIds");
        objectiveIds = Set.copyOf(objectiveIds);
    }
}

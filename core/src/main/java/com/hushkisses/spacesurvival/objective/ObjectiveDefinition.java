package com.hushkisses.spacesurvival.objective;

import java.util.Objects;
import java.util.Set;

public record ObjectiveDefinition(
        ObjectiveId id,
        String title,
        String description,
        ObjectiveCategory category,
        int targetProgress,
        int scoreValue,
        Set<String> tags
) {
    public ObjectiveDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(tags, "tags");
        if (title.isBlank()) throw new IllegalArgumentException("title");
        if (description.isBlank()) throw new IllegalArgumentException("description");
        if (targetProgress < 1) throw new IllegalArgumentException("targetProgress");
        if (scoreValue < 0) throw new IllegalArgumentException("scoreValue");
        tags = Set.copyOf(tags);
    }
}

package com.hushkisses.spacesurvival.event;

import com.hushkisses.spacesurvival.event.effect.EventEffect;

import java.util.List;
import java.util.Objects;

public record GameEventDefinition(
        GameEventId id,
        String displayName,
        String description,
        GameEventScale scale,
        List<EventEffect> effects
) {
    public GameEventDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(scale, "scale");
        Objects.requireNonNull(effects, "effects");
        if (displayName.isBlank()) throw new IllegalArgumentException("displayName");
        if (description.isBlank()) throw new IllegalArgumentException("description");
        effects = List.copyOf(effects);
    }
}

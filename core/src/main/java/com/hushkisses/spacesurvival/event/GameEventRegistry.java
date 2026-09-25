package com.hushkisses.spacesurvival.event;

import java.util.*;

public final class GameEventRegistry {

    private final Map<GameEventId, GameEventDefinition> events = new LinkedHashMap<>();

    public void register(GameEventDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (events.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("Duplicate event id: " + definition.id());
        }
    }

    public Optional<GameEventDefinition> find(GameEventId id) {
        return Optional.ofNullable(events.get(Objects.requireNonNull(id, "id")));
    }

    public GameEventDefinition require(GameEventId id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown event: " + id));
    }

    public List<GameEventDefinition> byScale(GameEventScale scale) {
        return events.values().stream()
                .filter(event -> event.scale() == scale)
                .toList();
    }

    public Collection<GameEventDefinition> all() {
        return Collections.unmodifiableCollection(events.values());
    }
}

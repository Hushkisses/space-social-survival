package com.hushkisses.spacesurvival.objective;

import java.util.*;

public final class ObjectiveRegistry {
    private final Map<ObjectiveId, ObjectiveDefinition> definitions = new LinkedHashMap<>();

    public void register(ObjectiveDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (definitions.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("Duplicate objective id: " + definition.id());
        }
    }

    public Optional<ObjectiveDefinition> find(ObjectiveId id) {
        return Optional.ofNullable(definitions.get(Objects.requireNonNull(id, "id")));
    }

    public ObjectiveDefinition require(ObjectiveId id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown objective: " + id));
    }

    public Collection<ObjectiveDefinition> all() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public int size() { return definitions.size(); }
}

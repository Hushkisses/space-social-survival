package com.hushkisses.spacesurvival.facility;

import java.util.*;

public final class FacilityRegistry {

    private final Map<FacilityId, FacilityState> facilities = new LinkedHashMap<>();

    public void register(FacilityDefinition definition) {
        Objects.requireNonNull(definition, "definition");

        if (facilities.containsKey(definition.id())) {
            throw new IllegalArgumentException("Duplicate facility id: " + definition.id());
        }

        facilities.put(definition.id(), new FacilityState(definition));
    }

    public Optional<FacilityState> find(FacilityId id) {
        return Optional.ofNullable(facilities.get(Objects.requireNonNull(id, "id")));
    }

    public FacilityState require(FacilityId id) {
        return find(id).orElseThrow(
                () -> new IllegalArgumentException("Unknown facility id: " + id)
        );
    }

    public List<FacilityStateSnapshot> snapshots() {
        return facilities.values().stream()
                .map(FacilityState::snapshot)
                .toList();
    }

    public int size() {
        return facilities.size();
    }

    public void resetAll() {
        facilities.values().forEach(FacilityState::reset);
    }
}

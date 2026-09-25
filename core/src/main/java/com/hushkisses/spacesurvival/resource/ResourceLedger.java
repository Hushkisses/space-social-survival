package com.hushkisses.spacesurvival.resource;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ResourceLedger {

    private final ResourceStore shared = new ResourceStore();
    private final Map<PlayerId, ResourceStore> personal = new LinkedHashMap<>();

    public ResourceStore shared() {
        return shared;
    }

    public ResourceStore personal(PlayerId playerId) {
        return personal.computeIfAbsent(
                Objects.requireNonNull(playerId, "playerId"),
                ignored -> new ResourceStore()
        );
    }

    public Map<PlayerId, Map<ResourceType, Integer>> personalSnapshot() {
        LinkedHashMap<PlayerId, Map<ResourceType, Integer>> copy = new LinkedHashMap<>();
        personal.forEach((playerId, store) -> copy.put(playerId, store.snapshot()));
        return Collections.unmodifiableMap(copy);
    }
}

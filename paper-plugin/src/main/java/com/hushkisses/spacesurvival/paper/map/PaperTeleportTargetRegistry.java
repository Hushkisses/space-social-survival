package com.hushkisses.spacesurvival.paper.map;

import com.hushkisses.spacesurvival.map.connection.TileConnectionPointRef;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PaperTeleportTargetRegistry {

    private final Map<TileConnectionPointRef, Location> targets = new HashMap<>();

    public void register(TileConnectionPointRef endpoint, Location location) {
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(location, "location");

        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Teleport target location must have a world");
        }
        if (targets.putIfAbsent(endpoint, location.clone()) != null) {
            throw new IllegalArgumentException("Duplicate teleport target: " + endpoint);
        }
    }

    public Optional<Location> find(TileConnectionPointRef endpoint) {
        Location location = targets.get(Objects.requireNonNull(endpoint, "endpoint"));
        return location == null ? Optional.empty() : Optional.of(location.clone());
    }

    public int size() {
        return targets.size();
    }
}

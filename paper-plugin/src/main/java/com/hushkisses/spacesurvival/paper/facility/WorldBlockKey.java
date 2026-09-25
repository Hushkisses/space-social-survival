package com.hushkisses.spacesurvival.paper.facility;

import org.bukkit.Location;

import java.util.Objects;
import java.util.UUID;

public record WorldBlockKey(UUID worldId, int x, int y, int z) {

    public WorldBlockKey {
        Objects.requireNonNull(worldId, "worldId");
    }

    public static WorldBlockKey of(Location location) {
        Objects.requireNonNull(location, "location");
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("location world");
        }
        return new WorldBlockKey(
                location.getWorld().getUID(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }
}

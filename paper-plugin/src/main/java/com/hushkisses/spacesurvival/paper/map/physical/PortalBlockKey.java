package com.hushkisses.spacesurvival.paper.map.physical;

import org.bukkit.Location;

import java.util.Objects;
import java.util.UUID;

public record PortalBlockKey(UUID worldId, int x, int y, int z) {

    public PortalBlockKey {
        Objects.requireNonNull(worldId, "worldId");
    }

    public static PortalBlockKey of(Location location) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("location world");
        }
        return new PortalBlockKey(
                location.getWorld().getUID(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }
}

package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.tile.ConnectionPointId;
import com.hushkisses.spacesurvival.map.tile.TileId;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public record PhysicalTilePlacement(
        TileId tileId,
        int minX,
        int floorY,
        int minZ,
        int size
) {
    public PhysicalTilePlacement {
        Objects.requireNonNull(tileId, "tileId");
        if (size < 9) throw new IllegalArgumentException("size");
    }

    public Location center(World world) {
        int half = size / 2;
        return new Location(world, minX + half + 0.5, floorY + 1.0, minZ + half + 0.5);
    }

    public Location portalPad(World world, ConnectionPointId pointId) {
        int half = size / 2;
        int maxX = minX + size - 1;
        int maxZ = minZ + size - 1;

        return switch (pointId.value()) {
            case "p1" -> new Location(world, minX + half + 0.5, floorY + 1.0, minZ + 2.5);
            case "p2" -> new Location(world, maxX - 1.5, floorY + 1.0, minZ + half + 0.5);
            case "p3" -> new Location(world, minX + half + 0.5, floorY + 1.0, maxZ - 1.5);
            case "p4" -> new Location(world, minX + 2.5, floorY + 1.0, minZ + half + 0.5);
            default -> center(world);
        };
    }

    public boolean contains(Location location) {
        if (location.getWorld() == null) return false;
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= minX && x < minX + size
                && z >= minZ && z < minZ + size
                && location.getBlockY() >= floorY
                && location.getBlockY() <= floorY + 6;
    }
}

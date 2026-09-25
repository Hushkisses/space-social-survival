package com.hushkisses.spacesurvival.paper.map;

import com.hushkisses.spacesurvival.map.connection.TileConnectionPointRef;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class PaperTeleportService {

    private final PaperTeleportTargetRegistry targets;

    public PaperTeleportService(PaperTeleportTargetRegistry targets) {
        this.targets = Objects.requireNonNull(targets, "targets");
    }

    public CompletableFuture<Boolean> teleport(
            Player player,
            TileConnectionPointRef target
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");

        Location location = targets.find(target).orElse(null);
        if (location == null) {
            return CompletableFuture.completedFuture(false);
        }

        return player.teleportAsync(location);
    }
}

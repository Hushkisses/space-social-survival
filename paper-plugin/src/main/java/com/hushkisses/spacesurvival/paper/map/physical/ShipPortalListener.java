package com.hushkisses.spacesurvival.paper.map.physical;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ShipPortalListener implements Listener {

    private final PaperShipWorldService shipWorldService;
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public ShipPortalListener(PaperShipWorldService shipWorldService) {
        this.shipWorldService = Objects.requireNonNull(shipWorldService, "shipWorldService");
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;

        Location from = event.getFrom();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if (to.getBlock().getType() != Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            return;
        }

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (cooldownUntil.getOrDefault(player.getUniqueId(), 0L) > now) {
            return;
        }

        shipWorldService.activeSnapshot()
                .flatMap(snapshot -> snapshot.portalDestination(to))
                .ifPresent(destination -> {
                    cooldownUntil.put(player.getUniqueId(), now + 1000L);
                    player.teleportAsync(destination);
                });
    }
}

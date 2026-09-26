package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.connection.ConnectionAccessDecision;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.item.FunctionalItemType;
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

    private final SpaceSurvivalPlugin plugin;
    private final PaperShipWorldService shipWorldService;
    private final PhysicalConnectionController connections;
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public ShipPortalListener(
            SpaceSurvivalPlugin plugin,
            PaperShipWorldService shipWorldService,
            PhysicalConnectionController connections
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.shipWorldService = Objects.requireNonNull(shipWorldService, "shipWorldService");
        this.connections = Objects.requireNonNull(connections, "connections");
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

        ConnectionAccessDecision access = connections.accessAt(
                to,
                plugin.shipState().power() > 0,
                plugin.functionalItemService().has(
                        player,
                        FunctionalItemType.SECURITY_KEYCARD
                )
        ).orElse(null);

        if (access != null && !access.allowed()) {
            cooldownUntil.put(player.getUniqueId(), now + 800L);
            player.sendMessage("§c[통로] §f" + denialMessage(access.denialReason()));
            return;
        }

        shipWorldService.activeSnapshot()
                .flatMap(snapshot -> snapshot.portalDestination(to))
                .ifPresent(destination -> {
                    cooldownUntil.put(player.getUniqueId(), now + 1000L);
                    player.teleportAsync(destination);
                });
    }

    private static String denialMessage(ConnectionAccessDecision.DenialReason reason) {
        return switch (reason) {
            case LOCKED -> "문이 잠겨 있습니다.";
            case POWER_REQUIRED -> "전력이 없어 통로가 작동하지 않습니다.";
            case KEYCARD_REQUIRED -> "이 통로는 키카드가 필요합니다.";
            case DISABLED -> "통로가 완전히 비활성화되어 있습니다.";
        };
    }
}

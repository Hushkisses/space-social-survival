package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ShipNavigationListener implements Listener {

    private final SpaceSurvivalPlugin plugin;
    private final PaperShipWorldService shipWorldService;
    private final Map<UUID, TileId> lastTile = new HashMap<>();

    public ShipNavigationListener(
            SpaceSurvivalPlugin plugin,
            PaperShipWorldService shipWorldService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.shipWorldService = Objects.requireNonNull(shipWorldService, "shipWorldService");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!changedBlock(event)) {
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.lobbyService().contains(PlayerId.of(player.getUniqueId()))) {
            lastTile.remove(player.getUniqueId());
            return;
        }

        PhysicalShipSnapshot snapshot = shipWorldService.activeSnapshot().orElse(null);
        if (snapshot == null) {
            return;
        }

        TileId tile = snapshot.tileAt(player.getLocation()).orElse(null);
        TileId previous = lastTile.put(player.getUniqueId(), tile);
        if (Objects.equals(previous, tile) || tile == null) {
            return;
        }

        String name = snapshot.tileDisplayName(tile);
        var definition = snapshot.definitions().get(tile);
        String category = definition == null
                ? "함선 구역"
                : switch (definition.category()) {
                    case CORE -> "핵심 시설";
                    case CORRIDOR -> "연결 통로";
                    case JUNCTION -> "교차 구역";
                    case AIRLOCK -> "에어록";
                    case AUXILIARY -> "보조 구역";
                };

        player.sendActionBar(Component.text(
                "현재 구역 · " + name + " · " + category
        ));
        player.playSound(
                player.getLocation(),
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                0.35f,
                1.25f
        );
    }

    private static boolean changedBlock(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return false;
        }
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }
}

package com.hushkisses.spacesurvival.paper.social;

import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SanctionEnforcementListener implements Listener {

    private final SpaceSurvivalPlugin plugin;
    private final Map<UUID, Long> warningCooldown = new HashMap<>();

    public SanctionEnforcementListener(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.sanctionStateRegistry()
                .state(PlayerId.of(player.getUniqueId()))
                .detained()) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if (plugin.shipWorldService().insideDetentionArea(to)) {
            return;
        }

        if (!plugin.shipWorldService().insideDetentionArea(from)) {
            plugin.shipWorldService().detentionSpawn()
                    .ifPresent(player::teleportAsync);
        } else {
            event.setTo(from);
        }

        long now = System.currentTimeMillis();
        if (warningCooldown.getOrDefault(player.getUniqueId(), 0L) <= now) {
            warningCooldown.put(player.getUniqueId(), now + 1200L);
            player.sendActionBar(Component.text(
                    "감금 상태 · 감금 구역 밖으로 이동할 수 없습니다"
            ));
        }
    }
}

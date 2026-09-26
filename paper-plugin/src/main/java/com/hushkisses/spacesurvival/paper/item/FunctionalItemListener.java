package com.hushkisses.spacesurvival.paper.item;

import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

public final class FunctionalItemListener implements Listener {

    private final SpaceSurvivalPlugin plugin;
    private final FunctionalItemService items;

    public FunctionalItemListener(
            SpaceSurvivalPlugin plugin,
            FunctionalItemService items
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.items = Objects.requireNonNull(items, "items");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleSync(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.radioRuntimeState().setRadio(
                PlayerId.of(event.getPlayer().getUniqueId()),
                false
        );
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleSync(player);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            scheduleSync(player);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        scheduleSync(event.getPlayer());
    }

    private void scheduleSync(Player player) {
        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> items.syncRadio(player)
        );
    }
}

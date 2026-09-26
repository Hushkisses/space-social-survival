package com.hushkisses.spacesurvival.paper.item;

import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import net.kyori.adventure.text.Component;
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
            items.typeOf(event.getItem().getItemStack()).ifPresent(type ->
                    player.sendActionBar(Component.text(
                            "장비 회수 · "
                                    + type.displayName().replaceAll("§.", "")
                                    + " | "
                                    + type.useLocation()
                    ))
            );
            scheduleSync(player);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        items.typeOf(event.getItemDrop().getItemStack()).ifPresent(type ->
                event.getPlayer().sendActionBar(Component.text(
                        "장비 이탈 · "
                                + type.displayName().replaceAll("§.", "")
                                + " | 다른 플레이어가 회수할 수 있습니다."
                ))
        );
        scheduleSync(event.getPlayer());
    }

    private void scheduleSync(Player player) {
        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> items.syncRadio(player)
        );
    }
}

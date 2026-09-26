package com.hushkisses.spacesurvival.paper.item;

import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.resource.ResourcePhysicalItemService;
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
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        var stack = event.getItem().getItemStack();

        items.typeOf(stack).ifPresent(type ->
                player.sendActionBar(Component.text(
                        "직업 장비 획득 · "
                                + stripColor(type.displayName())
                                + " · "
                                + FunctionalItemService.usedAt(type)
                ))
        );

        plugin.resourcePhysicalItemService().typeOf(stack).ifPresent(type ->
                player.sendActionBar(Component.text(
                        "자원 획득 · "
                                + stripColor(ResourcePhysicalItemService.displayName(type))
                                + " · 사용처 "
                                + ResourcePhysicalItemService.usedAt(type)
                ))
        );

        scheduleSync(player);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        items.typeOf(event.getItemDrop().getItemStack()).ifPresent(type ->
                event.getPlayer().sendMessage(
                        "§e[장비 분실 주의] §f"
                                + type.displayName()
                                + " §7— "
                                + FunctionalItemService.purpose(type)
                )
        );
        scheduleSync(event.getPlayer());
    }

    private static String stripColor(String value) {
        return value.replaceAll("§.", "");
    }

    private void scheduleSync(Player player) {
        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> items.syncRadio(player)
        );
    }
}

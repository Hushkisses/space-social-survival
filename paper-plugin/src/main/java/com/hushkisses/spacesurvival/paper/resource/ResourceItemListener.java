package com.hushkisses.spacesurvival.paper.resource;

import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.resource.ResourceType;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.Objects;

public final class ResourceItemListener implements Listener {

    private final SpaceSurvivalPlugin plugin;

    public ResourceItemListener(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ResourceType type = plugin.resourcePhysicalItemService()
                .typeOf(event.getItem().getItemStack())
                .orElse(null);
        if (type == null) {
            return;
        }

        int amount = event.getItem().getItemStack().getAmount();
        player.sendActionBar(Component.text(
                "획득 · " + resourceName(type)
                        + " ×" + amount
                        + " | 물리 자원 · 화물실 입고 필요"
        ));
    }

    private static String resourceName(ResourceType type) {
        return switch (type) {
            case REPAIR_PARTS -> "수리 부품";
            case CIRCUITS -> "회로판";
            case POWER_CELLS -> "전력 셀";
            case FUEL -> "연료";
            case MEDICAL_SUPPLIES -> "의료 물자";
            case BIO_SAMPLES -> "생체 샘플";
            case DATA_CORES -> "데이터 코어";
        };
    }
}

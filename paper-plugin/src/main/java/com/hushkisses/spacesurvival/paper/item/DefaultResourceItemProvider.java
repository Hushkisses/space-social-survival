package com.hushkisses.spacesurvival.paper.item;

import com.hushkisses.spacesurvival.integration.itemsadder.ItemsAdderBridge;
import com.hushkisses.spacesurvival.resource.ResourceType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class DefaultResourceItemProvider implements ResourceItemProvider {

    private static final Map<ResourceType, String> CUSTOM_IDS = createCustomIds();

    private final ItemsAdderBridge itemsAdderBridge;

    public DefaultResourceItemProvider(ItemsAdderBridge itemsAdderBridge) {
        this.itemsAdderBridge = Objects.requireNonNull(itemsAdderBridge, "itemsAdderBridge");
    }

    @Override
    public ItemStack create(ResourceType resourceType, int amount) {
        Objects.requireNonNull(resourceType, "resourceType");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be positive");
        }

        ItemStack item = itemsAdderBridge.createItem(CUSTOM_IDS.get(resourceType))
                .orElseGet(() -> new ItemStack(fallbackMaterial(resourceType)));

        item.setAmount(Math.min(amount, item.getMaxStackSize()));
        return item;
    }

    @Override
    public boolean customItemsAvailable() {
        return itemsAdderBridge.isAvailable();
    }

    private static Material fallbackMaterial(ResourceType type) {
        return switch (type) {
            case REPAIR_PARTS -> Material.IRON_NUGGET;
            case CIRCUITS -> Material.REDSTONE;
            case POWER_CELLS -> Material.GLOWSTONE_DUST;
            case FUEL -> Material.BLAZE_POWDER;
            case MEDICAL_SUPPLIES -> Material.PAPER;
            case BIO_SAMPLES -> Material.SLIME_BALL;
            case DATA_CORES -> Material.AMETHYST_SHARD;
        };
    }

    private static Map<ResourceType, String> createCustomIds() {
        EnumMap<ResourceType, String> ids = new EnumMap<>(ResourceType.class);
        ids.put(ResourceType.REPAIR_PARTS, "spacesurvival:repair_parts");
        ids.put(ResourceType.CIRCUITS, "spacesurvival:circuits");
        ids.put(ResourceType.POWER_CELLS, "spacesurvival:power_cells");
        ids.put(ResourceType.FUEL, "spacesurvival:fuel");
        ids.put(ResourceType.MEDICAL_SUPPLIES, "spacesurvival:medical_supplies");
        ids.put(ResourceType.BIO_SAMPLES, "spacesurvival:bio_samples");
        ids.put(ResourceType.DATA_CORES, "spacesurvival:data_cores");
        return Map.copyOf(ids);
    }
}

package com.hushkisses.spacesurvival.paper.resource;

import com.hushkisses.spacesurvival.paper.item.ResourceItemProvider;
import com.hushkisses.spacesurvival.resource.ResourceType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ResourcePhysicalItemService {

    private final ResourceItemProvider provider;
    private final NamespacedKey resourceKey;

    public ResourcePhysicalItemService(
            JavaPlugin plugin,
            ResourceItemProvider provider
    ) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.resourceKey = new NamespacedKey(
                Objects.requireNonNull(plugin, "plugin"),
                "resource_type"
        );
    }

    public ItemStack create(ResourceType type, int amount) {
        ItemStack item = provider.create(type, amount);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(
                resourceKey,
                PersistentDataType.STRING,
                type.name()
        );
        item.setItemMeta(meta);
        return item;
    }

    public Optional<ResourceType> typeOf(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }

        String value = item.getItemMeta()
                .getPersistentDataContainer()
                .get(resourceKey, PersistentDataType.STRING);

        if (value == null) return Optional.empty();

        try {
            return Optional.of(ResourceType.valueOf(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public Map<ResourceType, Integer> removeAllFrom(Player player) {
        EnumMap<ResourceType, Integer> removed = new EnumMap<>(ResourceType.class);
        ItemStack[] contents = player.getInventory().getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            ResourceType type = typeOf(item).orElse(null);
            if (type == null) continue;

            removed.merge(type, item.getAmount(), Integer::sum);
            player.getInventory().setItem(slot, null);
        }

        return Map.copyOf(removed);
    }

    public boolean give(Player player, ResourceType type, int amount) {
        ItemStack item = create(type, amount);
        var overflow = player.getInventory().addItem(item);
        overflow.values().forEach(
                remainder -> player.getWorld().dropItemNaturally(
                        player.getLocation(),
                        remainder
                )
        );
        return true;
    }
}

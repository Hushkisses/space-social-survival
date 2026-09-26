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
        meta.setDisplayName(displayName(type));
        meta.setLore(java.util.List.of(
                "§7물리 자원 · 현재 소지 중",
                "§f" + purpose(type),
                "",
                "§7주 사용 위치: §f" + useLocation(type),
                "§e화물실에 입고하면 공용 자원으로 전환됩니다."
        ));
        meta.getPersistentDataContainer().set(
                resourceKey,
                PersistentDataType.STRING,
                type.name()
        );
        item.setItemMeta(meta);
        return item;
    }

    private static String displayName(ResourceType type) {
        return switch (type) {
            case REPAIR_PARTS -> "§f수리 부품";
            case CIRCUITS -> "§c회로";
            case POWER_CELLS -> "§e전력 셀";
            case FUEL -> "§6연료";
            case MEDICAL_SUPPLIES -> "§a의료 물자";
            case BIO_SAMPLES -> "§d생체 샘플";
            case DATA_CORES -> "§b데이터 코어";
        };
    }

    private static String purpose(ResourceType type) {
        return switch (type) {
            case REPAIR_PARTS -> "손상된 선체·기관 계통 수리에 사용합니다.";
            case CIRCUITS -> "정밀 가공과 전자 계통 작업에 사용합니다.";
            case POWER_CELLS -> "함선 전력 복구에 사용합니다.";
            case FUEL -> "원자로·엔진 출력 안정화에 사용합니다.";
            case MEDICAL_SUPPLIES -> "치료·상태이상·감염 대응에 사용합니다.";
            case BIO_SAMPLES -> "연구실 생체 분석에 사용합니다.";
            case DATA_CORES -> "연구 결과와 고급 데이터 자원으로 사용합니다.";
        };
    }

    private static String useLocation(ResourceType type) {
        return switch (type) {
            case REPAIR_PARTS, POWER_CELLS, FUEL -> "기관실";
            case CIRCUITS -> "화물실";
            case MEDICAL_SUPPLIES -> "의료실";
            case BIO_SAMPLES, DATA_CORES -> "연구실";
        };
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

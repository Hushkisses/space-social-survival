package com.hushkisses.spacesurvival.paper.item;

import com.hushkisses.spacesurvival.integration.itemsadder.ItemsAdderBridge;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.DefaultRoleCatalog;
import com.hushkisses.spacesurvival.role.RoleId;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FunctionalItemService {

    private final JavaPlugin plugin;
    private final ItemsAdderBridge itemsAdder;
    private final NamespacedKey typeKey;

    public FunctionalItemService(JavaPlugin plugin, ItemsAdderBridge itemsAdder) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.itemsAdder = Objects.requireNonNull(itemsAdder, "itemsAdder");
        this.typeKey = new NamespacedKey(plugin, "functional_item");
    }

    public ItemStack create(FunctionalItemType type) {
        Objects.requireNonNull(type, "type");

        ItemStack item = itemsAdder.createItem(type.customItemId())
                .orElseGet(() -> new ItemStack(type.fallbackMaterial()));

        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(
                typeKey,
                PersistentDataType.STRING,
                type.name()
        );

        if (!meta.hasDisplayName()) {
            meta.setDisplayName(type.displayName());
        }

        meta.setLore(List.of(
                "§7우주 생존 기능성 장비",
                "§8분실하거나 빼앗길 수 있습니다."
        ));
        item.setItemMeta(meta);
        return item;
    }

    public Optional<FunctionalItemType> typeOf(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }

        String value = item.getItemMeta()
                .getPersistentDataContainer()
                .get(typeKey, PersistentDataType.STRING);

        if (value == null) return Optional.empty();

        try {
            return Optional.of(FunctionalItemType.valueOf(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public boolean has(Player player, FunctionalItemType type) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(type, "type");

        for (ItemStack item : player.getInventory().getContents()) {
            if (typeOf(item).orElse(null) == type) {
                return true;
            }
        }
        return false;
    }

    public void giveStarterItem(Player player, RoleId roleId) {
        FunctionalItemType itemType = starterItem(roleId);
        ItemStack item = create(itemType);

        var overflow = player.getInventory().addItem(item);
        overflow.values().forEach(
                remainder -> player.getWorld().dropItemNaturally(
                        player.getLocation(),
                        remainder
                )
        );

        player.sendMessage("§a[시작 장비] §f" + itemType.displayName());
    }

    public void syncRadio(Player player) {
        if (plugin instanceof com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin space) {
            space.radioRuntimeState().setRadio(
                    PlayerId.of(player.getUniqueId()),
                    has(player, FunctionalItemType.RADIO)
            );
        }
    }

    private static FunctionalItemType starterItem(RoleId roleId) {
        if (roleId.equals(DefaultRoleCatalog.ENGINEER)) {
            return FunctionalItemType.ENGINEERING_MULTITOOL;
        }
        if (roleId.equals(DefaultRoleCatalog.MEDIC)) {
            return FunctionalItemType.MEDICAL_SCANNER;
        }
        if (roleId.equals(DefaultRoleCatalog.SECURITY)) {
            return FunctionalItemType.SECURITY_KEYCARD;
        }
        if (roleId.equals(DefaultRoleCatalog.RESEARCHER)) {
            return FunctionalItemType.RESEARCH_SCANNER;
        }
        if (roleId.equals(DefaultRoleCatalog.NAV_COMMS)) {
            return FunctionalItemType.RADIO;
        }
        if (roleId.equals(DefaultRoleCatalog.CARGO_MAINTENANCE)) {
            return FunctionalItemType.CARGO_SCANNER;
        }
        throw new IllegalArgumentException("Unknown starter role: " + roleId);
    }
}

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
                "§7" + purpose(type),
                "§7주 사용처: §f" + usedAt(type),
                "",
                "§b기능성 직업 장비",
                "§8분실하거나 빼앗길 수 있습니다."
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static String purpose(FunctionalItemType type) {
        return switch (type) {
            case ENGINEERING_MULTITOOL -> "정밀 진단·전력 재배분·고급 수리에 필요합니다.";
            case MEDICAL_SCANNER -> "정밀 감염 검사·고급 치료·감염 억제에 필요합니다.";
            case SECURITY_KEYCARD -> "보안 시스템과 제한 통로 접근에 사용하는 인증 장비입니다.";
            case RESEARCH_SCANNER -> "정밀 생체 분석·외계 생명체 연구·사건 원인 분석에 필요합니다.";
            case RADIO -> "항법 목적지 설정·장거리 통신·구조 신호 운용에 필요합니다.";
            case CARGO_SCANNER -> "정밀 재고·희귀 자원 판별·고효율 가공에 필요합니다.";
        };
    }

    public static String usedAt(FunctionalItemType type) {
        return switch (type) {
            case ENGINEERING_MULTITOOL -> "기관실";
            case MEDICAL_SCANNER -> "의료실";
            case SECURITY_KEYCARD -> "보안 통로 / 보안 집행";
            case RESEARCH_SCANNER -> "연구실";
            case RADIO -> "함교";
            case CARGO_SCANNER -> "화물실";
        };
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

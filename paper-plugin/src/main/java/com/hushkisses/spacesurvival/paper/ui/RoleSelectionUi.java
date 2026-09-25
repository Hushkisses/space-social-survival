package com.hushkisses.spacesurvival.paper.ui;

import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.RoleDefinition;
import com.hushkisses.spacesurvival.role.RoleId;
import com.hushkisses.spacesurvival.role.RoleRegistry;
import com.hushkisses.spacesurvival.role.selection.RoleCandidateSet;
import com.hushkisses.spacesurvival.role.selection.RoleSelectionService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

public final class RoleSelectionUi {

    public static final String TITLE = "§8직업 선택";
    private static final int[] SLOTS = {11, 13, 15};

    private final RoleSelectionService selectionService;
    private final RoleRegistry roleRegistry;
    private final NamespacedKey roleIdKey;

    public RoleSelectionUi(
            JavaPlugin plugin,
            RoleSelectionService selectionService,
            RoleRegistry roleRegistry
    ) {
        Objects.requireNonNull(plugin, "plugin");
        this.selectionService = Objects.requireNonNull(selectionService, "selectionService");
        this.roleRegistry = Objects.requireNonNull(roleRegistry, "roleRegistry");
        this.roleIdKey = new NamespacedKey(plugin, "role_id");
    }

    public boolean open(Player player) {
        PlayerId playerId = PlayerId.of(player.getUniqueId());
        RoleCandidateSet set = selectionService.candidates(playerId).orElse(null);

        if (set == null) {
            player.sendMessage("§c아직 직업 후보가 생성되지 않았습니다.");
            return false;
        }

        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        for (int i = 0; i < set.candidates().size(); i++) {
            RoleId roleId = set.candidates().get(i);
            RoleDefinition role = roleRegistry.require(roleId);
            inventory.setItem(SLOTS[i], roleItem(role));
        }

        selectionService.selectedRole(playerId).ifPresent(roleId -> {
            RoleDefinition selected = roleRegistry.require(roleId);
            inventory.setItem(22, summaryItem(selected));
        });

        player.openInventory(inventory);
        return true;
    }

    public RoleId readRoleId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        String value = item.getItemMeta()
                .getPersistentDataContainer()
                .get(roleIdKey, PersistentDataType.STRING);

        return value == null ? null : new RoleId(value);
    }

    private ItemStack roleItem(RoleDefinition role) {
        ItemStack item = new ItemStack(materialFor(role.id()));
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§e" + role.displayName());
        meta.setLore(List.of(
                "§7" + role.description(),
                "",
                "§f직업 ID: §7" + role.id(),
                "§f최대 중복: §7" + role.maxCopies() + "명",
                "",
                "§a클릭하여 이 직업을 선택합니다."
        ));
        meta.getPersistentDataContainer().set(
                roleIdKey,
                PersistentDataType.STRING,
                role.id().value()
        );

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack summaryItem(RoleDefinition role) {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a선택 완료: " + role.displayName());
        meta.setLore(List.of("§7이미 직업 선택이 확정되었습니다."));
        item.setItemMeta(meta);
        return item;
    }

    private static Material materialFor(RoleId id) {
        return switch (id.value()) {
            case "engineer" -> Material.IRON_PICKAXE;
            case "medic" -> Material.GOLDEN_APPLE;
            case "security" -> Material.SHIELD;
            case "researcher" -> Material.SPYGLASS;
            case "nav_comms" -> Material.COMPASS;
            case "cargo_maintenance" -> Material.CHEST;
            default -> Material.PAPER;
        };
    }
}

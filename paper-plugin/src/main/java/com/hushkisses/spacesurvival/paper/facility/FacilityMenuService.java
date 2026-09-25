package com.hushkisses.spacesurvival.paper.facility;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.facility.action.*;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class FacilityMenuService implements Listener {

    private static final String TITLE_PREFIX = "§8시설 콘솔 — ";

    private final SpaceSurvivalPlugin plugin;
    private final FacilityActionExecutor executor;
    private final Map<UUID, FacilityMenuSession> sessions = new HashMap<>();

    public FacilityMenuService(
            SpaceSurvivalPlugin plugin,
            FacilityActionExecutor executor
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public void open(Player player, FacilityId facilityId) {
        var facility = plugin.facilityRegistry().require(facilityId);
        List<FacilityActionDefinition> actions =
                plugin.facilityActionRegistry().forFacility(facilityId);

        int size = actions.size() <= 9 ? 18 : 27;
        Inventory inventory = Bukkit.createInventory(
                null,
                size,
                TITLE_PREFIX + facility.definition().displayName()
        );

        inventory.setItem(0, statusItem(
                facility.definition().displayName(),
                facility.status()
        ));

        LinkedHashMap<Integer, FacilityActionId> slotActions = new LinkedHashMap<>();
        int slot = 9;

        for (FacilityActionDefinition action : actions) {
            if (slot >= size) break;

            var access = executor.access(player, action);
            inventory.setItem(slot, actionItem(action, access));
            slotActions.put(slot, action.id());
            slot++;
        }

        sessions.put(
                player.getUniqueId(),
                new FacilityMenuSession(facilityId, slotActions)
        );
        player.openInventory(inventory);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX)) return;

        event.setCancelled(true);

        FacilityMenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        FacilityActionId actionId = session.slotActions().get(event.getRawSlot());
        if (actionId == null) return;

        FacilityActionDefinition action = plugin.facilityActionRegistry()
                .find(actionId)
                .orElse(null);
        if (action == null) return;

        FacilityActionExecutionResult result = executor.execute(player, action);
        player.sendMessage(
                (result.success() ? "§a[시설] §f" : "§c[시설] §f")
                        + result.message()
        );

        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> open(player, session.facilityId())
        );
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player
                && event.getView().getTitle().startsWith(TITLE_PREFIX)) {
            sessions.remove(player.getUniqueId());
        }
    }

    private static ItemStack statusItem(String name, FacilityStatus status) {
        Material material = switch (status) {
            case NORMAL -> Material.LIME_CONCRETE;
            case DAMAGED -> Material.YELLOW_CONCRETE;
            case OFFLINE -> Material.RED_CONCRETE;
            case QUARANTINED -> Material.PURPLE_CONCRETE;
        };

        return item(
                material,
                "§f" + name + " §7— " + statusName(status),
                List.of("§8시설 상태에 따라 사용할 수 있는 기능이 달라집니다.")
        );
    }

    private static ItemStack actionItem(
            FacilityActionDefinition action,
            FacilityActionAccessDecision access
    ) {
        Material material = access.allowed()
                ? (action.tier() == FacilityActionTier.ADVANCED
                ? Material.NETHER_STAR
                : Material.IRON_INGOT)
                : Material.BARRIER;

        ArrayList<String> lore = new ArrayList<>();
        lore.add(action.tier() == FacilityActionTier.ADVANCED
                ? "§d고급 기능"
                : "§7기본 기능");

        action.requiredCapabilityOptional().ifPresent(
                capability -> lore.add("§7필요 직업 능력: §f" + capability.name())
        );

        if (access.allowed()) {
            lore.add("§a클릭하여 실행");
        } else {
            lore.add("§c사용 불가: " + denialName(access.denialReason()));
        }

        return item(material, "§f" + action.displayName(), lore);
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String statusName(FacilityStatus status) {
        return switch (status) {
            case NORMAL -> "§a정상";
            case DAMAGED -> "§e손상";
            case OFFLINE -> "§c정지";
            case QUARANTINED -> "§5격리";
        };
    }

    private static String denialName(FacilityActionAccessDecision.DenialReason reason) {
        return switch (reason) {
            case FACILITY_OFFLINE -> "시설 정지";
            case FACILITY_QUARANTINED -> "시설 격리";
            case FACILITY_DAMAGED_ADVANCED_UNAVAILABLE -> "시설 손상";
            case MISSING_ROLE_CAPABILITY -> "직업 권한 부족";
        };
    }
}

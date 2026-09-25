package com.hushkisses.spacesurvival.paper.ui;

import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.RoleDefinition;
import com.hushkisses.spacesurvival.role.RoleId;
import com.hushkisses.spacesurvival.role.RoleRegistry;
import com.hushkisses.spacesurvival.role.selection.RoleSelectionResult;
import com.hushkisses.spacesurvival.role.selection.RoleSelectionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class OpeningUiListener implements Listener {

    private final OpeningBriefingUi briefingUi;
    private final RoleSelectionUi roleSelectionUi;
    private final RoleSelectionService selectionService;
    private final RoleRegistry roleRegistry;

    public OpeningUiListener(
            OpeningBriefingUi briefingUi,
            RoleSelectionUi roleSelectionUi,
            RoleSelectionService selectionService,
            RoleRegistry roleRegistry
    ) {
        this.briefingUi = Objects.requireNonNull(briefingUi, "briefingUi");
        this.roleSelectionUi = Objects.requireNonNull(roleSelectionUi, "roleSelectionUi");
        this.selectionService = Objects.requireNonNull(selectionService, "selectionService");
        this.roleRegistry = Objects.requireNonNull(roleRegistry, "roleRegistry");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();

        if (OpeningBriefingUi.TITLE.equals(title)) {
            event.setCancelled(true);

            if (event.getRawSlot() == 22) {
                roleSelectionUi.open(player);
            }
            return;
        }

        if (!RoleSelectionUi.TITLE.equals(title)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        RoleId roleId = roleSelectionUi.readRoleId(clicked);
        if (roleId == null) {
            return;
        }

        PlayerId playerId = PlayerId.of(player.getUniqueId());
        RoleSelectionResult result = selectionService.select(playerId, roleId);

        switch (result) {
            case SELECTED -> {
                RoleDefinition role = roleRegistry.require(roleId);
                player.closeInventory();
                player.sendMessage("§a직업을 확정했습니다: " + role.displayName());
                player.sendMessage("§7초기 공통 목표: 생존 기반 복구");
            }
            case CANDIDATES_NOT_PREPARED -> {
                player.closeInventory();
                player.sendMessage("§c아직 직업 후보가 생성되지 않았습니다.");
            }
            case ROLE_NOT_OFFERED ->
                    player.sendMessage("§c본인에게 제시된 후보가 아닌 직업입니다.");
            case ROLE_FULL -> {
                player.sendMessage("§c해당 직업의 선택 가능 인원이 가득 찼습니다.");
                roleSelectionUi.open(player);
            }
            case ALREADY_SELECTED -> {
                RoleId selectedId = selectionService.selectedRole(playerId).orElseThrow();
                RoleDefinition selected = roleRegistry.require(selectedId);
                player.sendMessage("§e이미 직업을 확정했습니다: " + selected.displayName());
            }
        }
    }
}

package com.hushkisses.spacesurvival.paper.ui;

import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.RoleDefinition;
import com.hushkisses.spacesurvival.role.RoleId;
import com.hushkisses.spacesurvival.role.RoleRegistry;
import com.hushkisses.spacesurvival.role.selection.RoleSelectionResult;
import com.hushkisses.spacesurvival.role.selection.RoleSelectionService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class OpeningUiListener implements Listener {

    private final OpeningBriefingUi briefingUi;
    private final RoleSelectionUi roleSelectionUi;
    private final RoleSelectionService selectionService;
    private final RoleRegistry roleRegistry;
    private final CrewPdaService crewPdaService;
    private final Runnable selectionChanged;

    public OpeningUiListener(
            OpeningBriefingUi briefingUi,
            RoleSelectionUi roleSelectionUi,
            RoleSelectionService selectionService,
            RoleRegistry roleRegistry,
            CrewPdaService crewPdaService,
            Runnable selectionChanged
    ) {
        this.briefingUi = Objects.requireNonNull(briefingUi, "briefingUi");
        this.roleSelectionUi = Objects.requireNonNull(roleSelectionUi, "roleSelectionUi");
        this.selectionService = Objects.requireNonNull(selectionService, "selectionService");
        this.roleRegistry = Objects.requireNonNull(roleRegistry, "roleRegistry");
        this.crewPdaService = Objects.requireNonNull(crewPdaService, "crewPdaService");
        this.selectionChanged = Objects.requireNonNull(selectionChanged, "selectionChanged");
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
                player.sendMessage("§7개인 PDA에서 역할·개인 목표·첫 행동을 확인하십시오.");
                roleSelectionUi.removeMenuItem(player);
                crewPdaService.open(player);
                selectionChanged.run();
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
                roleSelectionUi.removeMenuItem(player);
                crewPdaService.open(player);
            }
        }
    }
    @EventHandler
    public void onRoleMenuInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!roleSelectionUi.isMenuItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        PlayerId playerId = PlayerId.of(player.getUniqueId());

        if (selectionService.selectedRole(playerId).isPresent()) {
            roleSelectionUi.removeMenuItem(player);
            crewPdaService.open(player);
            return;
        }

        roleSelectionUi.open(player);
    }

    @EventHandler
    public void onRoleMenuDrop(PlayerDropItemEvent event) {
        if (!roleSelectionUi.isMenuItem(event.getItemDrop().getItemStack())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendActionBar(
                Component.text("직업 선택 아이템은 직업을 고르기 전까지 버릴 수 없습니다.")
        );
    }

    @EventHandler
    public void onRoleSelectionClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!RoleSelectionUi.TITLE.equals(event.getView().getTitle())) {
            return;
        }

        PlayerId playerId = PlayerId.of(player.getUniqueId());
        if (selectionService.selectedRole(playerId).isPresent()) {
            return;
        }

        player.sendActionBar(
                Component.text("직업 선택 필요 · 핫바의 네더별을 우클릭하면 다시 열 수 있습니다.")
        );
    }

}

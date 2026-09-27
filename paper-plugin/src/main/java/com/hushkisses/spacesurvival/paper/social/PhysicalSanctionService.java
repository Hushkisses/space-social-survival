package com.hushkisses.spacesurvival.paper.social;

import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.social.sanction.SanctionChoice;
import com.hushkisses.spacesurvival.social.sanction.SanctionExecutionResult;
import com.hushkisses.spacesurvival.social.sanction.SanctionType;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class PhysicalSanctionService {

    private static final Set<Material> WEAPONS = EnumSet.of(
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD,
            Material.BOW,
            Material.CROSSBOW,
            Material.TRIDENT,
            Material.MACE
    );

    private final SpaceSurvivalPlugin plugin;

    public PhysicalSanctionService(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void apply(
            SanctionChoice choice,
            SanctionExecutionResult execution
    ) {
        if (!execution.executed() || choice.target() == null) {
            return;
        }

        Player target = plugin.getServer().getPlayer(choice.target().value());
        if (target == null) {
            return;
        }

        switch (choice.sanction()) {
            case NO_ACTION -> {
            }
            case MEDICAL_CHECK -> {
                var result = plugin.infectionService().test(choice.target(), true);
                target.sendMessage("§b[의료 검사] §f정밀 감염 검사 결과: " + result.name());
            }
            case DISARM -> disarm(target);
            case DETAIN -> detain(target);
            case ACCESS_RESTRICT -> restrictAccess(target);
            case EJECT -> eject(target);
        }

        plugin.telemetryService().event(
                "sanction.physical",
                choice.sanction().name() + ":" + target.getName()
        );
    }

    public boolean detained(Player player) {
        return plugin.sanctionStateRegistry()
                .state(PlayerId.of(player.getUniqueId()))
                .detained();
    }

    public boolean accessRestricted(Player player) {
        return plugin.sanctionStateRegistry()
                .state(PlayerId.of(player.getUniqueId()))
                .accessRestricted();
    }

    public boolean ejected(Player player) {
        return plugin.sanctionStateRegistry()
                .state(PlayerId.of(player.getUniqueId()))
                .ejected();
    }

    private void disarm(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || !WEAPONS.contains(item.getType())) continue;

            player.getInventory().setItem(slot, null);
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        player.sendMessage("§e[무장해제] §f소지 중이던 무기가 바닥에 내려졌습니다.");
    }

    private void detain(Player player) {
        var spawn = plugin.shipWorldService().detentionSpawn().orElse(null);
        if (spawn == null) {
            var snapshot = plugin.shipWorldService().activeSnapshot().orElse(null);
            if (snapshot == null) return;
            spawn = snapshot.bridgeSpawn();
        }

        player.teleportAsync(spawn);
        player.sendTitle(
                "§6감금 처분",
                "§f감금 구역 밖으로 이동할 수 없습니다",
                10,
                60,
                10
        );
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 0.8f);
        player.sendMessage("§6[감금] §f보안 처분으로 감금되었습니다.");
        player.sendMessage("§7가능: 감금 셀 내부 이동, PDA·인벤토리 사용, 허용된 통신");
        player.sendMessage("§7제한: 셀 밖 이동, 시설 작업, 함선 통로 이용");
        player.sendMessage("§8이 상태는 현재 제재 상태가 유지되는 동안 계속됩니다.");
    }

    private void restrictAccess(Player player) {
        player.sendTitle(
                "§c출입 권한 제한",
                "§f시설 콘솔과 제한 통로를 사용할 수 없습니다",
                10,
                50,
                10
        );
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 0.8f, 1.1f);
        player.sendMessage("§c[출입 제한] §f시설 콘솔과 모듈 간 통로 사용이 제한됩니다.");
        player.sendMessage("§7이동 가능한 공개 구역과 PDA 사용은 계속 가능합니다.");
    }

    private void eject(Player player) {
        var chamber = plugin.shipWorldService().ejectionChamber().orElse(null);
        if (chamber != null) {
            player.teleportAsync(chamber);
        }

        player.setGameMode(GameMode.ADVENTURE);
        player.sendTitle(
                "§4추방 절차",
                "§c에어록 감압 중...",
                5,
                40,
                5
        );
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.7f);
        player.sendMessage("§4[추방] §f에어록 추방 절차가 시작되었습니다.");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !ejected(player)) {
                return;
            }

            player.setGameMode(GameMode.SPECTATOR);
            plugin.shipWorldService().ejectionViewpoint()
                    .ifPresent(player::teleportAsync);

            player.sendTitle(
                    "§4추방 완료",
                    "§7생존 승무원으로서의 행동이 종료되었습니다",
                    10,
                    70,
                    10
            );
            player.sendMessage("§4[추방] §f우주선에서 추방되었습니다.");
            player.sendMessage("§7시설 작업·함선 내 이동은 불가능합니다.");
            player.sendMessage("§7개인 목표는 게임 종료 전까지 공개되지 않습니다.");
        }, 40L);
    }
}

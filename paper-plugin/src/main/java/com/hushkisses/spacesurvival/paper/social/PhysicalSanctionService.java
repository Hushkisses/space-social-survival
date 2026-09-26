package com.hushkisses.spacesurvival.paper.social;

import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.social.sanction.SanctionChoice;
import com.hushkisses.spacesurvival.social.sanction.SanctionExecutionResult;
import com.hushkisses.spacesurvival.social.sanction.SanctionType;
import org.bukkit.GameMode;
import org.bukkit.Material;
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
            case ACCESS_RESTRICT ->
                    target.sendMessage("§c[출입 제한] §f시설 콘솔과 모듈 간 통로 사용이 제한됩니다.");
            case EJECT -> eject(target);
        }

        plugin.telemetryService().event(
                "sanction.physical",
                choice.sanction().name() + ":" + target.getName()
        );
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
        var snapshot = plugin.shipWorldService().activeSnapshot().orElse(null);
        if (snapshot == null) return;

        var placement = snapshot.placements().get(new TileId("habitation"));
        if (placement == null) {
            player.teleportAsync(snapshot.bridgeSpawn());
        } else {
            player.teleportAsync(placement.center(snapshot.world()));
        }

        player.sendMessage("§6[감금] §f이동이 제한됩니다.");
    }

    private void eject(Player player) {
        var snapshot = plugin.shipWorldService().activeSnapshot().orElse(null);
        if (snapshot != null) {
            var airlock = snapshot.placements().entrySet().stream()
                    .filter(entry -> entry.getKey().value().startsWith("airlock"))
                    .findFirst()
                    .map(entry -> entry.getValue().center(snapshot.world()))
                    .orElse(snapshot.bridgeSpawn());
            player.teleportAsync(airlock);
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage("§4[추방] §f우주선에서 추방되었습니다.");
        });
    }
}

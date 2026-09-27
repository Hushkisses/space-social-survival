package com.hushkisses.spacesurvival.paper.death;

import com.hushkisses.spacesurvival.death.DeathCause;
import com.hushkisses.spacesurvival.death.DeathRecord;
import com.hushkisses.spacesurvival.death.InfectedPlayerState;
import com.hushkisses.spacesurvival.death.InfectedPostDeathGoal;
import com.hushkisses.spacesurvival.death.PostDeathForm;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.player.PlayerState;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PlayerDeathStateListener implements Listener {

    private final SpaceSurvivalPlugin plugin;
    private final Map<UUID, Location> deathLocations = new LinkedHashMap<>();

    public PlayerDeathStateListener(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerId playerId = PlayerId.of(player.getUniqueId());

        deathLocations.put(player.getUniqueId(), player.getLocation().clone());

        if (plugin.lobbyService().contains(playerId)) {
            event.setKeepInventory(false);
            int recoverableStacks = 0;
            int recoverableUnits = 0;

            for (var drop : event.getDrops()) {
                boolean functional = plugin.functionalItemService().typeOf(drop).isPresent();
                boolean resource = plugin.resourcePhysicalItemService().typeOf(drop).isPresent();
                if (functional || resource) {
                    recoverableStacks++;
                    recoverableUnits += drop.getAmount();
                }
            }

            plugin.telemetryService().add("loot.recoverable.stacks", recoverableStacks);
            plugin.telemetryService().add("loot.recoverable.units", recoverableUnits);
            plugin.telemetryService().event(
                    "loot.drop",
                    player.getName() + ":stacks=" + recoverableStacks + ":units=" + recoverableUnits
            );
        }

        PlayerState playerState = plugin.lobbyService().playerState(playerId).orElse(null);
        if (playerState == null) {
            return;
        }

        boolean infectedAtDeath = plugin.infectionService().state(playerId).infected();
        DeathCause cause = player.getKiller() == null
                ? DeathCause.ENVIRONMENT
                : DeathCause.COMBAT;

        DeathRecord record = plugin.deathService().registerDeath(
                playerState,
                cause,
                infectedAtDeath
        );

        InfectedPlayerState postDeath = plugin.infectedPlayerService().onDeath(
                record,
                plugin.scenarioEngine().active().orElse(null)
        );

        plugin.telemetryService().recordDeath(player.getName(), cause.name());
        player.sendMessage("§7[사망] §f소지 장비와 자원은 현장에 남아 다른 플레이어가 회수할 수 있습니다.");

        plugin.getLogger().info(
                "Player death registered: "
                        + player.getName()
                        + " cause="
                        + cause
                        + " postDeath="
                        + postDeath.form()
        );
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerId playerId = PlayerId.of(player.getUniqueId());

        Location deathLocation = deathLocations.remove(player.getUniqueId());
        if (deathLocation != null) {
            event.setRespawnLocation(deathLocation);
        }

        InfectedPlayerState state = plugin.infectedPlayerService().state(playerId).orElse(null);
        if (state == null) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (state.form() == PostDeathForm.INFECTED) {
                player.setGameMode(GameMode.ADVENTURE);
                player.sendTitle(
                        "§4감염체로 전환",
                        "§c생존자와의 일반 통신·수리·시설 조작이 제한됩니다",
                        10,
                        80,
                        10
                );
                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED,
                        0.9f,
                        0.8f
                );
                player.sendMessage("§4[감염체] §f사망 후 감염체로 전환되었습니다.");
                player.sendMessage("§7생존자와 일반 채팅을 주고받을 수 없고, 시설 복구·선체 수리를 수행할 수 없습니다.");
                player.sendMessage("§7새 목표:");
                for (InfectedPostDeathGoal goal : state.goals()) {
                    player.sendMessage("§c- " + goalName(goal));
                }
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendTitle(
                        "§7사망",
                        "§f생존자와 직접 대화할 수 없습니다",
                        10,
                        70,
                        10
                );
                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_BEACON_DEACTIVATE,
                        0.8f,
                        0.8f
                );
                player.sendMessage("§7[사망 상태] §f관전 상태로 전환되었습니다.");
                player.sendMessage("§7사망자끼리만 일반 채팅이 전달되며 생존자에게는 전달되지 않습니다.");
                player.sendMessage("§7개인 목표와 비밀 임무는 게임 종료 전까지 공개되지 않습니다.");
                player.sendMessage("§7사망 시 떨어진 장비·자원은 생존자가 계속 회수할 수 있습니다.");
            }
        });
    }

    private static String goalName(InfectedPostDeathGoal goal) {
        return switch (goal) {
            case INFECT_OTHERS -> "다른 플레이어에게 감염을 확산하십시오.";
            case BREACH_RESTRICTED_ZONE -> "제한 구역에 침입하십시오.";
            case ATTACK_FACILITY -> "함선 시설을 공격하십시오.";
        };
    }
}

package com.hushkisses.spacesurvival.paper.death;

import com.hushkisses.spacesurvival.death.DeathCause;
import com.hushkisses.spacesurvival.death.DeathRecord;
import com.hushkisses.spacesurvival.death.InfectedPlayerState;
import com.hushkisses.spacesurvival.death.PostDeathForm;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.player.PlayerState;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Objects;

public final class PlayerDeathStateListener implements Listener {

    private final SpaceSurvivalPlugin plugin;

    public PlayerDeathStateListener(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerId playerId = PlayerId.of(player.getUniqueId());

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

        InfectedPlayerState state = plugin.infectedPlayerService().state(playerId).orElse(null);
        if (state == null) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (state.form() == PostDeathForm.INFECTED) {
                player.setGameMode(GameMode.ADVENTURE);
                player.sendMessage("§4[감염체] §f사망 후 감염체로 전환되었습니다.");
                player.sendMessage("§7생존자와의 일반 통신·수리·일부 조작이 제한됩니다.");
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage("§7사망 상태입니다. 개인 목표는 게임 종료 전까지 공개되지 않습니다.");
            }
        });
    }
}

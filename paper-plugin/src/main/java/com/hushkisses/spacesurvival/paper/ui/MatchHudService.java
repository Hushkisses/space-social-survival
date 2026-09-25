package com.hushkisses.spacesurvival.paper.ui;

import com.hushkisses.spacesurvival.ending.ReturnStage;
import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.map.physical.PaperShipWorldService;
import com.hushkisses.spacesurvival.paper.map.physical.PhysicalShipSnapshot;
import com.hushkisses.spacesurvival.ship.ShipStateSnapshot;
import com.hushkisses.spacesurvival.time.CrisisStage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class MatchHudService {

    private final SpaceSurvivalPlugin plugin;
    private final PaperShipWorldService shipWorldService;
    private BukkitTask task;

    public MatchHudService(
            SpaceSurvivalPlugin plugin,
            PaperShipWorldService shipWorldService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.shipWorldService = Objects.requireNonNull(shipWorldService, "shipWorldService");
    }

    public void start() {
        if (task != null) return;

        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::render,
                10L,
                10L
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void render() {
        if (plugin.lobbyService().gameSession().isEmpty()) {
            return;
        }

        ShipStateSnapshot ship = plugin.shipState().snapshot();
        CrisisStage crisis = plugin.gameRuntimeService().currentCrisisStage();
        ReturnStage returnStage = plugin.returnObjectiveService().stage();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.lobbyService().contains(
                    com.hushkisses.spacesurvival.player.PlayerId.of(player.getUniqueId())
            )) {
                continue;
            }

            String area = currentArea(player);
            String text = "전력 " + ship.power() + "%"
                    + " | 산소 " + ship.oxygen() + "%"
                    + " | 선체 " + ship.hull() + "%"
                    + " | 위기 " + crisisName(crisis)
                    + " | 목표 " + stageName(returnStage)
                    + " | 구역 " + area;

            player.sendActionBar(Component.text(text));
        }
    }

    private String currentArea(Player player) {
        PhysicalShipSnapshot snapshot = shipWorldService.activeSnapshot().orElse(null);
        if (snapshot == null) return "미배치";

        TileId tileId = snapshot.tileAt(player.getLocation()).orElse(null);
        return tileId == null ? "함선 외부" : snapshot.tileDisplayName(tileId);
    }

    private static String crisisName(CrisisStage stage) {
        return switch (stage) {
            case STABLE -> "안정";
            case ALERT -> "경계";
            case CRISIS -> "위기";
            case COLLAPSE -> "붕괴";
        };
    }

    private static String stageName(ReturnStage stage) {
        return switch (stage) {
            case SURVIVAL_SYSTEMS -> "생존 기반 복구";
            case NAVIGATION -> "항법";
            case RETURN_PREPARATION -> "귀환 준비";
            case FINAL_HOLD -> "최종 버티기";
            case COMPLETED -> "귀환 완료";
            case FAILED -> "귀환 실패";
        };
    }
}

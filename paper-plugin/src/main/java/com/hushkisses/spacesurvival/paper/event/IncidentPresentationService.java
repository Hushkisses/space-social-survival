package com.hushkisses.spacesurvival.paper.event;

import com.hushkisses.spacesurvival.event.GameEventDefinition;
import com.hushkisses.spacesurvival.event.GameEventScale;
import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class IncidentPresentationService {

    private final SpaceSurvivalPlugin plugin;

    private BossBar majorBar;
    private BukkitTask hideTask;
    private BukkitTask resolutionTask;
    private String trackedEventId;

    public IncidentPresentationService(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void present(GameEventDefinition event) {
        Objects.requireNonNull(event, "event");

        String target = targetFacility(event.id().value());
        String response = responseHint(event.id().value());

        if (event.scale() == GameEventScale.LARGE) {
            presentMajor(event, target, response);
        } else {
            presentSmall(event, target, response);
        }

        startResolutionWatch(event.id().value(), event.displayName());
    }

    public void stop() {
        if (hideTask != null) {
            hideTask.cancel();
            hideTask = null;
        }
        if (resolutionTask != null) {
            resolutionTask.cancel();
            resolutionTask = null;
        }
        if (majorBar != null) {
            majorBar.removeAll();
            majorBar.setVisible(false);
            majorBar = null;
        }
        trackedEventId = null;
    }

    private void presentSmall(
            GameEventDefinition event,
            String target,
            String response
    ) {
        plugin.getServer().broadcastMessage(
                "§e[함선 경보] §f" + event.displayName()
        );
        plugin.getServer().broadcastMessage(
                "§7영향 구역: §f" + target + " §8| §7대응: §f" + response
        );

        for (Player player : onlineParticipants()) {
            player.sendActionBar(Component.text(
                    "경보 · " + event.displayName()
                            + " → " + target
                            + " · " + response
            ));
            player.playSound(
                    player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BELL,
                    0.8f,
                    0.8f
            );
        }
    }

    private void presentMajor(
            GameEventDefinition event,
            String target,
            String response
    ) {
        plugin.getServer().broadcastMessage(
                "§c§l[중대 함선 경보] §f" + event.displayName()
        );
        plugin.getServer().broadcastMessage(
                "§7영향 구역: §f" + target + " §8| §7권장 대응: §f" + response
        );

        for (Player player : onlineParticipants()) {
            player.sendTitle(
                    "§c§l" + event.displayName(),
                    "§f" + target + " · " + response,
                    5,
                    60,
                    15
            );
            player.playSound(
                    player.getLocation(),
                    Sound.BLOCK_BELL_RESONATE,
                    1.0f,
                    0.65f
            );
        }

        if (majorBar == null) {
            majorBar = Bukkit.createBossBar(
                    "",
                    BarColor.RED,
                    BarStyle.SEGMENTED_10
            );
        }

        majorBar.removeAll();
        majorBar.setColor(BarColor.RED);
        majorBar.setProgress(1.0);
        majorBar.setTitle(
                "§c§l" + event.displayName()
                        + " §7— §f" + target
                        + " §7/ §e" + response
        );
        for (Player player : onlineParticipants()) {
            majorBar.addPlayer(player);
        }
        majorBar.setVisible(true);

        if (hideTask != null) hideTask.cancel();
        hideTask = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    if (majorBar != null) {
                        majorBar.removeAll();
                        majorBar.setVisible(false);
                    }
                    hideTask = null;
                },
                20L * 8L
        );
    }

    private void startResolutionWatch(String eventId, String eventName) {
        trackedEventId = eventId;

        if (resolutionTask != null) {
            resolutionTask.cancel();
        }

        resolutionTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> {
                    if (!Objects.equals(trackedEventId, eventId)) {
                        return;
                    }
                    if (!isResolved(eventId)) {
                        return;
                    }

                    plugin.getServer().broadcastMessage(
                            "§a[안정화 완료] §f" + eventName + " 대응이 완료되었습니다."
                    );
                    for (Player player : onlineParticipants()) {
                        player.sendActionBar(Component.text(
                                "안정화 완료 · " + eventName
                        ));
                        player.playSound(
                                player.getLocation(),
                                Sound.BLOCK_NOTE_BLOCK_CHIME,
                                0.8f,
                                1.4f
                        );
                    }

                    trackedEventId = null;
                    if (resolutionTask != null) {
                        resolutionTask.cancel();
                        resolutionTask = null;
                    }
                },
                20L,
                20L
        );
    }

    private boolean isResolved(String id) {
        var ship = plugin.shipState().snapshot();

        return switch (id) {
            case "door_fault" ->
                    !plugin.gameEventRuntimeState().hasFlag("door_fault");
            case "comms_noise" ->
                    !plugin.radioRuntimeState().communicationsOutage();
            case "cargo_damage" ->
                    plugin.facilityRegistry()
                            .require(DefaultFacilityCatalog.CARGO)
                            .status() == FacilityStatus.NORMAL;
            case "medical_contamination" ->
                    plugin.facilityRegistry()
                            .require(DefaultFacilityCatalog.MEDICAL)
                            .status() == FacilityStatus.NORMAL;
            case "local_oxygen_drop" -> ship.oxygen() >= 50;
            case "small_fire" -> ship.hull() >= 50;
            case "reactor_runaway" ->
                    ship.reactor() >= 40
                            && plugin.facilityRegistry()
                            .require(DefaultFacilityCatalog.ENGINEERING)
                            .status() == FacilityStatus.NORMAL;
            case "hull_breach" ->
                    ship.hull() >= 50 && ship.oxygen() >= 50;
            case "total_power_failure" ->
                    ship.power() >= 50
                            && plugin.facilityRegistry()
                            .require(DefaultFacilityCatalog.ENGINEERING)
                            .status() != FacilityStatus.OFFLINE;
            default -> false;
        };
    }

    private java.util.List<Player> onlineParticipants() {
        return plugin.lobbyService().snapshot().players().stream()
                .map(id -> plugin.getServer().getPlayer(id.value()))
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .toList();
    }

    private static String targetFacility(String id) {
        return switch (id) {
            case "cargo_damage", "power_cell_depletion" -> "화물실";
            case "medical_contamination", "mass_infection" -> "의료실";
            case "reactor_runaway", "total_power_failure",
                    "local_oxygen_drop", "small_fire", "hull_breach" -> "기관실";
            case "comms_noise" -> "함교";
            case "alien_intrusion" -> "침입 감지 구역 / 보안 대응";
            case "door_fault" -> "고장 난 연결 통로";
            default -> "함선 상태 확인";
        };
    }

    private static String responseHint(String id) {
        return switch (id) {
            case "cargo_damage" -> "수리 부품 1개를 입고한 뒤 [화물실 설비 복구] 실행";
            case "power_cell_depletion" -> "전력 셀을 확보해 공용 창고에 입고";
            case "medical_contamination" -> "의료 물자 1개를 입고한 뒤 [의료실 오염 제거] 실행";
            case "mass_infection" -> "의료실 격리 상태와 감염 대응 절차 확인";
            case "reactor_runaway" -> "기관실에서 원자로 안정화 및 수리";
            case "total_power_failure" -> "기관실 상태 복구 후 전력 셀 투입";
            case "local_oxygen_drop" -> "수리 부품 1개를 입고한 뒤 기관실 [산소 계통 복구] 실행";
            case "small_fire", "hull_breach" -> "수리 부품을 준비해 기관실에서 선체 복구";
            case "comms_noise" -> "항법/통신 담당이 함교에서 장거리 통신 복구";
            case "alien_intrusion" -> "주변을 경계하고 보안·연구 장비로 대응";
            case "door_fault" -> "다른 경로를 찾거나 고급 수리로 통로 복구";
            default -> "PDA와 HUD의 최우선 행동 확인";
        };
    }
}

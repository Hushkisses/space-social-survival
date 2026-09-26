package com.hushkisses.spacesurvival.paper.event;

import com.hushkisses.spacesurvival.ending.ReturnRequirements;
import com.hushkisses.spacesurvival.event.GameEventDefinition;
import com.hushkisses.spacesurvival.event.GameEventScale;
import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.bukkit.Sound;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class IncidentPresentationService {

    private final SpaceSurvivalPlugin plugin;
    private final ReturnRequirements requirements = ReturnRequirements.developmentDefaults();
    private final Map<String, TrackedIncident> active = new LinkedHashMap<>();

    private BukkitTask task;
    private BossBar majorBar;
    private String displayedMajorId;

    public IncidentPresentationService(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void start() {
        if (task != null) return;

        majorBar = plugin.getServer().createBossBar(
                "§c함선 사건",
                org.bukkit.boss.BarColor.RED,
                BarStyle.SOLID
        );
        majorBar.setVisible(false);

        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::checkResolution,
                20L,
                20L
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        active.clear();
        displayedMajorId = null;
        if (majorBar != null) {
            majorBar.removeAll();
            majorBar.setVisible(false);
            majorBar = null;
        }
    }

    public void present(GameEventDefinition event) {
        Objects.requireNonNull(event, "event");

        IncidentHint hint = hint(event.id().value());
        for (Player player : participants()) {
            if (event.scale() == GameEventScale.LARGE) {
                player.sendTitle(
                        "§c§l" + event.displayName(),
                        "§f" + hint.target() + " · " + hint.action(),
                        5,
                        60,
                        15
                );
                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_BEACON_DEACTIVATE,
                        1.0f,
                        0.75f
                );
            } else {
                player.sendActionBar(
                        net.kyori.adventure.text.Component.text(
                                "사건 · " + event.displayName()
                                        + " → " + hint.target()
                                        + " | " + hint.need()
                        )
                );
                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_BELL,
                        0.65f,
                        1.1f
                );
            }

            player.sendMessage(
                    (event.scale() == GameEventScale.LARGE
                            ? "§c[대형 사건] §f"
                            : "§e[소형 사건] §f")
                            + event.displayName()
            );
            player.sendMessage("§7" + event.description());
            player.sendMessage("§7대응 위치: §e" + hint.target());
            player.sendMessage("§7필요: §f" + hint.need());
            player.sendMessage("§7권장 행동: §f" + hint.action());
        }

        if (trackable(event.id().value())) {
            active.put(
                    event.id().value(),
                    new TrackedIncident(event, hint)
            );
        }

        if (event.scale() == GameEventScale.LARGE) {
            showMajorBar(event, hint);
        }
    }

    public void clear() {
        active.clear();
        displayedMajorId = null;
        if (majorBar != null) {
            majorBar.removeAll();
            majorBar.setVisible(false);
        }
    }

    private void checkResolution() {
        if (active.isEmpty()) {
            return;
        }

        var iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!isResolved(entry.getKey())) {
                continue;
            }

            TrackedIncident incident = entry.getValue();
            iterator.remove();
            plugin.gameEventRuntimeState().setFlag(entry.getKey(), false);

            for (Player player : participants()) {
                player.sendMessage(
                        "§a[사건 안정화] §f"
                                + incident.event().displayName()
                                + " §7— 관련 공개 문제가 안정화되었습니다."
                );
                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_CHIME,
                        0.8f,
                        1.35f
                );
            }

            if (entry.getKey().equals(displayedMajorId)) {
                hideMajorBar();
            }
        }
    }

    private void showMajorBar(GameEventDefinition event, IncidentHint hint) {
        if (majorBar == null) return;

        majorBar.removeAll();
        majorBar.setColor(org.bukkit.boss.BarColor.RED);
        majorBar.setTitle(
                "§c§l" + event.displayName()
                        + " §7— §f" + hint.target()
                        + " §7| §e" + hint.need()
        );
        majorBar.setProgress(1.0);
        for (Player player : participants()) {
            majorBar.addPlayer(player);
        }
        majorBar.setVisible(true);
        displayedMajorId = event.id().value();
    }

    private void hideMajorBar() {
        displayedMajorId = null;
        if (majorBar == null) return;
        majorBar.removeAll();
        majorBar.setVisible(false);
    }

    private boolean isResolved(String id) {
        var ship = plugin.shipState().snapshot();

        return switch (id) {
            case "door_fault" -> !plugin.gameEventRuntimeState().hasFlag("door_fault");
            case "local_oxygen_drop" -> ship.oxygen() >= requirements.minOxygen();
            case "cargo_damage" -> plugin.facilityRegistry()
                    .require(DefaultFacilityCatalog.CARGO)
                    .status() == FacilityStatus.NORMAL;
            case "medical_contamination" -> plugin.facilityRegistry()
                    .require(DefaultFacilityCatalog.MEDICAL)
                    .status() == FacilityStatus.NORMAL;
            case "small_fire" -> ship.hull() >= requirements.minHull();
            case "comms_noise" -> !plugin.radioRuntimeState().communicationsOutage();
            case "reactor_runaway" -> ship.reactor() >= requirements.minReactor()
                    && plugin.facilityRegistry()
                    .require(DefaultFacilityCatalog.ENGINEERING)
                    .status() == FacilityStatus.NORMAL;
            case "hull_breach" -> ship.hull() >= requirements.minHull()
                    && ship.oxygen() >= requirements.minOxygen();
            case "mass_infection" -> plugin.facilityRegistry()
                    .require(DefaultFacilityCatalog.MEDICAL)
                    .status() != FacilityStatus.QUARANTINED;
            case "total_power_failure" -> ship.power() >= requirements.minPower()
                    && plugin.facilityRegistry()
                    .require(DefaultFacilityCatalog.ENGINEERING)
                    .status() != FacilityStatus.OFFLINE;
            default -> false;
        };
    }

    private boolean trackable(String id) {
        return switch (id) {
            case "door_fault",
                 "local_oxygen_drop",
                 "cargo_damage",
                 "medical_contamination",
                 "small_fire",
                 "comms_noise",
                 "reactor_runaway",
                 "hull_breach",
                 "mass_infection",
                 "total_power_failure" -> true;
            default -> false;
        };
    }

    private IncidentHint hint(String id) {
        return switch (id) {
            case "lighting_outage" -> new IncidentHint(
                    "기관실",
                    "전력 계통 점검",
                    "기관실에서 함선 전력 상태를 확인하십시오."
            );
            case "door_fault" -> new IncidentHint(
                    "기관실",
                    "엔지니어 + 멀티툴",
                    "고급 수리로 고장 난 통로를 복구하십시오."
            );
            case "local_oxygen_drop" -> new IncidentHint(
                    "기관실",
                    "산소·함선 상태 점검",
                    "함선 상태를 확인하고 우선 생존 수치를 안정화하십시오."
            );
            case "power_cell_depletion" -> new IncidentHint(
                    "화물실",
                    "전력 셀",
                    "보급 상자에서 전력 셀을 확보해 화물실에 입고하십시오."
            );
            case "cargo_damage" -> new IncidentHint(
                    "화물실",
                    "시설 상태 점검",
                    "화물실 콘솔에서 사용 가능한 대응 행동을 확인하십시오."
            );
            case "medical_contamination" -> new IncidentHint(
                    "의료실",
                    "의료 물자·시설 점검",
                    "의료실 상태와 사용 가능한 치료 기능을 확인하십시오."
            );
            case "small_fire" -> new IncidentHint(
                    "기관실",
                    "수리 부품",
                    "기관실에서 선체 안정도를 복구하십시오."
            );
            case "comms_noise" -> new IncidentHint(
                    "함교",
                    "항법/통신 담당 + 장거리 무전기",
                    "장거리 통신 계통을 정상화하십시오."
            );
            case "reactor_runaway" -> new IncidentHint(
                    "기관실",
                    "연료·수리 부품",
                    "원자로 출력과 기관실 상태를 안정화하십시오."
            );
            case "hull_breach" -> new IncidentHint(
                    "기관실",
                    "수리 부품",
                    "선체와 산소 수치를 우선 복구하십시오."
            );
            case "mass_infection" -> new IncidentHint(
                    "의료실",
                    "의료 물자·의료 담당",
                    "격리 상태와 감염 대응 기능을 확인하십시오."
            );
            case "alien_intrusion" -> new IncidentHint(
                    "연구실",
                    "보안 경계·연구 스캐너",
                    "침입 흔적을 경계하고 연구실에서 관련 신호를 분석하십시오."
            );
            case "total_power_failure" -> new IncidentHint(
                    "기관실",
                    "전력 셀·수리 부품",
                    "기관실과 주 전력 계통을 긴급 복구하십시오."
            );
            default -> new IncidentHint(
                    "함선 상태 확인",
                    "PDA 공용 상태",
                    "PDA와 HUD에서 현재 최우선 공개 문제를 확인하십시오."
            );
        };
    }

    private java.util.List<Player> participants() {
        return plugin.lobbyService().snapshot().players().stream()
                .map(PlayerId::value)
                .map(plugin.getServer()::getPlayer)
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .toList();
    }

    private record IncidentHint(String target, String need, String action) {
    }

    private record TrackedIncident(GameEventDefinition event, IncidentHint hint) {
    }
}

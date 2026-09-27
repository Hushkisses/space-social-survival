package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.item.FunctionalItemType;
import com.hushkisses.spacesurvival.paper.match.MatchSetupSnapshot;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class PlaytestBuildCommandHandler {

    private final SpaceSurvivalPlugin plugin;

    public PlaytestBuildCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean supports(String root) {
        return root.equalsIgnoreCase("playtest")
                || root.equalsIgnoreCase("playability")
                || root.equalsIgnoreCase("telemetry")
                || root.equalsIgnoreCase("item");
    }

    public boolean handle(CommandSender sender, String[] args) {
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "playtest" -> playtest(sender, args);
            case "playability" -> playability(sender, args);
            case "telemetry" -> telemetry(sender, args);
            case "item" -> item(sender, args);
            default -> false;
        };
    }

    private boolean playability(CommandSender sender, String[] args) {
        if (args.length < 2
                || args[1].equalsIgnoreCase("check")
                || args[1].equalsIgnoreCase("status")) {
            return playabilityCheck(sender);
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> playabilityStart(sender, args);
            case "reset" -> playabilityReset(sender);
            case "save" -> playabilitySave(sender);
            case "help" -> {
                sendPlayabilityUsage(sender);
                yield true;
            }
            default -> {
                sendPlayabilityUsage(sender);
                yield true;
            }
        };
    }

    private boolean playabilityStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§cPX-010 플레이 가능성 검증 시작 권한이 없습니다.");
            return true;
        }

        if (sender instanceof Player player
                && !plugin.lobbyService().contains(
                        com.hushkisses.spacesurvival.player.PlayerId.of(player.getUniqueId())
                )
                && plugin.lobbyService().gameSession().isEmpty()) {
            plugin.lobbyService().join(
                    com.hushkisses.spacesurvival.player.PlayerId.of(player.getUniqueId())
            );
        }

        var lobby = plugin.lobbyService().snapshot();
        if (lobby.playerCount() < 1 || lobby.playerCount() > 3) {
            sender.sendMessage(
                    "§cPX-010은 1~3인 검증 전용입니다. 현재 참가자: "
                            + lobby.playerCount()
                            + "명"
            );
            return true;
        }

        if (plugin.matchOrchestrator().status()
                != com.hushkisses.spacesurvival.paper.match.MatchLifecycleStatus.IDLE) {
            sender.sendMessage("§c이미 준비 또는 진행 중인 매치가 있습니다. 먼저 reset 하십시오.");
            return true;
        }

        long seed = parseSeed(sender, args, 2);
        if (seed == Long.MIN_VALUE) return true;

        try {
            MatchSetupSnapshot snapshot = plugin.matchOrchestrator().prepare(seed, true);
            plugin.telemetryService().event("playability", "px010-start");

            sender.sendMessage("§6========== PX-010 플레이 가능성 검증 ==========");
            sender.sendMessage("§a1~3인 개발 검증 매치를 준비했습니다.");
            sender.sendMessage("§7참가자: §f" + lobby.playerCount() + "명");
            sender.sendMessage("§7시드: §f" + seed);
            sender.sendMessage("§7함선 모듈: §f" + snapshot.generatedMap().tileIds().size());
            sender.sendMessage("§e이후에는 직업 선택·시설 작업·자원 사용·회의·귀환을 실제 플레이로 진행하십시오.");
            sender.sendMessage("§7점검: §f/space playability check");
            sender.sendMessage("§7강제 조건 도구: §f/space playability help");
        } catch (IllegalStateException exception) {
            sender.sendMessage("§cPX-010 검증 매치를 시작할 수 없습니다: " + exception.getMessage());
        }
        return true;
    }

    private boolean playabilityReset(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§cPX-010 검증 초기화 권한이 없습니다.");
            return true;
        }
        plugin.matchOrchestrator().reset();
        sender.sendMessage("§aPX-010 매치를 초기화했습니다.");
        sender.sendMessage("§7재시작 안정성 검증을 위해 다시 /space playability start [seed] 를 실행할 수 있습니다.");
        return true;
    }

    private boolean playabilitySave(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§cPX-010 텔레메트리 저장 권한이 없습니다.");
            return true;
        }
        File file = plugin.telemetryService().saveCurrent("px010-playability");
        sender.sendMessage("§aPX-010 텔레메트리를 저장했습니다: " + file.getAbsolutePath());
        return true;
    }

    private boolean playabilityCheck(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§cPX-010 점검 권한이 없습니다.");
            return true;
        }

        var telemetry = plugin.telemetryService().snapshot();
        Map<String, Long> counters = telemetry.counters();
        var lobby = plugin.lobbyService().snapshot();

        int participants = telemetry.playerCount() > 0
                ? telemetry.playerCount()
                : lobby.playerCount();

        boolean onboarding = counter(counters, "playability.onboarding.shown")
                >= Math.max(1, participants);
        boolean roles = counter(counters, "playability.role.selected")
                >= Math.max(1, participants);
        boolean privateObjectives = counter(counters, "playability.private_objective.available")
                >= Math.max(1, participants);
        boolean starterEquipment = counter(counters, "playability.starter.granted")
                >= Math.max(1, participants);

        boolean shipNavigation = plugin.matchOrchestrator().setupSnapshot()
                .map(snapshot -> !snapshot.generatedMap().connections().isEmpty())
                .orElse(false)
                && plugin.shipWorldService().activeSnapshot().isPresent();

        boolean caches = counter(counters, "resource.cache.count") > 0;
        boolean facilityUsed = counter(counters, "facility.action.success") > 0;
        boolean incidentExperienced = counter(counters, "incident.total") > 0;
        boolean meetingResolved = counter(counters, "meeting.resolved") > 0;

        boolean terminalState = counter(counters, "death.total") > 0
                || counter(counters, "sanction.physical.total") > 0;

        boolean resultProduced = plugin.matchResultRuntimeService().lastResult().isPresent();
        boolean telemetrySaved = telemetry.lastSavedFile() != null;

        long elapsedSeconds = elapsedSeconds(telemetry.startedAt(), telemetry.endedAt());
        boolean twentyMinutes = elapsedSeconds >= Duration.ofMinutes(20).toSeconds();

        boolean rematchStable = plugin.matchOrchestrator().developmentStartCount() >= 2
                && plugin.matchOrchestrator().resetCount() >= 1;

        int passed = 0;
        int total = 14;

        sender.sendMessage("§6========== PX-010 자동 점검 ==========");
        sender.sendMessage("§7참가자 기준: §f" + participants + "명");
        passed += check(sender, "온보딩 표시", onboarding,
                "표시 " + counter(counters, "playability.onboarding.shown") + "회");
        passed += check(sender, "직업 선택 완료", roles,
                counter(counters, "playability.role.selected") + "/" + participants);
        passed += check(sender, "개인 목표 사용 가능", privateObjectives,
                counter(counters, "playability.private_objective.available") + "/" + participants);
        passed += check(sender, "시작 장비 지급", starterEquipment,
                counter(counters, "playability.starter.granted") + "/" + participants);
        passed += check(sender, "함선/길찾기 생성", shipNavigation,
                "물리 함선 + 논리 연결");
        passed += check(sender, "보급 상자 생성", caches,
                "상자 " + counter(counters, "resource.cache.count") + "개");
        passed += check(sender, "시설 실제 사용", facilityUsed,
                "성공 " + counter(counters, "facility.action.success") + "회");
        passed += check(sender, "사건 대응 루프 경험", incidentExperienced,
                "사건 " + counter(counters, "incident.total") + "회");
        passed += check(sender, "회의 흐름 완료", meetingResolved,
                "완료 " + counter(counters, "meeting.resolved") + "회");
        passed += check(sender, "사망/제재 상태 경험", terminalState,
                "사망 " + counter(counters, "death.total") + "회");
        passed += check(sender, "귀환/결과 생성", resultProduced,
                resultProduced ? "최종 결과 존재" : "아직 결과 없음");
        passed += check(sender, "텔레메트리 파일 저장", telemetrySaved,
                telemetrySaved ? telemetry.lastSavedFile() : "저장 파일 없음");
        passed += check(sender, "20분 이상 실제 플레이", twentyMinutes,
                formatDuration(Duration.ofSeconds(elapsedSeconds)) + " / 20:00");
        passed += check(sender, "리셋 후 재시작", rematchStable,
                "개발 시작 " + plugin.matchOrchestrator().developmentStartCount()
                        + "회 / 리셋 " + plugin.matchOrchestrator().resetCount() + "회");

        sender.sendMessage("§6자동 증거: §f" + passed + "§7/§f" + total);
        sender.sendMessage("§e블라인드 관찰이 필요한 항목은 자동으로 합격 처리하지 않습니다:");
        sender.sendMessage("§7- 60초 안에 다음 행동을 스스로 찾는가");
        sender.sendMessage("§7- 외부 설명 없이 기관실·화물실을 찾는가");
        sender.sendMessage("§7- 직업 장비와 자원 2종의 용도를 설명할 수 있는가");
        sender.sendMessage("§7- 관리자 설명 없이 사건·회의·상태변화를 이해하는가");
        sender.sendMessage("§8강제 명령은 상황 생성에만 사용하고 해결 자체는 실제 플레이로 진행하십시오.");
        return true;
    }

    private static int check(
            CommandSender sender,
            String label,
            boolean passed,
            String detail
    ) {
        sender.sendMessage(
                (passed ? "§a[통과] " : "§e[대기] ")
                        + "§f"
                        + label
                        + " §8— "
                        + detail
        );
        return passed ? 1 : 0;
    }

    private static long counter(Map<String, Long> counters, String key) {
        return counters.getOrDefault(key, 0L);
    }

    private static long elapsedSeconds(Instant startedAt, Instant endedAt) {
        if (startedAt == null) return 0L;
        Instant end = endedAt == null ? Instant.now() : endedAt;
        return Math.max(0L, Duration.between(startedAt, end).toSeconds());
    }

    private static String formatDuration(Duration duration) {
        long seconds = duration.toSeconds();
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private static void sendPlayabilityUsage(CommandSender sender) {
        sender.sendMessage("§6[PX-010] §f1~3인 End-to-End 검증 도구");
        sender.sendMessage("§e/space playability start [seed] §7- 1~3인 검증 매치 시작");
        sender.sendMessage("§e/space playability check §7- 자동 증거 체크리스트");
        sender.sendMessage("§e/space playability reset §7- 매치 초기화/재시작 검증");
        sender.sendMessage("§e/space playability save §7- 현재 텔레메트리 저장");
        sender.sendMessage("§7상황 생성: /space director force <small|large>");
        sender.sendMessage("§7테스트 자원: /space resource add <shared|self> <type> <amount>");
        sender.sendMessage("§7최종 홀드 단축: /space return reset [holdSeconds]");
        sender.sendMessage("§8강제 도구는 테스트 조건 생성용이며 시설/자원/목표 플레이를 대신하면 안 됩니다.");
    }

    private boolean playtest(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            return playtestStatus(sender);
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> playtestStart(sender, args);
            case "reset" -> playtestReset(sender);
            case "preflight" -> playtestPreflight(sender);
            case "postmatch" -> playtestPostmatch(sender);
            default -> {
                sender.sendMessage("§c사용법: /space playtest status");
                sender.sendMessage("§c사용법: /space playtest start [seed]");
                sender.sendMessage("§c사용법: /space playtest reset");
                sender.sendMessage("§c사용법: /space playtest preflight");
                sender.sendMessage("§c사용법: /space playtest postmatch");
                yield true;
            }
        };
    }

    private boolean playtestStatus(CommandSender sender) {
        var lobby = plugin.lobbyService().snapshot();
        boolean playerReady = lobby.playerCount() >= lobby.minPlayers()
                && lobby.playerCount() <= lobby.maxPlayers()
                && !lobby.started();

        sender.sendMessage("§6[우주 생존] §f첫 멀티 플레이테스트 준비 상태");
        sender.sendMessage(
                "§7참가자: "
                        + (playerReady ? "§a" : "§e")
                        + lobby.playerCount()
                        + "§7/§f"
                        + lobby.minPlayers()
                        + "~"
                        + lobby.maxPlayers()
        );
        sender.sendMessage(
                "§7플레이테스트 시작 가능: "
                        + (playerReady ? "§a예" : "§c아니요")
        );
        sender.sendMessage(
                "§7ItemsAdder: §f"
                        + (plugin.resourceItemProvider().customItemsAvailable()
                        ? "연결됨"
                        : "미연결 — 바닐라 폴백 사용")
        );
        sender.sendMessage(
                "§7Simple Voice Chat: §f"
                        + plugin.simpleVoiceChatBridge().status()
        );
        sender.sendMessage(
                "§7PvE: §f"
                        + plugin.pveMobSpawner().backendStatus()
        );
        sender.sendMessage(
                "§7NBT 폴더: §f"
                        + plugin.shipWorldService()
                        .structureLoader()
                        .structuresDirectory()
                        .getAbsolutePath()
        );
        sender.sendMessage(
                "§7텔레메트리 폴더: §f"
                        + plugin.telemetryService().directory().getAbsolutePath()
        );

        var structurePlacements = plugin.shipWorldService().structurePlacements();
        if (!structurePlacements.isEmpty()) {
            long nbt = structurePlacements.values().stream()
                    .filter(result -> result.placed())
                    .count();
            sender.sendMessage(
                    "§7현재 함선 NBT: §f"
                            + nbt
                            + "/"
                            + structurePlacements.size()
                            + " §8(나머지는 폴백)"
            );
        }
        return true;
    }

    private boolean playtestPreflight(CommandSender sender) {
        var lobby = plugin.lobbyService().snapshot();
        long onlineParticipants = lobby.players().stream()
                .map(com.hushkisses.spacesurvival.player.PlayerId::value)
                .map(plugin.getServer()::getPlayer)
                .filter(Objects::nonNull)
                .count();

        boolean countReady = lobby.playerCount() >= lobby.minPlayers()
                && lobby.playerCount() <= lobby.maxPlayers();
        boolean allOnline = onlineParticipants == lobby.playerCount();
        boolean idle = !lobby.started()
                && plugin.matchOrchestrator().status()
                == com.hushkisses.spacesurvival.paper.match.MatchLifecycleStatus.IDLE;

        sender.sendMessage("§6========== 플레이테스트 Preflight ==========");
        sender.sendMessage("§7인원 규칙: " + (countReady ? "§a통과" : "§c실패")
                + " §f(" + lobby.playerCount() + "/" + lobby.minPlayers() + "~" + lobby.maxPlayers() + ")");
        sender.sendMessage("§7참가자 접속: " + (allOnline ? "§a통과" : "§c실패")
                + " §f(" + onlineParticipants + "/" + lobby.playerCount() + ")");
        sender.sendMessage("§7매치 상태: " + (idle ? "§a대기" : "§c진행/준비 중"));
        sender.sendMessage("§7ItemsAdder: §f"
                + (plugin.resourceItemProvider().customItemsAvailable() ? "연결됨" : "바닐라 폴백"));
        sender.sendMessage("§7Voice: §f" + plugin.simpleVoiceChatBridge().status());
        sender.sendMessage("§7PvE: §f" + plugin.pveMobSpawner().backendStatus());
        sender.sendMessage("§7NBT: §f"
                + plugin.shipWorldService().structureLoader().structuresDirectory().getAbsolutePath());
        sender.sendMessage("§7Telemetry: §f"
                + plugin.telemetryService().directory().getAbsolutePath());
        sender.sendMessage("§7최종 판정: "
                + (countReady && allOnline && idle ? "§a시작 가능" : "§e점검 필요"));
        return true;
    }

    private boolean playtestPostmatch(CommandSender sender) {
        sender.sendMessage("§6========== 플레이테스트 Postmatch ==========");

        var result = plugin.matchResultRuntimeService().lastResult().orElse(null);
        if (result == null) {
            sender.sendMessage("§e아직 최종 경기 결과가 없습니다.");
        } else {
            sender.sendMessage("§7공통 귀환: "
                    + (result.commonMissionCompleted() ? "§a성공" : "§c실패"));
            sender.sendMessage("§7승리자 수: §f" + result.winners().size());
            sender.sendMessage("§7MVP 수: §f" + result.mvps().size());
            sender.sendMessage("§7점수: §f" + result.players().stream()
                    .map(player -> player.score().total())
                    .toList());
        }

        var telemetry = plugin.telemetryService().snapshot();
        sender.sendMessage("§7시드: §f" + telemetry.seed());
        sender.sendMessage("§7인원: §f" + telemetry.playerCount());
        sender.sendMessage("§7시나리오: §f" + telemetry.scenario());
        sender.sendMessage("§7기록 이벤트 수: §f" + telemetry.eventCount());
        sender.sendMessage("§7카운터: §f" + telemetry.counters());
        sender.sendMessage("§7텔레메트리 파일: §f"
                + (telemetry.lastSavedFile() == null ? "없음" : telemetry.lastSavedFile()));
        return true;
    }

    private boolean playtestStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c플레이테스트 시작 권한이 없습니다.");
            return true;
        }

        long seed = parseSeed(sender, args, 2);
        if (seed == Long.MIN_VALUE) return true;

        try {
            MatchSetupSnapshot snapshot = plugin.matchOrchestrator()
                    .prepare(seed, false);
            sender.sendMessage("§a실제 인원 규칙으로 플레이테스트를 준비했습니다.");
            sender.sendMessage("§7시드: §f" + seed);
            sender.sendMessage("§7함선 모듈: §f" + snapshot.generatedMap().tileIds().size());
            sender.sendMessage("§7직업 선택이 끝나면 자동으로 ACTIVE가 됩니다.");
        } catch (IllegalStateException exception) {
            sender.sendMessage("§c플레이테스트를 시작할 수 없습니다: " + exception.getMessage());
        }
        return true;
    }

    private boolean playtestReset(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c플레이테스트 초기화 권한이 없습니다.");
            return true;
        }
        plugin.matchOrchestrator().reset();
        sender.sendMessage("§a플레이테스트 런타임을 초기화했습니다.");
        return true;
    }

    private boolean telemetry(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            var snapshot = plugin.telemetryService().snapshot();
            sender.sendMessage("§6[우주 생존] §f텔레메트리 상태");
            sender.sendMessage("§7활성: §f" + snapshot.active());
            sender.sendMessage("§7시드: §f" + snapshot.seed());
            sender.sendMessage("§7인원: §f" + snapshot.playerCount());
            sender.sendMessage("§7시나리오: §f" + snapshot.scenario());
            sender.sendMessage("§7이벤트 기록: §f" + snapshot.eventCount());
            sender.sendMessage("§7카운터: §f" + snapshot.counters());
            sender.sendMessage(
                    "§7최근 저장: §f"
                            + (snapshot.lastSavedFile() == null
                            ? "없음"
                            : snapshot.lastSavedFile())
            );
            return true;
        }

        if (args[1].equalsIgnoreCase("save")) {
            if (!sender.hasPermission("spacesurvival.admin")) {
                sender.sendMessage("§c텔레메트리 저장 권한이 없습니다.");
                return true;
            }
            File file = plugin.telemetryService().saveCurrent("manual");
            sender.sendMessage("§a텔레메트리를 저장했습니다: " + file.getAbsolutePath());
            return true;
        }

        sender.sendMessage("§c사용법: /space telemetry status");
        sender.sendMessage("§c사용법: /space telemetry save");
        return true;
    }

    private boolean item(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            sender.sendMessage("§6[우주 생존] §f기능성 아이템");
            for (FunctionalItemType type : FunctionalItemType.values()) {
                sender.sendMessage(
                        "§7- §f"
                                + type.name().toLowerCase(Locale.ROOT)
                                + " §8— "
                                + type.displayName()
                );
            }
            return true;
        }

        if (!args[1].equalsIgnoreCase("give") || args.length < 4) {
            sender.sendMessage("§c사용법: /space item list");
            sender.sendMessage("§c사용법: /space item give <player> <type>");
            return true;
        }

        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c기능성 아이템 지급 권한이 없습니다.");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage("§c접속 중인 플레이어를 찾을 수 없습니다.");
            return true;
        }

        FunctionalItemType type;
        try {
            type = FunctionalItemType.valueOf(args[3].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("§c알 수 없는 기능성 아이템입니다. /space item list");
            return true;
        }

        var item = plugin.functionalItemService().create(type);
        target.getInventory().addItem(item).values().forEach(
                overflow -> target.getWorld().dropItemNaturally(
                        target.getLocation(),
                        overflow
                )
        );
        plugin.functionalItemService().syncRadio(target);
        sender.sendMessage("§a" + target.getName() + "에게 " + type.displayName() + "§a을 지급했습니다.");
        return true;
    }

    private static long parseSeed(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            return ThreadLocalRandom.current().nextLong();
        }
        try {
            return Long.parseLong(args[index]);
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c시드는 정수여야 합니다.");
            return Long.MIN_VALUE;
        }
    }
}

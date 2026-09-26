package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.item.FunctionalItemType;
import com.hushkisses.spacesurvival.paper.match.MatchSetupSnapshot;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class PlaytestBuildCommandHandler {

    private final SpaceSurvivalPlugin plugin;

    public PlaytestBuildCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean supports(String root) {
        return root.equalsIgnoreCase("playtest")
                || root.equalsIgnoreCase("telemetry")
                || root.equalsIgnoreCase("item");
    }

    public boolean handle(CommandSender sender, String[] args) {
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "playtest" -> playtest(sender, args);
            case "telemetry" -> telemetry(sender, args);
            case "item" -> item(sender, args);
            default -> false;
        };
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

package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.lobby.LobbyJoinResult;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.match.MatchSetupSnapshot;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class MatchCommandHandler {

    private final SpaceSurvivalPlugin plugin;

    public MatchCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> start(sender, args, false);
            case "devstart" -> start(sender, args, true);
            case "status" -> status(sender);
            case "reset" -> reset(sender);
            case "bridge" -> bridge(sender);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean start(CommandSender sender, String[] args, boolean development) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c매치 시작 권한이 없습니다.");
            return true;
        }

        if (development
                && sender instanceof Player player
                && plugin.lobbyService().gameSession().isEmpty()) {
            LobbyJoinResult joinResult = plugin.lobbyService().join(
                    com.hushkisses.spacesurvival.player.PlayerId.of(player.getUniqueId())
            );

            if (joinResult == LobbyJoinResult.FULL) {
                sender.sendMessage("§c테스트 대기실 정원이 가득 차 강제 시작할 수 없습니다.");
                return true;
            }
            if (joinResult == LobbyJoinResult.MATCH_ALREADY_STARTED) {
                sender.sendMessage("§c이미 매치가 시작되어 강제 시작할 수 없습니다.");
                return true;
            }
        }

        long seed = parseSeed(sender, args, 2);
        if (seed == Long.MIN_VALUE) return true;

        try {
            MatchSetupSnapshot snapshot = plugin.matchOrchestrator()
                    .prepare(seed, development);

            sender.sendMessage(
                    development
                            ? "§a1인 테스트 강제 시작 준비가 완료되었습니다."
                            : "§a매치 준비가 완료되었습니다."
            );
            sender.sendMessage("§7시드: §f" + seed);
            sender.sendMessage("§7함선 모듈: §f" + snapshot.generatedMap().tileIds().size());
            sender.sendMessage("§7초기 사건: §f" + snapshot.initialEventCount());
            sender.sendMessage("§7갈등 축: §f" + snapshot.conflictAxes());
            sender.sendMessage("§8관리자 확인용 실제 시나리오: " + snapshot.scenario().definition().type());
            sender.sendMessage("§e플레이어가 직업을 모두 선택하면 ACTIVE 단계가 자동 시작됩니다.");
        } catch (IllegalStateException exception) {
            sender.sendMessage("§c매치를 준비할 수 없습니다: " + exception.getMessage());
        }
        return true;
    }

    private boolean status(CommandSender sender) {
        sender.sendMessage("§6[우주 생존] §f통합 매치 상태");
        sender.sendMessage("§7라이프사이클: §f" + plugin.matchOrchestrator().status());
        sender.sendMessage(
                "§7직업 선택: §f"
                        + plugin.roleSelectionService().selectedPlayerCount()
                        + "/"
                        + plugin.lobbyService().snapshot().playerCount()
        );

        MatchSetupSnapshot setup = plugin.matchOrchestrator().setupSnapshot().orElse(null);
        if (setup != null) {
            sender.sendMessage("§7시드: §f" + setup.seed());
            sender.sendMessage("§7함선 모듈: §f" + setup.generatedMap().tileIds().size());
            sender.sendMessage("§7초기 사건: §f" + setup.initialEventCount());

            if (sender.hasPermission("spacesurvival.admin")) {
                sender.sendMessage("§8실제 시나리오: " + setup.scenario().definition().type());
            } else {
                sender.sendMessage("§7실제 시나리오: §8기밀");
            }
        }
        return true;
    }

    private boolean reset(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c매치 초기화 권한이 없습니다.");
            return true;
        }

        plugin.matchOrchestrator().reset();
        sender.sendMessage("§a매치 런타임을 초기화하고 참가자를 대기실로 복귀시켰습니다.");
        return true;
    }

    private boolean bridge(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }

        var snapshot = plugin.shipWorldService().activeSnapshot().orElse(null);
        if (snapshot == null) {
            sender.sendMessage("§c아직 물리 함선이 생성되지 않았습니다.");
            return true;
        }

        player.teleportAsync(snapshot.bridgeSpawn());
        sender.sendMessage("§a함교로 이동합니다.");
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

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space match start [seed]");
        sender.sendMessage("§c사용법: /space match devstart [seed] §7- 최소 인원 무시 테스트 강제 시작");
        sender.sendMessage("§c사용법: /space match status");
        sender.sendMessage("§c사용법: /space match reset");
        sender.sendMessage("§c사용법: /space match bridge");
    }
}

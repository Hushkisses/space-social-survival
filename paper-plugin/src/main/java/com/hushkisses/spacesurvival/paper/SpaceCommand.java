package com.hushkisses.spacesurvival.paper;

import com.hushkisses.spacesurvival.core.BootstrapMarker;
import com.hushkisses.spacesurvival.lobby.LobbyJoinResult;
import com.hushkisses.spacesurvival.lobby.LobbyLeaveResult;
import com.hushkisses.spacesurvival.lobby.LobbySnapshot;
import com.hushkisses.spacesurvival.lobby.LobbyStartException;
import com.hushkisses.spacesurvival.paper.map.MapDebugService;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

final class SpaceCommand implements CommandExecutor {

    private final SpaceSurvivalPlugin plugin;
    private final MapDebugService mapDebugService = new MapDebugService();

    SpaceCommand(SpaceSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sendStatus(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("map")) {
            return handleMap(sender, args);
        }

        if (args[0].equalsIgnoreCase("lobby")) {
            return handleLobby(sender, args);
        }

        sendUsage(sender);
        return true;
    }

    private boolean handleLobby(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendLobbyUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "join" -> handleLobbyJoin(sender);
            case "leave" -> handleLobbyLeave(sender);
            case "status" -> {
                sendLobbyStatus(sender);
                yield true;
            }
            case "start" -> handleLobbyStart(sender);
            default -> {
                sendLobbyUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleLobbyJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }

        LobbyJoinResult result = plugin.lobbyService().join(
                PlayerId.of(player.getUniqueId())
        );

        switch (result) {
            case JOINED -> sender.sendMessage("§a대기실에 참가했습니다.");
            case ALREADY_JOINED -> sender.sendMessage("§e이미 대기실에 참가 중입니다.");
            case FULL -> sender.sendMessage("§c대기실 정원이 가득 찼습니다.");
            case MATCH_ALREADY_STARTED -> sender.sendMessage("§c이미 게임이 시작되어 참가할 수 없습니다.");
        }

        sendLobbyStatus(sender);
        return true;
    }

    private boolean handleLobbyLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }

        LobbyLeaveResult result = plugin.lobbyService().leave(
                PlayerId.of(player.getUniqueId())
        );

        switch (result) {
            case LEFT -> sender.sendMessage("§a대기실에서 나왔습니다.");
            case NOT_JOINED -> sender.sendMessage("§e현재 대기실에 참가하고 있지 않습니다.");
            case MATCH_ALREADY_STARTED -> sender.sendMessage("§c게임이 시작된 뒤에는 대기실에서 나갈 수 없습니다.");
        }

        sendLobbyStatus(sender);
        return true;
    }

    private boolean handleLobbyStart(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c게임 시작 권한이 없습니다.");
            return true;
        }

        try {
            plugin.lobbyService().start();
            sender.sendMessage("§a게임 시작 준비 단계로 전환했습니다.");
            sendLobbyStatus(sender);
        } catch (LobbyStartException exception) {
            LobbySnapshot snapshot = plugin.lobbyService().snapshot();
            sender.sendMessage(
                    "§c게임을 시작할 수 없습니다. 참가 인원: "
                            + snapshot.playerCount()
                            + "/"
                            + snapshot.minPlayers()
            );
        }

        return true;
    }

    private void sendLobbyStatus(CommandSender sender) {
        LobbySnapshot snapshot = plugin.lobbyService().snapshot();

        sender.sendMessage("§6[우주 생존] §f대기실 상태");
        sender.sendMessage(
                "§7참가 인원: §f"
                        + snapshot.playerCount()
                        + "§7/§f"
                        + snapshot.maxPlayers()
                        + " §8(최소 "
                        + snapshot.minPlayers()
                        + "명)"
        );
        sender.sendMessage("§7접속 중: §f" + snapshot.connectedPlayers());

        if (snapshot.started()) {
            sender.sendMessage("§7게임 단계: §e" + snapshot.gamePhase());
        } else {
            sender.sendMessage("§7게임 단계: §e대기 중");
        }
    }

    private boolean handleMap(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("generate")) {
            sender.sendMessage("§c사용법: /space map generate [seed]");
            return true;
        }

        long seed;
        if (args.length >= 3) {
            try {
                seed = Long.parseLong(args[2]);
            } catch (NumberFormatException exception) {
                sender.sendMessage("§c시드는 정수여야 합니다.");
                sender.sendMessage("§7사용법: /space map generate [seed]");
                return true;
            }
        } else {
            seed = ThreadLocalRandom.current().nextLong();
        }

        sendGeneratedMap(sender, mapDebugService.generate(seed));
        return true;
    }

    private void sendGeneratedMap(
            CommandSender sender,
            MapDebugService.DebugMapResult result
    ) {
        sender.sendMessage("§6[우주 생존] §f논리 맵 생성 결과");
        sender.sendMessage("§7시드: §f" + result.seed());
        sender.sendMessage("§7타일: §f" + result.map().tileIds().size());
        sender.sendMessage("§7연결: §f" + result.map().connections().size());
        sender.sendMessage("§7막다른 길: §f" + result.map().deadEndCount());

        sender.sendMessage("§e[타일]");
        for (String line : result.tileLines()) {
            sender.sendMessage("§7- §f" + line);
        }

        sender.sendMessage("§e[연결]");
        for (String line : result.connectionLines()) {
            sender.sendMessage("§7- §f" + line);
        }
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage("§6[우주 생존] §f개발 서버 상태");
        sender.sendMessage("§7플러그인: §a정상");
        sender.sendMessage("§7버전: §f" + plugin.getPluginMeta().getVersion());
        sender.sendMessage("§7코어: §f" + BootstrapMarker.moduleName());
        sender.sendMessage(
                "§7플레이어 설정: §f"
                        + plugin.configuration().game().minPlayers()
                        + "§7~§f"
                        + plugin.configuration().game().maxPlayers()
        );
        sender.sendMessage("§7현재 DEV: §eDEV-010 Lobby & Start Flow");
    }

    private void sendLobbyUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space lobby join");
        sender.sendMessage("§c사용법: /space lobby leave");
        sender.sendMessage("§c사용법: /space lobby status");
        sender.sendMessage("§c사용법: /space lobby start");
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space status");
        sender.sendMessage("§c사용법: /space lobby join|leave|status|start");
        sender.sendMessage("§c사용법: /space map generate [seed]");
    }
}

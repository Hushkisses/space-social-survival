package com.hushkisses.spacesurvival.paper;

import com.hushkisses.spacesurvival.core.BootstrapMarker;
import com.hushkisses.spacesurvival.paper.map.MapDebugService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

        sendUsage(sender);
        return true;
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
        sender.sendMessage("§7현재 DEV: §eDEV-009 Map Debug Tools");
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space status");
        sender.sendMessage("§c사용법: /space map generate [seed]");
    }
}

package com.hushkisses.spacesurvival.paper;

import com.hushkisses.spacesurvival.core.BootstrapMarker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

final class SpaceCommand implements CommandExecutor {

    private final SpaceSurvivalPlugin plugin;

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

        sender.sendMessage("§c사용법: /space status");
        return true;
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
        sender.sendMessage("§7현재 DEV: §eDEV-008 Constrained Random Map Generator");
    }
}

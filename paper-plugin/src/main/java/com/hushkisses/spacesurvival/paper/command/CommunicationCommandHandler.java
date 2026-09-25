package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Objects;

public final class CommunicationCommandHandler {

    private final SpaceSurvivalPlugin plugin;

    public CommunicationCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean supports(String root) {
        return root.equalsIgnoreCase("radio")
                || root.equalsIgnoreCase("voice");
    }

    public boolean handle(CommandSender sender, String[] args) {
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "radio" -> radio(sender, args);
            case "voice" -> voice(sender);
            default -> false;
        };
    }

    private boolean voice(CommandSender sender) {
        sender.sendMessage("§6[우주 생존] §f음성 통신 브리지");
        sender.sendMessage("§7Simple Voice Chat: §f" + plugin.simpleVoiceChatBridge().status());
        sender.sendMessage("§7사망자→생존자 통신: §c차단");
        sender.sendMessage("§7사망자 상호 통신: §a허용");
        return true;
    }

    private boolean radio(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            sender.sendMessage("§6[우주 생존] §f무전 통신 상태");
            sender.sendMessage("§7장거리 통신: §f" + plugin.radioRuntimeState().longRangeAvailable());
            sender.sendMessage("§7통신 장애: §f" + plugin.radioRuntimeState().communicationsOutage());
            sender.sendMessage("§7무전기 보유 인원: §f" + plugin.radioRuntimeState().radioHolders().size());
            return true;
        }

        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c무전 상태 변경 권한이 없습니다.");
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "give", "remove" -> radioHolder(sender, args);
            case "outage" -> radioOutage(sender, args);
            case "longrange" -> radioLongRange(sender, args);
            default -> {
                sender.sendMessage("§c사용법: /space radio status");
                sender.sendMessage("§c사용법: /space radio <give|remove> <player>");
                sender.sendMessage("§c사용법: /space radio outage <on|off>");
                sender.sendMessage("§c사용법: /space radio longrange <on|off>");
                yield true;
            }
        };
    }

    private boolean radioHolder(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c대상 플레이어 이름이 필요합니다.");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage("§c접속 중인 대상 플레이어를 찾을 수 없습니다.");
            return true;
        }

        boolean give = args[1].equalsIgnoreCase("give");
        plugin.radioRuntimeState().setRadio(PlayerId.of(target.getUniqueId()), give);
        sender.sendMessage(give ? "§a무전기를 지급 상태로 설정했습니다." : "§e무전기 보유 상태를 해제했습니다.");
        return true;
    }

    private boolean radioOutage(CommandSender sender, String[] args) {
        Boolean value = parseToggle(sender, args);
        if (value == null) return true;
        plugin.radioRuntimeState().setCommunicationsOutage(value);
        sender.sendMessage("§a통신 장애 상태를 변경했습니다.");
        return true;
    }

    private boolean radioLongRange(CommandSender sender, String[] args) {
        Boolean value = parseToggle(sender, args);
        if (value == null) return true;
        plugin.radioRuntimeState().setLongRangeEnabled(value);
        sender.sendMessage("§a장거리 통신 상태를 변경했습니다.");
        return true;
    }

    private static Boolean parseToggle(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c값은 on 또는 off여야 합니다.");
            return null;
        }
        if (args[2].equalsIgnoreCase("on")) return true;
        if (args[2].equalsIgnoreCase("off")) return false;
        sender.sendMessage("§c값은 on 또는 off여야 합니다.");
        return null;
    }
}

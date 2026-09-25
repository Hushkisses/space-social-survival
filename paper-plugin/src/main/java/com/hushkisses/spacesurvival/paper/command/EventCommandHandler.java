package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.event.*;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class EventCommandHandler {

    private final SpaceSurvivalPlugin plugin;

    public EventCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list" -> list(sender, args);
            case "trigger" -> trigger(sender, args);
            case "random" -> random(sender, args);
            case "status" -> status(sender);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean list(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /space event list <small|large>");
            return true;
        }

        GameEventScale scale = parseScale(sender, args[2]);
        if (scale == null) return true;

        sender.sendMessage("§6[우주 생존] §f" + scaleName(scale) + " 사건 목록");
        for (GameEventDefinition event : plugin.gameEventRegistry().byScale(scale)) {
            sender.sendMessage("§7- §f" + event.displayName() + " §8(" + event.id() + ")");
        }
        return true;
    }

    private boolean trigger(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c사건 발생 권한이 없습니다.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /space event trigger <eventId>");
            return true;
        }

        GameEventDefinition event = plugin.gameEventRegistry()
                .find(new GameEventId(args[2].toLowerCase(Locale.ROOT)))
                .orElse(null);

        if (event == null) {
            sender.sendMessage("§c존재하지 않는 사건 ID입니다.");
            return true;
        }

        plugin.gameEventEngine().trigger(event, plugin.gameEventContext());
        sender.sendMessage("§c[사건 발생] §f" + event.displayName());
        sender.sendMessage("§7" + event.description());
        return true;
    }

    private boolean random(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c사건 발생 권한이 없습니다.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /space event random <small|large> [seed]");
            return true;
        }

        GameEventScale scale = parseScale(sender, args[2]);
        if (scale == null) return true;

        long seed;
        if (args.length >= 4) {
            try {
                seed = Long.parseLong(args[3]);
            } catch (NumberFormatException exception) {
                sender.sendMessage("§c시드는 정수여야 합니다.");
                return true;
            }
        } else {
            seed = ThreadLocalRandom.current().nextLong();
        }

        GameEventDefinition event = new GameEventSelector().select(
                plugin.gameEventRegistry(),
                scale,
                new Random(seed)
        );
        plugin.gameEventEngine().trigger(event, plugin.gameEventContext());

        sender.sendMessage("§c[무작위 사건] §f" + event.displayName() + " §8시드=" + seed);
        return true;
    }

    private boolean status(CommandSender sender) {
        sender.sendMessage("§6[우주 생존] §f사건 상태");
        sender.sendMessage(
                "§7활성 플래그: §f"
                        + (plugin.gameEventRuntimeState().activeFlags().isEmpty()
                        ? "없음"
                        : plugin.gameEventRuntimeState().activeFlags())
        );
        sender.sendMessage("§7발생 이력 수: §f" + plugin.gameEventRuntimeState().history().size());

        if (!plugin.gameEventRuntimeState().history().isEmpty()) {
            var last = plugin.gameEventRuntimeState().history().getLast();
            sender.sendMessage("§7최근 사건: §f" + last.eventId());
        }
        return true;
    }

    private static GameEventScale parseScale(CommandSender sender, String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "small" -> GameEventScale.SMALL;
            case "large", "major" -> GameEventScale.LARGE;
            default -> {
                sender.sendMessage("§c사건 규모는 small 또는 large여야 합니다.");
                yield null;
            }
        };
    }

    private static String scaleName(GameEventScale scale) {
        return switch (scale) {
            case SMALL -> "소형";
            case MEDIUM -> "중형";
            case LARGE -> "대형";
        };
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space event list <small|large>");
        sender.sendMessage("§c사용법: /space event trigger <eventId>");
        sender.sendMessage("§c사용법: /space event random <small|large> [seed]");
        sender.sendMessage("§c사용법: /space event status");
    }
}

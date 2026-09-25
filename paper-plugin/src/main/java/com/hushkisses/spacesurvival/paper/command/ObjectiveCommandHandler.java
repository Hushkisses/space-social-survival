package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.objective.*;
import com.hushkisses.spacesurvival.objective.assignment.ObjectiveAssignmentService;
import com.hushkisses.spacesurvival.objective.conflict.*;
import com.hushkisses.spacesurvival.objective.secret.SecretMissionGrantResult;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class ObjectiveCommandHandler {

    private final SpaceSurvivalPlugin plugin;
    private List<ConflictSetDefinition> currentConflicts = List.of();

    public ObjectiveCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "prepare" -> prepare(sender, args);
            case "status" -> status(sender);
            case "secret" -> secret(sender, args);
            case "advance" -> advance(sender, args);
            case "catalog" -> catalog(sender);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean prepare(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c목표 배정 권한이 없습니다.");
            return true;
        }

        List<PlayerId> players = plugin.lobbyService().snapshot().players();
        if (players.isEmpty()) {
            sender.sendMessage("§c대기실 참가자가 없습니다.");
            return true;
        }

        long seed = parseSeed(sender, args, 2);
        if (seed == Long.MIN_VALUE) return true;

        plugin.resetObjectiveRuntime();

        ObjectiveRegistry registry = plugin.objectiveRegistry();
        currentConflicts = new ConflictSetSelector().select(
                DefaultConflictSetCatalog.create(registry),
                2,
                3,
                new Random(seed)
        );

        new ObjectiveAssignmentService(registry).assignBaseObjectives(
                players,
                currentConflicts,
                plugin.objectiveEngine(),
                new Random(seed ^ 0x5DEECE66DL)
        );

        sender.sendMessage("§a개인 기본 목표를 배정했습니다. 시드: " + seed);
        sender.sendMessage(
                "§7갈등 축: "
                        + currentConflicts.stream().map(set -> set.axis().name()).toList()
        );

        for (PlayerId playerId : players) {
            plugin.getServer().getPlayer(playerId.value());
            Player online = plugin.getServer().getPlayer(playerId.value());
            if (online != null) {
                ObjectiveInstance objective = plugin.objectiveEngine()
                        .objective(playerId, ObjectiveSlot.BASE)
                        .orElseThrow();
                online.sendMessage("§6[비공개 개인 목표] §f" + objective.definition().title());
                online.sendMessage("§7" + objective.definition().description());
            }
        }
        return true;
    }

    private boolean status(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c개인 목표 상태는 게임 안의 플레이어만 확인할 수 있습니다.");
            return true;
        }

        PlayerId playerId = PlayerId.of(player.getUniqueId());
        List<ObjectiveInstance> objectives = plugin.objectiveEngine().objectives(playerId);

        sender.sendMessage("§6[우주 생존] §f개인 목표");
        if (objectives.isEmpty()) {
            sender.sendMessage("§7배정된 목표가 없습니다.");
            return true;
        }

        for (ObjectiveInstance objective : objectives) {
            sender.sendMessage(
                    (objective.slot() == ObjectiveSlot.SECRET ? "§d[비밀] " : "§e[기본] ")
                            + "§f" + objective.definition().title()
            );
            sender.sendMessage(
                    "§7진행: " + objective.progress()
                            + "/" + objective.definition().targetProgress()
                            + " 상태=" + objective.status().name()
            );
        }
        return true;
    }

    private boolean secret(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c비밀 임무 디버그 배정은 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c비밀 임무 배정 권한이 없습니다.");
            return true;
        }

        long seed = parseSeed(sender, args, 2);
        if (seed == Long.MIN_VALUE) return true;

        SecretMissionGrantResult result = plugin.secretMissionService().grantRandom(
                PlayerId.of(player.getUniqueId()),
                List.copyOf(plugin.objectiveRegistry().all()),
                new Random(seed)
        );

        switch (result) {
            case GRANTED -> sender.sendMessage("§a비밀 임무를 추가했습니다.");
            case ALREADY_HAS_SECRET -> sender.sendMessage("§c이미 비밀 임무가 있습니다.");
            case NO_ELIGIBLE_OBJECTIVE -> sender.sendMessage("§c추가 가능한 비밀 임무가 없습니다.");
        }
        return true;
    }

    private boolean advance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c목표 진행 디버그는 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c목표 진행 권한이 없습니다.");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /space objective advance <base|secret> <amount>");
            return true;
        }

        ObjectiveSlot slot;
        try {
            slot = ObjectiveSlot.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("§c슬롯은 base 또는 secret이어야 합니다.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
            if (amount < 1) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c진행량은 1 이상의 정수여야 합니다.");
            return true;
        }

        ObjectiveInstance objective = plugin.objectiveEngine()
                .objective(PlayerId.of(player.getUniqueId()), slot)
                .orElse(null);
        if (objective == null) {
            sender.sendMessage("§c해당 슬롯에 목표가 없습니다.");
            return true;
        }

        objective.advance(amount);
        sender.sendMessage(
                "§a목표 진행: " + objective.definition().title()
                        + " " + objective.progress()
                        + "/" + objective.definition().targetProgress()
                        + " " + objective.status().name()
        );
        return true;
    }

    private boolean catalog(CommandSender sender) {
        sender.sendMessage("§6[우주 생존] §f목표 시스템");
        sender.sendMessage("§7초기 목표 수: §f" + plugin.objectiveRegistry().size());
        sender.sendMessage(
                "§7현재 갈등 축: §f"
                        + (currentConflicts.isEmpty()
                        ? "미선정"
                        : currentConflicts.stream().map(set -> set.axis().name()).toList())
        );
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
        sender.sendMessage("§c사용법: /space objective prepare [seed]");
        sender.sendMessage("§c사용법: /space objective status");
        sender.sendMessage("§c사용법: /space objective secret [seed]");
        sender.sendMessage("§c사용법: /space objective advance <base|secret> <amount>");
        sender.sendMessage("§c사용법: /space objective catalog");
    }
}

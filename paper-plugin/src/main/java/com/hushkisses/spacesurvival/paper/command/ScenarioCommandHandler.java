package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.infection.*;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.scenario.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class ScenarioCommandHandler {

    private final SpaceSurvivalPlugin plugin;

    public ScenarioCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean supports(String root) {
        return root.equalsIgnoreCase("scenario")
                || root.equalsIgnoreCase("infection");
    }

    public boolean handle(CommandSender sender, String[] args) {
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "scenario" -> scenario(sender, args);
            case "infection" -> infection(sender, args);
            default -> false;
        };
    }

    private boolean scenario(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendScenarioUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list" -> scenarioList(sender);
            case "start" -> scenarioStart(sender, args);
            case "status" -> scenarioStatus(sender);
            case "reset" -> scenarioReset(sender);
            default -> {
                sendScenarioUsage(sender);
                yield true;
            }
        };
    }

    private boolean scenarioList(CommandSender sender) {
        sender.sendMessage("§6[우주 생존] §f시나리오 목록");
        for (ScenarioDefinition definition : DefaultScenarioCatalog.create()) {
            sender.sendMessage("§7- §f" + definition.displayName() + " §8(" + definition.type().name().toLowerCase(Locale.ROOT) + ")");
        }
        return true;
    }

    private boolean scenarioStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c시나리오 시작 권한이 없습니다.");
            return true;
        }
        if (args.length < 3) {
            sendScenarioUsage(sender);
            return true;
        }

        ScenarioType type;
        try {
            type = ScenarioType.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("§c시나리오는 accident, sabotage, infection 중 하나여야 합니다.");
            return true;
        }

        long seed = parseSeed(sender, args, 3);
        if (seed == Long.MIN_VALUE) return true;

        List<PlayerId> participants = plugin.lobbyService().snapshot().players();
        if (participants.isEmpty()) {
            sender.sendMessage("§c대기실 참가자가 없습니다.");
            return true;
        }

        ScenarioDefinition definition = DefaultScenarioCatalog.create().stream()
                .filter(candidate -> candidate.type() == type)
                .findFirst()
                .orElseThrow();

        try {
            ScenarioRuntime runtime = plugin.scenarioEngine().activate(
                    definition,
                    participants,
                    new Random(seed)
            );

            plugin.getServer().broadcastMessage("§6[공개 브리핑] §f" + definition.publicBriefing());

            for (PlayerId hostile : runtime.initialHostiles()) {
                Player online = plugin.getServer().getPlayer(hostile.value());
                if (online != null) {
                    online.sendMessage("§4[기밀] §f당신에게 내부 공작 목표가 부여되었습니다.");
                    if (definition.hostilesKnowEachOther()) {
                        online.sendMessage("§7공작 인원 수: " + runtime.initialHostiles().size());
                    }
                }
            }

            sender.sendMessage("§a시나리오를 시작했습니다: " + definition.displayName() + " / seed=" + seed);
        } catch (IllegalStateException exception) {
            sender.sendMessage("§c이미 활성 시나리오가 있습니다. 먼저 reset 하십시오.");
        }
        return true;
    }

    private boolean scenarioStatus(CommandSender sender) {
        ScenarioRuntime runtime = plugin.scenarioEngine().active().orElse(null);
        sender.sendMessage("§6[우주 생존] §f시나리오 상태");

        if (runtime == null) {
            sender.sendMessage("§7활성 시나리오: §e없음");
            return true;
        }

        sender.sendMessage("§7공개 이름: §f" + runtime.definition().displayName());
        sender.sendMessage("§7초기 적대자 수: §f" + runtime.initialHostiles().size());
        if (sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§8숨은 진실: " + runtime.definition().hiddenTruth());
        }
        return true;
    }

    private boolean scenarioReset(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c시나리오 초기화 권한이 없습니다.");
            return true;
        }
        plugin.resetScenarioRuntime();
        sender.sendMessage("§a시나리오와 감염 런타임을 초기화했습니다.");
        return true;
    }

    private boolean infection(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendInfectionUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "outbreak" -> outbreak(sender, args);
            case "status" -> infectionStatus(sender, args);
            case "expose" -> infectionExpose(sender, args);
            case "advance" -> infectionAdvance(sender, args);
            case "test" -> infectionTest(sender, args);
            case "suppress" -> infectionSuppress(sender, args);
            case "cure" -> infectionCure(sender, args);
            default -> {
                sendInfectionUsage(sender);
                yield true;
            }
        };
    }

    private boolean outbreak(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c감염 사건 시작 권한이 없습니다.");
            return true;
        }

        ScenarioRuntime runtime = plugin.scenarioEngine().active().orElse(null);
        if (runtime == null || runtime.definition().type() != ScenarioType.INFECTION) {
            sender.sendMessage("§c현재 활성 시나리오가 감염 시나리오가 아닙니다.");
            return true;
        }

        long seed = parseSeed(sender, args, 2);
        if (seed == Long.MIN_VALUE) return true;

        try {
            PlayerId infected = plugin.infectionScenarioService().beginOutbreak(
                    runtime,
                    plugin.lobbyService().snapshot().players(),
                    plugin.infectionService(),
                    new Random(seed)
            );
            sender.sendMessage("§a감염 사건을 시작했습니다. 최초 감염자 UUID=" + infected.value());
            plugin.getServer().broadcastMessage("§c[사건] §f원인 불명의 감염 경보가 감지되었습니다.");
        } catch (IllegalStateException exception) {
            sender.sendMessage("§c감염 사건을 시작할 수 없습니다: " + exception.getMessage());
        }
        return true;
    }

    private boolean infectionStatus(CommandSender sender, String[] args) {
        Player target = resolvePlayer(sender, args, 2);
        if (target == null) return true;

        InfectionState state = plugin.infectionService().state(PlayerId.of(target.getUniqueId()));
        sender.sendMessage("§6[우주 생존] §f감염 상태: " + target.getName());
        sender.sendMessage("§7단계: §f" + state.stage());
        sender.sendMessage("§7진행도: §f" + state.progression());
        sender.sendMessage("§7억제: §f" + state.suppressed());
        return true;
    }

    private boolean infectionExpose(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) return denied(sender);
        Player target = resolvePlayer(sender, args, 2);
        if (target == null) return true;
        plugin.infectionService().expose(PlayerId.of(target.getUniqueId()));
        sender.sendMessage("§a감염 노출 상태를 적용했습니다.");
        return true;
    }

    private boolean infectionAdvance(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) return denied(sender);
        Player target = resolvePlayer(sender, args, 2);
        if (target == null) return true;
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /space infection advance <player> <amount>");
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

        plugin.infectionService().advance(PlayerId.of(target.getUniqueId()), amount);
        sender.sendMessage("§a감염 진행도를 변경했습니다.");
        return true;
    }

    private boolean infectionTest(CommandSender sender, String[] args) {
        Player target = resolvePlayer(sender, args, 2);
        if (target == null) return true;
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /space infection test <player> <basic|precise>");
            return true;
        }

        boolean precise;
        if (args[3].equalsIgnoreCase("precise")) precise = true;
        else if (args[3].equalsIgnoreCase("basic")) precise = false;
        else {
            sender.sendMessage("§c검사는 basic 또는 precise여야 합니다.");
            return true;
        }

        PlayerId targetId = PlayerId.of(target.getUniqueId());
        InfectionTestResult result = plugin.infectionService().test(targetId, precise);
        sender.sendMessage("§6[감염 검사] §f" + target.getName() + ": " + result.name());

        if (result == InfectionTestResult.POSITIVE) {
            plugin.pvpRuntimeState().setConfirmedInfected(targetId, true);
        }
        return true;
    }

    private boolean infectionSuppress(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) return denied(sender);
        Player target = resolvePlayer(sender, args, 2);
        if (target == null) return true;
        plugin.infectionService().suppress(PlayerId.of(target.getUniqueId()));
        sender.sendMessage("§a감염 진행을 억제했습니다.");
        return true;
    }

    private boolean infectionCure(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) return denied(sender);
        Player target = resolvePlayer(sender, args, 2);
        if (target == null) return true;

        PlayerId targetId = PlayerId.of(target.getUniqueId());
        plugin.infectionService().cure(targetId);
        plugin.pvpRuntimeState().setConfirmedInfected(targetId, false);
        sender.sendMessage("§a감염 상태를 제거했습니다.");
        return true;
    }

    private Player resolvePlayer(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            if (sender instanceof Player player) return player;
            sender.sendMessage("§c대상 플레이어 이름이 필요합니다.");
            return null;
        }

        Player target = plugin.getServer().getPlayerExact(args[index]);
        if (target == null) {
            sender.sendMessage("§c접속 중인 대상 플레이어를 찾을 수 없습니다.");
        }
        return target;
    }

    private static boolean denied(CommandSender sender) {
        sender.sendMessage("§c감염 상태 변경 권한이 없습니다.");
        return true;
    }

    private static long parseSeed(CommandSender sender, String[] args, int index) {
        if (args.length <= index) return ThreadLocalRandom.current().nextLong();
        try {
            return Long.parseLong(args[index]);
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c시드는 정수여야 합니다.");
            return Long.MIN_VALUE;
        }
    }

    private static void sendScenarioUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space scenario list");
        sender.sendMessage("§c사용법: /space scenario start <accident|sabotage|infection> [seed]");
        sender.sendMessage("§c사용법: /space scenario status");
        sender.sendMessage("§c사용법: /space scenario reset");
    }

    private static void sendInfectionUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space infection outbreak [seed]");
        sender.sendMessage("§c사용법: /space infection status [player]");
        sender.sendMessage("§c사용법: /space infection expose <player>");
        sender.sendMessage("§c사용법: /space infection advance <player> <amount>");
        sender.sendMessage("§c사용법: /space infection test <player> <basic|precise>");
        sender.sendMessage("§c사용법: /space infection suppress <player>");
        sender.sendMessage("§c사용법: /space infection cure <player>");
    }
}

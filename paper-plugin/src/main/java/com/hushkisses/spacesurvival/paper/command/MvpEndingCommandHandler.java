package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.death.DeathRecord;
import com.hushkisses.spacesurvival.death.InfectedPlayerState;
import com.hushkisses.spacesurvival.ending.FinalHoldSnapshot;
import com.hushkisses.spacesurvival.ending.ReturnStage;
import com.hushkisses.spacesurvival.objective.ObjectiveSlot;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.result.MatchResult;
import com.hushkisses.spacesurvival.result.PlayerMatchResult;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

public final class MvpEndingCommandHandler {

    private final SpaceSurvivalPlugin plugin;

    public MvpEndingCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean supports(String root) {
        return root.equalsIgnoreCase("death")
                || root.equalsIgnoreCase("infected")
                || root.equalsIgnoreCase("pve")
                || root.equalsIgnoreCase("return")
                || root.equalsIgnoreCase("result");
    }

    public boolean handle(CommandSender sender, String[] args) {
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "death" -> death(sender, args);
            case "infected" -> infected(sender, args);
            case "pve" -> pve(sender, args);
            case "return" -> returnFlow(sender, args);
            case "result" -> result(sender, args);
            default -> false;
        };
    }

    private boolean death(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            Player target = resolvePlayer(sender, args, 2);
            if (target == null) return true;

            PlayerId id = PlayerId.of(target.getUniqueId());
            DeathRecord record = plugin.deathService().record(id).orElse(null);

            sender.sendMessage("§6[우주 생존] §f사망 상태: " + target.getName());
            sender.sendMessage("§7생존: §f" + plugin.lobbyService().playerState(id)
                    .map(state -> state.isAlive())
                    .orElse(false));

            if (record == null) {
                sender.sendMessage("§7사망 기록: §e없음");
            } else {
                sender.sendMessage("§7원인: §f" + record.cause());
                sender.sendMessage("§7사망 당시 감염: §f" + record.infectedAtDeath());
            }
            return true;
        }

        if (args[1].equalsIgnoreCase("kill")) {
            if (!sender.hasPermission("spacesurvival.admin")) {
                sender.sendMessage("§c사망 테스트 권한이 없습니다.");
                return true;
            }

            Player target = resolvePlayer(sender, args, 2);
            if (target == null) return true;

            target.setHealth(0.0);
            sender.sendMessage("§e사망 이벤트를 발생시켰습니다: " + target.getName());
            return true;
        }

        sender.sendMessage("§c사용법: /space death status [player]");
        sender.sendMessage("§c사용법: /space death kill <player>");
        return true;
    }

    private boolean infected(CommandSender sender, String[] args) {
        Player target = resolvePlayer(sender, args, 2);
        if (target == null) return true;

        PlayerId id = PlayerId.of(target.getUniqueId());
        InfectedPlayerState state = plugin.infectedPlayerService().state(id).orElse(null);

        sender.sendMessage("§6[우주 생존] §f사망 후 상태: " + target.getName());
        if (state == null) {
            sender.sendMessage("§7사망 후 상태 기록이 없습니다.");
            return true;
        }

        sender.sendMessage("§7형태: §f" + state.form());
        sender.sendMessage("§7목표: §f" + state.goals());
        sender.sendMessage("§7수리 가능: §f" + state.canRepair());
        sender.sendMessage("§7무전 가능: §f" + state.canUseRadio());
        sender.sendMessage("§7일반 문 조작: §f" + state.canManipulateNormalDoors());
        return true;
    }

    private boolean pve(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            sender.sendMessage("§6[우주 생존] §fPvE 백엔드");
            sender.sendMessage("§7" + plugin.pveMobSpawner().backendStatus());
            sender.sendMessage("§7외부 몹 플러그인이 없으면 바닐라 엔티티 폴백을 사용합니다.");
            return true;
        }

        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§cPvE 소환 권한이 없습니다.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPvE 소환 테스트는 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!args[1].equalsIgnoreCase("spawn") || args.length < 3) {
            sender.sendMessage("§c사용법: /space pve spawn <infected|alien>");
            return true;
        }

        Entity entity = switch (args[2].toLowerCase(Locale.ROOT)) {
            case "infected" -> plugin.pveMobSpawner().spawnInfected(player.getLocation());
            case "alien" -> plugin.pveMobSpawner().spawnAlien(player.getLocation());
            default -> null;
        };

        if (entity == null) {
            sender.sendMessage("§c종류는 infected 또는 alien이어야 합니다.");
            return true;
        }

        sender.sendMessage("§aPvE 개체를 소환했습니다: " + entity.getType());
        return true;
    }

    private boolean returnFlow(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            sendReturnStatus(sender);
            return true;
        }

        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c귀환 디버그 권한이 없습니다.");
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "check" -> {
                boolean advanced = plugin.returnObjectiveService().advanceSurvivalSystems();
                sender.sendMessage(advanced
                        ? "§a생존 계통 복구를 확인했습니다. 항법 단계로 이동합니다."
                        : "§e생존 계통 조건이 아직 충족되지 않았거나 이미 다음 단계입니다.");
                sendReturnStatus(sender);
                yield true;
            }
            case "navigation" -> {
                sender.sendMessage(plugin.returnObjectiveService().markNavigationReady()
                        ? "§a항법 준비 완료."
                        : "§c현재 항법 단계가 아닙니다.");
                sendReturnStatus(sender);
                yield true;
            }
            case "prepare" -> {
                sender.sendMessage(plugin.returnObjectiveService().markReturnPreparationReady()
                        ? "§a귀환 준비 완료. 최종 버티기 단계가 열렸습니다."
                        : "§c현재 귀환 준비 단계가 아닙니다.");
                sendReturnStatus(sender);
                yield true;
            }
            case "hold" -> {
                try {
                    plugin.endingRuntimeService().startFinalHold();
                    sender.sendMessage("§6최종 버티기 타이머를 시작했습니다.");
                } catch (IllegalStateException exception) {
                    sender.sendMessage("§c최종 버티기를 시작할 수 없습니다: " + exception.getMessage());
                }
                sendReturnStatus(sender);
                yield true;
            }
            case "fail" -> {
                try {
                    plugin.endingRuntimeService().failFinalHold();
                    sender.sendMessage("§c최종 귀환이 실패 처리되었습니다.");
                } catch (IllegalStateException exception) {
                    sender.sendMessage("§c실패 처리할 수 없습니다: " + exception.getMessage());
                }
                sendReturnStatus(sender);
                yield true;
            }
            case "reset" -> {
                int seconds = plugin.configuration().balance().returnHoldSeconds();
                if (args.length >= 3) {
                    try {
                        seconds = Integer.parseInt(args[2]);
                        if (seconds < 1) throw new NumberFormatException();
                    } catch (NumberFormatException exception) {
                        sender.sendMessage("§c홀드 시간은 1 이상의 초 단위 정수여야 합니다.");
                        yield true;
                    }
                }
                plugin.resetEndingRuntime(Duration.ofSeconds(seconds));
                sender.sendMessage("§a귀환/결과 런타임을 초기화했습니다. 홀드=" + seconds + "초");
                sendReturnStatus(sender);
                yield true;
            }
            default -> {
                sender.sendMessage("§c사용법: /space return status|check|navigation|prepare|hold|fail|reset [holdSeconds]");
                yield true;
            }
        };
    }

    private void sendReturnStatus(CommandSender sender) {
        ReturnStage stage = plugin.returnObjectiveService().stage();
        FinalHoldSnapshot hold = plugin.finalHoldService().snapshot();

        sender.sendMessage("§6[우주 생존] §f귀환 상태");
        sender.sendMessage("§7단계: §f" + stage);
        sender.sendMessage("§7생존 계통 충족: §f" + plugin.returnObjectiveService().survivalSystemsReady());
        sender.sendMessage("§7최종 홀드: §f" + hold.status());
        sender.sendMessage("§7홀드 남은 시간: §f" + hold.remaining().toSeconds() + "초");
    }

    private boolean result(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendResultUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "contribution" -> setContribution(sender, args);
            case "scenario" -> setScenarioBonus(sender, args);
            case "evaluate" -> evaluateResult(sender);
            case "status" -> showLastResult(sender);
            default -> {
                sendResultUsage(sender);
                yield true;
            }
        };
    }

    private boolean setContribution(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c결과 값 변경 권한이 없습니다.");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /space result contribution <player> <0-3>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage("§c접속 중인 대상 플레이어를 찾을 수 없습니다.");
            return true;
        }

        int value;
        try {
            value = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c기여도는 0~3 정수여야 합니다.");
            return true;
        }

        try {
            plugin.commonContributionLedger().set(
                    PlayerId.of(target.getUniqueId()),
                    value
            );
            sender.sendMessage("§a공통 목표 기여도를 설정했습니다: " + value);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("§c기여도는 0~3 범위여야 합니다.");
        }
        return true;
    }

    private boolean setScenarioBonus(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c결과 값 변경 권한이 없습니다.");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /space result scenario <player> <bonus>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage("§c접속 중인 대상 플레이어를 찾을 수 없습니다.");
            return true;
        }

        int bonus;
        try {
            bonus = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c보너스는 정수여야 합니다.");
            return true;
        }

        plugin.matchResultRuntimeService().setScenarioBonus(
                PlayerId.of(target.getUniqueId()),
                bonus
        );
        sender.sendMessage("§a시나리오 보너스를 설정했습니다: " + bonus);
        return true;
    }

    private boolean evaluateResult(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c결과 판정 권한이 없습니다.");
            return true;
        }

        ReturnStage stage = plugin.returnObjectiveService().stage();
        if (stage != ReturnStage.COMPLETED && stage != ReturnStage.FAILED) {
            sender.sendMessage("§c귀환 결과가 확정된 뒤에만 최종 결과를 판정할 수 있습니다.");
            return true;
        }

        MatchResult result = plugin.matchResultRuntimeService()
                .evaluate(stage == ReturnStage.COMPLETED);
        sendMatchResult(sender, result, true);
        return true;
    }

    private boolean showLastResult(CommandSender sender) {
        MatchResult result = plugin.matchResultRuntimeService().lastResult().orElse(null);
        if (result == null) {
            sender.sendMessage("§e아직 판정된 결과가 없습니다.");
            return true;
        }
        sendMatchResult(sender, result, false);
        return true;
    }

    private void sendMatchResult(CommandSender sender, MatchResult result, boolean revealObjectives) {
        sender.sendMessage("§6========== 최종 결과 ==========");
        sender.sendMessage("§7공통 귀환: " + (result.commonMissionCompleted() ? "§a성공" : "§c실패"));
        sender.sendMessage("§e승리자: §f" + names(result.winners()));
        sender.sendMessage("§6MVP: §f" + names(result.mvps()));

        for (PlayerMatchResult playerResult : result.players()) {
            String name = playerName(playerResult.playerId());
            sender.sendMessage(
                    "§7- §f" + name
                            + " §8점수=" + playerResult.score().total()
                            + " 승리=" + playerResult.winner()
            );

            if (revealObjectives) {
                plugin.objectiveEngine().objective(playerResult.playerId(), ObjectiveSlot.BASE)
                        .ifPresent(objective -> sender.sendMessage(
                                "   §e기본 목표: §f"
                                        + objective.definition().title()
                                        + " §7[" + objective.status() + "]"
                        ));
                plugin.objectiveEngine().objective(playerResult.playerId(), ObjectiveSlot.SECRET)
                        .ifPresent(objective -> sender.sendMessage(
                                "   §d비밀 임무: §f"
                                        + objective.definition().title()
                                        + " §7[" + objective.status() + "]"
                        ));
            }
        }
    }

    private String names(java.util.Set<PlayerId> ids) {
        if (ids.isEmpty()) return "없음";
        return ids.stream().map(this::playerName).toList().toString();
    }

    private String playerName(PlayerId id) {
        String name = plugin.getServer().getOfflinePlayer(id.value()).getName();
        return name == null ? id.value().toString() : name;
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

    private static void sendResultUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space result contribution <player> <0-3>");
        sender.sendMessage("§c사용법: /space result scenario <player> <bonus>");
        sender.sendMessage("§c사용법: /space result evaluate");
        sender.sendMessage("§c사용법: /space result status");
    }
}

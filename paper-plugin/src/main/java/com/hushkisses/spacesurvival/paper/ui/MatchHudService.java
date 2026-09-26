package com.hushkisses.spacesurvival.paper.ui;

import com.hushkisses.spacesurvival.ending.ReturnStage;
import com.hushkisses.spacesurvival.guidance.PlayerGuidanceResolver;
import com.hushkisses.spacesurvival.guidance.PublicProblem;
import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.objective.ObjectiveSlot;
import com.hushkisses.spacesurvival.objective.ObjectiveStatus;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.map.physical.PaperShipWorldService;
import com.hushkisses.spacesurvival.paper.map.physical.PhysicalShipSnapshot;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.time.CrisisStage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.*;

public final class MatchHudService {

    private static final String OBJECTIVE_NAME = "space_hud";

    private final SpaceSurvivalPlugin plugin;
    private final PaperShipWorldService shipWorldService;
    private final PlayerGuidanceResolver guidanceResolver = new PlayerGuidanceResolver();
    private final Map<UUID, HudBoard> boards = new HashMap<>();

    private BukkitTask task;
    private BossBar crisisBar;

    public MatchHudService(
            SpaceSurvivalPlugin plugin,
            PaperShipWorldService shipWorldService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.shipWorldService = Objects.requireNonNull(shipWorldService, "shipWorldService");
    }

    public void start() {
        if (task != null) return;

        crisisBar = Bukkit.createBossBar(
                "§e함선 상태",
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        crisisBar.setVisible(false);

        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::render,
                10L,
                20L
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        shipWorldService.clearPriorityRoute();
        clearDisplays();

        if (crisisBar != null) {
            crisisBar.removeAll();
            crisisBar.setVisible(false);
            crisisBar = null;
        }
    }

    public void flashAction(Player player, boolean success, String message) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");

        player.sendActionBar(Component.text(
                (success ? "완료 · " : "실패 · ") + compact(message, 72)
        ));
    }

    private void render() {
        if (plugin.lobbyService().gameSession().isEmpty()) {
            shipWorldService.clearPriorityRoute();
            clearDisplays();
            return;
        }

        var ship = plugin.shipState().snapshot();
        CrisisStage crisis = plugin.gameRuntimeService().currentCrisisStage();
        ReturnStage returnStage = plugin.returnObjectiveService().stage();
        PublicProblem problem = guidanceResolver.resolve(
                returnStage,
                ship,
                plugin.facilityRegistry().snapshots()
        );
        String targetFacility = plugin.facilityRegistry()
                .require(problem.targetFacility())
                .definition()
                .displayName();
        shipWorldService.updatePriorityRoute(problem.targetFacility());

        Set<UUID> activePlayers = new HashSet<>();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerId playerId = PlayerId.of(player.getUniqueId());
            if (!plugin.lobbyService().contains(playerId)) {
                continue;
            }

            activePlayers.add(player.getUniqueId());
            renderScoreboard(
                    player,
                    playerId,
                    crisis,
                    problem,
                    targetFacility
            );
        }

        cleanupInactiveBoards(activePlayers);
        renderCrisisBar(crisis, problem, targetFacility, activePlayers);
    }

    private void renderScoreboard(
            Player player,
            PlayerId playerId,
            CrisisStage crisis,
            PublicProblem problem,
            String targetFacility
    ) {
        HudBoard hud = boards.computeIfAbsent(
                player.getUniqueId(),
                ignored -> createHudBoard()
        );

        ArrayList<String> lines = new ArrayList<>();
        lines.add("§7위치 §f" + compact(currentArea(player), 20));
        lines.add("§8────────────");
        lines.add("§b§l개인 목표");
        lines.addAll(personalObjectiveLines(playerId));

        if (plugin.objectiveEngine().objective(playerId, ObjectiveSlot.SECRET).isPresent()) {
            lines.add("§d비밀 임무 있음 §8· PDA 확인");
        }

        lines.add("§8────────────");
        lines.add("§e§l긴급 목표");
        lines.add(priorityColor(crisis) + compact(problem.title(), 26));
        lines.add("§7목표 §f" + compact(targetFacility, 20));
        lines.add("§7필요 §f" + compact(problem.need(), 24));
        lines.add("§8PDA 우클릭 · 상세");

        if (!lines.equals(hud.lines)) {
            for (String oldLine : hud.lines) {
                hud.scoreboard.resetScores(oldLine);
            }

            int score = lines.size();
            for (String line : lines) {
                hud.objective.getScore(line).setScore(score--);
            }

            hud.lines = List.copyOf(lines);
        }

        if (player.getScoreboard() != hud.scoreboard) {
            player.setScoreboard(hud.scoreboard);
        }
    }

    private HudBoard createHudBoard() {
        ScoreboardManager manager = Objects.requireNonNull(
                Bukkit.getScoreboardManager(),
                "scoreboardManager"
        );
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                Component.text("우주 생존")
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        return new HudBoard(scoreboard, objective);
    }

    private void renderCrisisBar(
            CrisisStage crisis,
            PublicProblem problem,
            String targetFacility,
            Set<UUID> activePlayers
    ) {
        if (crisisBar == null) {
            return;
        }

        boolean urgent = crisis == CrisisStage.CRISIS
                || crisis == CrisisStage.COLLAPSE;

        if (!urgent) {
            crisisBar.removeAll();
            crisisBar.setVisible(false);
            return;
        }

        crisisBar.setColor(
                crisis == CrisisStage.COLLAPSE
                        ? BarColor.RED
                        : BarColor.YELLOW
        );
        crisisBar.setTitle(
                (crisis == CrisisStage.COLLAPSE ? "§c§l함선 붕괴 위험" : "§6§l함선 위기")
                        + " §7— §f"
                        + compact(problem.title(), 30)
                        + " §7→ §e"
                        + compact(targetFacility, 18)
        );
        crisisBar.setProgress(crisisProgress(crisis));
        crisisBar.setVisible(true);

        List<Player> shown = List.copyOf(crisisBar.getPlayers());
        for (Player player : shown) {
            if (!activePlayers.contains(player.getUniqueId())) {
                crisisBar.removePlayer(player);
            }
        }

        for (UUID playerId : activePlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && !crisisBar.getPlayers().contains(player)) {
                crisisBar.addPlayer(player);
            }
        }
    }

    private void cleanupInactiveBoards(Set<UUID> activePlayers) {
        Iterator<Map.Entry<UUID, HudBoard>> iterator = boards.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, HudBoard> entry = iterator.next();
            if (activePlayers.contains(entry.getKey())) {
                continue;
            }

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.getScoreboard() == entry.getValue().scoreboard) {
                player.setScoreboard(mainScoreboard());
            }
            iterator.remove();
        }
    }

    private void clearDisplays() {
        for (Map.Entry<UUID, HudBoard> entry : boards.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.getScoreboard() == entry.getValue().scoreboard) {
                player.setScoreboard(mainScoreboard());
            }
        }
        boards.clear();

        if (crisisBar != null) {
            crisisBar.removeAll();
            crisisBar.setVisible(false);
        }
    }

    private Scoreboard mainScoreboard() {
        return Objects.requireNonNull(
                Bukkit.getScoreboardManager(),
                "scoreboardManager"
        ).getMainScoreboard();
    }



    private List<String> personalObjectiveLines(PlayerId playerId) {
        var objective = plugin.objectiveEngine()
                .objective(playerId, ObjectiveSlot.BASE)
                .orElse(null);

        if (objective == null) {
            return List.of("§7아직 배정되지 않았습니다.");
        }

        String title = "§f" + compact(objective.definition().title(), 24);
        String progress = switch (objective.status()) {
            case ACTIVE -> "§7진행 §f"
                    + objective.progress()
                    + "/"
                    + objective.definition().targetProgress();
            case COMPLETED -> "§a완료";
            case FAILED -> "§c실패";
        };

        return List.of(title, progress);
    }

    private String currentArea(Player player) {
        PhysicalShipSnapshot snapshot = shipWorldService.activeSnapshot().orElse(null);
        if (snapshot == null) return "미배치";

        TileId tileId = snapshot.tileAt(player.getLocation()).orElse(null);
        return tileId == null ? "함선 외부" : snapshot.tileDisplayName(tileId);
    }

    private static double crisisProgress(CrisisStage stage) {
        return switch (stage) {
            case STABLE -> 1.0;
            case ALERT -> 0.7;
            case CRISIS -> 0.4;
            case COLLAPSE -> 0.15;
        };
    }

    private static String priorityColor(CrisisStage stage) {
        return switch (stage) {
            case STABLE -> "§f";
            case ALERT -> "§e";
            case CRISIS -> "§6";
            case COLLAPSE -> "§c";
        };
    }

    private static String crisisColor(CrisisStage stage) {
        return switch (stage) {
            case STABLE -> "§a";
            case ALERT -> "§e";
            case CRISIS -> "§6";
            case COLLAPSE -> "§c";
        };
    }

    private static String compact(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(1, maxLength - 1)) + "…";
    }

    public static String crisisName(CrisisStage stage) {
        return switch (stage) {
            case STABLE -> "안정";
            case ALERT -> "경계";
            case CRISIS -> "위기";
            case COLLAPSE -> "붕괴";
        };
    }

    public static String stageName(ReturnStage stage) {
        return switch (stage) {
            case SURVIVAL_SYSTEMS -> "생존 기반 복구";
            case NAVIGATION -> "항법 복구";
            case RETURN_PREPARATION -> "귀환 준비";
            case FINAL_HOLD -> "최종 유지";
            case COMPLETED -> "귀환 완료";
            case FAILED -> "귀환 실패";
        };
    }

    private static final class HudBoard {
        private final Scoreboard scoreboard;
        private final Objective objective;
        private List<String> lines = List.of();

        private HudBoard(Scoreboard scoreboard, Objective objective) {
            this.scoreboard = scoreboard;
            this.objective = objective;
        }
    }
}

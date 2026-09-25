package com.hushkisses.spacesurvival.paper.match;

import com.hushkisses.spacesurvival.event.GameEventScale;
import com.hushkisses.spacesurvival.event.GameEventSelector;
import com.hushkisses.spacesurvival.game.GamePhase;
import com.hushkisses.spacesurvival.game.GameSession;
import com.hushkisses.spacesurvival.lobby.LobbyStartException;
import com.hushkisses.spacesurvival.objective.assignment.ObjectiveAssignmentService;
import com.hushkisses.spacesurvival.objective.conflict.ConflictSetDefinition;
import com.hushkisses.spacesurvival.objective.conflict.ConflictSetSelector;
import com.hushkisses.spacesurvival.objective.conflict.DefaultConflictSetCatalog;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.config.MatchSetupConfig;
import com.hushkisses.spacesurvival.paper.map.physical.PaperShipWorldService;
import com.hushkisses.spacesurvival.paper.map.physical.PhysicalShipSnapshot;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.scenario.DefaultScenarioCatalog;
import com.hushkisses.spacesurvival.scenario.ScenarioDefinition;
import com.hushkisses.spacesurvival.scenario.ScenarioRuntime;
import com.hushkisses.spacesurvival.scenario.ScenarioType;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.random.RandomGenerator;

public final class MatchOrchestrator {

    private final SpaceSurvivalPlugin plugin;
    private final PaperShipWorldService shipWorldService;

    private MatchLifecycleStatus status = MatchLifecycleStatus.IDLE;
    private MatchSetupSnapshot setupSnapshot;

    public MatchOrchestrator(
            SpaceSurvivalPlugin plugin,
            PaperShipWorldService shipWorldService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.shipWorldService = Objects.requireNonNull(shipWorldService, "shipWorldService");
    }

    public MatchSetupSnapshot prepare(long seed, boolean developmentBypassMinimum) {
        if (status != MatchLifecycleStatus.IDLE) {
            throw new IllegalStateException("Match orchestrator is not idle");
        }

        List<PlayerId> players = plugin.lobbyService().snapshot().players();
        if (players.isEmpty()) {
            throw new LobbyStartException("No players in lobby");
        }

        plugin.resetForNewMatch();

        GameSession session = developmentBypassMinimum
                ? plugin.lobbyService().startForDevelopment()
                : plugin.lobbyService().start();

        MatchSetupConfig config = plugin.configuration().matchSetup();
        Random random = new Random(seed);

        plugin.roleSelectionService().prepareCandidates(
                players,
                new Random(random.nextLong())
        );

        List<ConflictSetDefinition> conflicts = new ConflictSetSelector().select(
                DefaultConflictSetCatalog.create(plugin.objectiveRegistry()),
                2,
                3,
                new Random(random.nextLong())
        );

        new ObjectiveAssignmentService(plugin.objectiveRegistry()).assignBaseObjectives(
                players,
                conflicts,
                plugin.objectiveEngine(),
                new Random(random.nextLong())
        );

        RandomGenerator secretRandom = new Random(random.nextLong());
        for (PlayerId playerId : players) {
            if (secretRandom.nextInt(100) < config.secretMissionChancePercent()) {
                plugin.secretMissionService().grantRandom(
                        playerId,
                        List.copyOf(plugin.objectiveRegistry().all()),
                        secretRandom
                );
            }
        }

        ScenarioDefinition scenarioDefinition = selectScenario(
                config,
                new Random(random.nextLong())
        );
        ScenarioRuntime scenario = plugin.scenarioEngine().activate(
                scenarioDefinition,
                players,
                new Random(random.nextLong())
        );

        PhysicalShipSnapshot ship = shipWorldService.generateAndRender(
                seed,
                config
        );
        int resourceCaches = plugin.resourceWorldService().populate(ship, seed);
        plugin.getLogger().info("Physical resource caches placed: " + resourceCaches);

        int initialEventCount = config.initialSmallEventsMin()
                + random.nextInt(
                        config.initialSmallEventsMax()
                                - config.initialSmallEventsMin()
                                + 1
                );

        GameEventSelector selector = new GameEventSelector();
        for (int i = 0; i < initialEventCount; i++) {
            plugin.gameEventEngine().trigger(
                    selector.select(
                            plugin.gameEventRegistry(),
                            GameEventScale.SMALL,
                            random
                    ),
                    plugin.gameEventContext()
            );
        }

        session.transitionTo(GamePhase.BRIEFING);
        status = MatchLifecycleStatus.BRIEFING;

        setupSnapshot = new MatchSetupSnapshot(
                seed,
                ship.generatedMap(),
                scenario,
                conflicts.stream().map(set -> set.axis().name()).toList(),
                initialEventCount
        );

        plugin.telemetryService().start(
                seed,
                players.size(),
                scenarioDefinition.type().name()
        );
        plugin.telemetryService().add("resource.cache.count", resourceCaches);
        plugin.telemetryService().add("initial.incident.count", initialEventCount);
        plugin.telemetryService().event("match", "briefing");

        for (PlayerId playerId : players) {
            Player online = plugin.getServer().getPlayer(playerId.value());
            if (online == null) continue;

            online.getInventory().clear();
            online.setGameMode(GameMode.SURVIVAL);
            online.teleportAsync(ship.bridgeSpawn());
            plugin.openingBriefingUi().open(
                    online,
                    scenarioDefinition.publicBriefing(),
                    initialEventCount,
                    ship.generatedMap().tileIds().size()
            );
        }

        return setupSnapshot;
    }

    public boolean tryActivateIfReady() {
        if (status != MatchLifecycleStatus.BRIEFING) {
            return false;
        }

        int players = plugin.lobbyService().snapshot().playerCount();
        if (plugin.roleSelectionService().selectedPlayerCount() < players) {
            return false;
        }

        GameSession session = plugin.lobbyService().gameSession().orElseThrow();
        if (session.phase() != GamePhase.BRIEFING) {
            return false;
        }

        session.transitionTo(GamePhase.ACTIVE);
        plugin.starterKitService().giveRoleKits();
        plugin.gameRuntimeService().start();
        plugin.incidentDirector().start(setupSnapshot.seed());
        plugin.telemetryService().event("match", "active");
        status = MatchLifecycleStatus.ACTIVE;

        plugin.getServer().broadcastMessage(
                "§a[우주 생존] §f모든 승무원의 직업 선택이 완료되어 임무가 시작되었습니다."
        );
        return true;
    }

    public void refreshStatusFromSession() {
        plugin.lobbyService().gameSession().ifPresent(session -> {
            status = switch (session.phase()) {
                case WAITING, PREPARING -> MatchLifecycleStatus.IDLE;
                case BRIEFING -> MatchLifecycleStatus.BRIEFING;
                case ACTIVE -> MatchLifecycleStatus.ACTIVE;
                case RETURN_PHASE -> MatchLifecycleStatus.RETURN_PHASE;
                case FINISHED -> MatchLifecycleStatus.FINISHED;
            };
        });
    }

    public void reset() {
        plugin.incidentDirector().stop();
        plugin.telemetryService().finish("reset");
        if (plugin.gameRuntimeService().isRunning()) {
            plugin.gameRuntimeService().stop();
        }
        plugin.resetForNewMatch();
        plugin.lobbyService().resetForNextMatch();
        setupSnapshot = null;
        status = MatchLifecycleStatus.IDLE;
    }

    public MatchLifecycleStatus status() {
        refreshStatusFromSession();
        return status;
    }

    public Optional<MatchSetupSnapshot> setupSnapshot() {
        return Optional.ofNullable(setupSnapshot);
    }

    private static ScenarioDefinition selectScenario(
            MatchSetupConfig config,
            RandomGenerator random
    ) {
        int total = config.accidentWeight()
                + config.sabotageWeight()
                + config.infectionWeight();

        int roll = random.nextInt(total);
        ScenarioType selected;

        if (roll < config.accidentWeight()) {
            selected = ScenarioType.ACCIDENT;
        } else if (roll < config.accidentWeight() + config.sabotageWeight()) {
            selected = ScenarioType.SABOTAGE;
        } else {
            selected = ScenarioType.INFECTION;
        }

        return DefaultScenarioCatalog.create().stream()
                .filter(definition -> definition.type() == selected)
                .findFirst()
                .orElseThrow();
    }
}

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
import com.hushkisses.spacesurvival.resource.ResourceType;
import com.hushkisses.spacesurvival.ship.ShipMetric;
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
        applyStartingRecoveryState(config);
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
        var resourcePopulation = plugin.resourceWorldService().populate(ship, seed);
        plugin.getLogger().info(
                "Physical resource caches placed: "
                        + resourcePopulation.caches()
                        + ", stacks="
                        + resourcePopulation.stacks()
                        + ", units="
                        + resourcePopulation.units()
        );

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
        plugin.telemetryService().add("resource.cache.count", resourcePopulation.caches());
        plugin.telemetryService().add("resource.cache.stacks", resourcePopulation.stacks());
        plugin.telemetryService().add("resource.cache.units", resourcePopulation.units());
        plugin.telemetryService().add("initial.incident.count", initialEventCount);
        var openingShip = plugin.shipState().snapshot();
        plugin.telemetryService().add("initial.ship.power", openingShip.power());
        plugin.telemetryService().add("initial.ship.oxygen", openingShip.oxygen());
        plugin.telemetryService().add("initial.ship.hull", openingShip.hull());
        plugin.telemetryService().add("initial.ship.reactor", openingShip.reactor());
        plugin.telemetryService().event("match", "briefing");

        for (PlayerId playerId : players) {
            Player online = plugin.getServer().getPlayer(playerId.value());
            if (online == null) continue;

            online.getInventory().clear();
            online.setGameMode(GameMode.ADVENTURE);
            online.teleportAsync(ship.bridgeSpawn());
            plugin.roleSelectionUi().giveMenuItem(online);

            online.sendTitle(
                    "§c긴급 귀환 임무",
                    "§f직업을 선택하십시오",
                    5,
                    50,
                    10
            );
            online.sendMessage("§6[상황] §f" + scenarioDefinition.publicBriefing());
            online.sendMessage("§6[공통 목표] §f함선을 복구하고 귀환하십시오.");
            online.sendMessage(
                    "§7공개된 초기 문제 "
                            + initialEventCount
                            + "건 · 함선 모듈 "
                            + ship.generatedMap().tileIds().size()
                            + "개"
            );
            online.sendMessage(
                    "§e직업 후보 3개 중 하나를 선택하십시오. "
                            + "창을 닫아도 핫바의 네더별을 우클릭하면 다시 열립니다."
            );

            plugin.roleSelectionUi().open(online);
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

        for (PlayerId playerId : plugin.lobbyService().snapshot().players()) {
            Player online = plugin.getServer().getPlayer(playerId.value());
            if (online == null) continue;

            plugin.roleSelectionUi().removeMenuItem(online);
            online.setGameMode(GameMode.SURVIVAL);
        }

        plugin.starterKitService().giveRoleKits();
        plugin.crewPdaService().giveToParticipants();
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
        plugin.lobbyReadyService().resetOnlinePlayersToLobby();
    }

    public MatchLifecycleStatus status() {
        refreshStatusFromSession();
        return status;
    }

    public Optional<MatchSetupSnapshot> setupSnapshot() {
        return Optional.ofNullable(setupSnapshot);
    }


    private void applyStartingRecoveryState(MatchSetupConfig config) {
        plugin.shipState().set(ShipMetric.POWER, config.startingPower());
        plugin.shipState().set(ShipMetric.OXYGEN, config.startingOxygen());
        plugin.shipState().set(ShipMetric.HULL, config.startingHull());
        plugin.shipState().set(ShipMetric.REACTOR, config.startingReactor());

        var shared = plugin.resourceLedger().shared();
        if (config.startingRepairParts() > 0) {
            shared.add(ResourceType.REPAIR_PARTS, config.startingRepairParts());
        }
        if (config.startingPowerCells() > 0) {
            shared.add(ResourceType.POWER_CELLS, config.startingPowerCells());
        }
        if (config.startingFuel() > 0) {
            shared.add(ResourceType.FUEL, config.startingFuel());
        }
        if (config.startingMedicalSupplies() > 0) {
            shared.add(ResourceType.MEDICAL_SUPPLIES, config.startingMedicalSupplies());
        }

        plugin.getLogger().info(
                "Opening recovery state: power="
                        + config.startingPower()
                        + ", oxygen="
                        + config.startingOxygen()
                        + ", hull="
                        + config.startingHull()
                        + ", reactor="
                        + config.startingReactor()
        );
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

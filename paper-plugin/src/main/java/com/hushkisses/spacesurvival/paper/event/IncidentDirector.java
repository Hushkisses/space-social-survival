package com.hushkisses.spacesurvival.paper.event;

import com.hushkisses.spacesurvival.event.*;
import com.hushkisses.spacesurvival.map.connection.ConnectionState;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.config.MatchSetupConfig;
import com.hushkisses.spacesurvival.paper.map.physical.PhysicalConnectionController;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.scenario.ScenarioRuntime;
import com.hushkisses.spacesurvival.scenario.ScenarioType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.random.RandomGenerator;

public final class IncidentDirector {

    private final SpaceSurvivalPlugin plugin;
    private final PhysicalConnectionController connections;
    private final GameEventSelector selector = new GameEventSelector();

    private BukkitTask task;
    private Random random;
    private long nextSmallAtSeconds;
    private long nextMajorAtSeconds;
    private int majorEventsTriggered;
    private String lastEventId;

    public IncidentDirector(
            SpaceSurvivalPlugin plugin,
            PhysicalConnectionController connections
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public void start(long seed) {
        stop();

        MatchSetupConfig config = plugin.configuration().matchSetup();
        random = new Random(seed ^ 0x49A3B72DL);
        nextSmallAtSeconds = randomBetween(
                config.incidentSmallMinSeconds(),
                config.incidentSmallMaxSeconds()
        );
        nextMajorAtSeconds = randomBetween(
                config.incidentMajorFirstMinSeconds(),
                config.incidentMajorFirstMaxSeconds()
        );
        majorEventsTriggered = 0;
        lastEventId = null;

        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::tick,
                20L,
                20L
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isRunning() {
        return task != null;
    }

    public IncidentDirectorSnapshot snapshot() {
        return new IncidentDirectorSnapshot(
                isRunning(),
                nextSmallAtSeconds,
                nextMajorAtSeconds,
                majorEventsTriggered,
                lastEventId
        );
    }

    public GameEventDefinition force(GameEventScale scale) {
        if (random == null) {
            random = new Random();
        }
        GameEventDefinition event = selectEvent(scale);
        trigger(event);
        return event;
    }

    private void tick() {
        var runtime = plugin.gameRuntimeService().snapshot().orElse(null);
        if (runtime == null || !runtime.time().running()) {
            return;
        }

        long elapsed = runtime.time().elapsed().toSeconds();
        MatchSetupConfig config = plugin.configuration().matchSetup();

        if (elapsed >= nextSmallAtSeconds) {
            trigger(selectEvent(GameEventScale.SMALL));
            nextSmallAtSeconds = elapsed + randomBetween(
                    config.incidentSmallMinSeconds(),
                    config.incidentSmallMaxSeconds()
            );
        }

        if (majorEventsTriggered < 2 && elapsed >= nextMajorAtSeconds) {
            trigger(selectEvent(GameEventScale.LARGE));
            majorEventsTriggered++;

            if (majorEventsTriggered == 1) {
                nextMajorAtSeconds = randomBetween(
                        config.incidentMajorSecondMinSeconds(),
                        config.incidentMajorSecondMaxSeconds()
                );
            } else {
                nextMajorAtSeconds = Long.MAX_VALUE;
            }
        }
    }

    private GameEventDefinition selectEvent(GameEventScale scale) {
        if (scale == GameEventScale.LARGE) {
            ScenarioRuntime scenario = plugin.scenarioEngine().active().orElse(null);
            if (scenario != null) {
                if (scenario.definition().type() == ScenarioType.INFECTION
                        && majorEventsTriggered == 0) {
                    return requireEvent("mass_infection");
                }
                if (scenario.definition().type() == ScenarioType.SABOTAGE) {
                    List<String> sabotageEvents = List.of(
                            "reactor_runaway",
                            "total_power_failure",
                            "hull_breach"
                    );
                    return requireEvent(
                            sabotageEvents.get(random.nextInt(sabotageEvents.size()))
                    );
                }
            }
        }

        return selector.select(plugin.gameEventRegistry(), scale, random);
    }

    private GameEventDefinition requireEvent(String id) {
        return plugin.gameEventRegistry()
                .find(new GameEventId(id))
                .orElseThrow(() -> new IllegalStateException("Missing event: " + id));
    }

    public void trigger(GameEventDefinition event) {
        Objects.requireNonNull(event, "event");
        if (random == null) {
            random = new Random();
        }

        plugin.gameEventEngine().trigger(event, plugin.gameEventContext());
        lastEventId = event.id().value();
        plugin.telemetryService().recordIncident(lastEventId);

        applyWorldConsequences(event);
        plugin.incidentPresentationService().present(event);
    }

    private void applyWorldConsequences(GameEventDefinition event) {
        String id = event.id().value();

        switch (id) {
            case "door_fault" -> connections
                    .setRandomOpenConnection(ConnectionState.LOCKED, random)
                    .ifPresent(connectionId -> plugin.getServer().broadcastMessage(
                            "§e[통로 고장] §f통로 #" + connectionId + "이 잠겼습니다."
                    ));
            case "comms_noise" -> plugin.radioRuntimeState().setCommunicationsOutage(true);
            case "alien_intrusion" -> randomOnlineParticipant().ifPresent(
                    player -> plugin.pveMobSpawner().spawnAlien(player.getLocation())
            );
            case "mass_infection" -> beginInfectionOutbreakIfApplicable();
            default -> {
            }
        }
    }

    private void beginInfectionOutbreakIfApplicable() {
        ScenarioRuntime scenario = plugin.scenarioEngine().active().orElse(null);
        if (scenario == null || scenario.definition().type() != ScenarioType.INFECTION) {
            return;
        }
        if (!plugin.infectionService().infectedPlayers().isEmpty()) {
            return;
        }

        try {
            plugin.infectionScenarioService().beginOutbreak(
                    scenario,
                    plugin.lobbyService().snapshot().players(),
                    plugin.infectionService(),
                    random
            );
        } catch (IllegalStateException ignored) {
        }
    }

    private Optional<Player> randomOnlineParticipant() {
        List<Player> online = plugin.lobbyService().snapshot().players().stream()
                .map(PlayerId::value)
                .map(plugin.getServer()::getPlayer)
                .filter(Objects::nonNull)
                .toList();

        if (online.isEmpty()) return Optional.empty();
        return Optional.of(online.get(random.nextInt(online.size())));
    }

    private long randomBetween(int min, int max) {
        if (min == max) return min;
        return min + random.nextInt(max - min + 1);
    }
}

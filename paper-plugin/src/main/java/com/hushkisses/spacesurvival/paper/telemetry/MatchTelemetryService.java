package com.hushkisses.spacesurvival.paper.telemetry;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

public final class MatchTelemetryService {

    private final JavaPlugin plugin;
    private final File directory;

    private boolean active;
    private long seed;
    private int playerCount;
    private String scenario = "NONE";
    private Instant startedAt;
    private Instant endedAt;
    private String outcome = "RUNNING";
    private final Map<String, Long> counters = new LinkedHashMap<>();
    private final List<TelemetryEvent> events = new ArrayList<>();
    private File lastSavedFile;

    public MatchTelemetryService(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.directory = new File(plugin.getDataFolder(), "telemetry");
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning(
                    "Could not create telemetry directory: " + directory
            );
        }
    }

    public void start(long seed, int playerCount, String scenario) {
        if (active) {
            finish("restarted");
        }

        this.active = true;
        this.seed = seed;
        this.playerCount = playerCount;
        this.scenario = Objects.requireNonNull(scenario, "scenario");
        this.startedAt = Instant.now();
        this.endedAt = null;
        this.outcome = "RUNNING";
        this.counters.clear();
        this.events.clear();

        event("match", "active");
        increment("match.started");
    }

    public void increment(String key) {
        add(key, 1L);
    }

    public void add(String key, long amount) {
        if (!active) return;
        counters.merge(requireKey(key), amount, Math::addExact);
    }

    public void event(String category, String detail) {
        if (!active) return;
        events.add(new TelemetryEvent(
                Instant.now(),
                requireKey(category),
                Objects.requireNonNull(detail, "detail")
        ));
    }

    public void recordFacilityAction(String actionId, boolean success) {
        if (!active) return;
        increment("facility.action.total");
        increment("facility.action." + (success ? "success" : "failure"));
        increment("facility." + actionId + "." + (success ? "success" : "failure"));
        event("facility", actionId + ":" + (success ? "success" : "failure"));
    }

    public void recordIncident(String eventId) {
        if (!active) return;
        increment("incident.total");
        increment("incident." + eventId);
        event("incident", eventId);
    }

    public void recordDeath(String playerName, String cause) {
        if (!active) return;
        increment("death.total");
        increment("death." + cause.toLowerCase(Locale.ROOT));
        event("death", playerName + ":" + cause);
    }

    public void recordResourceDeposit(long amount) {
        if (!active) return;
        add("resource.deposited", amount);
    }

    public Optional<File> finish(String outcome) {
        if (!active) {
            return Optional.ofNullable(lastSavedFile);
        }

        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.endedAt = Instant.now();
        event("match", "finish:" + outcome);
        active = false;
        return Optional.of(save());
    }

    public File saveCurrent(String reason) {
        if (active) {
            event("telemetry", "manual-save:" + reason);
        }
        return save();
    }

    public TelemetrySnapshot snapshot() {
        return new TelemetrySnapshot(
                active,
                seed,
                playerCount,
                scenario,
                startedAt,
                counters,
                events.size(),
                lastSavedFile == null ? null : lastSavedFile.getAbsolutePath()
        );
    }

    public File directory() {
        return directory;
    }

    private File save() {
        String started = startedAt == null
                ? "not-started"
                : Long.toString(startedAt.toEpochMilli());
        File target = new File(
                directory,
                "match-" + started + "-" + seed + ".json"
        );

        try {
            Files.writeString(
                    target.toPath(),
                    toJson(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not save telemetry: " + target,
                    exception
            );
        }

        lastSavedFile = target;
        return target;
    }

    private String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        field(json, "seed", Long.toString(seed), false);
        field(json, "playerCount", Integer.toString(playerCount), false);
        field(json, "scenario", quote(scenario), false);
        field(json, "startedAt", quote(string(startedAt)), false);
        field(json, "endedAt", quote(string(endedAt)), false);
        field(json, "outcome", quote(outcome), false);

        json.append("  \"counters\": {");
        if (!counters.isEmpty()) json.append("\n");
        int counterIndex = 0;
        for (var entry : counters.entrySet()) {
            json.append("    ")
                    .append(quote(entry.getKey()))
                    .append(": ")
                    .append(entry.getValue());
            counterIndex++;
            json.append(counterIndex < counters.size() ? ",\n" : "\n");
        }
        json.append("  },\n");

        json.append("  \"events\": [");
        if (!events.isEmpty()) json.append("\n");
        for (int i = 0; i < events.size(); i++) {
            TelemetryEvent event = events.get(i);
            json.append("    {")
                    .append("\"at\": ").append(quote(event.at().toString()))
                    .append(", \"category\": ").append(quote(event.category()))
                    .append(", \"detail\": ").append(quote(event.detail()))
                    .append("}");
            json.append(i + 1 < events.size() ? ",\n" : "\n");
        }
        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    private static void field(
            StringBuilder json,
            String name,
            String value,
            boolean last
    ) {
        json.append("  ")
                .append(quote(name))
                .append(": ")
                .append(value)
                .append(last ? "\n" : ",\n");
    }

    private static String quote(String value) {
        if (value == null) return "null";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                + "\"";
    }

    private static String string(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static String requireKey(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("blank telemetry key");
        return value;
    }

    private record TelemetryEvent(
            Instant at,
            String category,
            String detail
    ) {
    }
}

package com.hushkisses.spacesurvival.paper.runtime;

import com.hushkisses.spacesurvival.time.CrisisEvaluator;
import com.hushkisses.spacesurvival.time.CrisisFactors;
import com.hushkisses.spacesurvival.time.CrisisStage;
import com.hushkisses.spacesurvival.time.CrisisThresholds;
import com.hushkisses.spacesurvival.time.GameTimer;
import com.hushkisses.spacesurvival.time.MatchTimeSnapshot;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class GameRuntimeService {

    private final JavaPlugin plugin;
    private final Duration targetDuration;
    private final CrisisEvaluator crisisEvaluator;

    private GameTimer timer;
    private CrisisStage crisisStage = CrisisStage.STABLE;
    private BukkitTask task;

    public GameRuntimeService(JavaPlugin plugin, int targetMatchMinutes) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");

        if (targetMatchMinutes < 1) {
            throw new IllegalArgumentException("targetMatchMinutes must be positive");
        }

        this.targetDuration = Duration.ofMinutes(targetMatchMinutes);
        this.crisisEvaluator = new CrisisEvaluator(
                CrisisThresholds.defaultForTargetMinutes(targetMatchMinutes)
        );
    }

    public void start() {
        if (timer != null && timer.isRunning()) {
            throw new IllegalStateException("Runtime is already running");
        }

        timer = new GameTimer(targetDuration);
        timer.start();
        crisisStage = CrisisStage.STABLE;

        if (task != null) {
            task.cancel();
        }

        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::refresh,
                20L,
                20L
        );
    }

    public void stop() {
        if (timer == null || !timer.isStarted()) {
            throw new IllegalStateException("Runtime has not started");
        }

        if (timer.isRunning()) {
            timer.stop();
        }

        if (task != null) {
            task.cancel();
            task = null;
        }

        refresh();
    }

    public boolean isRunning() {
        return timer != null && timer.isRunning();
    }

    public Optional<RuntimeSnapshot> snapshot() {
        if (timer == null || !timer.isStarted()) {
            return Optional.empty();
        }

        MatchTimeSnapshot time = timer.snapshot();
        CrisisStage stage = crisisEvaluator.evaluate(
                time.elapsed(),
                CrisisFactors.neutral()
        );

        return Optional.of(new RuntimeSnapshot(time, stage));
    }

    public CrisisStage currentCrisisStage() {
        return crisisStage;
    }

    private void refresh() {
        if (timer == null || !timer.isStarted()) {
            return;
        }

        MatchTimeSnapshot time = timer.snapshot();
        CrisisStage evaluated = crisisEvaluator.evaluate(
                time.elapsed(),
                CrisisFactors.neutral()
        );

        if (evaluated != crisisStage) {
            plugin.getLogger().info(
                    "Crisis stage changed: " + crisisStage + " -> " + evaluated
            );
            crisisStage = evaluated;
        }
    }

    public record RuntimeSnapshot(
            MatchTimeSnapshot time,
            CrisisStage crisisStage
    ) {
    }
}

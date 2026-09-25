package com.hushkisses.spacesurvival.paper.ending;

import com.hushkisses.spacesurvival.ending.FinalHoldService;
import com.hushkisses.spacesurvival.ending.FinalHoldStatus;
import com.hushkisses.spacesurvival.ending.ReturnObjectiveService;
import com.hushkisses.spacesurvival.game.GamePhase;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class EndingRuntimeService {

    private final SpaceSurvivalPlugin plugin;
    private final ReturnObjectiveService returnObjective;
    private final FinalHoldService finalHold;

    private BukkitTask task;

    public EndingRuntimeService(
            SpaceSurvivalPlugin plugin,
            ReturnObjectiveService returnObjective,
            FinalHoldService finalHold
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.returnObjective = Objects.requireNonNull(returnObjective, "returnObjective");
        this.finalHold = Objects.requireNonNull(finalHold, "finalHold");
    }

    public ReturnObjectiveService returnObjective() {
        return returnObjective;
    }

    public FinalHoldService finalHold() {
        return finalHold;
    }

    public void startFinalHold() {
        if (returnObjective.stage() != com.hushkisses.spacesurvival.ending.ReturnStage.FINAL_HOLD) {
            throw new IllegalStateException("Return objective is not ready for final hold");
        }

        finalHold.start();

        plugin.lobbyService().gameSession().ifPresent(session -> {
            if (session.phase() == GamePhase.ACTIVE) {
                session.transitionTo(GamePhase.RETURN_PHASE);
            }
        });

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

    public void failFinalHold() {
        finalHold.fail();
        returnObjective.fail();
        cancelTask();

        plugin.lobbyService().gameSession().ifPresent(session -> {
            if (!session.isFinished() && session.canTransitionTo(GamePhase.FINISHED)) {
                session.transitionTo(GamePhase.FINISHED);
            }
        });
    }

    public void refresh() {
        if (finalHold.status() != FinalHoldStatus.RUNNING) {
            return;
        }

        if (finalHold.refresh()) {
            returnObjective.complete();
            cancelTask();

            plugin.lobbyService().gameSession().ifPresent(session -> {
                if (!session.isFinished() && session.canTransitionTo(GamePhase.FINISHED)) {
                    session.transitionTo(GamePhase.FINISHED);
                }
            });

            plugin.getServer().broadcastMessage("§a[귀환 성공] §f최종 버티기 단계가 완료되었습니다.");
        }
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}

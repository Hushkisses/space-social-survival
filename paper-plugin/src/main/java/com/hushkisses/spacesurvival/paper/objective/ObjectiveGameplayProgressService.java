package com.hushkisses.spacesurvival.paper.objective;

import com.hushkisses.spacesurvival.objective.ObjectiveInstance;
import com.hushkisses.spacesurvival.objective.ObjectiveStatus;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.item.FunctionalItemType;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.resource.ResourceType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ObjectiveGameplayProgressService {

    private static final Map<String, Set<String>> ACTION_OBJECTIVES = Map.ofEntries(
            Map.entry("engineering.engine", Set.of("repair_engine")),
            Map.entry("engineering.advanced_repair", Set.of("repair_engine")),
            Map.entry("engineering.power", Set.of("stabilize_oxygen")),
            Map.entry("cargo.deposit.data_cores", Set.of("collect_data_cores")),
            Map.entry("cargo.deposit.bio_samples", Set.of("collect_bio_sample")),
            Map.entry("cargo.deposit.circuits", Set.of("collect_rare_parts")),
            Map.entry("bridge.destination", Set.of("change_destination")),
            Map.entry("bridge.return", Set.of("return_from_zone"))
    );

    private final SpaceSurvivalPlugin plugin;

    public ObjectiveGameplayProgressService(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void recordAction(Player player, String actionKey) {
        recordAction(PlayerId.of(player.getUniqueId()), actionKey, 1);
    }

    public void recordAction(PlayerId playerId, String actionKey, int amount) {
        if (amount < 1) return;
        Set<String> objectiveIds = ACTION_OBJECTIVES.get(actionKey);
        if (objectiveIds == null || objectiveIds.isEmpty()) return;

        for (ObjectiveInstance objective : plugin.objectiveEngine().objectives(playerId)) {
            if (objective.status() != ObjectiveStatus.ACTIVE) continue;
            if (!objectiveIds.contains(objective.definition().id().value())) continue;

            objective.advance(amount);
            plugin.telemetryService().event(
                    "objective",
                    playerId.value() + ":" + objective.definition().id().value()
                            + ":" + objective.progress()
            );
        }
    }

    public void recordDeposit(
            Player player,
            Map<ResourceType, Integer> deposited
    ) {
        PlayerId playerId = PlayerId.of(player.getUniqueId());

        deposited.forEach((type, amount) -> {
            String key = switch (type) {
                case DATA_CORES -> "cargo.deposit.data_cores";
                case BIO_SAMPLES -> "cargo.deposit.bio_samples";
                case CIRCUITS -> "cargo.deposit.circuits";
                default -> null;
            };
            if (key != null) {
                recordAction(playerId, key, amount);
            }
        });
    }

    public void finalizeEndState(Player player, boolean commonMissionCompleted) {
        PlayerId playerId = PlayerId.of(player.getUniqueId());

        for (ObjectiveInstance objective : plugin.objectiveEngine().objectives(playerId)) {
            if (objective.status() != ObjectiveStatus.ACTIVE) continue;

            boolean complete = switch (objective.definition().id().value()) {
                case "survive_return" ->
                        commonMissionCompleted && plugin.lobbyService().playerState(playerId)
                                .map(state -> state.isAlive())
                                .orElse(false);
                case "four_survivors" ->
                        commonMissionCompleted && alivePlayers() >= 4;
                case "keep_medical_normal" ->
                        plugin.facilityRegistry()
                                .require(com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog.MEDICAL)
                                .status()
                                == com.hushkisses.spacesurvival.facility.FacilityStatus.NORMAL;
                case "return_infected" ->
                        commonMissionCompleted && plugin.infectionService().state(playerId).infected();
                case "finish_with_equipment" ->
                        hasAnyFunctionalEquipment(player);
                case "keep_destination" ->
                        commonMissionCompleted
                                && plugin.returnObjectiveService().stage()
                                == com.hushkisses.spacesurvival.ending.ReturnStage.COMPLETED;
                default -> false;
            };

            if (complete) {
                objective.complete();
            }
        }
    }

    private int alivePlayers() {
        int alive = 0;
        for (PlayerId playerId : plugin.lobbyService().snapshot().players()) {
            if (plugin.lobbyService().playerState(playerId)
                    .map(state -> state.isAlive())
                    .orElse(false)) {
                alive++;
            }
        }
        return alive;
    }

    private boolean hasAnyFunctionalEquipment(Player player) {
        for (FunctionalItemType type : FunctionalItemType.values()) {
            if (plugin.functionalItemService().has(player, type)) {
                return true;
            }
        }
        return false;
    }
}

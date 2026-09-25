package com.hushkisses.spacesurvival.objective;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.*;

public final class ObjectiveEngine {

    private final Map<PlayerId, EnumMap<ObjectiveSlot, ObjectiveInstance>> byPlayer =
            new LinkedHashMap<>();
    private final Map<UUID, ObjectiveInstance> byId = new LinkedHashMap<>();

    public ObjectiveInstance assign(
            PlayerId playerId,
            ObjectiveDefinition definition,
            ObjectiveSlot slot
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(slot, "slot");

        EnumMap<ObjectiveSlot, ObjectiveInstance> slots =
                byPlayer.computeIfAbsent(playerId, ignored -> new EnumMap<>(ObjectiveSlot.class));

        if (slots.containsKey(slot)) {
            throw new IllegalStateException("Objective slot already occupied: " + slot);
        }

        ObjectiveInstance instance = new ObjectiveInstance(
                UUID.randomUUID(),
                playerId,
                definition,
                slot
        );
        slots.put(slot, instance);
        byId.put(instance.instanceId(), instance);
        return instance;
    }

    public Optional<ObjectiveInstance> objective(PlayerId playerId, ObjectiveSlot slot) {
        EnumMap<ObjectiveSlot, ObjectiveInstance> slots = byPlayer.get(
                Objects.requireNonNull(playerId, "playerId")
        );
        if (slots == null) return Optional.empty();
        return Optional.ofNullable(slots.get(Objects.requireNonNull(slot, "slot")));
    }

    public Optional<ObjectiveInstance> find(UUID instanceId) {
        return Optional.ofNullable(byId.get(Objects.requireNonNull(instanceId, "instanceId")));
    }

    public void advance(UUID instanceId, int amount) {
        find(instanceId).orElseThrow(
                () -> new IllegalArgumentException("Unknown objective instance: " + instanceId)
        ).advance(amount);
    }

    public List<ObjectiveInstance> objectives(PlayerId playerId) {
        EnumMap<ObjectiveSlot, ObjectiveInstance> slots = byPlayer.get(playerId);
        return slots == null ? List.of() : List.copyOf(slots.values());
    }

    public int completedScore(PlayerId playerId) {
        return objectives(playerId).stream()
                .filter(instance -> instance.status() == ObjectiveStatus.COMPLETED)
                .mapToInt(instance -> instance.definition().scoreValue())
                .sum();
    }
}

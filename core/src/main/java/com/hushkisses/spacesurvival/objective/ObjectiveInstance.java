package com.hushkisses.spacesurvival.objective;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.Objects;
import java.util.UUID;

public final class ObjectiveInstance {
    private final UUID instanceId;
    private final PlayerId owner;
    private final ObjectiveDefinition definition;
    private final ObjectiveSlot slot;
    private int progress;
    private ObjectiveStatus status = ObjectiveStatus.ACTIVE;

    public ObjectiveInstance(
            UUID instanceId,
            PlayerId owner,
            ObjectiveDefinition definition,
            ObjectiveSlot slot
    ) {
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.slot = Objects.requireNonNull(slot, "slot");
    }

    public UUID instanceId() { return instanceId; }
    public PlayerId owner() { return owner; }
    public ObjectiveDefinition definition() { return definition; }
    public ObjectiveSlot slot() { return slot; }
    public int progress() { return progress; }
    public ObjectiveStatus status() { return status; }

    public void advance(int amount) {
        if (amount < 1) throw new IllegalArgumentException("amount");
        if (status != ObjectiveStatus.ACTIVE) return;

        progress = Math.min(definition.targetProgress(), progress + amount);
        if (progress >= definition.targetProgress()) {
            status = ObjectiveStatus.COMPLETED;
        }
    }

    public void complete() {
        if (status == ObjectiveStatus.ACTIVE) {
            progress = definition.targetProgress();
            status = ObjectiveStatus.COMPLETED;
        }
    }

    public void fail() {
        if (status == ObjectiveStatus.ACTIVE) {
            status = ObjectiveStatus.FAILED;
        }
    }
}

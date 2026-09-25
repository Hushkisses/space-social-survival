package com.hushkisses.spacesurvival.facility;

import java.util.Objects;

public final class FacilityState {

    private final FacilityDefinition definition;
    private FacilityStatus status;

    public FacilityState(FacilityDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.status = FacilityStatus.NORMAL;
    }

    public FacilityDefinition definition() {
        return definition;
    }

    public FacilityStatus status() {
        return status;
    }

    public void setStatus(FacilityStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public void reset() {
        status = FacilityStatus.NORMAL;
    }

    public FacilityStateSnapshot snapshot() {
        return new FacilityStateSnapshot(
                definition.id(),
                definition.type(),
                definition.displayName(),
                status
        );
    }
}

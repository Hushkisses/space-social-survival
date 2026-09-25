package com.hushkisses.spacesurvival.facility;

import java.util.Objects;

public record FacilityDefinition(
        FacilityId id,
        FacilityType type,
        String displayName,
        String description
) {

    public FacilityDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(description, "description");

        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }
}

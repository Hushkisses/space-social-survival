package com.hushkisses.spacesurvival.resource;

import java.util.Objects;

public record ResourceDefinition(
        ResourceType type,
        String displayName,
        ResourceStoragePreference storagePreference
) {
    public ResourceDefinition {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(storagePreference, "storagePreference");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}

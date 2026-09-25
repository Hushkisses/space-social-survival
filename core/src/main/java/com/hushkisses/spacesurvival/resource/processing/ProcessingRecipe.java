package com.hushkisses.spacesurvival.resource.processing;

import com.hushkisses.spacesurvival.resource.ResourceType;

import java.util.*;

public record ProcessingRecipe(
        ProcessingRecipeId id,
        String displayName,
        Map<ResourceType, Integer> inputs,
        Map<ResourceType, Integer> outputs,
        int durationSeconds
) {
    public ProcessingRecipe {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(outputs, "outputs");
        if (displayName.isBlank()) throw new IllegalArgumentException("displayName");
        if (inputs.isEmpty()) throw new IllegalArgumentException("inputs");
        if (outputs.isEmpty()) throw new IllegalArgumentException("outputs");
        if (durationSeconds < 0) throw new IllegalArgumentException("durationSeconds");

        inputs = immutablePositiveMap(inputs);
        outputs = immutablePositiveMap(outputs);
    }

    private static Map<ResourceType, Integer> immutablePositiveMap(Map<ResourceType, Integer> source) {
        EnumMap<ResourceType, Integer> copy = new EnumMap<>(ResourceType.class);
        source.forEach((type, amount) -> {
            Objects.requireNonNull(type, "resource type");
            if (amount == null || amount < 1) throw new IllegalArgumentException("resource amount");
            copy.put(type, amount);
        });
        return Collections.unmodifiableMap(copy);
    }
}

package com.hushkisses.spacesurvival.resource.processing;

import com.hushkisses.spacesurvival.resource.ResourceStore;
import com.hushkisses.spacesurvival.resource.ResourceType;

import java.util.Map;
import java.util.Objects;

public final class ProcessingService {

    public ProcessingResult process(ResourceStore store, ProcessingRecipe recipe) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(recipe, "recipe");

        for (Map.Entry<ResourceType, Integer> input : recipe.inputs().entrySet()) {
            if (!store.has(input.getKey(), input.getValue())) {
                return ProcessingResult.INSUFFICIENT_INPUTS;
            }
        }

        recipe.inputs().forEach((type, amount) -> {
            boolean removed = store.remove(type, amount);
            if (!removed) {
                throw new IllegalStateException("Processing pre-check was violated");
            }
        });

        recipe.outputs().forEach(store::add);
        return ProcessingResult.SUCCESS;
    }
}

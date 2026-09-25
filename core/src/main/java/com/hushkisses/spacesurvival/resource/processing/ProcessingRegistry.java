package com.hushkisses.spacesurvival.resource.processing;

import java.util.*;

public final class ProcessingRegistry {
    private final Map<ProcessingRecipeId, ProcessingRecipe> recipes = new LinkedHashMap<>();

    public void register(ProcessingRecipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        if (recipes.putIfAbsent(recipe.id(), recipe) != null) {
            throw new IllegalArgumentException("Duplicate recipe id: " + recipe.id());
        }
    }

    public Optional<ProcessingRecipe> find(ProcessingRecipeId id) {
        return Optional.ofNullable(recipes.get(Objects.requireNonNull(id, "id")));
    }

    public Collection<ProcessingRecipe> all() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public int size() { return recipes.size(); }
}

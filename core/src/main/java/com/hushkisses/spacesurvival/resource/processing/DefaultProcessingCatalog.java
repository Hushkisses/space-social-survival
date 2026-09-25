package com.hushkisses.spacesurvival.resource.processing;

import com.hushkisses.spacesurvival.resource.ResourceType;

import java.util.Map;

public final class DefaultProcessingCatalog {

    private DefaultProcessingCatalog() {
    }

    public static ProcessingRegistry createRegistry() {
        ProcessingRegistry registry = new ProcessingRegistry();

        registry.register(new ProcessingRecipe(
                new ProcessingRecipeId("circuit_salvage"),
                "회로판 회수",
                Map.of(ResourceType.REPAIR_PARTS, 2),
                Map.of(ResourceType.CIRCUITS, 1),
                20
        ));

        registry.register(new ProcessingRecipe(
                new ProcessingRecipeId("power_cell_refurbish"),
                "전력 셀 재생",
                Map.of(
                        ResourceType.REPAIR_PARTS, 1,
                        ResourceType.CIRCUITS, 1
                ),
                Map.of(ResourceType.POWER_CELLS, 1),
                30
        ));

        return registry;
    }
}

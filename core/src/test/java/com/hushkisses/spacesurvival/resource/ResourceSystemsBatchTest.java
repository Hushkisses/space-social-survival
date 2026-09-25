package com.hushkisses.spacesurvival.resource;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.resource.node.*;
import com.hushkisses.spacesurvival.resource.processing.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ResourceSystemsBatchTest {

    @Test
    void storeSupportsAtomicLookingTransfer() {
        ResourceStore personal = new ResourceStore();
        ResourceStore shared = new ResourceStore();

        personal.add(ResourceType.REPAIR_PARTS, 5);

        assertTrue(personal.transferTo(shared, ResourceType.REPAIR_PARTS, 3));
        assertEquals(2, personal.quantity(ResourceType.REPAIR_PARTS));
        assertEquals(3, shared.quantity(ResourceType.REPAIR_PARTS));

        assertFalse(personal.transferTo(shared, ResourceType.REPAIR_PARTS, 3));
        assertEquals(2, personal.quantity(ResourceType.REPAIR_PARTS));
        assertEquals(3, shared.quantity(ResourceType.REPAIR_PARTS));
    }

    @Test
    void resourceNodeGenerationIsDeterministic() {
        List<ResourceSpawnSlot> slots = List.of(
                new ResourceSpawnSlot("eng-1", DefaultFacilityCatalog.ENGINEERING),
                new ResourceSpawnSlot("eng-2", DefaultFacilityCatalog.ENGINEERING),
                new ResourceSpawnSlot("med-1", DefaultFacilityCatalog.MEDICAL),
                new ResourceSpawnSlot("med-2", DefaultFacilityCatalog.MEDICAL),
                new ResourceSpawnSlot("res-1", DefaultFacilityCatalog.RESEARCH),
                new ResourceSpawnSlot("cargo-1", DefaultFacilityCatalog.CARGO),
                new ResourceSpawnSlot("hab-1", DefaultFacilityCatalog.HABITATION)
        );
        ResourceNodeGenerationConfig config =
                new ResourceNodeGenerationConfig(4, 6, 1, 3);
        ResourceNodeGenerator generator = new ResourceNodeGenerator();

        List<ResourceNode> first = generator.generate(slots, config, new Random(12345));
        List<ResourceNode> second = generator.generate(slots, config, new Random(12345));

        assertEquals(first, second);
        assertTrue(first.size() >= 4 && first.size() <= 6);
        assertEquals(first.size(),
                first.stream().map(ResourceNode::spawnSlotId).distinct().count());
    }

    @Test
    void processingConsumesInputsAndProducesOutputs() {
        ResourceStore store = new ResourceStore();
        store.add(ResourceType.REPAIR_PARTS, 4);

        ProcessingRecipe recipe = DefaultProcessingCatalog.createRegistry()
                .find(new ProcessingRecipeId("circuit_salvage"))
                .orElseThrow();

        ProcessingResult result = new ProcessingService().process(store, recipe);

        assertEquals(ProcessingResult.SUCCESS, result);
        assertEquals(2, store.quantity(ResourceType.REPAIR_PARTS));
        assertEquals(1, store.quantity(ResourceType.CIRCUITS));
    }

    @Test
    void failedProcessingDoesNotConsumeAnything() {
        ResourceStore store = new ResourceStore();
        store.add(ResourceType.REPAIR_PARTS, 1);

        ProcessingRecipe recipe = DefaultProcessingCatalog.createRegistry()
                .find(new ProcessingRecipeId("circuit_salvage"))
                .orElseThrow();

        assertEquals(
                ProcessingResult.INSUFFICIENT_INPUTS,
                new ProcessingService().process(store, recipe)
        );
        assertEquals(1, store.quantity(ResourceType.REPAIR_PARTS));
        assertEquals(0, store.quantity(ResourceType.CIRCUITS));
    }
}

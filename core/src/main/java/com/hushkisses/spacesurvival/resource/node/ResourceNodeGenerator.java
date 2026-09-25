package com.hushkisses.spacesurvival.resource.node;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.resource.ResourceType;

import java.util.*;
import java.util.random.RandomGenerator;

public final class ResourceNodeGenerator {

    public List<ResourceNode> generate(
            List<ResourceSpawnSlot> slots,
            ResourceNodeGenerationConfig config,
            RandomGenerator random
    ) {
        Objects.requireNonNull(slots, "slots");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(random, "random");

        if (slots.size() < config.minNodes()) {
            throw new IllegalArgumentException("Not enough spawn slots");
        }

        ArrayList<ResourceSpawnSlot> shuffled = new ArrayList<>(slots);
        shuffle(shuffled, random);

        int upper = Math.min(config.maxNodes(), shuffled.size());
        int count = config.minNodes() + random.nextInt(upper - config.minNodes() + 1);

        ArrayList<ResourceNode> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ResourceSpawnSlot slot = shuffled.get(i);
            ResourceType type = chooseResource(slot.facilityId(), random);
            int quantity = config.minQuantity()
                    + random.nextInt(config.maxQuantity() - config.minQuantity() + 1);

            result.add(new ResourceNode(
                    new ResourceNodeId("node_" + (i + 1)),
                    type,
                    quantity,
                    slot.id(),
                    slot.facilityId()
            ));
        }

        return List.copyOf(result);
    }

    private static ResourceType chooseResource(FacilityId facilityId, RandomGenerator random) {
        List<ResourceType> pool;

        if (facilityId.equals(DefaultFacilityCatalog.ENGINEERING)) {
            pool = List.of(
                    ResourceType.REPAIR_PARTS,
                    ResourceType.CIRCUITS,
                    ResourceType.POWER_CELLS,
                    ResourceType.FUEL
            );
        } else if (facilityId.equals(DefaultFacilityCatalog.MEDICAL)) {
            pool = List.of(
                    ResourceType.MEDICAL_SUPPLIES,
                    ResourceType.BIO_SAMPLES
            );
        } else if (facilityId.equals(DefaultFacilityCatalog.RESEARCH)) {
            pool = List.of(
                    ResourceType.BIO_SAMPLES,
                    ResourceType.DATA_CORES,
                    ResourceType.CIRCUITS
            );
        } else if (facilityId.equals(DefaultFacilityCatalog.BRIDGE)) {
            pool = List.of(
                    ResourceType.DATA_CORES,
                    ResourceType.CIRCUITS,
                    ResourceType.POWER_CELLS
            );
        } else if (facilityId.equals(DefaultFacilityCatalog.HABITATION)) {
            pool = List.of(
                    ResourceType.MEDICAL_SUPPLIES,
                    ResourceType.REPAIR_PARTS,
                    ResourceType.POWER_CELLS
            );
        } else {
            pool = List.of(ResourceType.values());
        }

        return pool.get(random.nextInt(pool.size()));
    }

    private static <T> void shuffle(List<T> list, RandomGenerator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }
}

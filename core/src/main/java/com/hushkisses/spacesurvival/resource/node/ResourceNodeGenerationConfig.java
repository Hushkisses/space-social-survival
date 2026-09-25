package com.hushkisses.spacesurvival.resource.node;

public record ResourceNodeGenerationConfig(
        int minNodes,
        int maxNodes,
        int minQuantity,
        int maxQuantity
) {
    public ResourceNodeGenerationConfig {
        if (minNodes < 1 || maxNodes < minNodes) throw new IllegalArgumentException("node count");
        if (minQuantity < 1 || maxQuantity < minQuantity) throw new IllegalArgumentException("quantity");
    }
}

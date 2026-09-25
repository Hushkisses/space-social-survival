package com.hushkisses.spacesurvival.objective.conflict;

import java.util.*;
import java.util.random.RandomGenerator;

public final class ConflictSetSelector {

    public List<ConflictSetDefinition> select(
            List<ConflictSetDefinition> available,
            int minCount,
            int maxCount,
            RandomGenerator random
    ) {
        Objects.requireNonNull(available, "available");
        Objects.requireNonNull(random, "random");
        if (minCount < 1 || maxCount < minCount || available.size() < minCount) {
            throw new IllegalArgumentException("invalid conflict-set count");
        }

        ArrayList<ConflictSetDefinition> shuffled = new ArrayList<>(available);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            Collections.swap(shuffled, i, random.nextInt(i + 1));
        }

        int upper = Math.min(maxCount, shuffled.size());
        int count = minCount + random.nextInt(upper - minCount + 1);
        return List.copyOf(shuffled.subList(0, count));
    }
}

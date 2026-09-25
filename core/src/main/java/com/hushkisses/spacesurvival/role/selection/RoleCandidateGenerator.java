package com.hushkisses.spacesurvival.role.selection;

import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.RoleDefinition;
import com.hushkisses.spacesurvival.role.RoleId;
import com.hushkisses.spacesurvival.role.RoleRegistry;

import java.util.*;
import java.util.random.RandomGenerator;

public final class RoleCandidateGenerator {

    public Map<PlayerId, RoleCandidateSet> generate(
            List<PlayerId> players,
            RoleRegistry registry,
            RandomGenerator random
    ) {
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(random, "random");

        if (players.isEmpty()) {
            throw new IllegalArgumentException("At least one player is required");
        }
        if (players.stream().distinct().count() != players.size()) {
            throw new IllegalArgumentException("Player list contains duplicates");
        }

        List<RoleId> roleIds = registry.all().stream()
                .map(RoleDefinition::id)
                .toList();

        if (roleIds.size() < 3) {
            throw new IllegalArgumentException("At least 3 roles are required");
        }

        LinkedHashMap<PlayerId, RoleCandidateSet> result = new LinkedHashMap<>();

        for (PlayerId playerId : players) {
            ArrayList<RoleId> shuffled = new ArrayList<>(roleIds);
            shuffle(shuffled, random);

            List<RoleId> candidates = List.copyOf(shuffled.subList(0, 3));
            result.put(playerId, new RoleCandidateSet(playerId, candidates));
        }

        return Collections.unmodifiableMap(result);
    }

    private static <T> void shuffle(List<T> list, RandomGenerator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }
}

package com.hushkisses.spacesurvival.role.selection;

import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.RoleDefinition;
import com.hushkisses.spacesurvival.role.RoleId;
import com.hushkisses.spacesurvival.role.RoleRegistry;

import java.util.*;
import java.util.random.RandomGenerator;

public final class RoleSelectionService {

    private final RoleRegistry registry;
    private final RoleCandidateGenerator generator;
    private Map<PlayerId, RoleCandidateSet> candidateSets = Map.of();
    private final Map<PlayerId, RoleId> selections = new LinkedHashMap<>();
    private final Map<RoleId, Integer> selectedCounts = new HashMap<>();

    public RoleSelectionService(RoleRegistry registry) {
        this(registry, new RoleCandidateGenerator());
    }

    public RoleSelectionService(
            RoleRegistry registry,
            RoleCandidateGenerator generator
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.generator = Objects.requireNonNull(generator, "generator");
    }

    public void prepareCandidates(
            List<PlayerId> players,
            RandomGenerator random
    ) {
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(random, "random");

        if (!selections.isEmpty()) {
            throw new IllegalStateException("Cannot regenerate candidates after selection has started");
        }

        candidateSets = generator.generate(players, registry, random);
        selectedCounts.clear();
    }

    public Optional<RoleCandidateSet> candidates(PlayerId playerId) {
        return Optional.ofNullable(
                candidateSets.get(Objects.requireNonNull(playerId, "playerId"))
        );
    }

    public RoleSelectionResult select(PlayerId playerId, RoleId roleId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(roleId, "roleId");

        if (selections.containsKey(playerId)) {
            return RoleSelectionResult.ALREADY_SELECTED;
        }

        RoleCandidateSet candidateSet = candidateSets.get(playerId);
        if (candidateSet == null) {
            return RoleSelectionResult.CANDIDATES_NOT_PREPARED;
        }

        if (!candidateSet.contains(roleId)) {
            return RoleSelectionResult.ROLE_NOT_OFFERED;
        }

        RoleDefinition role = registry.require(roleId);
        int selected = selectedCounts.getOrDefault(roleId, 0);
        if (selected >= role.maxCopies()) {
            return RoleSelectionResult.ROLE_FULL;
        }

        selections.put(playerId, roleId);
        selectedCounts.put(roleId, selected + 1);
        return RoleSelectionResult.SELECTED;
    }

    public Optional<RoleId> selectedRole(PlayerId playerId) {
        return Optional.ofNullable(
                selections.get(Objects.requireNonNull(playerId, "playerId"))
        );
    }

    public int selectedCount(RoleId roleId) {
        return selectedCounts.getOrDefault(
                Objects.requireNonNull(roleId, "roleId"),
                0
        );
    }

    public int preparedPlayerCount() {
        return candidateSets.size();
    }

    public int selectedPlayerCount() {
        return selections.size();
    }

    public Map<PlayerId, RoleId> selections() {
        return Collections.unmodifiableMap(selections);
    }

    public void reset() {
        candidateSets = Map.of();
        selections.clear();
        selectedCounts.clear();
    }
}

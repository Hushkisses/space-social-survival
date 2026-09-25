package com.hushkisses.spacesurvival.role.selection;

import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.RoleId;

import java.util.List;
import java.util.Objects;

public record RoleCandidateSet(
        PlayerId playerId,
        List<RoleId> candidates
) {

    public RoleCandidateSet {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(candidates, "candidates");

        candidates = List.copyOf(candidates);

        if (candidates.size() != 3) {
            throw new IllegalArgumentException("Exactly 3 role candidates are required");
        }
        if (candidates.stream().distinct().count() != 3) {
            throw new IllegalArgumentException("Role candidates must be distinct");
        }
    }

    public boolean contains(RoleId roleId) {
        return candidates.contains(Objects.requireNonNull(roleId, "roleId"));
    }
}

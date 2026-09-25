package com.hushkisses.spacesurvival.role;

import java.util.*;

public final class RoleRegistry {

    private final Map<RoleId, RoleDefinition> roles = new LinkedHashMap<>();

    public void register(RoleDefinition role) {
        Objects.requireNonNull(role, "role");

        if (roles.putIfAbsent(role.id(), role) != null) {
            throw new IllegalArgumentException("Duplicate role id: " + role.id());
        }
    }

    public Optional<RoleDefinition> find(RoleId id) {
        return Optional.ofNullable(roles.get(Objects.requireNonNull(id, "id")));
    }

    public RoleDefinition require(RoleId id) {
        return find(id).orElseThrow(
                () -> new IllegalArgumentException("Unknown role id: " + id)
        );
    }

    public Collection<RoleDefinition> all() {
        return Collections.unmodifiableCollection(roles.values());
    }

    public int size() {
        return roles.size();
    }
}

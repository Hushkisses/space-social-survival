package com.hushkisses.spacesurvival.role;

import java.util.*;

public final class RoleDefinition {

    private final RoleId id;
    private final String displayName;
    private final String description;
    private final int maxCopies;
    private final Set<RolePassive> passives;
    private final Set<RoleCapability> capabilities;

    public RoleDefinition(
            RoleId id,
            String displayName,
            String description,
            int maxCopies,
            Collection<RolePassive> passives,
            Collection<RoleCapability> capabilities
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = requireText(displayName, "displayName");
        this.description = requireText(description, "description");

        if (maxCopies < 1) {
            throw new IllegalArgumentException("maxCopies must be at least 1");
        }

        this.maxCopies = maxCopies;
        this.passives = immutableEnumSet(passives, RolePassive.class);
        this.capabilities = immutableEnumSet(capabilities, RoleCapability.class);
    }

    public RoleId id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public int maxCopies() {
        return maxCopies;
    }

    public Set<RolePassive> passives() {
        return passives;
    }

    public Set<RoleCapability> capabilities() {
        return capabilities;
    }

    public boolean hasPassive(RolePassive passive) {
        return passives.contains(Objects.requireNonNull(passive, "passive"));
    }

    public boolean hasCapability(RoleCapability capability) {
        return capabilities.contains(Objects.requireNonNull(capability, "capability"));
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static <E extends Enum<E>> Set<E> immutableEnumSet(
            Collection<E> values,
            Class<E> enumClass
    ) {
        Objects.requireNonNull(values, "values");
        EnumSet<E> set = EnumSet.noneOf(enumClass);
        for (E value : values) {
            set.add(Objects.requireNonNull(value, "enum value"));
        }
        return Collections.unmodifiableSet(set);
    }
}

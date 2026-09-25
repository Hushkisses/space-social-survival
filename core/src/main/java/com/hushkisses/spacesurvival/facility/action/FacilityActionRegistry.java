package com.hushkisses.spacesurvival.facility.action;

import com.hushkisses.spacesurvival.facility.FacilityId;

import java.util.*;

public final class FacilityActionRegistry {

    private final Map<FacilityActionId, FacilityActionDefinition> actions = new LinkedHashMap<>();

    public void register(FacilityActionDefinition action) {
        Objects.requireNonNull(action, "action");
        if (actions.putIfAbsent(action.id(), action) != null) {
            throw new IllegalArgumentException("Duplicate facility action id: " + action.id());
        }
    }

    public Optional<FacilityActionDefinition> find(FacilityActionId id) {
        return Optional.ofNullable(actions.get(Objects.requireNonNull(id, "id")));
    }

    public List<FacilityActionDefinition> forFacility(FacilityId facilityId) {
        Objects.requireNonNull(facilityId, "facilityId");
        return actions.values().stream()
                .filter(action -> action.facilityId().equals(facilityId))
                .toList();
    }

    public Collection<FacilityActionDefinition> all() {
        return Collections.unmodifiableCollection(actions.values());
    }

    public int size() {
        return actions.size();
    }
}

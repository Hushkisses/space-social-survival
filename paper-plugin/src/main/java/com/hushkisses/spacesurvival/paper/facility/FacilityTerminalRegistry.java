package com.hushkisses.spacesurvival.paper.facility;

import com.hushkisses.spacesurvival.facility.FacilityId;
import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FacilityTerminalRegistry {

    private final Map<WorldBlockKey, FacilityId> terminals = new LinkedHashMap<>();

    public void clear() {
        terminals.clear();
    }

    public void register(Location location, FacilityId facilityId) {
        terminals.put(
                WorldBlockKey.of(location),
                Objects.requireNonNull(facilityId, "facilityId")
        );
    }

    public Optional<FacilityId> facilityAt(Location location) {
        return Optional.ofNullable(terminals.get(WorldBlockKey.of(location)));
    }

    public int size() {
        return terminals.size();
    }
}

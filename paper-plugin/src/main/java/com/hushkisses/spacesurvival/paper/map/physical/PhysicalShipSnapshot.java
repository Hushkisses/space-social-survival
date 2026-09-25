package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.generation.GeneratedMap;
import com.hushkisses.spacesurvival.map.tile.TileDefinition;
import com.hushkisses.spacesurvival.map.tile.TileId;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public record PhysicalShipSnapshot(
        long seed,
        World world,
        GeneratedMap generatedMap,
        Map<TileId, TileDefinition> definitions,
        Map<TileId, PhysicalTilePlacement> placements,
        Map<PortalBlockKey, Location> portalDestinations
) {
    public PhysicalShipSnapshot {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(generatedMap, "generatedMap");
        definitions = Map.copyOf(definitions);
        placements = Map.copyOf(placements);

        LinkedHashMap<PortalBlockKey, Location> copied = new LinkedHashMap<>();
        portalDestinations.forEach((key, value) -> copied.put(key, value.clone()));
        portalDestinations = Collections.unmodifiableMap(copied);
    }

    public Location bridgeSpawn() {
        PhysicalTilePlacement placement = placements.get(new TileId("bridge"));
        if (placement == null) {
            placement = placements.values().iterator().next();
        }
        return placement.center(world);
    }

    public Optional<Location> portalDestination(Location sourceBlock) {
        Location destination = portalDestinations.get(PortalBlockKey.of(sourceBlock));
        return destination == null ? Optional.empty() : Optional.of(destination.clone());
    }

    public Optional<TileId> tileAt(Location location) {
        return placements.values().stream()
                .filter(placement -> placement.contains(location))
                .map(PhysicalTilePlacement::tileId)
                .findFirst();
    }

    public String tileDisplayName(TileId tileId) {
        TileDefinition definition = definitions.get(tileId);
        return definition == null ? tileId.value() : definition.displayName();
    }
}

package com.hushkisses.spacesurvival.map.tile;

import java.util.*;

public final class TileRegistry {

    private final Map<TileId, TileDefinition> tiles = new LinkedHashMap<>();

    public void register(TileDefinition definition) {
        Objects.requireNonNull(definition, "definition");

        if (tiles.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("Duplicate tile id: " + definition.id());
        }
    }

    public Optional<TileDefinition> find(TileId id) {
        return Optional.ofNullable(tiles.get(Objects.requireNonNull(id, "id")));
    }

    public TileDefinition require(TileId id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown tile id: " + id));
    }

    public Collection<TileDefinition> all() {
        return Collections.unmodifiableCollection(tiles.values());
    }

    public List<TileDefinition> byCategory(TileCategory category) {
        Objects.requireNonNull(category, "category");

        return tiles.values().stream()
                .filter(tile -> tile.category() == category)
                .toList();
    }

    public int size() {
        return tiles.size();
    }
}

package com.hushkisses.spacesurvival.paper.map;

import com.hushkisses.spacesurvival.map.generation.ConstrainedRandomMapGenerator;
import com.hushkisses.spacesurvival.map.generation.GeneratedConnection;
import com.hushkisses.spacesurvival.map.generation.GeneratedMap;
import com.hushkisses.spacesurvival.map.generation.MapGenerationConstraints;
import com.hushkisses.spacesurvival.map.tile.TileDefinition;
import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MapDebugService {

    private static final MapGenerationConstraints DEBUG_CONSTRAINTS =
            new MapGenerationConstraints(10, 14, 6, 6, 500);

    private final ConstrainedRandomMapGenerator generator = new ConstrainedRandomMapGenerator();
    private final List<TileDefinition> pool = DebugTilePool.create();
    private final Map<TileId, TileDefinition> definitions = pool.stream()
            .collect(Collectors.toUnmodifiableMap(TileDefinition::id, Function.identity()));

    public DebugMapResult generate(long seed) {
        GeneratedMap map = generator.generate(pool, DEBUG_CONSTRAINTS, new Random(seed));
        return new DebugMapResult(seed, map, definitions);
    }

    public record DebugMapResult(
            long seed,
            GeneratedMap map,
            Map<TileId, TileDefinition> definitions
    ) {

        public List<String> tileLines() {
            return map.tileIds().stream()
                    .map(id -> {
                        TileDefinition definition = definitions.get(id);
                        return id + " [" + definition.category() + "]";
                    })
                    .toList();
        }

        public List<String> connectionLines() {
            return map.connections().stream()
                    .map(MapDebugService.DebugMapResult::connectionLine)
                    .toList();
        }

        private static String connectionLine(GeneratedConnection connection) {
            return connection.first() + " <-> " + connection.second();
        }
    }
}

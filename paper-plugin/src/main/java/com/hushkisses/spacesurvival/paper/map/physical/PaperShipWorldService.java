package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.map.generation.CompactSingleDeckMapGenerator;
import com.hushkisses.spacesurvival.map.generation.GeneratedConnection;
import com.hushkisses.spacesurvival.map.generation.GeneratedMap;
import com.hushkisses.spacesurvival.map.generation.MapGenerationConstraints;
import com.hushkisses.spacesurvival.map.tile.DefaultTileCatalog;
import com.hushkisses.spacesurvival.map.tile.TileCategory;
import com.hushkisses.spacesurvival.map.tile.TileDefinition;
import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.config.MatchSetupConfig;
import com.hushkisses.spacesurvival.paper.facility.FacilityTerminalRegistry;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PaperShipWorldService {

    public static final String WORLD_NAME = "space_ship_compact_v5";

    private static final int FLOOR_Y = CompactSingleDeckLayoutPlanner.FLOOR_Y;
    private static final int ROOM_HEIGHT = 5;
    private static final int CORRIDOR_HALF_WIDTH = 1;

    private final CompactSingleDeckMapGenerator generator = new CompactSingleDeckMapGenerator();
    private final CompactSingleDeckLayoutPlanner layoutPlanner =
            new CompactSingleDeckLayoutPlanner();
    private final ShipModuleStructureLoader structureLoader;
    private final FacilityTerminalRegistry terminals;
    private final PhysicalConnectionController connections;

    private PhysicalShipSnapshot activeSnapshot;
    private Map<TileId, StructurePlacementResult> structurePlacements = Map.of();
    private final Map<RouteDirection, Location> routeMarkers = new LinkedHashMap<>();
    private final Set<LightPoint> lightPoints = new LinkedHashSet<>();
    private TileId highlightedRouteTarget;
    private LightingStage lightingStage;

    public PaperShipWorldService(
            JavaPlugin plugin,
            FacilityTerminalRegistry terminals,
            PhysicalConnectionController connections
    ) {
        this.structureLoader = new ShipModuleStructureLoader(
                Objects.requireNonNull(plugin, "plugin")
        );
        this.terminals = Objects.requireNonNull(terminals, "terminals");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public PhysicalShipSnapshot generateAndRender(long seed, MatchSetupConfig config) {
        Objects.requireNonNull(config, "config");

        List<TileDefinition> pool = DefaultTileCatalog.create();
        Map<TileId, TileDefinition> definitions = pool.stream()
                .collect(Collectors.toUnmodifiableMap(TileDefinition::id, Function.identity()));

        GeneratedMap generated = generator.generate(
                pool,
                new MapGenerationConstraints(
                        config.mapMinTiles(),
                        config.mapMaxTiles(),
                        config.mapMaxDeadEnds(),
                        config.mapMaxCoreDistance(),
                        config.mapMaxAttempts()
                ),
                new Random(seed)
        );

        Map<TileId, PhysicalTilePlacement> placements =
                layoutPlanner.plan(generated.tileIds());

        World world = resolveWorld();
        normalizeWorld(world);
        clearLegacyDisplays(world);
        clearBuildArea(world);

        terminals.clear();
        connections.reset();
        routeMarkers.clear();
        lightPoints.clear();
        highlightedRouteTarget = null;
        lightingStage = null;

        LinkedHashMap<TileId, StructurePlacementResult> placementResults =
                new LinkedHashMap<>();

        for (TileId tileId : generated.tileIds()) {
            PhysicalTilePlacement placement = placements.get(tileId);

            // MAP-V5 intentionally ignores legacy large NBT rooms. The compact
            // spatial skeleton is the source of truth until its scale is accepted.
            placementResults.put(
                    tileId,
                    new StructurePlacementResult(
                            false,
                            "MAP-V5",
                            "compact procedural room"
                    )
            );

            renderModule(world, placement, definitions.get(tileId));
            renderRoomSign(world, placement, definitions.get(tileId));

            FacilityId facilityId = coreFacility(tileId);
            if (facilityId != null) {
                renderFacilityTerminal(world, placement, facilityId);
            }
        }

        LinkedHashSet<PhysicalDeckCell> corridorCells = new LinkedHashSet<>();
        LinkedHashSet<PhysicalDeckCell> centerLines = new LinkedHashSet<>();
        LinkedHashMap<PhysicalDeckCell, Material> navigationLines = new LinkedHashMap<>();
        ArrayList<RenderedConnection> renderedConnections = new ArrayList<>();

        int connectionIndex = 0;
        for (GeneratedConnection connection : generated.connections()) {
            PhysicalTilePlacement first = placements.get(connection.first().tileId());
            PhysicalTilePlacement second = placements.get(connection.second().tileId());

            Doorway firstDoor = doorwayToward(world, first, second);
            Doorway secondDoor = doorwayToward(world, second, first);

            List<PhysicalDeckCell> centerPath = routeCorridor(
                    firstDoor.outside(),
                    secondDoor.outside(),
                    placements.values(),
                    seed ^ ((long) connectionIndex * 0x9E3779B9L)
            );

            centerLines.addAll(centerPath);
            corridorCells.addAll(expandCorridor(centerPath));

            Material routeColor = routeColor(connection);
            if (routeColor != null) {
                for (PhysicalDeckCell cell : centerPath) {
                    navigationLines.put(cell, routeColor);
                }
            }

            renderedConnections.add(new RenderedConnection(
                    connection,
                    firstDoor,
                    secondDoor
            ));
            connectionIndex++;
        }

        renderCorridorNetwork(
                world,
                corridorCells,
                centerLines,
                navigationLines,
                placements.values()
        );

        LinkedHashMap<PortalBlockKey, Location> legacyPortals = new LinkedHashMap<>();
        int connectionId = 0;

        for (RenderedConnection rendered : renderedConnections) {
            GeneratedConnection connection = rendered.connection();

            carveDoorway(rendered.firstDoor().threshold());
            carveDoorway(rendered.secondDoor().threshold());
            renderConnectionThreshold(rendered.firstDoor().threshold());
            renderConnectionThreshold(rendered.secondDoor().threshold());

            connections.register(
                    connectionId++,
                    connection,
                    rendered.firstDoor().threshold(),
                    rendered.secondDoor().threshold()
            );

            routeMarkers.put(
                    new RouteDirection(
                            connection.first().tileId(),
                            connection.second().tileId()
                    ),
                    markerLocation(world, rendered.firstDoor().outside())
            );
            routeMarkers.put(
                    new RouteDirection(
                            connection.second().tileId(),
                            connection.first().tileId()
                    ),
                    markerLocation(world, rendered.secondDoor().outside())
            );
        }

        renderHubWayfinding(world, placements);
        renderInteriorWayfinding(world, placements);
        structurePlacements = Collections.unmodifiableMap(placementResults);

        activeSnapshot = new PhysicalShipSnapshot(
                seed,
                world,
                generated,
                definitions,
                placements,
                legacyPortals,
                corridorCells
        );
        return activeSnapshot;
    }

    public Optional<PhysicalShipSnapshot> activeSnapshot() {
        return Optional.ofNullable(activeSnapshot);
    }

    public Map<TileId, StructurePlacementResult> structurePlacements() {
        return structurePlacements;
    }

    public ShipModuleStructureLoader structureLoader() {
        return structureLoader;
    }

    public void updateLighting(int power) {
        if (activeSnapshot == null) {
            return;
        }

        LightingStage next = LightingStage.fromPower(power);
        if (next == lightingStage) {
            return;
        }
        lightingStage = next;

        World world = activeSnapshot.world();
        for (LightPoint point : lightPoints) {
            world.getBlockAt(point.x(), point.fixtureY(), point.z())
                    .setType(next.fixture(), false);

            var block = world.getBlockAt(point.x(), point.lightY(), point.z());
            block.setType(Material.LIGHT, false);
            Light data = (Light) block.getBlockData();
            data.setLevel(next.lightLevel());
            block.setBlockData(data, false);
        }
    }

    public void updatePriorityRoute(FacilityId facilityId) {
        Objects.requireNonNull(facilityId, "facilityId");
        updatePriorityRoute(new TileId(facilityId.value()));
    }

    public void updatePriorityRoute(TileId target) {
        Objects.requireNonNull(target, "target");

        PhysicalShipSnapshot ship = activeSnapshot;
        if (ship == null || !ship.generatedMap().tileIds().contains(target)) {
            clearPriorityRoute();
            return;
        }

        if (target.equals(highlightedRouteTarget)) {
            return;
        }

        highlightedRouteTarget = target;

        for (Map.Entry<RouteDirection, Location> entry : routeMarkers.entrySet()) {
            RouteDirection direction = entry.getKey();
            int fromDistance = ship.generatedMap().distance(direction.from(), target);
            int toDistance = ship.generatedMap().distance(direction.to(), target);
            boolean towardTarget = !direction.from().equals(target)
                    && toDistance < fromDistance;

            entry.getValue().getBlock().setType(
                    towardTarget ? Material.LIME_CARPET : Material.AIR,
                    false
            );
        }
    }

    public void clearPriorityRoute() {
        highlightedRouteTarget = null;
        for (Location marker : routeMarkers.values()) {
            if (marker.getWorld() != null) {
                marker.getBlock().setType(Material.AIR, false);
            }
        }
    }

    public String deckName(Location location) {
        return "주 갑판";
    }

    private static void normalizeWorld(World world) {
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(18000L);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
    }

    private static void clearLegacyDisplays(World world) {
        world.getEntitiesByClass(TextDisplay.class).stream()
                .filter(entity -> entity.getScoreboardTags().stream()
                        .anyMatch(tag -> tag.startsWith("spacesurvival_")))
                .forEach(TextDisplay::remove);
    }

    private static World resolveWorld() {
        World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) {
            return existing;
        }

        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);

        World world = creator.createWorld();
        if (world == null) {
            throw new IllegalStateException("Could not create world: " + WORLD_NAME);
        }

        normalizeWorld(world);
        return world;
    }

    private static void clearBuildArea(World world) {
        int minX = CompactSingleDeckLayoutPlanner.SHIP_MIN_X - 6;
        int maxX = CompactSingleDeckLayoutPlanner.SHIP_MAX_X + 6;
        int minZ = CompactSingleDeckLayoutPlanner.SHIP_MIN_Z - 6;
        int maxZ = CompactSingleDeckLayoutPlanner.SHIP_MAX_Z + 6;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = FLOOR_Y - 1; y <= FLOOR_Y + ROOM_HEIGHT + 2; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private void renderModule(
            World world,
            PhysicalTilePlacement placement,
            TileDefinition definition
    ) {
        int minX = placement.minX();
        int minZ = placement.minZ();
        int maxX = minX + placement.size() - 1;
        int maxZ = minZ + placement.size() - 1;

        Material accent = accent(definition.category());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.getBlockAt(x, FLOOR_Y, z).setType(Material.SMOOTH_STONE, false);
                world.getBlockAt(x, FLOOR_Y + ROOM_HEIGHT, z)
                        .setType(Material.IRON_BLOCK, false);

                boolean wall = x == minX || x == maxX || z == minZ || z == maxZ;
                if (wall) {
                    for (int y = FLOOR_Y + 1; y < FLOOR_Y + ROOM_HEIGHT; y++) {
                        world.getBlockAt(x, y, z).setType(
                                y == FLOOR_Y + 2
                                        ? accent
                                        : Material.LIGHT_GRAY_CONCRETE,
                                false
                        );
                    }
                } else {
                    for (int y = FLOOR_Y + 1; y < FLOOR_Y + ROOM_HEIGHT; y++) {
                        world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    }
                }
            }
        }

        Location center = placement.center(world);
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        world.getBlockAt(cx, FLOOR_Y, cz).setType(roomFloorAccent(definition), false);

        registerLightPoint(cx, FLOOR_Y + ROOM_HEIGHT - 1, FLOOR_Y + ROOM_HEIGHT, cz);
        if (placement.size() >= 11) {
            registerLightPoint(
                    minX + 2,
                    FLOOR_Y + ROOM_HEIGHT - 1,
                    FLOOR_Y + ROOM_HEIGHT,
                    minZ + 2
            );
            registerLightPoint(
                    maxX - 2,
                    FLOOR_Y + ROOM_HEIGHT - 1,
                    FLOOR_Y + ROOM_HEIGHT,
                    maxZ - 2
            );
        }

        world.getBlockAt(minX + 2, FLOOR_Y + 1, minZ + 2)
                .setType(Material.IRON_BLOCK, false);
    }

    private static void renderRoomSign(
            World world,
            PhysicalTilePlacement placement,
            TileDefinition definition
    ) {
        int x = placement.minX() + 1;
        int z = placement.minZ() + 1;

        var block = world.getBlockAt(x, FLOOR_Y + 1, z);
        block.setType(Material.OAK_SIGN, false);

        if (block.getBlockData() instanceof Rotatable rotatable) {
            rotatable.setRotation(org.bukkit.block.BlockFace.SOUTH);
            block.setBlockData(rotatable, false);
        }

        if (block.getState() instanceof Sign sign) {
            sign.setLine(0, "[" + definition.displayName() + "]");
            sign.setLine(1, categoryName(definition.category()));
            sign.update(true, false);
        }
    }

    private static void renderHubWayfinding(
            World world,
            Map<TileId, PhysicalTilePlacement> placements
    ) {
        PhysicalTilePlacement hub = placements.get(new TileId("junction_1"));
        if (hub == null) return;

        int cx = hub.center(world).getBlockX();
        int cz = hub.center(world).getBlockZ();

        standingSign(world, cx - 3, FLOOR_Y + 1, cz, "← 함교", "BRIDGE");
        standingSign(world, cx + 3, FLOOR_Y + 1, cz, "기관실 →", "ENGINE");
        standingSign(world, cx, FLOOR_Y + 1, cz - 3, "↑ 생활·화물", "NORTH");
        standingSign(world, cx, FLOOR_Y + 1, cz + 3, "↓ 의료·연구", "SOUTH");

        world.getBlockAt(cx, FLOOR_Y, cz).setType(Material.YELLOW_CONCRETE, false);
    }

    private static void renderInteriorWayfinding(
            World world,
            Map<TileId, PhysicalTilePlacement> placements
    ) {
        PhysicalTilePlacement hub = placements.get(new TileId("junction_1"));
        if (hub != null) {
            int cx = hub.center(world).getBlockX();
            int cz = hub.center(world).getBlockZ();

            paintFloorLine(world, cx, cz, hub.minX(), cz, Material.LIGHT_BLUE_CONCRETE);
            paintFloorLine(
                    world,
                    cx,
                    cz,
                    hub.minX() + hub.size() - 1,
                    cz,
                    Material.RED_CONCRETE
            );
            paintFloorLine(world, cx, cz, cx, hub.minZ(), Material.LIME_CONCRETE);
            paintFloorLine(
                    world,
                    cx,
                    cz,
                    cx,
                    hub.minZ() + hub.size() - 1,
                    Material.PINK_CONCRETE
            );
            world.getBlockAt(cx, FLOOR_Y, cz).setType(Material.YELLOW_CONCRETE, false);
        }

        PhysicalTilePlacement habitation = placements.get(new TileId("habitation"));
        if (habitation != null) {
            int cx = habitation.center(world).getBlockX();
            int cz = habitation.center(world).getBlockZ();
            paintFloorLine(
                    world,
                    cx,
                    cz,
                    habitation.minX() + habitation.size() - 1,
                    cz,
                    Material.ORANGE_CONCRETE
            );
        }

        PhysicalTilePlacement medical = placements.get(new TileId("medical"));
        if (medical != null) {
            int cx = medical.center(world).getBlockX();
            int cz = medical.center(world).getBlockZ();
            paintFloorLine(
                    world,
                    cx,
                    cz,
                    medical.minX() + medical.size() - 1,
                    cz,
                    Material.PURPLE_CONCRETE
            );
        }
    }

    private static void paintFloorLine(
            World world,
            int startX,
            int startZ,
            int endX,
            int endZ,
            Material material
    ) {
        int x = startX;
        int z = startZ;
        world.getBlockAt(x, FLOOR_Y, z).setType(material, false);

        while (x != endX) {
            x += Integer.compare(endX, x);
            world.getBlockAt(x, FLOOR_Y, z).setType(material, false);
        }
        while (z != endZ) {
            z += Integer.compare(endZ, z);
            world.getBlockAt(x, FLOOR_Y, z).setType(material, false);
        }
    }

    private static Material routeColor(GeneratedConnection connection) {
        String first = connection.first().tileId().value();
        String second = connection.second().tileId().value();

        if (pair(first, second, "bridge", "junction_1")) {
            return Material.LIGHT_BLUE_CONCRETE;
        }
        if (pair(first, second, "junction_1", "engineering")) {
            return Material.RED_CONCRETE;
        }
        if (pair(first, second, "junction_1", "habitation")) {
            return Material.LIME_CONCRETE;
        }
        if (pair(first, second, "habitation", "cargo")) {
            return Material.ORANGE_CONCRETE;
        }
        if (pair(first, second, "junction_1", "medical")) {
            return Material.PINK_CONCRETE;
        }
        if (pair(first, second, "medical", "research")) {
            return Material.PURPLE_CONCRETE;
        }
        return null;
    }

    private static boolean pair(
            String first,
            String second,
            String a,
            String b
    ) {
        return (first.equals(a) && second.equals(b))
                || (first.equals(b) && second.equals(a));
    }

    private static void standingSign(
            World world,
            int x,
            int y,
            int z,
            String first,
            String second
    ) {
        var block = world.getBlockAt(x, y, z);
        block.setType(Material.OAK_SIGN, false);

        if (block.getBlockData() instanceof Rotatable rotatable) {
            rotatable.setRotation(org.bukkit.block.BlockFace.SOUTH);
            block.setBlockData(rotatable, false);
        }

        if (block.getState() instanceof Sign sign) {
            sign.setLine(0, first);
            sign.setLine(1, second);
            sign.update(true, false);
        }
    }

    private void renderFacilityTerminal(
            World world,
            PhysicalTilePlacement placement,
            FacilityId facilityId
    ) {
        Location terminal = new Location(
                world,
                placement.minX() + 2,
                FLOOR_Y + 1,
                placement.minZ() + placement.size() - 3
        );

        terminal.getBlock().setType(Material.LODESTONE, false);
        terminals.register(terminal, facilityId);
    }

    private static Doorway doorwayToward(
            World world,
            PhysicalTilePlacement from,
            PhysicalTilePlacement toward
    ) {
        Location fromCenter = from.center(world);
        Location targetCenter = toward.center(world);
        double dx = targetCenter.getX() - fromCenter.getX();
        double dz = targetCenter.getZ() - fromCenter.getZ();

        int centerX = fromCenter.getBlockX();
        int centerZ = fromCenter.getBlockZ();
        int maxX = from.minX() + from.size() - 1;
        int maxZ = from.minZ() + from.size() - 1;

        if (Math.abs(dx) >= Math.abs(dz)) {
            if (dx >= 0) {
                Location threshold = new Location(world, maxX, FLOOR_Y + 1, centerZ);
                return new Doorway(
                        threshold,
                        new PhysicalDeckCell(maxX + 1, FLOOR_Y, centerZ)
                );
            }

            Location threshold = new Location(world, from.minX(), FLOOR_Y + 1, centerZ);
            return new Doorway(
                    threshold,
                    new PhysicalDeckCell(from.minX() - 1, FLOOR_Y, centerZ)
            );
        }

        if (dz >= 0) {
            Location threshold = new Location(world, centerX, FLOOR_Y + 1, maxZ);
            return new Doorway(
                    threshold,
                    new PhysicalDeckCell(centerX, FLOOR_Y, maxZ + 1)
            );
        }

        Location threshold = new Location(world, centerX, FLOOR_Y + 1, from.minZ());
        return new Doorway(
                threshold,
                new PhysicalDeckCell(centerX, FLOOR_Y, from.minZ() - 1)
        );
    }

    private void renderCorridorNetwork(
            World world,
            Set<PhysicalDeckCell> corridorCells,
            Set<PhysicalDeckCell> centerLines,
            Map<PhysicalDeckCell, Material> navigationLines,
            Collection<PhysicalTilePlacement> placements
    ) {
        for (PhysicalDeckCell cell : corridorCells) {
            if (insideAnyRoom(cell.x(), cell.z(), placements)) {
                continue;
            }

            Material floor = navigationLines.getOrDefault(
                    cell,
                    centerLines.contains(cell)
                            ? Material.WHITE_CONCRETE
                            : Material.SMOOTH_STONE
            );

            world.getBlockAt(cell.x(), FLOOR_Y, cell.z()).setType(floor, false);
            world.getBlockAt(cell.x(), FLOOR_Y + ROOM_HEIGHT, cell.z())
                    .setType(Material.IRON_BLOCK, false);

            for (int y = FLOOR_Y + 1; y < FLOOR_Y + ROOM_HEIGHT; y++) {
                world.getBlockAt(cell.x(), y, cell.z()).setType(Material.AIR, false);
            }

            if (centerLines.contains(cell)
                    && Math.floorMod(cell.x() * 31 + cell.z() * 17, 7) == 0) {
                registerLightPoint(
                        cell.x(),
                        FLOOR_Y + ROOM_HEIGHT - 1,
                        FLOOR_Y + ROOM_HEIGHT,
                        cell.z()
                );
            }
        }

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (PhysicalDeckCell cell : corridorCells) {
            for (int[] direction : directions) {
                int x = cell.x() + direction[0];
                int z = cell.z() + direction[1];
                PhysicalDeckCell neighbor = new PhysicalDeckCell(x, FLOOR_Y, z);

                if (corridorCells.contains(neighbor)
                        || insideAnyRoom(x, z, placements)) {
                    continue;
                }

                for (int y = FLOOR_Y + 1; y < FLOOR_Y + ROOM_HEIGHT; y++) {
                    world.getBlockAt(x, y, z).setType(
                            y == FLOOR_Y + 2
                                    ? Material.IRON_BLOCK
                                    : Material.LIGHT_GRAY_CONCRETE,
                            false
                    );
                }
            }
        }
    }

    private static List<PhysicalDeckCell> routeCorridor(
            PhysicalDeckCell start,
            PhysicalDeckCell end,
            Collection<PhysicalTilePlacement> placements,
            long seed
    ) {
        List<PhysicalDeckCell> xFirst = orthogonalPath(start, end, true);
        List<PhysicalDeckCell> zFirst = orthogonalPath(start, end, false);

        int xBlocked = blockedCells(xFirst, placements, start, end);
        int zBlocked = blockedCells(zFirst, placements, start, end);

        if (xBlocked == 0 && zBlocked == 0) {
            return (seed & 1L) == 0L ? xFirst : zFirst;
        }
        if (xBlocked == 0) return xFirst;
        if (zBlocked == 0) return zFirst;

        return breadthFirstPath(start, end, placements);
    }

    private static List<PhysicalDeckCell> orthogonalPath(
            PhysicalDeckCell start,
            PhysicalDeckCell end,
            boolean xFirst
    ) {
        ArrayList<PhysicalDeckCell> path = new ArrayList<>();
        int x = start.x();
        int z = start.z();
        path.add(new PhysicalDeckCell(x, FLOOR_Y, z));

        if (xFirst) {
            while (x != end.x()) {
                x += Integer.compare(end.x(), x);
                path.add(new PhysicalDeckCell(x, FLOOR_Y, z));
            }
            while (z != end.z()) {
                z += Integer.compare(end.z(), z);
                path.add(new PhysicalDeckCell(x, FLOOR_Y, z));
            }
        } else {
            while (z != end.z()) {
                z += Integer.compare(end.z(), z);
                path.add(new PhysicalDeckCell(x, FLOOR_Y, z));
            }
            while (x != end.x()) {
                x += Integer.compare(end.x(), x);
                path.add(new PhysicalDeckCell(x, FLOOR_Y, z));
            }
        }

        return path;
    }

    private static int blockedCells(
            List<PhysicalDeckCell> path,
            Collection<PhysicalTilePlacement> placements,
            PhysicalDeckCell start,
            PhysicalDeckCell end
    ) {
        int blocked = 0;
        for (PhysicalDeckCell cell : path) {
            if (cell.equals(start) || cell.equals(end)) continue;
            if (insideAnyRoom(cell.x(), cell.z(), placements)) {
                blocked++;
            }
        }
        return blocked;
    }

    private static List<PhysicalDeckCell> breadthFirstPath(
            PhysicalDeckCell start,
            PhysicalDeckCell end,
            Collection<PhysicalTilePlacement> placements
    ) {
        int minX = CompactSingleDeckLayoutPlanner.SHIP_MIN_X - 3;
        int maxX = CompactSingleDeckLayoutPlanner.SHIP_MAX_X + 3;
        int minZ = CompactSingleDeckLayoutPlanner.SHIP_MIN_Z - 3;
        int maxZ = CompactSingleDeckLayoutPlanner.SHIP_MAX_Z + 3;

        ArrayDeque<PhysicalDeckCell> queue = new ArrayDeque<>();
        Map<PhysicalDeckCell, PhysicalDeckCell> previous = new HashMap<>();

        queue.add(start);
        previous.put(start, null);

        int[][] directions = {{1, 0}, {0, 1}, {0, -1}, {-1, 0}};

        while (!queue.isEmpty()) {
            PhysicalDeckCell current = queue.removeFirst();
            if (current.equals(end)) break;

            for (int[] direction : directions) {
                PhysicalDeckCell next = new PhysicalDeckCell(
                        current.x() + direction[0],
                        FLOOR_Y,
                        current.z() + direction[1]
                );

                if (next.x() < minX
                        || next.x() > maxX
                        || next.z() < minZ
                        || next.z() > maxZ) {
                    continue;
                }
                if (previous.containsKey(next)) continue;
                if (!next.equals(end)
                        && !next.equals(start)
                        && insideAnyRoom(next.x(), next.z(), placements)) {
                    continue;
                }

                previous.put(next, current);
                queue.addLast(next);
            }
        }

        if (!previous.containsKey(end)) {
            return orthogonalPath(start, end, true);
        }

        ArrayList<PhysicalDeckCell> reversed = new ArrayList<>();
        for (PhysicalDeckCell current = end; current != null; current = previous.get(current)) {
            reversed.add(current);
        }
        Collections.reverse(reversed);
        return reversed;
    }

    private static Set<PhysicalDeckCell> expandCorridor(
            Collection<PhysicalDeckCell> centerPath
    ) {
        LinkedHashSet<PhysicalDeckCell> expanded = new LinkedHashSet<>();
        for (PhysicalDeckCell center : centerPath) {
            for (int dx = -CORRIDOR_HALF_WIDTH; dx <= CORRIDOR_HALF_WIDTH; dx++) {
                for (int dz = -CORRIDOR_HALF_WIDTH; dz <= CORRIDOR_HALF_WIDTH; dz++) {
                    expanded.add(new PhysicalDeckCell(
                            center.x() + dx,
                            FLOOR_Y,
                            center.z() + dz
                    ));
                }
            }
        }
        return expanded;
    }

    private static boolean insideAnyRoom(
            int x,
            int z,
            Collection<PhysicalTilePlacement> placements
    ) {
        for (PhysicalTilePlacement placement : placements) {
            if (x >= placement.minX()
                    && x < placement.minX() + placement.size()
                    && z >= placement.minZ()
                    && z < placement.minZ() + placement.size()) {
                return true;
            }
        }
        return false;
    }

    private static void carveDoorway(Location threshold) {
        if (threshold.getWorld() == null) return;

        int x = threshold.getBlockX();
        int z = threshold.getBlockZ();
        int baseY = threshold.getBlockY();

        for (int y = baseY; y <= baseY + 1; y++) {
            threshold.getWorld().getBlockAt(x, y, z).setType(Material.AIR, false);
        }
    }

    private static void renderConnectionThreshold(Location threshold) {
        World world = threshold.getWorld();
        if (world == null) return;

        world.getBlockAt(
                threshold.getBlockX(),
                threshold.getBlockY() - 1,
                threshold.getBlockZ()
        ).setType(Material.GOLD_BLOCK, false);
        threshold.getBlock().setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
    }

    private static Location markerLocation(World world, PhysicalDeckCell cell) {
        return new Location(world, cell.x(), FLOOR_Y + 1, cell.z());
    }

    private void registerLightPoint(
            int x,
            int lightY,
            int fixtureY,
            int z
    ) {
        lightPoints.add(new LightPoint(x, lightY, fixtureY, z));
    }

    private static FacilityId coreFacility(TileId tileId) {
        return switch (tileId.value()) {
            case "bridge", "engineering", "medical", "research", "cargo", "habitation" ->
                    new FacilityId(tileId.value());
            default -> null;
        };
    }

    private static String categoryName(TileCategory category) {
        return switch (category) {
            case CORE -> "핵심 시설";
            case CORRIDOR -> "통로";
            case JUNCTION -> "중앙 허브";
            case AIRLOCK -> "에어록";
            case AUXILIARY -> "보조 시설";
        };
    }

    private static Material roomFloorAccent(TileDefinition definition) {
        return switch (definition.id().value()) {
            case "bridge" -> Material.LIGHT_BLUE_CONCRETE;
            case "engineering" -> Material.RED_CONCRETE;
            case "medical" -> Material.PINK_CONCRETE;
            case "research" -> Material.PURPLE_CONCRETE;
            case "cargo" -> Material.ORANGE_CONCRETE;
            case "habitation" -> Material.LIME_CONCRETE;
            default -> switch (definition.category()) {
                case JUNCTION -> Material.YELLOW_CONCRETE;
                case AIRLOCK -> Material.CYAN_CONCRETE;
                case AUXILIARY -> Material.GREEN_CONCRETE;
                default -> Material.WHITE_CONCRETE;
            };
        };
    }

    private static Material accent(TileCategory category) {
        return switch (category) {
            case CORE -> Material.LIGHT_BLUE_CONCRETE;
            case CORRIDOR -> Material.WHITE_CONCRETE;
            case JUNCTION -> Material.YELLOW_CONCRETE;
            case AIRLOCK -> Material.CYAN_CONCRETE;
            case AUXILIARY -> Material.GREEN_CONCRETE;
        };
    }

    private record RouteDirection(TileId from, TileId to) {
        private RouteDirection {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
        }
    }

    private record Doorway(
            Location threshold,
            PhysicalDeckCell outside
    ) {
    }

    private record RenderedConnection(
            GeneratedConnection connection,
            Doorway firstDoor,
            Doorway secondDoor
    ) {
    }

    private record LightPoint(
            int x,
            int lightY,
            int fixtureY,
            int z
    ) {
    }

    private enum LightingStage {
        NORMAL(15, Material.WHITE_STAINED_GLASS),
        REDUCED(13, Material.YELLOW_STAINED_GLASS),
        EMERGENCY(10, Material.RED_STAINED_GLASS),
        BLACKOUT(4, Material.GRAY_STAINED_GLASS);

        private final int lightLevel;
        private final Material fixture;

        LightingStage(int lightLevel, Material fixture) {
            this.lightLevel = lightLevel;
            this.fixture = fixture;
        }

        private int lightLevel() {
            return lightLevel;
        }

        private Material fixture() {
            return fixture;
        }

        private static LightingStage fromPower(int power) {
            if (power >= 70) return NORMAL;
            if (power >= 40) return REDUCED;
            if (power >= 15) return EMERGENCY;
            return BLACKOUT;
        }
    }
}

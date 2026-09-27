package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.map.generation.CohesiveDeckMapGenerator;
import com.hushkisses.spacesurvival.map.generation.GeneratedConnection;
import com.hushkisses.spacesurvival.map.generation.GeneratedMap;
import com.hushkisses.spacesurvival.map.generation.MapGenerationConstraints;
import com.hushkisses.spacesurvival.map.tile.DefaultTileCatalog;
import com.hushkisses.spacesurvival.map.tile.TileCategory;
import com.hushkisses.spacesurvival.map.tile.TileDefinition;
import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.config.MatchSetupConfig;
import com.hushkisses.spacesurvival.paper.facility.FacilityTerminalRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PaperShipWorldService {

    public static final String WORLD_NAME = "space_ship_compact_v3";

    private static final int ROOM_HEIGHT = 5;
    private static final int CORRIDOR_HALF_WIDTH = 1;
    private static final int CLEAR_MARGIN = 8;

    private final CohesiveDeckMapGenerator generator = new CohesiveDeckMapGenerator();
    private final CohesiveShipLayoutPlanner layoutPlanner = new CohesiveShipLayoutPlanner();
    private final ShipModuleStructureLoader structureLoader;
    private final FacilityTerminalRegistry terminals;
    private final PhysicalConnectionController connections;

    private PhysicalShipSnapshot activeSnapshot;
    private Map<TileId, StructurePlacementResult> structurePlacements = Map.of();
    private final Map<RouteDirection, TextDisplay> portalLabels = new LinkedHashMap<>();
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
                layoutPlanner.plan(generated.tileIds(), seed);

        World world = resolveWorld();
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(18000L);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        clearNavigationDisplays(world);
        clearBuildArea(world);

        terminals.clear();
        connections.reset();
        portalLabels.clear();
        lightPoints.clear();
        highlightedRouteTarget = null;
        lightingStage = null;

        LinkedHashMap<TileId, StructurePlacementResult> placementResults =
                new LinkedHashMap<>();

        for (TileId tileId : generated.tileIds()) {
            PhysicalTilePlacement placement = placements.get(tileId);

            // MAP-V3 deliberately does not load the legacy 15x15 NBT modules.
            // Spatial scale comes first; new structures can be authored after this
            // compact footprint is accepted in playtest.
            placementResults.put(
                    tileId,
                    new StructurePlacementResult(
                            false,
                            "MAP-V3",
                            "compact procedural room"
                    )
            );
            renderModule(world, placement, definitions.get(tileId));
            renderRoomLabel(world, placement, definitions.get(tileId));

            FacilityId facilityId = coreFacility(tileId);
            if (facilityId != null) {
                renderFacilityTerminal(world, placement, facilityId);
            }
        }

        LinkedHashSet<PhysicalDeckCell> corridorCells = new LinkedHashSet<>();
        LinkedHashSet<PhysicalDeckCell> centerLines = new LinkedHashSet<>();
        ArrayList<RenderedConnection> renderedConnections = new ArrayList<>();

        int connectionIndex = 0;
        for (GeneratedConnection connection : generated.connections()) {
            PhysicalTilePlacement first = placements.get(connection.first().tileId());
            PhysicalTilePlacement second = placements.get(connection.second().tileId());

            if (first.floorY() != second.floorY()) {
                VerticalConnector vertical = renderVerticalConnector(world, first, second);
                corridorCells.addAll(vertical.corridorCells());
                renderedConnections.add(new RenderedConnection(
                        connection,
                        vertical.firstThreshold(),
                        vertical.secondThreshold()
                ));
                connectionIndex++;
                continue;
            }

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
            renderedConnections.add(new RenderedConnection(
                    connection,
                    firstDoor.threshold(),
                    secondDoor.threshold()
            ));
            connectionIndex++;
        }

        renderCorridorNetwork(
                world,
                corridorCells,
                centerLines,
                placements.values()
        );

        LinkedHashMap<PortalBlockKey, Location> legacyPortals = new LinkedHashMap<>();
        int connectionId = 0;

        for (RenderedConnection rendered : renderedConnections) {
            GeneratedConnection connection = rendered.connection();
            Location firstThreshold = rendered.firstThreshold();
            Location secondThreshold = rendered.secondThreshold();

            carveDoorway(firstThreshold);
            carveDoorway(secondThreshold);
            renderConnectionThreshold(firstThreshold);
            renderConnectionThreshold(secondThreshold);

            TextDisplay firstLabel = renderPortalDestinationLabel(
                    firstThreshold,
                    definitions.get(connection.second().tileId())
            );
            TextDisplay secondLabel = renderPortalDestinationLabel(
                    secondThreshold,
                    definitions.get(connection.first().tileId())
            );

            if (firstLabel != null) {
                portalLabels.put(
                        new RouteDirection(
                                connection.first().tileId(),
                                connection.second().tileId()
                        ),
                        firstLabel
                );
            }
            if (secondLabel != null) {
                portalLabels.put(
                        new RouteDirection(
                                connection.second().tileId(),
                                connection.first().tileId()
                        ),
                        secondLabel
                );
            }

            connections.register(
                    connectionId++,
                    connection,
                    firstThreshold,
                    secondThreshold
            );
        }

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
        if (ship == null) {
            return;
        }

        if (!ship.generatedMap().tileIds().contains(target)) {
            clearPriorityRoute();
            return;
        }

        if (target.equals(highlightedRouteTarget)) {
            return;
        }

        highlightedRouteTarget = target;

        for (Map.Entry<RouteDirection, TextDisplay> entry : portalLabels.entrySet()) {
            RouteDirection direction = entry.getKey();
            TextDisplay display = entry.getValue();

            if (!display.isValid()) {
                continue;
            }

            int fromDistance = ship.generatedMap().distance(direction.from(), target);
            int toDistance = ship.generatedMap().distance(direction.to(), target);
            boolean towardTarget = !direction.from().equals(target)
                    && toDistance < fromDistance;

            renderPortalDestinationText(
                    display,
                    ship.definitions().get(direction.to()),
                    towardTarget
            );
        }
    }

    public void clearPriorityRoute() {
        PhysicalShipSnapshot ship = activeSnapshot;
        highlightedRouteTarget = null;

        if (ship == null) {
            return;
        }

        for (Map.Entry<RouteDirection, TextDisplay> entry : portalLabels.entrySet()) {
            TextDisplay display = entry.getValue();
            if (!display.isValid()) {
                continue;
            }
            renderPortalDestinationText(
                    display,
                    ship.definitions().get(entry.getKey().to()),
                    false
            );
        }
    }

    public String deckName(Location location) {
        return location.getBlockY() >= CohesiveShipLayoutPlanner.UPPER_FLOOR_Y
                ? "상부 데크"
                : "하부 데크";
    }

    private static void clearNavigationDisplays(World world) {
        world.getEntitiesByClass(TextDisplay.class).stream()
                .filter(entity -> entity.getScoreboardTags().contains("spacesurvival_nav"))
                .forEach(TextDisplay::remove);
    }

    private static void renderRoomLabel(
            World world,
            PhysicalTilePlacement placement,
            TileDefinition definition
    ) {
        if (definition == null) return;

        Location location = placement.center(world).add(0.0, 3.1, 0.0);
        TextDisplay display = world.spawn(location, TextDisplay.class);
        display.addScoreboardTag("spacesurvival_nav");
        display.setBillboard(Display.Billboard.CENTER);

        String deck = placement.floorY() == CohesiveShipLayoutPlanner.UPPER_FLOOR_Y
                ? "상부"
                : "하부";
        String zone = zoneName(placement.minX());

        display.text(
                Component.text(deck + " " + zone + " · ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("◆ ", accentColor(definition.category())))
                        .append(Component.text(definition.displayName(), NamedTextColor.WHITE))
        );
    }

    private static TextDisplay renderPortalDestinationLabel(
            Location pad,
            TileDefinition destination
    ) {
        if (pad.getWorld() == null || destination == null) return null;

        Location labelLocation = pad.clone().add(0.0, 1.8, 0.0);
        TextDisplay display = pad.getWorld().spawn(labelLocation, TextDisplay.class);
        display.addScoreboardTag("spacesurvival_nav");
        display.addScoreboardTag("spacesurvival_portal_label");
        display.setBillboard(Display.Billboard.CENTER);
        renderPortalDestinationText(display, destination, false);
        return display;
    }

    private static void renderPortalDestinationText(
            TextDisplay display,
            TileDefinition destination,
            boolean priority
    ) {
        if (destination == null) return;

        if (priority) {
            display.text(
                    Component.text("◆ ", NamedTextColor.RED)
                            .append(Component.text(destination.displayName(), NamedTextColor.RED))
            );
            return;
        }

        display.text(
                Component.text("→ ", NamedTextColor.YELLOW)
                        .append(Component.text(destination.displayName(), NamedTextColor.WHITE))
        );
    }

    private static NamedTextColor accentColor(TileCategory category) {
        return switch (category) {
            case CORE -> NamedTextColor.AQUA;
            case CORRIDOR -> NamedTextColor.WHITE;
            case JUNCTION -> NamedTextColor.YELLOW;
            case AIRLOCK -> NamedTextColor.GOLD;
            case AUXILIARY -> NamedTextColor.GREEN;
        };
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

        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(18000L);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        return world;
    }

    private static void clearBuildArea(World world) {
        int minX = CohesiveShipLayoutPlanner.SHIP_MIN_X - 6;
        int maxX = CohesiveShipLayoutPlanner.SHIP_MAX_X + 6;
        int minZ = CohesiveShipLayoutPlanner.SHIP_MIN_Z - 6;
        int maxZ = CohesiveShipLayoutPlanner.SHIP_MAX_Z + 6;
        int minY = CohesiveShipLayoutPlanner.LOWER_FLOOR_Y - 1;
        int maxY = CohesiveShipLayoutPlanner.UPPER_FLOOR_Y + ROOM_HEIGHT + 2;

        // Fixed envelope: omitted optional rooms from an earlier seed cannot leave
        // stale blocks behind.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
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
        int floor = placement.floorY();

        Material accent = accent(definition.category());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.getBlockAt(x, floor, z).setType(Material.SMOOTH_STONE, false);
                world.getBlockAt(x, floor + ROOM_HEIGHT, z)
                        .setType(Material.POLISHED_DEEPSLATE, false);

                boolean wall = x == minX || x == maxX || z == minZ || z == maxZ;
                if (wall) {
                    for (int y = floor + 1; y < floor + ROOM_HEIGHT; y++) {
                        Material wallMaterial = y == floor + 2
                                ? accent
                                : Material.GRAY_CONCRETE;
                        world.getBlockAt(x, y, z).setType(wallMaterial, false);
                    }
                } else {
                    for (int y = floor + 1; y < floor + ROOM_HEIGHT; y++) {
                        world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    }
                }
            }
        }

        Location center = placement.center(world);
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        world.getBlockAt(cx, floor, cz).setType(zoneStripe(placement.minX()), false);
        registerLightPoint(cx, floor + ROOM_HEIGHT - 1, floor + ROOM_HEIGHT, cz);

        world.getBlockAt(minX + 2, floor + 1, minZ + 2)
                .setType(Material.IRON_BLOCK, false);
        world.getBlockAt(maxX - 2, floor + 1, maxZ - 2)
                .setType(Material.IRON_BLOCK, false);
    }

    private void registerRoomLight(PhysicalTilePlacement placement) {
        Location center = placement.center(
                activeSnapshot == null ? resolveWorld() : activeSnapshot.world()
        );
        registerLightPoint(
                center.getBlockX(),
                placement.floorY() + ROOM_HEIGHT - 1,
                placement.floorY() + ROOM_HEIGHT,
                center.getBlockZ()
        );
    }

    private void renderCorridorNetwork(
            World world,
            Set<PhysicalDeckCell> corridorCells,
            Set<PhysicalDeckCell> centerLines,
            Collection<PhysicalTilePlacement> placements
    ) {
        for (PhysicalDeckCell cell : corridorCells) {
            if (insideAnyRoom(cell.x(), cell.floorY(), cell.z(), placements)) {
                continue;
            }

            int floorY = cell.floorY();
            Material floor = centerLines.contains(cell)
                    ? zoneStripe(cell.x())
                    : Material.SMOOTH_STONE;

            world.getBlockAt(cell.x(), floorY, cell.z()).setType(floor, false);
            world.getBlockAt(cell.x(), floorY + ROOM_HEIGHT, cell.z())
                    .setType(Material.POLISHED_DEEPSLATE, false);

            for (int y = floorY + 1; y < floorY + ROOM_HEIGHT; y++) {
                world.getBlockAt(cell.x(), y, cell.z()).setType(Material.AIR, false);
            }

            if (centerLines.contains(cell)
                    && Math.floorMod(cell.x() * 31 + cell.z() * 17, 11) == 0) {
                registerLightPoint(
                        cell.x(),
                        floorY + ROOM_HEIGHT - 1,
                        floorY + ROOM_HEIGHT,
                        cell.z()
                );
            }
        }

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (PhysicalDeckCell cell : corridorCells) {
            int floorY = cell.floorY();

            for (int[] direction : directions) {
                int x = cell.x() + direction[0];
                int z = cell.z() + direction[1];
                PhysicalDeckCell neighbor = new PhysicalDeckCell(x, floorY, z);

                if (corridorCells.contains(neighbor)
                        || insideAnyRoom(x, floorY, z, placements)) {
                    continue;
                }

                for (int y = floorY + 1; y < floorY + ROOM_HEIGHT; y++) {
                    world.getBlockAt(x, y, z)
                            .setType(
                                    y == floorY + 2
                                            ? Material.IRON_BLOCK
                                            : Material.DEEPSLATE_TILES,
                                    false
                            );
                }
            }
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
                placement.floorY() + 1,
                placement.minZ() + 2
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
        int floorY = from.floorY();

        if (Math.abs(dx) >= Math.abs(dz)) {
            if (dx >= 0) {
                Location threshold = new Location(world, maxX, floorY + 1, centerZ);
                return new Doorway(
                        threshold,
                        new PhysicalDeckCell(maxX + 1, floorY, centerZ)
                );
            }

            Location threshold = new Location(world, from.minX(), floorY + 1, centerZ);
            return new Doorway(
                    threshold,
                    new PhysicalDeckCell(from.minX() - 1, floorY, centerZ)
            );
        }

        if (dz >= 0) {
            Location threshold = new Location(world, centerX, floorY + 1, maxZ);
            return new Doorway(
                    threshold,
                    new PhysicalDeckCell(centerX, floorY, maxZ + 1)
            );
        }

        Location threshold = new Location(world, centerX, floorY + 1, from.minZ());
        return new Doorway(
                threshold,
                new PhysicalDeckCell(centerX, floorY, from.minZ() - 1)
        );
    }

    private VerticalConnector renderVerticalConnector(
            World world,
            PhysicalTilePlacement first,
            PhysicalTilePlacement second
    ) {
        PhysicalTilePlacement lower = first.floorY() < second.floorY() ? first : second;
        PhysicalTilePlacement upper = first.floorY() < second.floorY() ? second : first;

        int centerZ = lower.minZ() + lower.size() / 2;
        int startX = lower.minX() + 2;
        int endX = startX + 7;

        LinkedHashSet<PhysicalDeckCell> cells = new LinkedHashSet<>();

        for (int i = 0; i < 8; i++) {
            int x = startX + i;
            int stepY = lower.floorY() + 1 + i;

            world.getBlockAt(x, stepY, centerZ)
                    .setType(Material.POLISHED_ANDESITE, false);

            for (int clearY = stepY + 1; clearY <= stepY + 2; clearY++) {
                world.getBlockAt(x, clearY, centerZ).setType(Material.AIR, false);
            }

            for (int side : new int[]{-1, 1}) {
                for (int wallY = stepY; wallY <= stepY + 2; wallY++) {
                    world.getBlockAt(x, wallY, centerZ + side)
                            .setType(Material.IRON_BARS, false);
                }
            }
        }

        Location lowerThreshold = new Location(
                world,
                startX - 1,
                lower.floorY() + 1,
                centerZ
        );
        Location upperThreshold = new Location(
                world,
                endX + 1,
                upper.floorY() + 1,
                centerZ
        );

        for (int x = startX - 1; x <= endX + 1; x++) {
            cells.add(new PhysicalDeckCell(x, lower.floorY(), centerZ));
            cells.add(new PhysicalDeckCell(x, upper.floorY(), centerZ));
        }

        registerLightPoint(
                startX + 2,
                lower.floorY() + ROOM_HEIGHT - 1,
                lower.floorY() + ROOM_HEIGHT,
                centerZ
        );
        registerLightPoint(
                endX - 1,
                upper.floorY() + ROOM_HEIGHT - 1,
                upper.floorY() + ROOM_HEIGHT,
                centerZ
        );

        return first == lower
                ? new VerticalConnector(lowerThreshold, upperThreshold, cells)
                : new VerticalConnector(upperThreshold, lowerThreshold, cells);
    }

    private static List<PhysicalDeckCell> routeCorridor(
            PhysicalDeckCell start,
            PhysicalDeckCell end,
            Collection<PhysicalTilePlacement> placements,
            long seed
    ) {
        if (start.floorY() != end.floorY()) {
            throw new IllegalArgumentException("Flat corridor endpoints must share a deck");
        }

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
        int floorY = start.floorY();
        path.add(new PhysicalDeckCell(x, floorY, z));

        if (xFirst) {
            while (x != end.x()) {
                x += Integer.compare(end.x(), x);
                path.add(new PhysicalDeckCell(x, floorY, z));
            }
            while (z != end.z()) {
                z += Integer.compare(end.z(), z);
                path.add(new PhysicalDeckCell(x, floorY, z));
            }
        } else {
            while (z != end.z()) {
                z += Integer.compare(end.z(), z);
                path.add(new PhysicalDeckCell(x, floorY, z));
            }
            while (x != end.x()) {
                x += Integer.compare(end.x(), x);
                path.add(new PhysicalDeckCell(x, floorY, z));
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
            if (insideAnyRoom(cell.x(), cell.floorY(), cell.z(), placements)) {
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
        Bounds bounds = bounds(placements, CLEAR_MARGIN - 2);
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
                        current.floorY(),
                        current.z() + direction[1]
                );

                if (next.x() < bounds.minX()
                        || next.x() > bounds.maxX()
                        || next.z() < bounds.minZ()
                        || next.z() > bounds.maxZ()) {
                    continue;
                }
                if (previous.containsKey(next)) continue;
                if (!next.equals(end)
                        && !next.equals(start)
                        && insideAnyRoom(
                                next.x(),
                                next.floorY(),
                                next.z(),
                                placements
                        )) {
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
                            center.floorY(),
                            center.z() + dz
                    ));
                }
            }
        }
        return expanded;
    }

    private static boolean insideAnyRoom(
            int x,
            int floorY,
            int z,
            Collection<PhysicalTilePlacement> placements
    ) {
        for (PhysicalTilePlacement placement : placements) {
            if (placement.floorY() != floorY) {
                continue;
            }
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

        int x = threshold.getBlockX();
        int y = threshold.getBlockY();
        int z = threshold.getBlockZ();

        world.getBlockAt(x, y - 1, z).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(x, y, z).setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
    }

    private void registerLightPoint(
            int x,
            int lightY,
            int fixtureY,
            int z
    ) {
        lightPoints.add(new LightPoint(x, lightY, fixtureY, z));
    }

    private static Bounds bounds(
            Collection<PhysicalTilePlacement> placements,
            int margin
    ) {
        int minX = placements.stream().mapToInt(PhysicalTilePlacement::minX).min().orElse(0);
        int minZ = placements.stream().mapToInt(PhysicalTilePlacement::minZ).min().orElse(0);
        int maxX = placements.stream()
                .mapToInt(p -> p.minX() + p.size() - 1)
                .max().orElse(0);
        int maxZ = placements.stream()
                .mapToInt(p -> p.minZ() + p.size() - 1)
                .max().orElse(0);

        return new Bounds(
                minX - margin,
                maxX + margin,
                minZ - margin,
                maxZ + margin
        );
    }

    private static String zoneName(int x) {
        if (x < 30) return "선수";
        if (x < 70) return "중앙";
        return "후방";
    }

    private static Material zoneStripe(int x) {
        if (x < 30) return Material.LIGHT_BLUE_CONCRETE;
        if (x < 70) return Material.WHITE_CONCRETE;
        return Material.RED_CONCRETE;
    }

    private static FacilityId coreFacility(TileId tileId) {
        return switch (tileId.value()) {
            case "bridge", "engineering", "medical", "research", "cargo", "habitation" ->
                    new FacilityId(tileId.value());
            default -> null;
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
            Location firstThreshold,
            Location secondThreshold
    ) {
    }

    private record VerticalConnector(
            Location firstThreshold,
            Location secondThreshold,
            Set<PhysicalDeckCell> corridorCells
    ) {
    }

    private record Bounds(
            int minX,
            int maxX,
            int minZ,
            int maxZ
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
        REDUCED(11, Material.YELLOW_STAINED_GLASS),
        EMERGENCY(7, Material.RED_STAINED_GLASS),
        BLACKOUT(2, Material.GRAY_STAINED_GLASS);

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

    private static Material accent(TileCategory category) {
        return switch (category) {
            case CORE -> Material.LIGHT_BLUE_CONCRETE;
            case CORRIDOR -> Material.WHITE_CONCRETE;
            case JUNCTION -> Material.YELLOW_CONCRETE;
            case AIRLOCK -> Material.ORANGE_CONCRETE;
            case AUXILIARY -> Material.LIME_CONCRETE;
        };
    }
}

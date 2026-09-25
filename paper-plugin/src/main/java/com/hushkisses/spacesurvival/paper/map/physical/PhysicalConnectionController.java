package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.connection.ConnectionAccessContext;
import com.hushkisses.spacesurvival.map.connection.ConnectionAccessDecision;
import com.hushkisses.spacesurvival.map.connection.ConnectionAccessPolicy;
import com.hushkisses.spacesurvival.map.connection.ConnectionState;
import com.hushkisses.spacesurvival.map.generation.GeneratedConnection;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;

public final class PhysicalConnectionController {

    private final ConnectionAccessPolicy accessPolicy = new ConnectionAccessPolicy();
    private final Map<Integer, RuntimeConnection> connections = new LinkedHashMap<>();
    private final Map<PortalBlockKey, Integer> pads = new LinkedHashMap<>();

    public void reset() {
        connections.clear();
        pads.clear();
    }

    public void register(
            int id,
            GeneratedConnection connection,
            Location firstPad,
            Location secondPad
    ) {
        if (connections.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate physical connection id: " + id);
        }

        RuntimeConnection runtime = new RuntimeConnection(
                id,
                Objects.requireNonNull(connection, "connection"),
                firstPad.clone(),
                secondPad.clone(),
                ConnectionState.OPEN
        );

        connections.put(id, runtime);
        pads.put(PortalBlockKey.of(firstPad), id);
        pads.put(PortalBlockKey.of(secondPad), id);
        render(runtime);
    }

    public Optional<ConnectionAccessDecision> accessAt(
            Location portalPad,
            boolean powerAvailable,
            boolean keycardAvailable
    ) {
        Integer id = pads.get(PortalBlockKey.of(portalPad));
        if (id == null) return Optional.empty();

        RuntimeConnection runtime = connections.get(id);
        return Optional.of(accessPolicy.evaluate(
                runtime.state,
                new ConnectionAccessContext(powerAvailable, keycardAvailable)
        ));
    }

    public OptionalInt connectionIdAt(Location portalPad) {
        Integer id = pads.get(PortalBlockKey.of(portalPad));
        return id == null ? OptionalInt.empty() : OptionalInt.of(id);
    }

    public boolean setState(int id, ConnectionState state) {
        RuntimeConnection runtime = connections.get(id);
        if (runtime == null) return false;
        runtime.state = Objects.requireNonNull(state, "state");
        render(runtime);
        return true;
    }

    public OptionalInt setRandomOpenConnection(
            ConnectionState state,
            Random random
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(random, "random");

        List<RuntimeConnection> candidates = connections.values().stream()
                .filter(connection -> connection.state == ConnectionState.OPEN)
                .toList();

        if (candidates.isEmpty()) return OptionalInt.empty();

        RuntimeConnection selected = candidates.get(random.nextInt(candidates.size()));
        selected.state = state;
        render(selected);
        return OptionalInt.of(selected.id);
    }

    public int openAllFaultedConnections() {
        int changed = 0;
        for (RuntimeConnection runtime : connections.values()) {
            if (runtime.state != ConnectionState.OPEN) {
                runtime.state = ConnectionState.OPEN;
                render(runtime);
                changed++;
            }
        }
        return changed;
    }

    public List<PhysicalConnectionSnapshot> snapshots() {
        return connections.values().stream()
                .map(runtime -> new PhysicalConnectionSnapshot(
                        runtime.id,
                        runtime.connection,
                        runtime.state
                ))
                .toList();
    }

    public int size() {
        return connections.size();
    }

    private static void render(RuntimeConnection runtime) {
        Material indicator = switch (runtime.state) {
            case OPEN -> Material.GOLD_BLOCK;
            case POWER_REQUIRED, KEYCARD_REQUIRED -> Material.YELLOW_CONCRETE;
            case LOCKED, DISABLED -> Material.RED_CONCRETE;
        };

        setIndicator(runtime.firstPad, indicator);
        setIndicator(runtime.secondPad, indicator);
    }

    private static void setIndicator(Location pad, Material material) {
        if (pad.getWorld() == null) return;
        pad.getWorld().getBlockAt(
                pad.getBlockX(),
                pad.getBlockY() - 1,
                pad.getBlockZ()
        ).setType(material, false);
    }

    private static final class RuntimeConnection {
        private final int id;
        private final GeneratedConnection connection;
        private final Location firstPad;
        private final Location secondPad;
        private ConnectionState state;

        private RuntimeConnection(
                int id,
                GeneratedConnection connection,
                Location firstPad,
                Location secondPad,
                ConnectionState state
        ) {
            this.id = id;
            this.connection = connection;
            this.firstPad = firstPad;
            this.secondPad = secondPad;
            this.state = state;
        }
    }
}

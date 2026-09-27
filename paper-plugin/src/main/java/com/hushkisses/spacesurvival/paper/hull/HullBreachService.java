package com.hushkisses.spacesurvival.paper.hull;

import com.hushkisses.spacesurvival.integration.itemsadder.ItemsAdderBridge;
import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.map.physical.PhysicalShipSnapshot;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.resource.ResourceType;
import com.hushkisses.spacesurvival.ship.ShipMetric;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class HullBreachService implements Listener {

    private static final int BREACH_COUNT = 2;
    private static final int HULL_RESTORE_PER_BREACH = 8;
    private static final String ENTITY_TAG = "spacesurvival_hull_breach";

    private final SpaceSurvivalPlugin plugin;
    private final ItemsAdderBridge itemsAdder;
    private final Map<UUID, Breach> byInteraction = new LinkedHashMap<>();

    public HullBreachService(
            SpaceSurvivalPlugin plugin,
            ItemsAdderBridge itemsAdder
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.itemsAdder = Objects.requireNonNull(itemsAdder, "itemsAdder");
    }

    public void populate(PhysicalShipSnapshot ship, long seed) {
        Objects.requireNonNull(ship, "ship");
        clear(ship);

        ArrayList<TileId> candidates = candidateTiles(ship);

        Collections.shuffle(candidates, new Random(seed ^ 0x48B3A2D5L));
        int count = Math.min(BREACH_COUNT, candidates.size());

        for (int i = 0; i < count; i++) {
            spawnBreach(ship, candidates.get(i), i + 1);
        }
    }


    public void ensureRepairTarget(PhysicalShipSnapshot ship) {
        Objects.requireNonNull(ship, "ship");
        if (unresolvedCount() > 0) {
            return;
        }

        ArrayList<TileId> candidates = candidateTiles(ship);
        if (candidates.isEmpty()) {
            return;
        }

        Set<TileId> previouslyUsed = new HashSet<>();
        for (Breach breach : byInteraction.values()) {
            previouslyUsed.add(breach.tileId);
        }

        List<TileId> fresh = candidates.stream()
                .filter(tileId -> !previouslyUsed.contains(tileId))
                .toList();
        List<TileId> pool = fresh.isEmpty() ? candidates : fresh;

        Random random = new Random(
                ship.seed()
                        ^ 0x17C43B21L
                        ^ ((long) byInteraction.size() * 31L)
        );
        TileId selected = pool.get(random.nextInt(pool.size()));
        spawnBreach(ship, selected, byInteraction.size() + 1);

        plugin.getServer().broadcastMessage(
                "§c[선체 경보] §f추가 균열이 감지되었습니다: §e"
                        + ship.tileDisplayName(selected)
        );
    }

    private ArrayList<TileId> candidateTiles(PhysicalShipSnapshot ship) {
        ArrayList<TileId> candidates = new ArrayList<>();
        for (TileId tileId : ship.generatedMap().tileIds()) {
            String id = tileId.value();
            if (id.equals("bridge")
                    || id.equals("engineering")
                    || id.equals("cargo")
                    || id.equals("habitation")) {
                continue;
            }
            candidates.add(tileId);
        }
        return candidates;
    }

    public Optional<TileId> primaryUnrepairedTile() {
        return byInteraction.values().stream()
                .filter(breach -> !breach.repaired)
                .map(breach -> breach.tileId)
                .findFirst();
    }

    public int unresolvedCount() {
        return (int) byInteraction.values().stream()
                .filter(breach -> !breach.repaired)
                .count();
    }

    public void clear(PhysicalShipSnapshot ship) {
        if (ship != null) {
            ship.world().getEntities().stream()
                    .filter(entity -> entity.getScoreboardTags().contains(ENTITY_TAG))
                    .forEach(Entity::remove);
        }
        byInteraction.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        Breach breach = byInteraction.get(event.getRightClicked().getUniqueId());
        if (breach == null || breach.repaired) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        PlayerId playerId = PlayerId.of(player.getUniqueId());

        if (plugin.deathService().isDead(playerId)) {
            player.sendMessage(
                    plugin.infectedPlayerService().isInfectedForm(playerId)
                            ? "§4[감염체] §f감염체 상태에서는 선체를 수리할 수 없습니다."
                            : "§7[사망 상태] §f사망 상태에서는 선체를 수리할 수 없습니다."
            );
            return;
        }

        if (!plugin.resourcePhysicalItemService().remove(
                player,
                ResourceType.REPAIR_PARTS,
                1
        )) {
            player.sendMessage("§c[선체 균열] §f현장 수리에는 소지 중인 수리 부품 1개가 필요합니다.");
            player.sendActionBar(Component.text(
                    "수리 부품이 없습니다 · 화물실에서 현장 수리 부품을 수령하십시오"
            ));
            return;
        }

        breach.repaired = true;
        removeEntity(breach.interactionId);
        removeEntity(breach.itemDisplayId);
        removeEntity(breach.textDisplayId);

        int nextHull = Math.min(
                100,
                plugin.shipState().hull() + HULL_RESTORE_PER_BREACH
        );
        plugin.shipState().set(ShipMetric.HULL, nextHull);

        String roomName = plugin.shipWorldService()
                .activeSnapshot()
                .map(snapshot -> snapshot.tileDisplayName(breach.tileId))
                .orElse(breach.tileId.value());

        plugin.getServer().broadcastMessage(
                "§a[선체] §f" + roomName
                        + " 균열 봉합 완료 · 선체 "
                        + nextHull
                        + "%"
        );
        player.playSound(
                player.getLocation(),
                Sound.BLOCK_ANVIL_USE,
                0.7f,
                1.25f
        );

        plugin.telemetryService().increment("hull.breach.repaired");
        plugin.telemetryService().event(
                "hull_repair",
                breach.tileId.value() + ":" + nextHull
        );
    }

    private void spawnBreach(
            PhysicalShipSnapshot ship,
            TileId tileId,
            int ordinal
    ) {
        var placement = ship.placements().get(tileId);
        if (placement == null) return;

        Location base = placement.center(ship.world()).add(0.0, 1.15, 0.0);

        ItemStack visual = itemsAdder.createItem("spacesurvival:hull_breach")
                .orElseGet(() -> new ItemStack(Material.CRACKED_STONE_BRICKS));

        ItemDisplay itemDisplay = ship.world().spawn(base, ItemDisplay.class);
        itemDisplay.addScoreboardTag(ENTITY_TAG);
        itemDisplay.setBillboard(Display.Billboard.CENTER);
        itemDisplay.setItemStack(visual);

        TextDisplay textDisplay = ship.world().spawn(
                base.clone().add(0.0, 1.15, 0.0),
                TextDisplay.class
        );
        textDisplay.addScoreboardTag(ENTITY_TAG);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.text(
                Component.text("⚠ 선체 균열 #" + ordinal, NamedTextColor.RED)
                        .append(Component.newline())
                        .append(Component.text(
                                "수리 부품 1개 · 우클릭",
                                NamedTextColor.YELLOW
                        ))
        );

        Interaction interaction = ship.world().spawn(base, Interaction.class);
        interaction.addScoreboardTag(ENTITY_TAG);
        interaction.setInteractionWidth(1.4f);
        interaction.setInteractionHeight(1.8f);
        interaction.setResponsive(true);

        Breach breach = new Breach(
                tileId,
                interaction.getUniqueId(),
                itemDisplay.getUniqueId(),
                textDisplay.getUniqueId()
        );
        byInteraction.put(interaction.getUniqueId(), breach);
    }

    private void removeEntity(UUID id) {
        Entity entity = plugin.getServer().getEntity(id);
        if (entity != null) {
            entity.remove();
        }
    }

    private static final class Breach {
        private final TileId tileId;
        private final UUID interactionId;
        private final UUID itemDisplayId;
        private final UUID textDisplayId;
        private boolean repaired;

        private Breach(
                TileId tileId,
                UUID interactionId,
                UUID itemDisplayId,
                UUID textDisplayId
        ) {
            this.tileId = tileId;
            this.interactionId = interactionId;
            this.itemDisplayId = itemDisplayId;
            this.textDisplayId = textDisplayId;
        }
    }
}

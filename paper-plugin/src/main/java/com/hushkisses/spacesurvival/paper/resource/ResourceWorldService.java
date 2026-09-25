package com.hushkisses.spacesurvival.paper.resource;

import com.hushkisses.spacesurvival.map.tile.TileCategory;
import com.hushkisses.spacesurvival.paper.map.physical.PhysicalShipSnapshot;
import com.hushkisses.spacesurvival.resource.ResourceType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class ResourceWorldService {

    private final ResourcePhysicalItemService items;

    public ResourceWorldService(ResourcePhysicalItemService items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    public int populate(PhysicalShipSnapshot ship, long seed) {
        Objects.requireNonNull(ship, "ship");
        Random random = new Random(seed ^ 0x7025B11DL);
        int caches = 0;

        for (var entry : ship.placements().entrySet()) {
            var definition = ship.definitions().get(entry.getKey());
            if (definition == null) continue;

            boolean eligible = definition.category() != TileCategory.CORE
                    || entry.getKey().value().equals("cargo")
                    || entry.getKey().value().equals("habitation");

            if (!eligible) continue;
            if (definition.category() != TileCategory.CORE && random.nextDouble() > 0.72) {
                continue;
            }

            var placement = entry.getValue();
            Location location = new Location(
                    ship.world(),
                    placement.minX() + placement.size() - 3,
                    placement.floorY() + 1,
                    placement.minZ() + 2
            );

            location.getBlock().setType(Material.BARREL, false);
            if (!(location.getBlock().getState() instanceof Barrel barrel)) {
                continue;
            }

            fill(barrel.getInventory(), random);
            barrel.setCustomName("§8비상 보급 상자");
            barrel.update(true, false);
            caches++;
        }

        return caches;
    }

    private void fill(Inventory inventory, Random random) {
        List<ResourceType> common = new ArrayList<>(List.of(
                ResourceType.REPAIR_PARTS,
                ResourceType.CIRCUITS,
                ResourceType.POWER_CELLS,
                ResourceType.FUEL,
                ResourceType.MEDICAL_SUPPLIES
        ));

        int rolls = 3 + random.nextInt(3);
        for (int i = 0; i < rolls; i++) {
            ResourceType type = common.get(random.nextInt(common.size()));
            int amount = 1 + random.nextInt(2);
            inventory.addItem(items.create(type, amount));
        }

        if (random.nextDouble() < 0.28) {
            inventory.addItem(items.create(ResourceType.BIO_SAMPLES, 1));
        }
        if (random.nextDouble() < 0.18) {
            inventory.addItem(items.create(ResourceType.DATA_CORES, 1));
        }
    }
}

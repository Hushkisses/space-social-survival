package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.tile.TileId;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;

public final class ShipModuleStructureLoader {

    private final JavaPlugin plugin;
    private final File structuresDirectory;

    public ShipModuleStructureLoader(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.structuresDirectory = new File(plugin.getDataFolder(), "structures");
        if (!structuresDirectory.exists() && !structuresDirectory.mkdirs()) {
            plugin.getLogger().warning(
                    "Could not create structure directory: " + structuresDirectory
            );
        }
    }

    public StructurePlacementResult placeIfAvailable(
            TileId tileId,
            Location origin,
            Random random
    ) {
        Objects.requireNonNull(tileId, "tileId");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(random, "random");

        File file = structureFile(tileId);
        if (!file.isFile()) {
            return StructurePlacementResult.missing(file.getAbsolutePath());
        }

        try {
            Structure structure = Bukkit.getStructureManager().loadStructure(file);
            structure.place(
                    origin,
                    false,
                    StructureRotation.NONE,
                    Mirror.NONE,
                    0,
                    1.0f,
                    random
            );

            var size = structure.getSize();
            return StructurePlacementResult.success(
                    file.getAbsolutePath(),
                    size.getBlockX() + "x" + size.getBlockY() + "x" + size.getBlockZ()
            );
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().warning(
                    "Failed to load structure " + file + ": " + exception.getMessage()
            );
            return StructurePlacementResult.failed(
                    file.getAbsolutePath(),
                    exception.getClass().getSimpleName() + ": " + exception.getMessage()
            );
        }
    }

    public File structureFile(TileId tileId) {
        return new File(structuresDirectory, tileId.value() + ".nbt");
    }

    public File structuresDirectory() {
        return structuresDirectory;
    }
}

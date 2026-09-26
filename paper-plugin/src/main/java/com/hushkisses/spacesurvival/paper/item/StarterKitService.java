package com.hushkisses.spacesurvival.paper.item;

import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class StarterKitService {

    private final SpaceSurvivalPlugin plugin;
    private final FunctionalItemService items;

    public StarterKitService(
            SpaceSurvivalPlugin plugin,
            FunctionalItemService items
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.items = Objects.requireNonNull(items, "items");
    }

    public void giveRoleKits() {
        for (PlayerId playerId : plugin.lobbyService().snapshot().players()) {
            Player player = plugin.getServer().getPlayer(playerId.value());
            if (player == null) continue;

            plugin.roleSelectionService().selectedRole(playerId)
                    .ifPresent(roleId -> {
                        items.giveStarterItem(player, roleId);
                        items.syncRadio(player);
                    });
        }
    }
}

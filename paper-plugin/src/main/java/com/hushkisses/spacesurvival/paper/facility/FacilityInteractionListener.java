package com.hushkisses.spacesurvival.paper.facility;

import com.hushkisses.spacesurvival.facility.FacilityId;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Objects;

public final class FacilityInteractionListener implements Listener {

    private final FacilityTerminalRegistry terminals;
    private final FacilityMenuService menus;

    public FacilityInteractionListener(
            FacilityTerminalRegistry terminals,
            FacilityMenuService menus
    ) {
        this.terminals = Objects.requireNonNull(terminals, "terminals");
        this.menus = Objects.requireNonNull(menus, "menus");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        FacilityId facilityId = terminals
                .facilityAt(event.getClickedBlock().getLocation())
                .orElse(null);

        if (facilityId == null) {
            return;
        }

        event.setCancelled(true);
        menus.open(event.getPlayer(), facilityId);
    }
}

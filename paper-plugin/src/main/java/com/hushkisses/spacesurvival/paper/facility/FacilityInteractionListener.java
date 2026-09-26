package com.hushkisses.spacesurvival.paper.facility;

import com.hushkisses.spacesurvival.facility.FacilityId;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Objects;

public final class FacilityInteractionListener implements Listener {

    private final com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin plugin;
    private final FacilityTerminalRegistry terminals;
    private final FacilityMenuService menus;

    public FacilityInteractionListener(
            com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin plugin,
            FacilityTerminalRegistry terminals,
            FacilityMenuService menus
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.terminals = Objects.requireNonNull(terminals, "terminals");
        this.menus = Objects.requireNonNull(menus, "menus");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
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

        if (plugin.physicalSanctionService().accessRestricted(event.getPlayer())
                || plugin.physicalSanctionService().ejected(event.getPlayer())) {
            event.getPlayer().sendMessage("§c[출입 제한] §f현재 시설 콘솔을 사용할 수 없습니다.");
            return;
        }

        menus.open(event.getPlayer(), facilityId);
    }
}

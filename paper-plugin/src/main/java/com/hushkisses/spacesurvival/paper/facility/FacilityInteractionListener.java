package com.hushkisses.spacesurvival.paper.facility;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.player.PlayerId;
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

        PlayerId playerId = PlayerId.of(event.getPlayer().getUniqueId());
        if (plugin.deathService().isDead(playerId)) {
            boolean infectedForm = plugin.infectedPlayerService()
                    .isInfectedForm(playerId);
            event.getPlayer().sendMessage(
                    infectedForm
                            ? "§4[감염체] §f감염체 상태에서는 시설 콘솔을 조작할 수 없습니다."
                            : "§7[사망 상태] §f사망 상태에서는 시설 콘솔을 조작할 수 없습니다."
            );
            return;
        }

        if (plugin.physicalSanctionService().detained(event.getPlayer())) {
            event.getPlayer().sendMessage(
                    "§6[감금] §f감금 상태에서는 시설 작업을 수행할 수 없습니다."
            );
            return;
        }

        if (plugin.meetingGuiService().meetingActive()) {
            event.getPlayer().sendMessage(
                    "§e[회의] §f현재 회의가 진행 중입니다. 토론·투표가 끝난 뒤 시설 작업을 재개할 수 있습니다."
            );
            return;
        }

        if (plugin.physicalSanctionService().accessRestricted(event.getPlayer())
                || plugin.physicalSanctionService().ejected(event.getPlayer())) {
            event.getPlayer().sendMessage("§c[출입 제한] §f현재 시설 콘솔을 사용할 수 없습니다.");
            return;
        }

        menus.open(event.getPlayer(), facilityId);
    }
}

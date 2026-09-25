package com.hushkisses.spacesurvival.paper.pvp;

import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.RoleCapability;
import com.hushkisses.spacesurvival.social.pvp.PvpPermissionContext;
import com.hushkisses.spacesurvival.time.CrisisStage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Objects;

public final class ConditionalPvpListener implements Listener {

    private final SpaceSurvivalPlugin plugin;

    public ConditionalPvpListener(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        PlayerId attackerId = PlayerId.of(attacker.getUniqueId());
        PlayerId targetId = PlayerId.of(target.getUniqueId());

        boolean securityAuthorized = plugin.roleSelectionService()
                .selectedRole(attackerId)
                .map(plugin.roleRegistry()::require)
                .map(role -> role.hasCapability(RoleCapability.EMERGENCY_LIMITED_PVP))
                .orElse(false);

        PvpPermissionContext context = new PvpPermissionContext(
                plugin.pvpRuntimeState().emergencyDeclared(),
                securityAuthorized,
                plugin.pvpRuntimeState().isConfirmedInfected(targetId),
                plugin.pvpRuntimeState().scenarioAllows(),
                plugin.gameRuntimeService().currentCrisisStage() == CrisisStage.COLLAPSE,
                plugin.pvpRuntimeState().specialEventAllows()
        );

        if (!plugin.conditionalPvpPolicy().isAllowed(context)) {
            event.setCancelled(true);
            attacker.sendMessage("§c현재는 승무원 간 공격이 제한되어 있습니다.");
        }
    }
}

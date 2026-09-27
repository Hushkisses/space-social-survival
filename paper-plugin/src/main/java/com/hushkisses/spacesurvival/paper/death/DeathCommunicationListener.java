package com.hushkisses.spacesurvival.paper.death;

import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Objects;

@SuppressWarnings("deprecation")
public final class DeathCommunicationListener implements Listener {

    private final SpaceSurvivalPlugin plugin;

    public DeathCommunicationListener(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        PlayerId senderId = PlayerId.of(sender.getUniqueId());

        if (!plugin.lobbyService().contains(senderId)) {
            return;
        }

        boolean senderDead = plugin.deathService().isDead(senderId);

        event.getRecipients().removeIf(recipient -> {
            PlayerId recipientId = PlayerId.of(recipient.getUniqueId());
            if (!plugin.lobbyService().contains(recipientId)) {
                return false;
            }

            boolean recipientDead = plugin.deathService().isDead(recipientId);
            return senderDead != recipientDead;
        });

        if (senderDead) {
            event.setFormat("§8[사망자] §7%s§8: §7%s");
        }
    }
}

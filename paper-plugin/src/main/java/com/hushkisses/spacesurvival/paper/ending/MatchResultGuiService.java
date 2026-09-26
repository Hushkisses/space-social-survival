package com.hushkisses.spacesurvival.paper.ending;

import com.hushkisses.spacesurvival.objective.ObjectiveSlot;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.result.MatchResult;
import com.hushkisses.spacesurvival.result.PlayerMatchResult;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MatchResultGuiService {

    private static final String TITLE = "§8최종 경기 결과";

    private final SpaceSurvivalPlugin plugin;

    public MatchResultGuiService(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void openForAll(MatchResult result) {
        for (PlayerId playerId : plugin.lobbyService().snapshot().players()) {
            Player player = plugin.getServer().getPlayer(playerId.value());
            if (player != null) {
                open(player, result);
            }
        }
    }

    public void open(Player viewer, MatchResult result) {
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);

        inventory.setItem(4, item(
                result.commonMissionCompleted()
                        ? Material.LIME_CONCRETE
                        : Material.RED_CONCRETE,
                result.commonMissionCompleted()
                        ? "§a공통 귀환 성공"
                        : "§c공통 귀환 실패",
                List.of(
                        "§7승리자: §f" + names(result.winners()),
                        "§7MVP: §f" + names(result.mvps())
                )
        ));

        int slot = 9;
        for (PlayerMatchResult playerResult : result.players()) {
            if (slot >= 45) break;

            PlayerId playerId = playerResult.playerId();
            String name = playerName(playerId);

            ArrayList<String> lore = new ArrayList<>();
            lore.add(playerResult.winner() ? "§a승리" : "§7비승리");
            lore.add("§7총점: §f" + playerResult.score().total());
            lore.add("§7생존: §f+" + playerResult.score().survival());
            lore.add("§7기본 목표: §f+" + playerResult.score().baseObjective());
            lore.add("§7비밀 임무: §f+" + playerResult.score().secretObjective());
            lore.add("§7공통 기여: §f+" + playerResult.score().commonContribution());
            lore.add("§7시나리오 보너스: §f+" + playerResult.score().scenarioBonus());

            plugin.objectiveEngine().objective(playerId, ObjectiveSlot.BASE)
                    .ifPresent(objective -> lore.add(
                            "§e기본 목표 공개: §f"
                                    + objective.definition().title()
                                    + " §7[" + objective.status() + "]"
                    ));
            plugin.objectiveEngine().objective(playerId, ObjectiveSlot.SECRET)
                    .ifPresent(objective -> lore.add(
                            "§d비밀 임무 공개: §f"
                                    + objective.definition().title()
                                    + " §7[" + objective.status() + "]"
                    ));

            if (result.mvps().contains(playerId)) {
                lore.add("§6★ MVP");
            }

            inventory.setItem(
                    slot++,
                    item(
                            result.mvps().contains(playerId)
                                    ? Material.NETHER_STAR
                                    : Material.PAPER,
                            "§f" + name,
                            lore
                    )
            );
        }

        viewer.openInventory(inventory);
    }

    private String names(java.util.Set<PlayerId> ids) {
        if (ids.isEmpty()) return "없음";
        return ids.stream().map(this::playerName).toList().toString();
    }

    private String playerName(PlayerId id) {
        String name = plugin.getServer().getOfflinePlayer(id.value()).getName();
        return name == null ? id.value().toString().substring(0, 8) : name;
    }

    private static ItemStack item(
            Material material,
            String name,
            List<String> lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}

package com.hushkisses.spacesurvival.paper.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class OpeningBriefingUi {

    public static final String TITLE = "§8우주선 사고 브리핑";

    public void open(Player player) {
        open(
                player,
                "우주선이 심각한 손상을 입었습니다. 주요 시설을 복구하고 귀환 절차를 완료하십시오.",
                0,
                0
        );
    }

    public void open(
            Player player,
            String publicBriefing,
            int initialIncidentCount,
            int generatedTileCount
    ) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        inventory.setItem(10, item(
                Material.WRITABLE_BOOK,
                "§6공개 사고 브리핑",
                List.of(
                        "§7" + publicBriefing,
                        "",
                        "§8숨은 시나리오 진실은 공개되지 않습니다."
                )
        ));

        inventory.setItem(13, item(
                Material.REDSTONE_TORCH,
                "§c초기 문제",
                List.of(
                        "§7감지된 초기 사건: §f" + initialIncidentCount + "건",
                        "§7핵심 우주선 상태와 시설을 직접 확인하십시오.",
                        "§8시스템 정보는 거짓말하지 않지만 모든 정보를 보여주지는 않습니다."
                )
        ));

        inventory.setItem(16, item(
                Material.COMPASS,
                "§b공통 목표 — 생존 기반 복구",
                List.of(
                        "§7전력·산소·선체·원자로를 안정화하십시오.",
                        "§7생존 기반이 확보되면 항법과 귀환 준비로 진행됩니다.",
                        "§7이번 함선 모듈 수: §f" + generatedTileCount
                )
        ));

        inventory.setItem(22, item(
                Material.NETHER_STAR,
                "§e직업 후보 확인",
                List.of(
                        "§f클릭하여 직업 후보 3개를 확인합니다.",
                        "§7직업은 전문 분야이며 숨은 진영과 무관합니다.",
                        "§7모든 승무원의 선택이 끝나면 임무가 자동 시작됩니다."
                )
        ));

        player.openInventory(inventory);
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

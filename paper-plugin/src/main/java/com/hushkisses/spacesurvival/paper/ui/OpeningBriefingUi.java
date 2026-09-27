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

        inventory.setItem(4, item(
                Material.WRITABLE_BOOK,
                "§c현재 상황",
                List.of(
                        "§f" + publicBriefing,
                        "",
                        "§7시작 위치: §f함교",
                        "§8공개되지 않은 시나리오 진실은 표시하지 않습니다."
                )
        ));

        inventory.setItem(10, item(
                Material.COMPASS,
                "§b공통 목표 — 우주선 복구 후 귀환",
                List.of(
                        "§7① 생존 기반 복구",
                        "§7② 항법 복구",
                        "§7③ 귀환 준비",
                        "§7④ 최종 유지",
                        "",
                        "§f지금은 우선 함선의 생존 기반을 안정화하십시오."
                )
        ));

        inventory.setItem(13, item(
                Material.REDSTONE_TORCH,
                "§c공개된 초기 문제",
                List.of(
                        "§7감지된 초기 사건: §f" + initialIncidentCount + "건",
                        "§7생성된 함선 모듈: §f" + generatedTileCount + "개",
                        "§7임무 시작 후 HUD와 공용 상태에서",
                        "§7가장 중요한 공개 문제를 계속 안내합니다."
                )
        ));

        inventory.setItem(16, item(
                Material.KNOWLEDGE_BOOK,
                "§e첫 행동",
                List.of(
                        "§f1. 아래에서 직업 후보를 확인합니다.",
                        "§f2. 직업을 선택합니다.",
                        "§f3. 개인 PDA에서 목표와 장비를 확인합니다.",
                        "§f4. HUD가 안내하는 시설로 이동합니다."
                )
        ));

        inventory.setItem(22, item(
                Material.NETHER_STAR,
                "§e직업 후보 3개 확인",
                List.of(
                        "§f클릭하여 직업을 선택합니다.",
                        "§7직업은 공개 전문 분야이며 숨은 진영과 무관합니다.",
                        "§7선택 후 개인 목표와 역할 설명을 바로 확인할 수 있습니다.",
                        "",
                        "§a클릭"
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

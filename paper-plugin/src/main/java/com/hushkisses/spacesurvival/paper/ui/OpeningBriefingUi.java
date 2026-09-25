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
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        inventory.setItem(10, item(
                Material.WRITABLE_BOOK,
                "§6공개 사고 브리핑",
                List.of(
                        "§7우주선이 심각한 손상을 입었습니다.",
                        "§7여러 핵심 계통이 불안정한 상태입니다.",
                        "§f승무원들은 협력하여 귀환 준비를 진행해야 합니다."
                )
        ));

        inventory.setItem(13, item(
                Material.REDSTONE_TORCH,
                "§c초기 문제",
                List.of(
                        "§7• 전력 출력 저하",
                        "§7• 의료실 보조전원 정지",
                        "§7• 장거리 통신 불능",
                        "§8※ 현재는 DEV 브리핑용 기본 표시입니다."
                )
        ));

        inventory.setItem(16, item(
                Material.COMPASS,
                "§b공통 목표 — 1단계",
                List.of(
                        "§f생존 기반 복구",
                        "§7전력·산소·선체 등 생존 기반을 안정화하십시오.",
                        "§8실제 수치 연동은 ShipState 구현 후 적용됩니다."
                )
        ));

        inventory.setItem(22, item(
                Material.NETHER_STAR,
                "§e직업 후보 확인",
                List.of(
                        "§f클릭하여 직업 후보 3개를 확인합니다.",
                        "§7직업은 전문 분야이며 숨은 진영과 무관합니다."
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

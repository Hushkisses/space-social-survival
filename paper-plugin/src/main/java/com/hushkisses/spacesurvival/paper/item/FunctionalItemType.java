package com.hushkisses.spacesurvival.paper.item;

import org.bukkit.Material;

public enum FunctionalItemType {
    ENGINEERING_MULTITOOL(
            "spacesurvival:engineering_multitool",
            "§b엔지니어 멀티툴",
            Material.IRON_PICKAXE
    ),
    MEDICAL_SCANNER(
            "spacesurvival:medical_scanner",
            "§a의료 스캐너",
            Material.CLOCK
    ),
    SECURITY_KEYCARD(
            "spacesurvival:security_keycard",
            "§c보안 키카드",
            Material.TRIPWIRE_HOOK
    ),
    RESEARCH_SCANNER(
            "spacesurvival:research_scanner",
            "§d연구 스캐너",
            Material.SPYGLASS
    ),
    RADIO(
            "spacesurvival:radio",
            "§e장거리 무전기",
            Material.COMPASS
    ),
    CARGO_SCANNER(
            "spacesurvival:cargo_scanner",
            "§6화물 스캐너",
            Material.RECOVERY_COMPASS
    );

    private final String customItemId;
    private final String displayName;
    private final Material fallbackMaterial;

    FunctionalItemType(
            String customItemId,
            String displayName,
            Material fallbackMaterial
    ) {
        this.customItemId = customItemId;
        this.displayName = displayName;
        this.fallbackMaterial = fallbackMaterial;
    }

    public String customItemId() { return customItemId; }
    public String displayName() { return displayName; }
    public Material fallbackMaterial() { return fallbackMaterial; }
}

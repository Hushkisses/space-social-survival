package com.hushkisses.spacesurvival.paper.item;

import org.bukkit.Material;

public enum FunctionalItemType {
    ENGINEERING_MULTITOOL(
            "spacesurvival:engineering_multitool",
            "§b엔지니어 멀티툴",
            Material.IRON_PICKAXE,
            "정밀 진단·전력 재배분·고급 수리에 사용합니다.",
            "기관실",
            "엔지니어"
    ),
    MEDICAL_SCANNER(
            "spacesurvival:medical_scanner",
            "§a의료 스캐너",
            Material.CLOCK,
            "정밀 감염 검사·고급 치료·감염 억제에 사용합니다.",
            "의료실",
            "의료 담당"
    ),
    SECURITY_KEYCARD(
            "spacesurvival:security_keycard",
            "§c보안 키카드",
            Material.TRIPWIRE_HOOK,
            "보안 통로와 보안 집행 기능에 사용합니다.",
            "보안 통로·제재 집행",
            "보안 담당"
    ),
    RESEARCH_SCANNER(
            "spacesurvival:research_scanner",
            "§d연구 스캐너",
            Material.SPYGLASS,
            "정밀 생체 분석·외계 생명체 연구·사건 원인 분석에 사용합니다.",
            "연구실",
            "연구 담당"
    ),
    RADIO(
            "spacesurvival:radio",
            "§e장거리 무전기",
            Material.COMPASS,
            "항법 목적지 설정과 장거리 통신 기능에 사용합니다.",
            "함교",
            "항법/통신 담당"
    ),
    CARGO_SCANNER(
            "spacesurvival:cargo_scanner",
            "§6화물 스캐너",
            Material.RECOVERY_COMPASS,
            "정밀 재고·희귀 자원 판별·고효율 가공에 사용합니다.",
            "화물실",
            "화물/정비 담당"
    );

    private final String customItemId;
    private final String displayName;
    private final Material fallbackMaterial;
    private final String purpose;
    private final String useLocation;
    private final String roleName;

    FunctionalItemType(
            String customItemId,
            String displayName,
            Material fallbackMaterial,
            String purpose,
            String useLocation,
            String roleName
    ) {
        this.customItemId = customItemId;
        this.displayName = displayName;
        this.fallbackMaterial = fallbackMaterial;
        this.purpose = purpose;
        this.useLocation = useLocation;
        this.roleName = roleName;
    }

    public String customItemId() { return customItemId; }
    public String displayName() { return displayName; }
    public Material fallbackMaterial() { return fallbackMaterial; }
    public String purpose() { return purpose; }
    public String useLocation() { return useLocation; }
    public String roleName() { return roleName; }
}

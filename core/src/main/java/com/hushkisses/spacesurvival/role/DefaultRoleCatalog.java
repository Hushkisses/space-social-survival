package com.hushkisses.spacesurvival.role;

import java.util.List;

public final class DefaultRoleCatalog {

    public static final RoleId ENGINEER = new RoleId("engineer");
    public static final RoleId MEDIC = new RoleId("medic");
    public static final RoleId SECURITY = new RoleId("security");
    public static final RoleId RESEARCHER = new RoleId("researcher");
    public static final RoleId NAV_COMMS = new RoleId("nav_comms");
    public static final RoleId CARGO_MAINTENANCE = new RoleId("cargo_maintenance");

    private DefaultRoleCatalog() {
    }

    public static RoleRegistry createRegistry() {
        RoleRegistry registry = new RoleRegistry();
        for (RoleDefinition role : createDefinitions()) {
            registry.register(role);
        }
        return registry;
    }

    public static List<RoleDefinition> createDefinitions() {
        return List.of(
                new RoleDefinition(
                        ENGINEER,
                        "엔지니어",
                        "수리와 전력 계통에 특화된 기술 직업",
                        2,
                        List.of(RolePassive.REPAIR_EFFICIENCY),
                        List.of(
                                RoleCapability.DIAGNOSE_FAULT,
                                RoleCapability.ADVANCED_REPAIR,
                                RoleCapability.REDISTRIBUTE_POWER
                        )
                ),
                new RoleDefinition(
                        MEDIC,
                        "의무관",
                        "치료와 감염 관리에 특화된 의료 직업",
                        2,
                        List.of(RolePassive.TREATMENT_EFFICIENCY),
                        List.of(
                                RoleCapability.PRECISE_INFECTION_TEST,
                                RoleCapability.ADVANCED_TREATMENT,
                                RoleCapability.SUPPRESS_INFECTION
                        )
                ),
                new RoleDefinition(
                        SECURITY,
                        "보안요원",
                        "제압과 보안 집행에 특화된 직업",
                        2,
                        List.of(RolePassive.RESTRAINT_AND_WEAPON_EFFICIENCY),
                        List.of(
                                RoleCapability.EXECUTE_DISARM,
                                RoleCapability.EXECUTE_DETENTION,
                                RoleCapability.USE_SECURITY_SYSTEM,
                                RoleCapability.EMERGENCY_LIMITED_PVP
                        )
                ),
                new RoleDefinition(
                        RESEARCHER,
                        "연구원",
                        "분석과 외계 생명체 연구에 특화된 직업",
                        2,
                        List.of(RolePassive.ANALYSIS_SPEED),
                        List.of(
                                RoleCapability.PRECISE_BIO_ANALYSIS,
                                RoleCapability.RESEARCH_ALIEN_LIFE,
                                RoleCapability.IDENTIFY_EVENT_CAUSE
                        )
                ),
                new RoleDefinition(
                        NAV_COMMS,
                        "항법/통신 담당",
                        "항법 정보와 장거리 통신에 특화된 직업",
                        2,
                        List.of(RolePassive.COMMUNICATION_EFFICIENCY),
                        List.of(
                                RoleCapability.PRECISE_NAVIGATION_DATA,
                                RoleCapability.CHANGE_DESTINATION,
                                RoleCapability.LONG_RANGE_COMMUNICATION,
                                RoleCapability.DISTRESS_SIGNAL
                        )
                ),
                new RoleDefinition(
                        CARGO_MAINTENANCE,
                        "화물/정비 담당",
                        "자원 운반, 재고 확인, 가공에 특화된 직업",
                        2,
                        List.of(RolePassive.CARGO_AND_PROCESSING_EFFICIENCY),
                        List.of(
                                RoleCapability.PRECISE_INVENTORY_CHECK,
                                RoleCapability.IDENTIFY_RARE_RESOURCE,
                                RoleCapability.HIGH_EFFICIENCY_PROCESSING
                        )
                )
        );
    }
}

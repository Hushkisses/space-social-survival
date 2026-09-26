package com.hushkisses.spacesurvival.facility.action;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.role.RoleCapability;

import java.util.List;

public final class DefaultFacilityActionCatalog {

    private DefaultFacilityActionCatalog() {
    }

    public static FacilityActionRegistry createRegistry() {
        FacilityActionRegistry registry = new FacilityActionRegistry();
        createDefinitions().forEach(registry::register);
        return registry;
    }

    public static List<FacilityActionDefinition> createDefinitions() {
        return List.of(
                basic("bridge.objective", DefaultFacilityCatalog.BRIDGE, "공통 목표 확인"),
                basic("bridge.ship_status", DefaultFacilityCatalog.BRIDGE, "우주선 핵심 상태 확인"),
                basic("bridge.meeting", DefaultFacilityCatalog.BRIDGE, "회의 소집"),
                basic("bridge.return", DefaultFacilityCatalog.BRIDGE, "귀환 절차 시작"),
                advanced("bridge.destination", DefaultFacilityCatalog.BRIDGE, "목적지 설정", RoleCapability.CHANGE_DESTINATION),
                advanced("bridge.long_range_comms", DefaultFacilityCatalog.BRIDGE, "장거리 통신 관리", RoleCapability.LONG_RANGE_COMMUNICATION),

                basic("engineering.power", DefaultFacilityCatalog.ENGINEERING, "전력 관리"),
                basic("engineering.engine", DefaultFacilityCatalog.ENGINEERING, "엔진 관리"),
                basic("engineering.repair", DefaultFacilityCatalog.ENGINEERING, "핵심 수리"),
                advanced("engineering.diagnose", DefaultFacilityCatalog.ENGINEERING, "정밀 고장 진단", RoleCapability.DIAGNOSE_FAULT),
                advanced("engineering.redistribute", DefaultFacilityCatalog.ENGINEERING, "전력 재배분", RoleCapability.REDISTRIBUTE_POWER),
                advanced("engineering.advanced_repair", DefaultFacilityCatalog.ENGINEERING, "고급 수리", RoleCapability.ADVANCED_REPAIR),

                basic("medical.treat", DefaultFacilityCatalog.MEDICAL, "기본 치료"),
                basic("medical.clear_status", DefaultFacilityCatalog.MEDICAL, "상태이상 제거"),
                basic("medical.infection_test", DefaultFacilityCatalog.MEDICAL, "기본 감염 검사"),
                basic("medical.decontaminate", DefaultFacilityCatalog.MEDICAL, "의료실 오염 제거"),
                advanced("medical.precise_test", DefaultFacilityCatalog.MEDICAL, "감염 정밀 검사", RoleCapability.PRECISE_INFECTION_TEST),
                advanced("medical.advanced_treatment", DefaultFacilityCatalog.MEDICAL, "고급 치료", RoleCapability.ADVANCED_TREATMENT),
                advanced("medical.suppress_infection", DefaultFacilityCatalog.MEDICAL, "감염 억제", RoleCapability.SUPPRESS_INFECTION),

                basic("research.sample", DefaultFacilityCatalog.RESEARCH, "샘플 분석"),
                basic("research.data", DefaultFacilityCatalog.RESEARCH, "데이터 분석"),
                basic("research.alien_material", DefaultFacilityCatalog.RESEARCH, "외계물질 연구"),
                advanced("research.precise_bio", DefaultFacilityCatalog.RESEARCH, "정밀 생체 분석", RoleCapability.PRECISE_BIO_ANALYSIS),
                advanced("research.alien_life", DefaultFacilityCatalog.RESEARCH, "외계 생명체 연구", RoleCapability.RESEARCH_ALIEN_LIFE),
                advanced("research.event_cause", DefaultFacilityCatalog.RESEARCH, "사건 원인 확인", RoleCapability.IDENTIFY_EVENT_CAUSE),

                basic("cargo.store", DefaultFacilityCatalog.CARGO, "자원 저장"),
                basic("cargo.deposit", DefaultFacilityCatalog.CARGO, "소지 자원 공용 창고 입고"),
                basic("cargo.sort", DefaultFacilityCatalog.CARGO, "자원 분류"),
                basic("cargo.process", DefaultFacilityCatalog.CARGO, "자원 가공"),
                basic("cargo.repair", DefaultFacilityCatalog.CARGO, "화물실 설비 복구"),
                advanced("cargo.inventory", DefaultFacilityCatalog.CARGO, "정밀 재고 확인", RoleCapability.PRECISE_INVENTORY_CHECK),
                advanced("cargo.rare", DefaultFacilityCatalog.CARGO, "희귀 자원 판별", RoleCapability.IDENTIFY_RARE_RESOURCE),
                advanced("cargo.efficient_process", DefaultFacilityCatalog.CARGO, "고효율 가공", RoleCapability.HIGH_EFFICIENCY_PROCESSING),

                basic("habitation.supply", DefaultFacilityCatalog.HABITATION, "기본 보급"),
                basic("habitation.maintenance", DefaultFacilityCatalog.HABITATION, "장비 정비"),
                basic("habitation.locker", DefaultFacilityCatalog.HABITATION, "개인 보관함")
        );
    }

    private static FacilityActionDefinition basic(String id, FacilityId facilityId, String displayName) {
        return new FacilityActionDefinition(
                new FacilityActionId(id),
                facilityId,
                displayName,
                FacilityActionTier.BASIC,
                null
        );
    }

    private static FacilityActionDefinition advanced(
            String id,
            FacilityId facilityId,
            String displayName,
            RoleCapability capability
    ) {
        return new FacilityActionDefinition(
                new FacilityActionId(id),
                facilityId,
                displayName,
                FacilityActionTier.ADVANCED,
                capability
        );
    }
}

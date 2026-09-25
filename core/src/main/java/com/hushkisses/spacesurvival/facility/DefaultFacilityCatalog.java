package com.hushkisses.spacesurvival.facility;

import java.util.List;

public final class DefaultFacilityCatalog {

    public static final FacilityId BRIDGE = new FacilityId("bridge");
    public static final FacilityId ENGINEERING = new FacilityId("engineering");
    public static final FacilityId MEDICAL = new FacilityId("medical");
    public static final FacilityId RESEARCH = new FacilityId("research");
    public static final FacilityId CARGO = new FacilityId("cargo");
    public static final FacilityId HABITATION = new FacilityId("habitation");

    private DefaultFacilityCatalog() {
    }

    public static FacilityRegistry createRegistry() {
        FacilityRegistry registry = new FacilityRegistry();
        for (FacilityDefinition definition : createDefinitions()) {
            registry.register(definition);
        }
        return registry;
    }

    public static List<FacilityDefinition> createDefinitions() {
        return List.of(
                new FacilityDefinition(BRIDGE, FacilityType.BRIDGE, "함교",
                        "공통 목표, 우주선 상태, 회의 및 귀환 절차를 관리하는 핵심 시설"),
                new FacilityDefinition(ENGINEERING, FacilityType.ENGINEERING, "기관실",
                        "전력, 엔진, 원자로 및 핵심 수리를 담당하는 시설"),
                new FacilityDefinition(MEDICAL, FacilityType.MEDICAL, "의료실",
                        "치료, 상태이상 제거, 감염 검사와 의료 처리를 담당하는 시설"),
                new FacilityDefinition(RESEARCH, FacilityType.RESEARCH, "연구실",
                        "샘플, 데이터, 외계물질 분석을 담당하는 시설"),
                new FacilityDefinition(CARGO, FacilityType.CARGO, "화물실",
                        "자원 저장, 분류, 판별 및 가공을 담당하는 시설"),
                new FacilityDefinition(HABITATION, FacilityType.HABITATION, "생활구역",
                        "보급, 장비 정비, 개인 보관과 비공개 활동이 일어나는 시설")
        );
    }
}

package com.hushkisses.spacesurvival.event;

import com.hushkisses.spacesurvival.event.effect.EventFlagEffect;
import com.hushkisses.spacesurvival.event.effect.FacilityStatusEffect;
import com.hushkisses.spacesurvival.event.effect.SharedResourceDeltaEffect;
import com.hushkisses.spacesurvival.event.effect.ShipMetricDeltaEffect;
import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.resource.ResourceType;
import com.hushkisses.spacesurvival.ship.ShipMetric;

import java.util.List;

public final class DefaultSmallEventCatalog {

    private DefaultSmallEventCatalog() {
    }

    public static List<GameEventDefinition> create() {
        return List.of(
                event("lighting_outage", "조명 정지", "일부 구역의 조명이 정지합니다.",
                        new EventFlagEffect("lighting_outage", true)),
                event("door_fault", "문 고장", "일부 연결 통로의 문이 오작동합니다.",
                        new EventFlagEffect("door_fault", true)),
                event("local_oxygen_drop", "국지 산소 저하", "산소 계통에 작은 누출이 발생합니다.",
                        new ShipMetricDeltaEffect(ShipMetric.OXYGEN, -5)),
                event("power_cell_depletion", "전력 셀 고갈", "공용 전력 셀 일부가 소모됩니다.",
                        new SharedResourceDeltaEffect(ResourceType.POWER_CELLS, -1)),
                event("cargo_damage", "화물 파손", "화물실 설비 일부가 손상됩니다.",
                        new FacilityStatusEffect(DefaultFacilityCatalog.CARGO, FacilityStatus.DAMAGED)),
                event("medical_contamination", "의료품 오염", "의료실이 오염되어 복구가 필요합니다.",
                        new FacilityStatusEffect(DefaultFacilityCatalog.MEDICAL, FacilityStatus.DAMAGED),
                        new EventFlagEffect("medical_contamination", true)),
                event("small_fire", "소형 화재", "선체 내부에서 소형 화재가 발생합니다.",
                        new ShipMetricDeltaEffect(ShipMetric.HULL, -4),
                        new EventFlagEffect("small_fire", true)),
                event("comms_noise", "통신 잡음", "장거리 통신에 잡음이 발생합니다.",
                        new EventFlagEffect("comms_noise", true))
        );
    }

    private static GameEventDefinition event(
            String id,
            String name,
            String description,
            com.hushkisses.spacesurvival.event.effect.EventEffect... effects
    ) {
        return new GameEventDefinition(
                new GameEventId(id),
                name,
                description,
                GameEventScale.SMALL,
                List.of(effects)
        );
    }
}

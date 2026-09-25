package com.hushkisses.spacesurvival.event;

import com.hushkisses.spacesurvival.event.effect.EventFlagEffect;
import com.hushkisses.spacesurvival.event.effect.FacilityStatusEffect;
import com.hushkisses.spacesurvival.event.effect.ShipMetricDeltaEffect;
import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.ship.ShipMetric;

import java.util.List;

public final class DefaultMajorEventCatalog {

    private DefaultMajorEventCatalog() {
    }

    public static List<GameEventDefinition> create() {
        return List.of(
                event("reactor_runaway", "원자로 폭주", "원자로 출력이 불안정해지고 기관실이 손상됩니다.",
                        new ShipMetricDeltaEffect(ShipMetric.REACTOR, -35),
                        new FacilityStatusEffect(DefaultFacilityCatalog.ENGINEERING, FacilityStatus.DAMAGED),
                        new EventFlagEffect("reactor_runaway", true)),
                event("hull_breach", "선체 균열", "대규모 선체 균열과 산소 손실이 발생합니다.",
                        new ShipMetricDeltaEffect(ShipMetric.HULL, -35),
                        new ShipMetricDeltaEffect(ShipMetric.OXYGEN, -15),
                        new EventFlagEffect("hull_breach", true)),
                event("mass_infection", "대규모 감염 경보", "의료실 격리가 필요한 감염 경보가 발생합니다.",
                        new FacilityStatusEffect(DefaultFacilityCatalog.MEDICAL, FacilityStatus.QUARANTINED),
                        new EventFlagEffect("mass_infection", true)),
                event("alien_intrusion", "외계 생물 침입", "미확인 생명체 침입 흔적이 감지됩니다.",
                        new ShipMetricDeltaEffect(ShipMetric.HULL, -10),
                        new EventFlagEffect("alien_intrusion", true)),
                event("total_power_failure", "전체 전력 붕괴", "주 전력 계통이 완전히 정지합니다.",
                        new ShipMetricDeltaEffect(ShipMetric.POWER, -100),
                        new FacilityStatusEffect(DefaultFacilityCatalog.ENGINEERING, FacilityStatus.OFFLINE),
                        new EventFlagEffect("total_power_failure", true))
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
                GameEventScale.LARGE,
                List.of(effects)
        );
    }
}

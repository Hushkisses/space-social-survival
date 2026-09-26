package com.hushkisses.spacesurvival.guidance;

import com.hushkisses.spacesurvival.ending.ReturnRequirements;
import com.hushkisses.spacesurvival.ending.ReturnStage;
import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityStateSnapshot;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.ship.ShipStateSnapshot;

import java.util.List;
import java.util.Objects;

public final class PlayerGuidanceResolver {

    private final ReturnRequirements requirements;

    public PlayerGuidanceResolver() {
        this(ReturnRequirements.developmentDefaults());
    }

    public PlayerGuidanceResolver(ReturnRequirements requirements) {
        this.requirements = Objects.requireNonNull(requirements, "requirements");
    }

    public PublicProblem resolve(
            ReturnStage stage,
            ShipStateSnapshot ship,
            List<FacilityStateSnapshot> facilities
    ) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(ship, "ship");
        Objects.requireNonNull(facilities, "facilities");

        FacilityStateSnapshot engineering = find(facilities, DefaultFacilityCatalog.ENGINEERING);
        FacilityStateSnapshot bridge = find(facilities, DefaultFacilityCatalog.BRIDGE);

        if (unusable(engineering)) {
            return new PublicProblem(
                    "기관실을 사용할 수 없습니다",
                    DefaultFacilityCatalog.ENGINEERING,
                    "기관실 상태 정상화",
                    "기관실 상태와 접근 조건을 확인하십시오"
            );
        }
        if (unusable(bridge)) {
            return new PublicProblem(
                    "함교를 사용할 수 없습니다",
                    DefaultFacilityCatalog.BRIDGE,
                    "함교 상태 정상화",
                    "함교 상태와 접근 조건을 확인하십시오"
            );
        }

        if (ship.power() < requirements.minPower()) {
            return new PublicProblem(
                    "전력 부족 " + ship.power() + "%",
                    DefaultFacilityCatalog.ENGINEERING,
                    "전력 셀",
                    "기관실에서 전력 관리를 실행하십시오"
            );
        }
        if (ship.hull() < requirements.minHull()) {
            return new PublicProblem(
                    "선체 안정도 부족 " + ship.hull() + "%",
                    DefaultFacilityCatalog.ENGINEERING,
                    "수리 부품",
                    "기관실에서 핵심 수리를 실행하십시오"
            );
        }
        if (ship.reactor() < requirements.minReactor()) {
            return new PublicProblem(
                    "원자로/엔진 출력 부족 " + ship.reactor() + "%",
                    DefaultFacilityCatalog.ENGINEERING,
                    "연료",
                    "기관실에서 엔진 관리를 실행하십시오"
            );
        }
        if (ship.oxygen() < requirements.minOxygen()) {
            return new PublicProblem(
                    "산소 수준 저하 " + ship.oxygen() + "%",
                    DefaultFacilityCatalog.ENGINEERING,
                    "관련 시설 상태 점검",
                    "기관실과 함선 상태를 확인해 산소 저하 원인을 대응하십시오"
            );
        }

        FacilityStateSnapshot blocked = facilities.stream()
                .filter(this::unusable)
                .findFirst()
                .orElse(null);
        if (blocked != null) {
            return new PublicProblem(
                    blocked.displayName() + " 사용 불가",
                    blocked.id(),
                    "시설 상태 정상화",
                    blocked.displayName() + "의 상태와 접근 조건을 확인하십시오"
            );
        }

        FacilityStateSnapshot damaged = facilities.stream()
                .filter(facility -> facility.status() == FacilityStatus.DAMAGED)
                .findFirst()
                .orElse(null);
        if (damaged != null) {
            return new PublicProblem(
                    damaged.displayName() + " 손상",
                    damaged.id(),
                    "시설 점검",
                    damaged.displayName() + " 콘솔에서 사용 가능한 복구 행동을 확인하십시오"
            );
        }

        return switch (stage) {
            case SURVIVAL_SYSTEMS -> new PublicProblem(
                    "생존 기반 복구 조건 확인",
                    DefaultFacilityCatalog.BRIDGE,
                    "전력·산소·선체·원자로 안정",
                    "함교에서 공통 목표를 확인하고 다음 귀환 단계로 진행하십시오"
            );
            case NAVIGATION -> new PublicProblem(
                    "항법 목적지 설정 필요",
                    DefaultFacilityCatalog.BRIDGE,
                    "항법/통신 담당 + 장거리 무전기",
                    "함교에서 목적지를 설정하십시오"
            );
            case RETURN_PREPARATION -> new PublicProblem(
                    "귀환 준비 확인 필요",
                    DefaultFacilityCatalog.BRIDGE,
                    "주요 시설 안정 상태",
                    "함교에서 귀환 준비를 확인하십시오"
            );
            case FINAL_HOLD -> new PublicProblem(
                    "최종 귀환 유지 단계",
                    DefaultFacilityCatalog.BRIDGE,
                    "엔진·전력·항법·산소 유지",
                    "함교에서 최종 절차를 유지하며 핵심 수치를 방어하십시오"
            );
            case COMPLETED -> new PublicProblem(
                    "공통 임무 완료",
                    DefaultFacilityCatalog.BRIDGE,
                    "결과 확인",
                    "최종 결과 화면을 확인하십시오"
            );
            case FAILED -> new PublicProblem(
                    "공통 임무 실패",
                    DefaultFacilityCatalog.BRIDGE,
                    "결과 확인",
                    "최종 결과 화면을 확인하십시오"
            );
        };
    }

    private FacilityStateSnapshot find(
            List<FacilityStateSnapshot> facilities,
            com.hushkisses.spacesurvival.facility.FacilityId id
    ) {
        return facilities.stream()
                .filter(facility -> facility.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private boolean unusable(FacilityStateSnapshot facility) {
        return facility != null
                && (facility.status() == FacilityStatus.OFFLINE
                || facility.status() == FacilityStatus.QUARANTINED);
    }
}

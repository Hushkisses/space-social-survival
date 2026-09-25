package com.hushkisses.spacesurvival.objective;

import java.util.List;
import java.util.Set;

public final class DefaultObjectiveCatalog {

    private DefaultObjectiveCatalog() {
    }

    public static ObjectiveRegistry createRegistry() {
        ObjectiveRegistry registry = new ObjectiveRegistry();
        createDefinitions().forEach(registry::register);
        return registry;
    }

    public static List<ObjectiveDefinition> createDefinitions() {
        return List.of(
                objective("survive_return", "살아서 귀환", "생존 상태로 귀환 절차를 완료한다.", ObjectiveCategory.SURVIVAL, 1, "survival", "escape"),
                objective("four_survivors", "다수 생존", "생존자 4명 이상을 유지한 채 귀환한다.", ObjectiveCategory.SURVIVAL, 1, "survival", "escape"),
                objective("repair_engine", "엔진 완전 복구", "기관 계통을 완전 복구 상태로 만든다.", ObjectiveCategory.PROTECTION, 1, "reactor"),
                objective("stabilize_oxygen", "산소 안정도 유지", "산소 계통을 안정 상태로 유지한다.", ObjectiveCategory.PROTECTION, 1, "survival"),
                objective("protect_player", "특정 승무원 보호", "배정된 승무원이 생존하도록 한다.", ObjectiveCategory.PROTECTION, 1, "survival"),
                objective("keep_medical_normal", "의료실 정상 유지", "게임 종료 시 의료실을 정상 상태로 유지한다.", ObjectiveCategory.PROTECTION, 1, "infection"),
                objective("collect_data_cores", "데이터 코어 확보", "데이터 코어 2개를 확보한다.", ObjectiveCategory.COLLECTION, 2, "data"),
                objective("collect_bio_sample", "생체 샘플 확보", "생체 샘플을 확보한다.", ObjectiveCategory.COLLECTION, 1, "sample", "infection"),
                objective("collect_rare_parts", "희귀 부품 확보", "희귀 부품 2개를 확보한다.", ObjectiveCategory.COLLECTION, 2, "cargo"),
                objective("obtain_admin_keycard", "관리자 키카드 확보", "관리자 키카드를 확보한다.", ObjectiveCategory.COLLECTION, 1, "cargo"),
                objective("dispose_bio_sample", "생체 샘플 폐기", "지정된 생체 샘플을 폐기한다.", ObjectiveCategory.DISPOSAL, 1, "sample"),
                objective("eliminate_infected", "감염체 제거", "지정된 감염체 제거 조건을 달성한다.", ObjectiveCategory.DISPOSAL, 1, "infection"),
                objective("delete_data", "특정 데이터 삭제", "지정된 데이터를 삭제한다.", ObjectiveCategory.DISPOSAL, 1, "data"),
                objective("isolate_alien_material", "외계물질 격리", "외계물질을 안전하게 격리한다.", ObjectiveCategory.DISPOSAL, 1, "sample"),
                objective("keep_destination", "기본 목적지 유지", "초기 목적지를 변경하지 않고 귀환한다.", ObjectiveCategory.NAVIGATION, 1, "navigation"),
                objective("change_destination", "목적지 변경", "지정된 목적지로 항로를 변경한다.", ObjectiveCategory.NAVIGATION, 1, "navigation"),
                objective("prevent_nav_change", "항법 변경 방지", "다른 승무원의 목적지 변경을 저지한다.", ObjectiveCategory.INTERFERENCE, 1, "navigation"),
                objective("prevent_full_repair", "시설 완전 복구 방지", "지정된 시설이 완전 복구되는 것을 막는다.", ObjectiveCategory.INTERFERENCE, 1, "reactor"),
                objective("limit_resource", "특정 자원량 제한", "지정된 자원의 보유량이 기준을 넘지 않게 한다.", ObjectiveCategory.INTERFERENCE, 1, "cargo"),
                objective("disrupt_player", "특정 임무 방해", "배정된 승무원의 임무 진행을 방해한다.", ObjectiveCategory.INTERFERENCE, 1, "survival"),
                objective("return_infected", "감염 상태로 귀환", "감염 상태를 유지한 채 귀환한다.", ObjectiveCategory.STATE, 1, "infection", "escape"),
                objective("no_treatment", "치료 없이 생존", "의료 치료를 받지 않고 생존한다.", ObjectiveCategory.STATE, 1, "survival"),
                objective("finish_with_equipment", "특정 장비 보유", "지정 장비를 가진 상태로 게임을 종료한다.", ObjectiveCategory.STATE, 1, "cargo"),
                objective("return_from_zone", "특정 구역에서 귀환 개시", "지정 구역에서 귀환 절차 개시 조건을 달성한다.", ObjectiveCategory.NAVIGATION, 1, "escape")
        );
    }

    private static ObjectiveDefinition objective(
            String id,
            String title,
            String description,
            ObjectiveCategory category,
            int target,
            String... tags
    ) {
        return new ObjectiveDefinition(
                new ObjectiveId(id),
                title,
                description,
                category,
                target,
                100,
                Set.of(tags)
        );
    }
}

package com.hushkisses.spacesurvival.scenario;

import java.util.List;

public final class DefaultScenarioCatalog {

    private DefaultScenarioCatalog() {
    }

    public static List<ScenarioDefinition> create() {
        return List.of(
                new ScenarioDefinition(
                        ScenarioType.ACCIDENT,
                        "단순 사고",
                        "원자로 사고로 주요 계통이 손상되었습니다. 주요 시설을 복구하고 귀환 절차를 완료하십시오.",
                        "초기 적대자는 없습니다. 시설 고장과 개인 목표 충돌이 핵심입니다.",
                        0,
                        0,
                        false
                ),
                new ScenarioDefinition(
                        ScenarioType.SABOTAGE,
                        "내부 공작",
                        "원자로 사고로 주요 계통이 손상되었습니다. 주요 시설을 복구하고 귀환 절차를 완료하십시오.",
                        "0~2명의 공작 목표 보유자가 존재할 수 있습니다. 직접 살해보다 공작이 중심입니다.",
                        0,
                        2,
                        true
                ),
                new ScenarioDefinition(
                        ScenarioType.INFECTION,
                        "감염",
                        "원자로 사고로 주요 계통이 손상되었습니다. 주요 시설을 복구하고 귀환 절차를 완료하십시오.",
                        "초반에는 일반 사고처럼 시작하며 중반 이후 감염 사건이 발생할 수 있습니다.",
                        0,
                        0,
                        false
                )
        );
    }
}

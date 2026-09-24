# 우주선 사회적 생존게임 개발실행계획 v1.0

## 0. 문서 목적

이 문서는 `우주선 사회적 생존게임 최종 기획서 v1.0`을 실제 Paper 서버 플러그인으로 구현하기 위한 개발 기준서다.

핵심 목표는 다음과 같다.

- 6명 기준, 최대 10명 지원
- 30~60분 단판
- 평균 40~45분
- 공통 목표 + 개인 목표 + 시나리오 + 사건
- 모듈식 랜덤 타일 맵
- 조건부 PvP
- 회의/처분 시스템
- Simple Voice Chat 연동
- ItemsAdder / MythicMobs / ModelEngine 활용
- 짧은 개발기간 안에 플레이 가능한 MVP 확보
- 이후 콘텐츠를 모듈 추가 방식으로 확장

개발 원칙은 다음 한 문장으로 정리한다.

> 핵심 게임 상태는 자체 Paper 플러그인이 관리하고, 외부 플러그인은 표현·콘텐츠 계층으로 사용한다.

---

# 1. Source of Truth

개발이 시작된 이후에는 다음 순서대로 내용을 신뢰한다.

1. GitHub `main` 브랜치의 최신 실제 소스
2. `docs/PROJECT-STATE.md`
3. `docs/game-design-v1.0.md`
4. `docs/development-plan-v1.0.md`
5. 과거 채팅 내용

과거 대화나 기억보다 GitHub의 최신 실제 코드와 `PROJECT-STATE.md`가 항상 우선한다.

새 개발 세션을 시작할 때 반드시 다음을 확인한다.

- 최신 HEAD
- 현재 브랜치
- `docs/PROJECT-STATE.md`
- 현재 DEV 상태
- 최근 완료 DEV
- 다음 DEV
- 관련 기존 코드
- 관련 테스트

기존 구현을 추측해서 중복 구현하지 않는다.

---

# 2. GitHub 저장소

## 2.1 저장소 이름

예정 저장소:

```text
Hushkisses/space-social-survival
```

저장소가 생성된 후 해당 저장소를 프로젝트의 유일한 개발 저장소로 사용한다.

## 2.2 기본 브랜치

```text
main
```

`main`은 항상 빌드 가능한 상태를 유지하는 것을 원칙으로 한다.

## 2.3 DEV 작업 브랜치

개발 단위별로 다음 형식을 사용한다.

```text
dev/DEV-001-bootstrap
dev/DEV-002-game-session
dev/DEV-003-player-state
```

소규모 수정은 다음 형식을 사용할 수 있다.

```text
fix/DEV-023-itemsadder-id
hotfix/voicechat-connection
```

## 2.4 기본 개발 흐름

```text
main 최신화
↓
DEV 브랜치 생성
↓
기존 코드 확인
↓
구현
↓
자동 테스트
↓
로컬 Paper 서버 검증
↓
PROJECT-STATE.md 갱신
↓
commit / push
↓
main 병합
↓
main 기준 최종 검증
```

혼자 개발하는 경우에도 DEV 단위 경계를 명확하게 유지한다.

---

# 3. 기술스택

## 3.1 서버

```text
Paper 26.2 build 123
```

Paper API를 기본 서버 API로 사용한다.

NMS 직접 접근은 초기 MVP에서 금지한다.

정말 필요한 경우에만 별도 adapter 계층을 둔다.

---

## 3.2 Java

```text
Java 25
```

서버 실행과 빌드 toolchain 모두 Java 25를 기준으로 한다.

---

## 3.3 빌드

```text
Gradle 9.7.1
Gradle Wrapper 필수
Kotlin DSL
```

파일:

```text
build.gradle.kts
settings.gradle.kts
gradle.properties
gradlew
gradlew.bat
```

로컬에 설치된 Gradle보다 Wrapper를 우선 사용한다.

---

## 3.4 구현 언어

```text
Java
```

핵심 플러그인 코드는 Java로 통일한다.

Kotlin을 혼용하지 않는다.

목적:

- Paper API 예제와 호환성
- 디버깅 단순화
- 빌드 복잡도 감소
- AI 코드 생성/검토 일관성 확보

---

# 4. 외부 플러그인

## 4.1 필수 또는 준필수

### Simple Voice Chat

역할:

- 근거리 음성
- 사망자 통신 분리
- 향후 무전 채널
- 시설 상태에 따른 통신 제어

자체 플러그인에서 Voice Chat API를 adapter로 감싼다.

게임 로직이 Simple Voice Chat API를 직접 호출하지 않게 한다.

예:

```text
core
↓
VoiceService
↓
SimpleVoiceChatAdapter
```

---

### ItemsAdder

역할:

- 커스텀 자원
- 의료키트
- 키카드
- 생체 샘플
- 데이터 코어
- 공구
- 무전기
- UI용 아이콘

핵심 게임 로직에서는 ItemsAdder ID를 직접 사용하지 않는다.

예:

```text
GameItem.BIO_SAMPLE
↓
ItemRegistry
↓
space:bio_sample
```

---

## 4.2 2차 적용

### MythicMobs

적용 시점:

```text
감염 시나리오 / PvE 단계
```

역할:

- 감염체
- 외계 생명체
- 특수 AI
- 보스성 이벤트

게임 승패나 시나리오 상태를 MythicMobs 변수에 저장하지 않는다.

---

### ModelEngine

적용 시점:

```text
게임성 검증 이후
```

역할:

- 외계 생명체 모델
- 감염체 모델
- 고급 애니메이션
- 특수 오브젝트

초기 MVP 필수 요소가 아니다.

---

# 5. 데이터 저장

## 5.1 런타임 게임 상태

메모리에서 관리한다.

예:

```text
GameSession
PlayerState
ShipState
ScenarioState
ObjectiveState
MeetingState
MapInstance
```

한 판의 상태를 DB에서 실시간 읽어 게임을 돌리지 않는다.

---

## 5.2 영구 데이터

초기에는 다음만 영구 저장한다.

- 플레이 횟수
- 승리 횟수
- MVP 횟수
- 해금 직업
- 해금 외형
- 업적
- 설정값

MVP 초기 구현에서는 파일 기반 저장으로 시작해도 된다.

권장:

```text
JSON 또는 YAML
```

장기 성장 기능이 실제로 커질 경우 SQLite로 이전한다.

DB 추상화 계층을 만들어 저장소 교체가 가능하도록 한다.

---

# 6. 프로젝트 모듈 구조

초기에는 멀티모듈 Gradle을 사용한다.

```text
space-social-survival/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle.properties
├─ gradlew
├─ gradlew.bat
├─ gradle/
│
├─ core/
├─ paper-plugin/
├─ integrations/
│   ├─ itemsadder/
│   ├─ voicechat/
│   ├─ mythicmobs/
│   └─ modelengine/
│
├─ dev-server/
├─ docs/
└─ tools/
```

---

# 7. `core` 모듈

Paper API에 최대한 의존하지 않는 순수 Java 게임 도메인이다.

담당:

- GameSession
- PlayerState
- Objective
- ConflictSet
- Scenario
- Event
- ShipState
- FacilityState
- CrisisState
- ResultEvaluator
- 승패 규칙
- 목표 배정 알고리즘
- 시나리오 규칙
- 맵 그래프 검증

장점:

- JUnit 테스트 용이
- Paper 없이 핵심 로직 검증 가능
- 외부 플러그인 교체 영향 최소화

---

# 8. `paper-plugin` 모듈

Paper 서버와 실제 상호작용하는 모듈이다.

담당:

- Plugin Bootstrap
- Command
- Listener
- Inventory GUI
- ActionBar / BossBar / HUD
- Bukkit Player 연결
- Block/Entity Interaction
- Teleport
- Damage/PvP 제어
- Scheduler
- Paper event → core event 변환

---

# 9. `integrations` 모듈

외부 플러그인과의 연결만 담당한다.

예:

```text
integrations:voicechat
integrations:itemsadder
integrations:mythicmobs
integrations:modelengine
```

각 adapter는 외부 플러그인이 없을 때 안전하게 비활성화 가능해야 한다.

초기에는 `voicechat`, `itemsadder`만 실제 구현해도 된다.

---

# 10. 패키지 구조

기본 Java 패키지는 다음과 같이 잡는다.

```text
com.hushkisses.spacesurvival
```

예:

```text
com.hushkisses.spacesurvival.game
com.hushkisses.spacesurvival.player
com.hushkisses.spacesurvival.role
com.hushkisses.spacesurvival.objective
com.hushkisses.spacesurvival.scenario
com.hushkisses.spacesurvival.event
com.hushkisses.spacesurvival.map
com.hushkisses.spacesurvival.facility
com.hushkisses.spacesurvival.resource
com.hushkisses.spacesurvival.meeting
com.hushkisses.spacesurvival.combat
com.hushkisses.spacesurvival.result
com.hushkisses.spacesurvival.integration
```

---

# 11. 설정 파일 구조

```text
plugins/SpaceSurvival/
├─ config.yml
├─ balance.yml
├─ roles.yml
├─ objectives.yml
├─ scenarios.yml
├─ events.yml
├─ resources.yml
├─ maps/
│   ├─ tiles.yml
│   └─ layouts/
└─ data/
```

역할:

### config.yml
서버 기능 설정.

### balance.yml
밸런스 수치.

### roles.yml
직업 수치.

### objectives.yml
개인 목표 정의.

### scenarios.yml
시나리오 정의.

### events.yml
사건 정의.

### resources.yml
자원/아이템 매핑.

### maps/
모듈 타일 등록 및 맵 규칙.

---

# 12. 데이터 주도 설계

신규 콘텐츠 추가 시 Java 수정 없이 가능한 영역을 최대화한다.

데이터화 우선 대상:

- 직업 기본 수치
- 개인 목표
- 목표 점수
- 사건
- 사건 가중치
- 시나리오별 사건 풀
- 시나리오 확률
- 자원량
- 타일 정보
- 연결 규칙
- ItemsAdder ID

Java 구현이 필요한 영역:

- 새로운 행동 유형
- 새로운 조건 평가기
- 새로운 이벤트 효과
- 새로운 시설 기능
- 새로운 외부 플러그인 adapter

---

# 13. 핵심 인터페이스

초기 설계 시 다음 개념을 명시적으로 분리한다.

```text
GameService
GameSession
PlayerGameState

Objective
ObjectiveCondition
ObjectiveProgress
ObjectiveAssignmentService

Scenario
ScenarioDefinition
ScenarioRuntime

GameEvent
EventTrigger
EventEffect

ShipState
Facility
FacilityState

MapDefinition
MapInstance
Sector
Room
Connection

ResultEvaluator

ItemService
VoiceService
MobService
ModelService
```

외부 시스템은 interface를 통해 접근한다.

---

# 14. 이벤트 기반 내부 구조

시스템끼리 직접 강하게 결합하지 않는다.

예:

```text
플레이어가 생체 샘플 획득
↓
GameEvent: ITEM_ACQUIRED
↓
ObjectiveSystem 갱신
↓
ScenarioSystem 조건 확인
↓
EventSystem 후속 사건 확인
```

또는:

```text
기관실 손상
↓
FACILITY_DAMAGED
↓
ShipState 갱신
↓
CrisisSystem 갱신
↓
HUD 갱신
```

초기에는 복잡한 외부 EventBus 라이브러리 대신 자체 단순 dispatcher로 충분하다.

---

# 15. 맵 기술 구조

맵은 실제 건축 좌표가 아니라 논리 ID로 제어한다.

```text
MapDefinition
 ├─ TileDefinition
 ├─ SectorType
 ├─ ConnectionPoint
 └─ GenerationConstraint
```

실행 중:

```text
MapInstance
 ├─ TileInstance
 ├─ ConnectionInstance
 ├─ FacilityInstance
 └─ PlayerLocationState
```

---

# 16. 타일 저장 방식

초기 권장 방식:

```text
Structure Block NBT 또는 Paper/WorldEdit 기반 배치
```

단, 게임 코어는 타일 배치 구현을 몰라야 한다.

추상화:

```text
TilePlacementService
```

구현 예:

```text
StructureTilePlacementService
```

향후 WorldEdit 방식으로 바꿔도 core 영향이 없어야 한다.

---

# 17. TP 연결

각 타일에는 `ConnectionPoint`를 둔다.

예:

```text
id: medical_north
type: AIRLOCK
target: cargo_south
```

문 또는 특정 구역 진입 시:

```text
출발 ConnectionPoint
→ 상태 확인
→ 목표 ConnectionPoint
→ 안전 위치 계산
→ TP
```

지원 상태:

```text
OPEN
LOCKED
POWER_REQUIRED
KEYCARD_REQUIRED
DISABLED
```

---

# 18. 테스트 전략

## 18.1 자동 테스트

`core`는 JUnit 5 기반으로 적극 테스트한다.

필수 테스트:

- GameSession 상태 전이
- Objective 진행
- ConflictSet 배정
- 6~10명 목표 배정
- Scenario 선택
- ResultEvaluator
- 위기 단계 계산
- 맵 그래프 연결 검증
- 접근 불가능 구역 검출

---

## 18.2 Paper 통합 테스트

Paper-dependent 로직은 자동 테스트보다 개발 서버 검증 비중을 높인다.

검증 대상:

- 이벤트 Listener
- GUI
- TP
- 인벤토리
- Damage
- 외부 플러그인 adapter

---

## 18.3 배포 전 원칙

```text
test
↓
build
↓
deploy
```

테스트 실패 시 플러그인을 dev-server에 복사하지 않는다.

---

# 19. 개발 서버 구조

저장소 내:

```text
dev-server/
├─ README.md
├─ quick-deploy.bat
├─ start-dev.bat
├─ stop 안내
└─ server/
```

`server/` 자체는 `.gitignore` 처리한다.

권장 흐름:

```text
서버 stop
↓
git pull
↓
quick-deploy.bat
↓
테스트
↓
빌드
↓
plugins/에 jar 복사
↓
start-dev.bat
↓
콘솔/인게임 검증
```

---

# 20. Git ignore

최소:

```text
.gradle/
build/
**/build/
.idea/
.vscode/
*.iml

dev-server/server/
logs/
crash-reports/
world/
world_nether/
world_the_end/

run/
*.db
*.sqlite
.env
```

외부 유료 플러그인 JAR은 저장소에 커밋하지 않는다.

예:

- ItemsAdder
- MythicMobs Premium
- ModelEngine
- 기타 라이선스 파일

---

# 21. GitHub 문서 구조

```text
docs/
├─ game-design-v1.0.md
├─ development-plan-v1.0.md
├─ PROJECT-STATE.md
├─ ARCHITECTURE.md
├─ MAP-FORMAT.md
└─ DEV/
    ├─ DEV-001.md
    ├─ DEV-002.md
    └─ ...
```

---

# 22. PROJECT-STATE.md

이 파일은 새 채팅 및 새 개발 세션에서 가장 먼저 확인하는 상태 문서다.

형식:

```markdown
# PROJECT STATE

## Current HEAD
<commit>

## Current milestone
Milestone A

## Last completed DEV
DEV-000

## Current DEV
DEV-001

## Next DEV
DEV-002

## Implemented
- ...

## Pending validation
- ...

## Known issues
- ...

## Environment
- Java 25
- Gradle 9.7.1
- Paper 26.2 build 123
```

DEV 완료 시 반드시 갱신한다.

---

# 23. DEV 문서 규칙

각 DEV는 다음 구조를 가진다.

```markdown
# DEV-XXX 제목

## Goal

## Scope

## Non-goals

## Existing code to inspect

## Implementation

## Tests

## Manual validation

## Completion criteria

## PROJECT-STATE update
```

---

# 24. 커밋 규칙

기본 형식:

```text
DEV-001 bootstrap Paper plugin project
DEV-008 add constrained map generator
DEV-026 implement objective conflict sets
```

보완:

```text
DEV-026.1 fix objective assignment duplicate edge case
```

문서:

```text
docs: update project state after DEV-026
```

가능하면 한 DEV를 하나의 논리적 커밋 또는 소수 커밋으로 유지한다.

---

# 25. Pull Request 규칙

혼자 개발하더라도 큰 DEV는 PR 사용을 권장한다.

PR 제목:

```text
DEV-026 Objective conflict sets
```

본문:

```text
## Summary

## Changed

## Tests

## Manual validation

## Known limitations
```

초기 빠른 개발 기간에는 작은 DEV를 `main`에 직접 병합할 수 있으나, Source of Truth 문서 갱신은 생략하지 않는다.

---

# 26. GitHub Actions

초기 CI를 구축한다.

파일:

```text
.github/workflows/build.yml
```

트리거:

```text
push
pull_request
```

실행:

```text
checkout
setup Java 25
./gradlew test
./gradlew build
```

유료 플러그인이 없어도 `core`와 plugin compile이 가능하도록 compileOnly 또는 optional adapter 구조를 사용한다.

CI에서 유료 plugin JAR을 요구하면 안 된다.

---

# 27. 외부 플러그인 의존성 처리

## 공개 Maven 저장소가 있는 경우

`compileOnly` 또는 API dependency 사용.

## 공개 저장소가 없는 유료 플러그인

선택:

1. 공식 API artifact가 있으면 API만 의존
2. 없으면 adapter를 reflection/soft dependency 방식으로 최소화
3. 로컬 proprietary jar를 Git에 올리지 않음
초기 DEV 전에 각 플러그인의 실제 API 제공 여부를 확인한다.

---

# 28. plugin.yml / paper-plugin.yml

초기에는 Paper 플러그인 메타데이터를 명확하게 유지한다.

예상:

```text
name: SpaceSurvival
main: com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin
version: 0.1.0-SNAPSHOT
api-version: '26.2'
```

외부 플러그인은 가능한 한 hard depend보다 soft depend를 선호한다.

단, Voice Chat이 게임의 필수 조건으로 확정된 운영 서버에서는 실행 시 필수 플러그인 검사를 수행한다.

---

# 29. 서버 플레이어 출력

플레이어가 보는 모든 메시지는 한국어를 기본으로 한다.

대상:

- 명령어
- 오류
- 경고
- 거부 이유
- 도움말
- HUD
- 회의
- 목표
- 브리핑

내부 로그 및 개발 오류는 영문 사용 가능.

텍스트는 코드 곳곳에 하드코딩하지 않고 message key 기반으로 관리한다.

```text
messages_ko.yml
```

향후 다국어 추가가 가능하도록 한다.

---

# 30. 로깅

콘솔 로그 prefix:

```text
[SpaceSurvival]
```

레벨:

```text
INFO
WARN
ERROR
DEBUG
```

게임 중 중요 상태 전이는 명확히 기록한다.

예:

```text
[SpaceSurvival] Game session started: session=...
[SpaceSurvival] Scenario selected: INTERNAL_SABOTAGE
[SpaceSurvival] Crisis level changed: WARNING -> CRISIS
```

비밀 목표 내용은 일반 운영 로그에서 과도하게 노출하지 않는다.

DEBUG 모드에서만 상세 출력 가능하도록 한다.

---

# 31. 명령어

초기 관리자 명령:

```text
/space status
/space start
/space stop
/space debug
/space player
/space map
/space scenario
/space objective
```

일반 플레이어 명령은 최소화한다.

게임 경험은 명령어 입력보다 UI/상호작용 중심으로 만든다.

---

# 32. 초기 Milestone

## Milestone A — Playable Foundation

범위:

```text
DEV-001 ~ DEV-019
```

목표:

- 서버 플러그인 정상 로딩
- 6명 참가
- GameSession
- 직업 선택
- 랜덤 모듈 맵
- 시설 상호작용
- 기본 공통 목표

---

## Milestone B — Core Game

범위:

```text
DEV-020 ~ DEV-031
```

목표:

- 자원
- 가공
- 개인 목표
- ConflictSet
- 사건
- 단순 사고 시나리오

이 단계에서 첫 6인 플레이테스트를 한다.

---

## Milestone C — Social Survival

범위:

```text
DEV-032 ~ DEV-044
```

목표:

- 회의
- 처분
- 조건부 PvP
- Voice Chat
- 내부 공작
- 감염

---

## Milestone D — MVP 1.0

범위:

```text
DEV-045 ~ DEV-052
```

목표:

- 사망 후 플레이
- PvE
- 귀환 최종 단계
- 결과 계산
- 승리자
- MVP

---

# 33. DEV 전체 계획

## Foundation

```text
DEV-001 Project Bootstrap
DEV-002 GameSession Core
DEV-003 PlayerState
DEV-004 Configuration System
```

## Map

```text
DEV-005 Sector/Room/Connection Model
DEV-006 Tile Registration
DEV-007 Connection & TP
DEV-008 Constrained Random Map Generator
DEV-009 Map Debug Tools
```

## Game Flow

```text
DEV-010 Lobby & Start Flow
DEV-011 Role Framework
DEV-012 Role Candidate Selection
DEV-013 Briefing & Initial Goal UI
DEV-014 Game Timer & Crisis
```

## Ship

```text
DEV-015 ShipState
DEV-016 Facility Framework
DEV-017 Engineering Facility
DEV-018 Medical Facility
DEV-019 Remaining Facilities v1
```

## Resource

```text
DEV-020 Resource Framework
DEV-021 ResourceNode Random Placement
DEV-022 Processing
DEV-023 ItemsAdder Integration
```

## Objective

```text
DEV-024 Objective Engine
DEV-025 Initial Objectives
DEV-026 ConflictSet
DEV-027 Objective Assignment
DEV-028 Secret Mission
```

## Event

```text
DEV-029 Event Engine
DEV-030 Small Event Set
DEV-031 Major Event Set
```

## Social

```text
DEV-032 Meeting System
DEV-033 Emergency Meeting
DEV-034 Sanction Voting
DEV-035 Sanction Execution
DEV-036 Conditional PvP
```

## Communication

```text
DEV-037 Voice Chat Integration
DEV-038 Radio System
DEV-039 Dead Communication
```

## Scenario

```text
DEV-040 Scenario Engine
DEV-041 Accident Scenario
DEV-042 Sabotage Scenario
DEV-043 Infection System
DEV-044 Infection Scenario
```

## Death/PvE

```text
DEV-045 Death State
DEV-046 Infected Player State
DEV-047 MythicMobs Integration
DEV-048 ModelEngine Integration (optional before MVP)
```

## Ending

```text
DEV-049 Return Objective
DEV-050 Final Hold
DEV-051 Result Evaluator
DEV-052 Winners & MVP
```

---

# 34. MVP 이후 번호 영역

확장 시 번호를 기능군으로 묶는다.

```text
DEV-060+ Objectives
DEV-070+ Roles
DEV-080+ Events
DEV-090+ Map Tiles
DEV-100+ Scenarios
DEV-110+ Endings
DEV-120+ Meta Progression
```

필요하면:

```text
DEV-031.1
DEV-031.2
```

형태의 보완 DEV를 허용한다.

---

# 35. DEV 완료 기준

DEV는 코드를 작성했다고 완료가 아니다.

다음 조건을 모두 만족해야 COMPLETE로 변경한다.

1. 목표 기능 구현
2. 관련 자동 테스트 통과
3. 기존 테스트 회귀 없음
4. 서버 빌드 성공
5. 필요한 인게임 검증 완료
6. 문서 갱신
7. `PROJECT-STATE.md` 갱신
8. GitHub `main` 반영

실서버 검증이 필요한데 아직 하지 못한 경우:

```text
IMPLEMENTED
```

로 표시한다.

검증까지 끝났을 때:

```text
COMPLETE
```

로 변경한다.

---

# 36. 개발 상태 용어

```text
PLANNED
IN_PROGRESS
IMPLEMENTED
VALIDATION_PENDING
COMPLETE
BLOCKED
```

권장 사용:

- PLANNED: 미착수
- IN_PROGRESS: 개발 중
- IMPLEMENTED: 코드 구현 완료
- VALIDATION_PENDING: 실서버 검증 대기
- COMPLETE: 최종 검증 완료
- BLOCKED: 외부 요인으로 진행 불가

---

# 37. 첫 커밋에 포함할 파일

저장소 생성 직후 첫 커밋 목표:

```text
README.md
.gitignore
.gitattributes

settings.gradle.kts
build.gradle.kts
gradle.properties
gradlew
gradlew.bat
gradle/

core/
paper-plugin/

docs/
  game-design-v1.0.md
  development-plan-v1.0.md
  PROJECT-STATE.md
  ARCHITECTURE.md

.github/
  workflows/
    build.yml
```

---

# 38. 첫 PROJECT-STATE

초기값:

```text
Current Milestone: Milestone A
Last Completed DEV: none
Current DEV: DEV-001
Next DEV: DEV-002
Status: PLANNED
```

---

# 39. GitHub 연동 운영 규칙

향후 ChatGPT 개발 세션에서는 GitHub가 연결되어 있다는 전제에서 다음 방식으로 작업한다.

매 개발 시작 시:

```text
1. GitHub main 최신 HEAD 조회
2. docs/PROJECT-STATE.md 확인
3. 관련 DEV 문서 확인
4. 관련 실제 코드 읽기
5. 구현
6. 테스트 코드 작성
7. GitHub 변경 반영
8. PROJECT-STATE 갱신
```

과거 채팅 내용만 보고 기존 구조를 추측하지 않는다.

---

# 40. 저장소 생성 후 즉시 할 작업

저장소:

```text
Hushkisses/space-social-survival
```

생성 후:

```bash
git clone https://github.com/Hushkisses/space-social-survival.git
cd space-social-survival
```

그 다음 DEV-001에서 프로젝트 bootstrap을 수행한다.

---

# 41. 현재 확정 기술스택 요약

```text
Server:
Paper 26.2 build 123

Runtime:
Java 25

Language:
Java

Build:
Gradle 9.7.1 Wrapper
Kotlin DSL

Testing:
JUnit 5

Voice:
Simple Voice Chat + API Adapter

Custom Items:
ItemsAdder

PvE:
MythicMobs

Models:
ModelEngine

Persistence:
Runtime memory
File persistence first
SQLite migration-ready

Repository:
GitHub
Hushkisses/space-social-survival

Main branch:
main

Architecture:
core + paper-plugin + integrations

Source of Truth:
GitHub main
docs/PROJECT-STATE.md
```

---

# 42. 개발 시작 조건

다음이 완료되면 DEV-001을 시작할 수 있다.

- GitHub 저장소 생성
- `main` 브랜치 존재
- 기획서 업로드
- 개발실행계획 업로드
- PROJECT-STATE 초기 생성
- Java 25 사용 가능
- Paper 26.2 build 123 개발 서버 준비 가능
- Gradle Wrapper 생성

이후 실제 구현은 DEV-001부터 순서대로 진행한다.
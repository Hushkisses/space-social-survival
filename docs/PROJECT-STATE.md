# PROJECT STATE

## Repository
- Hushkisses/space-social-survival
- Default branch: main

## Current HEAD
- Resolve the latest GitHub main HEAD at the start of every development session.
- Do not embed a self-referential HEAD value in this file.

## Current milestone
Milestone C — Social / Communication / Scenario

## Last completed DEV
- DEV-031 Major Event Set — COMPLETE
- DEV-030 Small Event Set — COMPLETE
- DEV-029 Event Engine — COMPLETE
- DEV-028 Secret Mission — COMPLETE
- DEV-027 Objective Assignment — COMPLETE
- DEV-026 ConflictSet — COMPLETE
- DEV-025 Initial Objectives — COMPLETE
- DEV-024 Objective Engine — COMPLETE
- DEV-023 ItemsAdder Integration — COMPLETE
- DEV-022 Processing — COMPLETE
- DEV-021 ResourceNode Random Placement — COMPLETE
- DEV-020 Resource Framework — COMPLETE
- DEV-019 Remaining Facilities v1 — COMPLETE
- DEV-018 Medical Facility — COMPLETE
- DEV-017 Engineering Facility — COMPLETE
- DEV-016 and earlier — COMPLETE

## Current DEV
- DEV-045 Death State
- Status: PLANNED

## Next DEV
- DEV-046 Infected Player State

## Batch implementation

### Social
- DEV-032 Meeting System
  - regular meeting session
  - bridge/infrastructure requirement
  - cooldown
- DEV-033 Emergency Meeting
  - body / infection / reactor / security / special-event reasons
  - bypasses regular infrastructure/cooldown restrictions
- DEV-034 Sanction Voting
  - no action / medical check / disarm / detain / access restriction / eject
  - one vote per participant; revote replaces vote
  - tie resolves to no action
- DEV-035 Sanction Execution
  - prerequisite-aware execution
  - sanction runtime state
  - disarm blocks PvP
  - detention blocks player movement
  - access-restriction/ejection authoritative flags await physical map/death systems
- DEV-036 Conditional PvP
  - default human PvP blocked
  - allowed by collapse, scenario, special event, confirmed infection
  - emergency security authorization path

### Communication
- DEV-037 Voice Chat Integration
  - optional Simple Voice Chat Bukkit bridge
  - dynamic plugin/API availability status
  - no startup dependency when voice chat is absent
- DEV-038 Radio System
  - radio ownership
  - long-range enable/disable
  - communication outage state
- DEV-039 Dead Communication
  - dead-to-living communication denied
  - dead-to-dead communication allowed
  - rule is in core and will bind to actual death state in DEV-045

### Scenario
- DEV-040 Scenario Engine
  - one active scenario
  - hidden initial hostile assignment
- DEV-041 Accident Scenario
  - zero initial hostile players
- DEV-042 Sabotage Scenario
  - configurable 0-2 initial hostile assignments
  - public briefing separate from hidden truth
- DEV-043 Infection System
  - NONE / EXPOSED / LATENT / SYMPTOMATIC / SUPPRESSED
  - progression, suppression, cure
  - basic test can return INCONCLUSIVE; precise test returns truthful infection result
  - positive test can mark confirmed infection for PvP policy
- DEV-044 Infection Scenario
  - begins as normal accident
  - seeded hidden initial infection event
  - public infection warning does not reveal infected identity

## Paper debug commands
- /space meeting ...
- /space sanction ...
- /space pvp ...
- /space radio ...
- /space voice
- /space scenario ...
- /space infection ...

## Verified
- DEV-001 through DEV-031 Windows integrated test/build/Paper validation SUCCESS
- DEV-032 through DEV-044 GitHub Actions full test/build SUCCESS
- DEV-032 through DEV-044 Windows integrated test/build/Paper validation SUCCESS

## Pending validation
- None for DEV-032 through DEV-044

## Scope notes
- Meeting cooldown is currently a development default of 3 minutes.
- Physical access restriction and airlock ejection effects require later physical map/death integration.
- Simple Voice Chat is optional. The bridge must not prevent startup when it is absent.
- Death communication policy is implemented in core; actual dead-player binding starts with DEV-045.
- Infection death/conversion into infected creatures remains DEV-046.
- Sabotage hostile count 0-2 follows the design baseline and remains balance-configurable later.
- Paper remains intentionally pinned to 26.2 build 123.

## Environment
- Java 25.0.4.1
- Gradle 9.7.1 Wrapper
- Paper 26.2 build 123
- Language: Java
- Build scripts: Gradle Kotlin DSL
- Tests: JUnit 5

## Source of Truth
1. GitHub main latest source
2. docs/PROJECT-STATE.md
3. docs/game-design-v1.0.md
4. docs/development-plan-v1.0.md

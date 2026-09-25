# PROJECT STATE

## Repository
- Hushkisses/space-social-survival
- Default branch: main

## Current HEAD
- Resolve the latest GitHub main HEAD at the start of every development session.
- Do not embed a self-referential HEAD value in this file.

## Current milestone
Milestone D — Death / PvE / Ending

## Last completed DEV
- DEV-044 Infection Scenario — COMPLETE
- DEV-043 Infection System — COMPLETE
- DEV-042 Sabotage Scenario — COMPLETE
- DEV-041 Accident Scenario — COMPLETE
- DEV-040 Scenario Engine — COMPLETE
- DEV-039 Dead Communication — COMPLETE
- DEV-038 Radio System — COMPLETE
- DEV-037 Voice Chat Integration — COMPLETE
- DEV-036 Conditional PvP — COMPLETE
- DEV-035 Sanction Execution — COMPLETE
- DEV-034 Sanction Voting — COMPLETE
- DEV-033 Emergency Meeting — COMPLETE
- DEV-032 Meeting System — COMPLETE
- DEV-031 and earlier — COMPLETE

## Current DEV
- DEV-045 through DEV-052 MVP Ending Batch
- Status: IMPLEMENTED / VALIDATION_PENDING
- Development branch: dev/DEV-045-052-mvp-ending-batch

## Next DEV
- MVP gameplay roadmap complete after this batch
- next work should be integration hardening, physical map/content, HUD/UX, balance, and playtest-driven extension tickets

## Batch implementation

### Death / infected post-death
- DEV-045 Death State
  - Bukkit player death bound to core PlayerState
  - idempotent DeathRecord
  - death cause and infected-at-death metadata
  - personal objectives remain unrevealed until final result
  - normal dead players respawn as spectators
- DEV-046 Infected Player State
  - infected death in infection scenario converts to INFECTED post-death form
  - infected goals: infect others / breach restricted zone / attack facility
  - living communication, repair, radio and ordinary door-use restrictions represented in core
  - infected post-death attacks are allowed by conditional PvP policy

### PvE integrations
- DEV-047 MythicMobs Integration
  - optional runtime/reflection bridge
  - Mythic mob spawn attempt by logical mob ID
  - vanilla mob fallback when plugin/API/mob definition is unavailable
- DEV-048 ModelEngine Integration
  - optional runtime/API availability bridge
  - no startup dependency when ModelEngine is absent
  - visual model assets remain content work, not game-state authority

### Ending
- DEV-049 Return Objective
  - SURVIVAL_SYSTEMS -> NAVIGATION -> RETURN_PREPARATION -> FINAL_HOLD
  - ship/facility health gate before navigation
  - return thresholds are development defaults and not final balance
- DEV-050 Final Hold
  - configurable hold duration from BalanceConfig
  - runtime scheduler
  - GameSession ACTIVE -> RETURN_PHASE -> FINISHED integration
  - explicit failure path
- DEV-051 Result Evaluator
  - common mission success gate
  - survival / base objective / secret mission / common contribution / scenario bonus score breakdown
  - survival is a score bonus, not mandatory winner condition
- DEV-052 Winners & MVP
  - common return success + base objective completion qualifies a winner
  - multiple winners supported
  - highest score among winners is MVP
  - tied MVPs supported
  - objective details are revealed only during final result output

## Paper debug commands
- /space death ...
- /space infected ...
- /space pve ...
- /space return ...
- /space result ...

## Verified
- DEV-001 through DEV-044 Windows integrated test/build/Paper validation SUCCESS

## Pending validation
- GitHub Actions full test/build for DEV-045 through DEV-052
- Windows integrated test/build
- Paper startup
- death/infected respawn smoke flow
- PvE fallback/plugin bridge smoke flow
- return/final hold smoke flow
- result/winner/MVP smoke flow

## Scope notes
- Return health thresholds are development defaults: power 50, oxygen 50, hull 50, reactor 40.
- Final hold duration uses existing returnHoldSeconds configuration; current project default is 240 seconds.
- Debug command can reset the ending runtime with a shorter hold duration for smoke testing only.
- MythicMobs IDs SpaceInfected / SpaceAlien are development integration IDs; missing definitions fall back to vanilla mobs.
- ModelEngine visual application requires actual model assets/content and remains optional before MVP.
- Result scoring uses design baseline development values: survival +3, base objective +5, secret mission +3, common contribution 0-3; scenario bonus is scenario-specific.
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

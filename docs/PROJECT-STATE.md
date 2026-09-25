# PROJECT STATE

## Repository
- Hushkisses/space-social-survival
- Default branch: main

## Current HEAD
- Resolve the latest GitHub main HEAD at the start of every development session.
- Do not embed a self-referential HEAD value in this file.

## Current milestone
Milestone B — Gameplay Systems

## Last completed DEV
- DEV-016 Facility Framework — COMPLETE
- DEV-015 ShipState — COMPLETE
- DEV-014 Game Timer & Crisis — COMPLETE
- DEV-013 Briefing & Initial Goal UI — COMPLETE
- DEV-012 Role Candidate Selection — COMPLETE
- DEV-011 Role Framework — COMPLETE
- DEV-010 Lobby & Start Flow — COMPLETE
- DEV-009 Map Debug Tools — COMPLETE
- DEV-008 Constrained Random Map Generator — COMPLETE
- DEV-007 Connection & TP — COMPLETE
- DEV-006 Tile Registration — COMPLETE
- DEV-005 Sector/Room/Connection Model — COMPLETE
- DEV-004 Configuration System — COMPLETE
- DEV-003 PlayerState — COMPLETE
- DEV-002 GameSession Core — COMPLETE
- DEV-001 Project Bootstrap — COMPLETE

## Current DEV
- DEV-017 through DEV-031 Gameplay Systems Batch
- Status: IMPLEMENTED / VALIDATION_PENDING
- Development branch: dev/DEV-017-031-gameplay-systems-batch

## Next DEV
- DEV-032 Meeting System

## Batch implementation
### Facilities
- DEV-017 Engineering Facility
  - ship diagnosis
  - bounded POWER/HULL/REACTOR adjustment
  - engineering availability enforcement
- DEV-018 Medical Facility
  - generic patient health
  - WOUNDED / CONTAMINATED / EXHAUSTED conditions
  - treatment and condition removal
  - infection-specific truth remains deferred to DEV-043
- DEV-019 Remaining Facilities v1
  - shared basic/advanced facility action catalog
  - role capability metadata
  - facility-state action access policy

### Resources
- DEV-020 Resource Framework
  - seven core resource types
  - personal/shared ResourceStore and ResourceLedger
- DEV-021 ResourceNode Random Placement
  - deterministic logical spawn-slot generator
  - facility-biased resource selection
- DEV-022 Processing
  - data-driven processing recipes
  - atomic input check/consume/output
- DEV-023 ItemsAdder Integration
  - optional reflection-based bridge
  - Paper ResourceItemProvider
  - vanilla fallback when ItemsAdder is absent

### Objectives
- DEV-024 Objective Engine
  - BASE and SECRET slots
  - progress/completion/failure/scoring
- DEV-025 Initial Objectives
  - 24 initial objective definitions from design
- DEV-026 ConflictSet
  - 8 conflict axes
  - deterministic 2-3 axis selection
- DEV-027 Objective Assignment
  - conflict-focused base objective distribution
- DEV-028 Secret Mission
  - maximum one additional secret mission per player

### Events
- DEV-029 Event Engine
  - event registry, effects, history and runtime flags
  - ship/facility/shared-resource effect adapters
- DEV-030 Small Event Set
  - 8 initial small events
- DEV-031 Major Event Set
  - reactor runaway
  - hull breach
  - mass infection signal
  - alien intrusion signal
  - total power failure

## Batch debug commands
- /space engineering ...
- /space medical ...
- /space actions <facilityId>
- /space resource ...
- /space objective ...
- /space event ...

## Verified
- DEV-001 through DEV-016 Windows test/build and Paper validation SUCCESS

## Pending validation
- GitHub Actions test/build for DEV-017 through DEV-031 batch
- Windows gradlew.bat test
- Windows gradlew.bat build
- Paper plugin startup
- integrated facility/resource/objective/event smoke flow

## Balance and scope notes
- Numerical repair, treatment, resource-node, recipe, and event effects are development defaults, not final balance values.
- Medical infection truth and infected-player state remain deferred to DEV-043/046.
- Small-event door/lighting/comms effects use event flags until their physical subsystems exist.
- ItemsAdder integration is optional and must not prevent startup when ItemsAdder is absent.
- Physical map coordinates/Structure Block placement remain intentionally deferred.
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

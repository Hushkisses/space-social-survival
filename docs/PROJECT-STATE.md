# PROJECT STATE

## Repository
- Hushkisses/space-social-survival
- Default branch: main

## Current HEAD
- Resolve the latest GitHub main HEAD at the start of every development session.
- Do not embed a self-referential HEAD value in this file.

## Current milestone
Milestone A — Playable Foundation

## Last completed DEV
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
- DEV-015 ShipState
- Status: IMPLEMENTED
- Development branch: dev/DEV-015-ship-state

## Next DEV
- DEV-016 Facility Framework

## Implemented
- DEV-015 ShipState with power, oxygen, hull, reactor percentages
- DEV-015 ShipStateSnapshot and ShipMetric
- DEV-015 ShipCrisisPressure
- DEV-015 live ShipState integration into DEV-014 crisis evaluation
- DEV-015 /space ship status|set|reset
- DEV-014 game timer and crisis foundation
- DEV-013 opening briefing and role selection UI
- DEV-012 role candidate generation and selection
- DEV-011 role framework and six-role catalog
- DEV-010 lobby and start flow

## ShipState design decision
- Initial authoritative ship metrics are power, oxygen, hull stability, and reactor stability.
- Values are integer percentages 0..100.
- Power, oxygen, and hull directly support the initial common-goal design.
- Reactor stability is included because reactor condition is explicitly a crisis driver.
- Crisis-pressure weights are isolated development defaults and are not final balance values.

## Crisis integration
- Healthy ship state slightly offsets time-based crisis pressure.
- Damaged/critical systems add increasing external pressure.
- GameRuntimeService now evaluates crisis stage using the live ShipState snapshot.
- Facility, infection, fire, creature, and event pressure remain future extensions.

## Verified
- DEV-001 through DEV-014 Windows test/build and Paper validation SUCCESS

## Pending validation
- DEV-015 Windows `gradlew.bat test`
- DEV-015 Windows `gradlew.bat build`
- DEV-015 Paper plugin load
- DEV-015 `space status`
- DEV-015 `space ship status`
- DEV-015 `space ship set power 10`
- DEV-015 runtime start/status and confirm crisis responds to degraded ship state
- DEV-015 `space ship reset`

## Known issues
- ShipState currently models global percentages only; facility-local state begins in DEV-016.
- Pressure weights are development defaults and require multiplayer balance testing.
- Runtime start remains an admin/debug command.
- Physical coordinates/Structure Block placement remain intentionally deferred.
- Paper startup reports build 123 is behind the current latest build; project remains intentionally pinned to Paper 26.2 build 123 until deliberately changed.

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

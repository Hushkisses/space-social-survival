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
- Status: PLANNED

## Next DEV
- DEV-016 Facility Framework

## Implemented
- DEV-014 game timer and crisis foundation
- DEV-013 opening briefing and role selection UI
- DEV-012 role candidate generation and selection
- DEV-011 role framework and six-role catalog
- DEV-010 lobby and start flow
- DEV-009 deterministic map debug tools
- DEV-008 constrained logical random map generation
- DEV-007 connection access states and Paper teleport adapter

## Verified
- DEV-001 through DEV-014 Windows test/build and Paper validation SUCCESS

## Pending validation
- None for DEV-014

## Known issues
- DEV-014 external crisis pressure is neutral until ShipState and later incident systems exist.
- Runtime start is currently an admin/debug command; automatic ACTIVE-phase binding will be added when the full game flow is wired.
- Initial problem/common-goal UI still uses baseline placeholder content until ShipState/Facility/Event/Objective systems provide live values.
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

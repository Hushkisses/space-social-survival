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
- DEV-016 Facility Framework
- Status: IMPLEMENTED
- Development branch: dev/DEV-016-facility-framework

## Next DEV
- DEV-017 Engineering Facility

## Implemented
- DEV-016 FacilityId / FacilityType / FacilityStatus
- DEV-016 FacilityDefinition / FacilityState / FacilityStateSnapshot
- DEV-016 FacilityRegistry
- DEV-016 six default facilities
- DEV-016 /space facility list|status|set|reset
- DEV-015 global ShipState and crisis-pressure integration
- DEV-014 game timer and crisis foundation
- DEV-013 opening briefing and role selection UI

## Facility framework decision
- Player-facing facility state remains simple: NORMAL / DAMAGED / OFFLINE / QUARANTINED.
- Static facility identity and mutable runtime state are separated.
- The six default facilities are bridge, engineering, medical, research, cargo, habitation.
- Facility-specific numerical sub-state and actions are deferred to later DEV tickets.

## Verified
- DEV-001 through DEV-015 Windows test/build and Paper validation SUCCESS

## Pending validation
- DEV-016 Windows `gradlew.bat test`
- DEV-016 Windows `gradlew.bat build`
- DEV-016 Paper plugin load
- DEV-016 `space status`
- DEV-016 `space facility list`
- DEV-016 `space facility set engineering damaged`
- DEV-016 `space facility status engineering`
- DEV-016 `space facility reset`

## Known issues
- Facility status does not yet change ShipState; engineering-specific integration begins in DEV-017.
- Medical behavior begins in DEV-018.
- Remaining facility-specific functions begin in DEV-019.
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

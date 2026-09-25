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
- DEV-010 Lobby & Start Flow
- Status: IMPLEMENTED
- Development branch: dev/DEV-010-lobby-start-flow

## Next DEV
- DEV-011 Role Framework

## Implemented
- DEV-010 LobbyService
- DEV-010 configured min/max player enforcement
- DEV-010 join / leave / reconnect / disconnect flow
- DEV-010 GameSession creation and WAITING -> PREPARING start transition
- DEV-010 /space lobby join|leave|status|start
- DEV-010 Paper join/quit connection listener
- DEV-010 ordinary-player command permission and admin-only start
- DEV-009 deterministic map debug tools
- DEV-008 constrained logical random map generation
- DEV-007 connection access states and Paper teleport adapter
- DEV-006 logical tile registration
- DEV-005 logical Sector/Room/Connection model
- DEV-004 validated configuration system
- DEV-003 PlayerState domain model
- DEV-002 GameSession lifecycle core

## Map build decision
- Physical ship modules will be built on a superflat/flat world for easier construction and predictable placement.
- Core map logic remains coordinate-independent.

## Verified
- DEV-001 through DEV-009 Windows test/build and Paper validation SUCCESS
- DEV-009 deterministic seed validation SUCCESS for seed 12345

## Pending validation
- DEV-010 Windows `gradlew.bat test`
- DEV-010 Windows `gradlew.bat build`
- DEV-010 Paper plugin load
- DEV-010 `space status`
- DEV-010 player `/space lobby join`
- DEV-010 `space lobby status`
- DEV-010 insufficient-player `space lobby start` rejection

## Known issues
- DEV-010 successful Paper-side match start with 6+ real players is covered by core tests but not required for single-developer smoke validation.
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

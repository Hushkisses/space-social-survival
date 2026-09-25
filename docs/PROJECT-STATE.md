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
- DEV-007 Connection & TP — COMPLETE
- DEV-006 Tile Registration — COMPLETE
- DEV-005 Sector/Room/Connection Model — COMPLETE
- DEV-004 Configuration System — COMPLETE
- DEV-003 PlayerState — COMPLETE
- DEV-002 GameSession Core — COMPLETE
- DEV-001 Project Bootstrap — COMPLETE

## Current DEV
- DEV-008 Constrained Random Map Generator
- Status: IMPLEMENTED
- Development branch: dev/DEV-008-random-map-generator

## Next DEV
- DEV-009 Map Debug Tools

## Implemented
- DEV-008 configurable generation constraints
- DEV-008 GeneratedMap graph model
- DEV-008 constrained random tile selection
- DEV-008 mandatory CORE tile inclusion
- DEV-008 connection-point capacity enforcement
- DEV-008 reachability / dead-end / core-distance validation
- DEV-008 deterministic seeded generation tests
- DEV-007 connection access states and Paper teleport adapter
- DEV-006 logical tile registration
- DEV-005 logical Sector/Room/Connection model
- DEV-004 validated configuration system
- DEV-003 PlayerState domain model
- DEV-002 GameSession lifecycle core

## Map build decision
- Physical ship modules will be built on a superflat/flat world for easier construction and predictable placement.
- DEV-008 remains coordinate-independent; physical flat-world placement is a later Paper-layer concern.

## Verified
- DEV-001 through DEV-007 Windows test/build and Paper validation SUCCESS

## Pending validation
- DEV-008 Windows `gradlew.bat test`
- DEV-008 Windows `gradlew.bat build`
- DEV-008 Paper smoke test: plugin load and `space status`

## Known issues
- DEV-008 currently generates logical topology only; physical coordinates and structure placement are intentionally deferred.
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

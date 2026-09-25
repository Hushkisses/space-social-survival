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
- DEV-005 Sector/Room/Connection Model — COMPLETE
- DEV-004 Configuration System — COMPLETE
- DEV-003 PlayerState — COMPLETE
- DEV-002 GameSession Core — COMPLETE
- DEV-001 Project Bootstrap — COMPLETE

## Current DEV
- DEV-006 Tile Registration
- Status: IMPLEMENTED
- Development branch: dev/DEV-006-tile-registration

## Next DEV
- DEV-007 Connection & TP

## Implemented
- DEV-006 TileId / TileCategory
- DEV-006 ConnectionPointId / ConnectionPointType
- DEV-006 ConnectionPointDefinition
- DEV-006 TileDefinition validation
- DEV-006 TileRegistry and category queries
- DEV-005 logical Sector/Room/Connection model
- DEV-004 validated configuration system
- DEV-003 PlayerState domain model
- DEV-002 GameSession lifecycle core
- Java 25 Gradle multi-module project
- Paper 26.2 build 123 API dependency
- JUnit 5 baseline
- Gradle 9.7.1 Wrapper
- GitHub Actions build workflow
- Windows quick-deploy and dev-server start scripts

## Verified
- DEV-001 Windows test/build and Paper validation SUCCESS
- DEV-002 Windows test/build and Paper validation SUCCESS
- DEV-003 Windows test/build and Paper validation SUCCESS
- DEV-004 Windows test/build and Paper validation SUCCESS
- DEV-005 Windows test/build and Paper validation SUCCESS

## Pending validation
- DEV-006 Windows `gradlew.bat test`
- DEV-006 Windows `gradlew.bat build`
- DEV-006 Paper smoke test: plugin load and `space status`

## Known issues
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

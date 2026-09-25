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
- DEV-011 Role Framework
- Status: IMPLEMENTED
- Development branch: dev/DEV-011-role-framework

## Next DEV
- DEV-012 Role Candidate Selection

## Implemented
- DEV-011 RoleId / RoleDefinition / RoleRegistry
- DEV-011 role passive metadata
- DEV-011 role capability metadata
- DEV-011 initial six-role catalog
- DEV-011 per-role duplicate-limit support
- DEV-010 lobby and start flow
- DEV-009 deterministic map debug tools
- DEV-008 constrained logical random map generation
- DEV-007 connection access states and Paper teleport adapter
- DEV-006 logical tile registration
- DEV-005 logical Sector/Room/Connection model
- DEV-004 validated configuration system
- DEV-003 PlayerState domain model
- DEV-002 GameSession lifecycle core

## Role framework decision
- Roles represent profession/specialty, not hidden alignment.
- Everyone keeps basic actions; role metadata grants passive advantages and advanced capabilities.
- The six initial default roles currently use maxCopies=2.
- Support for maxCopies=1 exists, but which roles should be singleton is deferred to DEV-012 balancing rather than hardcoded now.

## Map build decision
- Physical ship modules will be built on a superflat/flat world for easier construction and predictable placement.
- Core map logic remains coordinate-independent.

## Verified
- DEV-001 through DEV-010 Windows test/build and Paper validation SUCCESS

## Pending validation
- DEV-011 Windows `gradlew.bat test`
- DEV-011 Windows `gradlew.bat build`
- DEV-011 Paper plugin load
- DEV-011 `space status`

## Known issues
- Role bonuses/capabilities are metadata only until their corresponding facility/action systems are implemented.
- Role candidate generation and player role choice are intentionally deferred to DEV-012.
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

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
- DEV-013 Briefing & Initial Goal UI
- Status: IMPLEMENTED
- Development branch: dev/DEV-013-briefing-goal-ui

## Next DEV
- DEV-014 Game Timer & Crisis

## Implemented
- DEV-013 opening accident briefing inventory UI
- DEV-013 initial-problem and common-goal presentation
- DEV-013 three-candidate role selection inventory GUI
- DEV-013 click-to-select integration with RoleSelectionService
- DEV-013 automatic briefing UI open after role candidate preparation
- DEV-013 /space briefing and /space role gui developer reopen commands
- DEV-012 role candidate generation and selection
- DEV-011 role framework and six-role catalog
- DEV-010 lobby and start flow
- DEV-009 deterministic map debug tools
- DEV-008 constrained logical random map generation
- DEV-007 connection access states and Paper teleport adapter
- DEV-006 logical tile registration
- DEV-005 logical Sector/Room/Connection model
- DEV-004 validated configuration system
- DEV-003 PlayerState domain model
- DEV-002 GameSession lifecycle core

## Opening flow implementation note
- DEV-013 presents the public accident briefing, initial problem examples, common objective, and role selection.
- Private objective assignment is intentionally deferred to DEV-024+ Objective Engine.
- Starter equipment/resource distribution is intentionally deferred to DEV-020+ resource/item work.
- Initial problem values shown in DEV-013 are presentation placeholders from the design baseline, not live ShipState values.

## Role framework decision
- Roles represent profession/specialty, not hidden alignment.
- Everyone keeps basic actions; role metadata grants passive advantages and advanced capabilities.
- Initial defaults currently use maxCopies=2.
- Support for maxCopies=1 is active in the selection engine, but no role is hardcoded singleton yet.

## Map build decision
- Physical ship modules will be built on a superflat/flat world for easier construction and predictable placement.
- Core map logic remains coordinate-independent.

## Verified
- DEV-001 through DEV-012 Windows test/build and Paper validation SUCCESS

## Pending validation
- DEV-013 Windows `gradlew.bat test`
- DEV-013 Windows `gradlew.bat build`
- DEV-013 Paper plugin load
- DEV-013 `space status`
- DEV-013 player lobby join
- DEV-013 `space role prepare 12345`
- DEV-013 briefing GUI display
- DEV-013 click `직업 후보 확인`
- DEV-013 role candidate GUI displays exactly 3 choices
- DEV-013 candidate click selects role and closes GUI

## Known issues
- Initial problem/common-goal UI uses baseline placeholder content until ShipState/Facility/Event/Objective systems provide live values.
- Candidate generation can present a role that becomes full before a later player chooses it; selection is then rejected and the player must choose another offered role.
- Role bonuses/capabilities remain metadata until corresponding facility/action systems are implemented.
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

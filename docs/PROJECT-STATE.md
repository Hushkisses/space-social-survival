# PROJECT STATE

## Repository
- Hushkisses/space-social-survival
- Default branch: main

## Current HEAD
- Resolve the latest GitHub main HEAD at the start of every development session.
- Do not embed a self-referential HEAD value in this file.

## Current milestone
Playtest Integration

## Last completed
- DEV-001 through DEV-059 gameplay + vertical slice — COMPLETE

## Current batch
- PT-001 through PT-005 Playtest Integration
- Status: IMPLEMENTED / VALIDATION_PENDING
- Development branch: dev/PT-001-005-playtest-integration

## Included

### PT-001 Facility Interaction Layer
- six core facility modules receive an in-world LODESTONE console
- right-clicking a console resolves the logical FacilityId
- facility status and selected role capability are authoritative for access
- facility functions execute through existing core services rather than debug commands
- representative live actions include:
  - bridge status / meeting / return flow
  - engineering power, reactor, hull repair and diagnosis
  - medical treatment, condition removal, infection tests and suppression
  - research analysis
  - cargo inventory and processing
  - habitation supply / maintenance / locker

### PT-002 Facility GUI
- per-facility inventory GUI
- basic vs advanced action presentation
- unavailable actions show the concrete reason:
  - facility offline
  - quarantine
  - damaged advanced function
  - missing role capability
- action results are player-facing Korean messages
- GUI refreshes after execution

### PT-003 Structure/NBT Ship Module Loader
- expected directory:
  - plugins/SpaceSurvival/structures/
- file convention:
  - bridge.nbt
  - engineering.nbt
  - medical.nbt
  - research.nbt
  - cargo.nbt
  - habitation.nbt
  - corridor_1.nbt etc.
- Bukkit StructureManager loads raw NBT files
- NBT is placed before terminals/connection pads
- missing or failed NBT automatically falls back to the development room shell
- /space structure status reports NBT vs fallback per generated tile

### PT-004 Physical Connections / Doors
- every generated logical connection now has runtime ConnectionState
- states:
  - OPEN
  - LOCKED
  - POWER_REQUIRED
  - KEYCARD_REQUIRED
  - DISABLED
- portal-pad indicator:
  - gold = open
  - yellow = conditional access
  - red = blocked
- traversal checks existing core ConnectionAccessPolicy
- door_fault incidents can lock an actual generated connection
- engineering advanced repair or /space door repair restores faulted connections
- debug:
  - /space door list
  - /space door set <id> <state>
  - /space door repair
- keycard item binding remains PT-006 Functional Item Layer

### PT-005 Automatic Incident Director
- begins automatically when BRIEFING -> ACTIVE
- stops on match reset / plugin shutdown
- small incidents repeat on configurable random intervals
- first and second major incident windows are configurable
- default playtest schedule:
  - small: every 180-360 seconds
  - first major: 1080-1440 seconds
  - second major: 1920-2280 seconds
- infection scenario first major prefers mass infection
- sabotage major incidents prefer destructive system failures
- incident world consequences:
  - door fault locks physical connection
  - communication noise disrupts long-range radio
  - alien intrusion spawns PvE
  - mass infection can start hidden infection outbreak
- /space director status
- /space director force <small|large>

## Balance migration
New incident keys are added to legacy balance.yml automatically while preserving existing values.

## Verified
- DEV-001 through DEV-059 automated and Windows/Paper validation SUCCESS

## Pending validation
- GitHub Actions full test/build for PT-001 through PT-005
- Windows full test/build
- Paper startup
- /space match devstart
- right-click all six facility consoles
- role-gated advanced facility action
- NBT fallback / structure status
- door state blocking and repair
- Incident Director force small/large
- match reset and second devstart

## Next batch
- PT-006 Functional Item Layer
- PT-007 Resource Pickup / Physical Inventory Flow
- PT-008 Meeting & Sanction GUI
- PT-009 Match Telemetry
- PT-010 First 6-10 Player Playtest Build

## Reserved extension ranges
- DEV-060+ Objectives
- DEV-070+ Roles
- DEV-080+ Events
- DEV-090+ Map Tiles
- DEV-100+ Scenarios
- DEV-110+ Endings
- DEV-120+ Meta Progression

## Scope notes
- structure assets are optional; missing assets never block startup.
- current module cell geometry remains the development layout until final asset dimensions are locked.
- keycard-required connections are supported by policy but physical keycard item ownership is PT-006.
- facility resource-consuming actions use the existing ResourceLedger; physical resource acquisition is PT-007.
- all numeric repair/treatment/event timings remain playtest defaults.
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

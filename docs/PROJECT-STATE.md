# PROJECT STATE

## Repository
- Hushkisses/space-social-survival
- Default branch: main

## Current HEAD
- Resolve the latest GitHub main HEAD at the start of every development session.
- Do not embed a self-referential HEAD value in this file.

## Current milestone
First Multiplayer Playtest Build

## Last completed
- DEV-001 through DEV-059 gameplay + vertical slice — COMPLETE

## Dependency batch
- PT-001 through PT-005 Playtest Integration
- Status: VALIDATION_PENDING
- PR: #21
- GitHub Actions Build #507 SUCCESS
- Windows/Paper integrated validation remains required before merge

## Current batch
- PT-006 through PT-010 First Multiplayer Playtest Build
- Status: VALIDATION_PENDING
- Development branch: dev/PT-006-010-playtest-build
- This is a stacked branch based on dev/PT-001-005-playtest-integration until PR #21 is validated and merged.

## Included

### PT-006 Functional Item Layer
- role starter equipment:
  - Engineer: engineering multitool
  - Medic: medical scanner
  - Security: security keycard
  - Researcher: research scanner
  - Nav/Comms: long-range radio
  - Cargo/Maintenance: cargo scanner
- ItemsAdder custom IDs are preferred when available.
- Vanilla fallback items remain fully functional.
- all functional items carry plugin PDC identity.
- selected advanced facility actions require both:
  - the correct role capability
  - the physical role equipment in the player's inventory
- SECURITY_KEYCARD satisfies KEYCARD_REQUIRED physical connections.
- RADIO possession synchronizes with RadioRuntimeState.
- starter equipment is given automatically when all roles are selected and ACTIVE begins.
- /space item list
- /space item give <player> <type>

### PT-007 Resource Pickup / Physical Inventory Flow
- generated ship modules receive deterministic emergency supply barrels.
- physical resource items use the existing ItemsAdder-or-vanilla ResourceItemProvider plus PDC resource identity.
- players physically carry looted resources in Minecraft inventory.
- Cargo facility adds:
  - carried resource deposit to shared ResourceLedger.
- Habitation supply withdrawal gives a physical resource item.
- deposited physical resources are counted by telemetry.
- resource caches are regenerated per match seed.

### PT-008 Meeting & Sanction GUI
- bridge meeting action opens a sanction GUI for all online participants.
- vote flow:
  - choose sanction type
  - choose target when required
  - cast vote
  - auto-resolve when all online participants have voted
- available sanctions:
  - no action
  - medical check
  - disarm
  - detain
  - access restrict
  - eject
- execution still uses existing core SanctionExecutor and real prerequisites.
- security-authority sanctions require a living selected Security role.
- eject requires an airlock tile.
- existing /space meeting and /space sanction command paths delegate to the same GUI runtime.
- runtime is reset between matches.

### PT-009 Match Telemetry
- telemetry begins during match setup.
- tracked examples:
  - match start / activation / finish reason
  - initial incident count
  - resource cache count
  - automatic incidents by ID
  - facility action attempts/success/failure by ID
  - resource deposits
  - meetings and sanctions
  - player deaths and death causes
- JSON output directory:
  - plugins/SpaceSurvival/telemetry/
- telemetry is written on:
  - return success
  - return failure
  - match reset
  - server shutdown
  - manual save
- /space telemetry status
- /space telemetry save

### PT-010 First 6-10 Player Playtest Build
- /space playtest status
  - lobby readiness
  - player count
  - ItemsAdder status
  - voice bridge status
  - PvE backend
  - NBT directory
  - telemetry directory
  - current NBT/fallback count after generation
- /space playtest start [seed]
  - uses production minimum-player rule
  - no one-player bypass
- /space playtest reset
- one-player smoke testing remains available separately through:
  - /space match devstart [seed]
- /space status reports:
  - PT-006~010 First Multiplayer Playtest Build

## Playtest loop
1. 6~10 players join lobby.
2. operator checks /space playtest status.
3. operator runs /space playtest start [seed].
4. players choose roles.
5. starter role equipment is granted.
6. players loot physical supply caches.
7. players move resources to Cargo and deposit them.
8. facility actions require roles/equipment/resources.
9. automatic incidents pressure the ship.
10. meetings and sanctions run through GUI.
11. match results and playtest telemetry are saved.

## Verified
- DEV-001 through DEV-059 automated and Windows/Paper validation SUCCESS
- PT-001 through PT-005 GitHub Actions full test/build SUCCESS (Build #507)
- PT-006 through PT-010 GitHub Actions full test/build SUCCESS (Build #535)

## Pending validation
### PT-001 through PT-005 prerequisite
- Windows full test/build
- Paper startup
- facility console interaction
- NBT fallback
- physical doors
- incident director
- match reset/rematch

### PT-006 through PT-010
- Windows full test/build
- role starter equipment
- keycard-required connection access
- equipment-gated advanced facility action
- physical resource cache looting
- Cargo physical resource deposit
- meeting/sanction GUI
- telemetry JSON write/readability
- production /space playtest start with 6~10 players
- reset and second match

## Reserved extension ranges
- DEV-060+ Objectives
- DEV-070+ Roles
- DEV-080+ Events
- DEV-090+ Map Tiles
- DEV-100+ Scenarios
- DEV-110+ Endings
- DEV-120+ Meta Progression

## Scope notes
- balance values remain development/playtest defaults.
- ItemsAdder, Simple Voice Chat, MythicMobs and ModelEngine remain adapter-based optional integrations.
- missing custom content uses functional fallbacks instead of blocking startup.
- meeting sanctions now have UI, but physical detention cells/ejection cinematics are later content polish.
- telemetry is local JSON for first playtests; database aggregation is intentionally deferred.
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

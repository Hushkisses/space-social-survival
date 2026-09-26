# PROJECT STATE

## Repository
- Hushkisses/space-social-survival
- Default branch: main

## Current milestone
Playtest Hardening

## Last completed
- DEV-001 through DEV-059 gameplay + vertical slice — COMPLETE
- PT-001 through PT-005 Playtest Integration — COMPLETE
- PT-006 through PT-010 First Multiplayer Playtest Build — COMPLETE_FOR_PLAYTEST
- PR #21 and PR #22 merged to main

## Current batch
- PT-011 through PT-015 Playtest Hardening
- Status: IMPLEMENTED / VALIDATION_PENDING
- Development branch: dev/PT-011-015-playtest-hardening

## PT-011 Physical Gameplay -> Objective Progress
- existing objective assignments now progress from real gameplay actions.
- facility actions can advance matching objectives.
- Cargo deposits advance collection objectives by deposited amount.
- end-state objectives are finalized from actual survival/facility/infection/equipment state.
- final result evaluation runs objective finalization before scoring.
- objective progress is recorded to telemetry.

## PT-012 Physical Sanction Consequences
- successful meeting sanctions now produce in-world consequences.
- medical check performs a precise infection test.
- disarm drops carried weapons.
- detain teleports to Habitation and existing movement enforcement prevents movement.
- access restrict blocks module portal traversal and facility console use.
- eject moves to an airlock when available and changes the player to spectator.
- physical sanction actions are recorded to telemetry.

## PT-013 Death Loot / Recovery
- participants no longer keep inventory on death.
- functional role equipment and physical resource items remain in normal death drops.
- other players can recover those dropped items through normal Minecraft pickup.
- recoverable plugin-tagged stack/unit counts are recorded to telemetry.
- radio ownership remains inventory-driven and re-syncs after inventory changes.

## PT-014 Match Result GUI
- return success and failure automatically evaluate the match.
- objective end-state completion is finalized before scoring.
- all online participants receive a final results GUI.
- GUI displays:
  - common mission success/failure
  - winners
  - MVP(s)
  - each player's total score
  - score component breakdown
  - revealed base objective and secret mission
- tied winners/MVP remain supported.

## PT-015 Operator Preflight / Postmatch
- /space playtest preflight
  - configured player-count rule
  - participant online state
  - match idle state
  - ItemsAdder status/fallback
  - voice bridge
  - PvE backend
  - NBT directory
  - telemetry directory
  - final start-ready verdict
- /space playtest postmatch
  - common mission result
  - winner/MVP counts
  - score list
  - seed / player count / scenario
  - telemetry counters
  - telemetry file path

## Validation status
- GitHub Actions pending for PT-011 through PT-015.
- Windows/Paper integrated validation pending.
- empirical 6~10 player balance validation remains separate from code completion.

## Next likely phase
- real 6~10 player session and telemetry review.
- then choose expansion tickets based on actual playtest evidence rather than adding content blindly.
- reserved ranges remain:
  - DEV-060+ Objectives
  - DEV-070+ Roles
  - DEV-080+ Events
  - DEV-090+ Map Tiles
  - DEV-100+ Scenarios
  - DEV-110+ Endings
  - DEV-120+ Meta Progression

## Environment
- Java 25.0.4.1
- Gradle 9.7.1 Wrapper
- Paper 26.2 build 123
- PostgreSQL available later if aggregate telemetry becomes necessary

## Source of Truth
1. GitHub main latest source
2. docs/PROJECT-STATE.md
3. docs/game-design-v1.0.md
4. docs/development-plan-v1.0.md

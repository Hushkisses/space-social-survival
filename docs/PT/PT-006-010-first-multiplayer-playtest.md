# PT-006 through PT-010 First Multiplayer Playtest Build

## Goal

Convert the vertical slice into a build that can be handed to 6–10 players without relying on admin commands for the core gameplay loop.

## PT-006 Functional Items

Each selected role receives one physical functional tool when the match becomes ACTIVE.

- Engineer: engineering multitool
- Medic: medical scanner
- Security: security keycard
- Researcher: research scanner
- Nav/Comms: long-range radio
- Cargo/Maintenance: cargo scanner

Advanced facility functions can require the matching tool in addition to the role capability. Security keycards open KEYCARD_REQUIRED connections. Radio possession is synchronized to the communication runtime.

## PT-007 Physical Resources

The generated ship receives seeded emergency supply barrels. Resource stacks are plugin-tagged physical items.

Players:
1. loot items,
2. carry them physically,
3. reach Cargo,
4. deposit carried resources into the authoritative shared ResourceLedger.

Habitation supply actions withdraw shared resources back into physical inventory.

## PT-008 Meeting / Sanction GUI

A bridge meeting opens GUI voting to all online participants.

Flow:
1. sanction type,
2. target if needed,
3. vote,
4. automatic resolution after all online participants vote,
5. SanctionExecutor prerequisite check,
6. runtime enforcement.

Command-based meeting/sanction debug paths use the same runtime.

## PT-009 Telemetry

Every playtest produces local JSON data under:

`plugins/SpaceSurvival/telemetry/`

Tracked data includes incidents, facility actions, resources, meetings, sanctions, deaths, seed, player count, scenario and finish outcome.

## PT-010 Playtest Operator Surface

- `/space playtest status`
- `/space playtest start [seed]`
- `/space playtest reset`
- `/space telemetry status|save`
- `/space item list|give`

Production playtest start enforces the configured six-player minimum. The explicit `match devstart` command remains the separate one-player smoke-test path.

## Merge dependency

This branch is stacked on PT-001~005. PR #21 must receive Windows/Paper validation before this batch can be promoted to main.

# PT-001 through PT-005 Playtest Integration

## Goal

Replace command-centric smoke testing with normal Minecraft interactions and create the first playtestable ship loop.

## PT-001 Facility Interaction Layer
Core facility actions are exposed through physical consoles and execute against the existing authoritative systems.

## PT-002 Facility GUI
Facility actions are displayed in an inventory GUI. Role capability and facility state determine access.

## PT-003 Structure/NBT Module Loader
Place optional raw Structure NBT files from:

`plugins/SpaceSurvival/structures/<tileId>.nbt`

Missing/invalid assets fall back to the existing development room, so content production cannot block game development.

## PT-004 Physical Connections / Doors
Generated logical edges gain runtime connection states and visible portal indicators. Traversal uses the existing core access policy.

## PT-005 Incident Director
Incidents are scheduled automatically after match activation. Major events are scenario-aware and selected world effects are bound to physical systems.

## Validation

1. GitHub Actions full test/build.
2. Windows full test/build/deploy.
3. Start a one-player dev match.
4. Use facility consoles without debug commands.
5. Verify an advanced action is role-gated.
6. Check structure fallback report.
7. Lock/repair a physical connection and verify traversal.
8. Force small and large incidents.
9. Reset and start a second match without server restart.

## Deferred

- PT-006 real functional/keycard/radio/scanner items
- PT-007 physical resource pickups and storage loop
- PT-008 meeting/sanction GUI
- PT-009 telemetry
- PT-010 first 6-10 player playtest build

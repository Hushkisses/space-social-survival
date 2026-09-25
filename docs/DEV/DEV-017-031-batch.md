# DEV-017 through DEV-031 Gameplay Systems Batch

## Purpose

This batch intentionally groups fifteen tightly related DEV tickets so that the developer can validate one integrated gameplay slice instead of stopping after every ticket.

## Included DEV tickets

### Facility systems
- DEV-017 Engineering Facility
- DEV-018 Medical Facility
- DEV-019 Remaining Facilities v1

### Resource systems
- DEV-020 Resource Framework
- DEV-021 ResourceNode Random Placement
- DEV-022 Processing
- DEV-023 ItemsAdder Integration

### Objective systems
- DEV-024 Objective Engine
- DEV-025 Initial Objectives
- DEV-026 ConflictSet
- DEV-027 Objective Assignment
- DEV-028 Secret Mission

### Event systems
- DEV-029 Event Engine
- DEV-030 Small Event Set
- DEV-031 Major Event Set

## Architecture

The batch preserves the existing dependency direction:

- core contains gameplay rules and state.
- paper-plugin adapts commands, Bukkit players, inventories and runtime presentation.
- ItemsAdder integration is optional and uses a reflection bridge.
- no external plugin is authoritative for game state.

## Validation strategy

1. GitHub Actions runs the full JUnit suite and build.
2. CI failures are fixed on the batch branch before asking for Windows validation.
3. Windows then performs one integrated test/build/deploy.
4. Paper smoke validation exercises facilities, resources, objectives and events together.
5. Only after the integrated Windows validation do DEV-017 through DEV-031 become COMPLETE.

## Deferred systems

The following are deliberately not faked inside this batch:

- real infection truth / conversion: DEV-043 and DEV-046
- meeting/sanction systems: DEV-032 through DEV-036
- voice/radio: DEV-037 through DEV-039
- scenario selection: DEV-040 through DEV-044
- death/PvE: DEV-045 through DEV-048
- final return/result evaluation: DEV-049 through DEV-052

## Balance

All numeric values introduced by the batch are development defaults and should remain easy to tune. They are not final game-balance commitments.

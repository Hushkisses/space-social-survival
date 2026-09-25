# PROJECT STATE

## Repository
- Hushkisses/space-social-survival
- Default branch: main

## Current HEAD
- Resolve the latest GitHub main HEAD at the start of every development session.
- Do not embed a self-referential HEAD value in this file.

## Current milestone
Post-MVP Vertical Slice Integration

## Last completed DEV
- DEV-001 through DEV-052 MVP gameplay roadmap — COMPLETE

## Current DEV
- DEV-053 through DEV-059 Vertical Slice Integration
- Status: VALIDATION_PENDING
- Development branch: dev/DEV-053-059-vertical-slice

## Next DEV
- playtest-driven content, UX, and balance tickets
- reserved extension ranges remain:
  - DEV-060+ Objectives
  - DEV-070+ Roles
  - DEV-080+ Events
  - DEV-090+ Map Tiles
  - DEV-100+ Scenarios
  - DEV-110+ Endings
  - DEV-120+ Meta Progression

## Batch implementation

### DEV-053 Match Setup Pipeline
- /space match start [seed]
- OP-only /space match devstart [seed] for one-player smoke tests
- lobby -> PREPARING -> BRIEFING integration
- seeded role candidate preparation
- seeded objective conflict-set selection and base-objective assignment
- configurable secret-mission chance
- configurable scenario weights
- configurable initial small-event count
- all setup balance values loaded from balance.yml

### DEV-054 Automatic Match Activation
- all participants receive the briefing GUI
- role selection remains a player choice
- after the final participant confirms a role:
  - GameSession BRIEFING -> ACTIVE
  - GameRuntime timer starts automatically
  - server broadcasts match activation

### DEV-055 Production MVP Tile Catalog
- 20-tile initial pool from design:
  - six named core facilities
  - five corridors
  - three junctions
  - two airlocks
  - four auxiliary rooms
- core facilities: bridge / engineering / medical / research / cargo / habitation

### DEV-056 Physical Dev Ship
- dedicated flat world: space_ship_dev
- logical generated map rendered as independent 15x15 development modules
- generated connections rendered as gold pressure-plate teleport portals
- every logical connection is traversable in both directions
- players spawn in the bridge module
- final art/build assets can later replace room shells without changing core map logic

### DEV-057 Live HUD
- action-bar HUD while a match session exists
- power / oxygen / hull
- crisis stage
- current common return stage
- current physical room name
- does not reveal other-player locations, hidden objectives, resources, or scenario truth

### DEV-058 Live Briefing GUI
- public scenario briefing
- initial incident count
- generated module count
- role candidate selection
- explicitly preserves hidden scenario truth

### DEV-059 Playtest Harness / Rematch Reset
- /space match status
- /space match reset
- /space match bridge
- rematch resets:
  - lobby life states
  - role selections
  - resources
  - events
  - radio
  - death/post-death state
  - sanctions
  - objectives
  - scenario/infection/PvP runtime
  - timer / ending runtime
- lobby participants are retained for fast repeated tests
- production start still enforces 6-player minimum
- development start bypasses minimum only through explicit admin command

## Verified
- DEV-001 through DEV-052 Windows integrated test/build/Paper validation SUCCESS
- DEV-053 through DEV-059 GitHub Actions full test/build SUCCESS
- DEV-053.1 legacy balance.yml migration hotfix GitHub Actions test/build SUCCESS (Build run #483)

## Pending validation
- Windows full test/build
- Paper startup
- one-player /space match devstart flow
- physical ship generation and portal traversal
- briefing GUI -> role selection -> automatic ACTIVE transition
- live action-bar HUD
- /space match reset and second devstart in same server process

## Hotfixes
- DEV-053.1 Legacy balance configuration migration
  - existing server balance.yml files from DEV-052 and earlier do not contain the new map/objective/event/scenario keys.
  - missing keys are now inserted with current development defaults at startup.
  - existing user-configured values are preserved.
  - the migrated file is saved automatically.
  - manual deletion of balance.yml is not required.

## Scope notes
- physical ship rooms are development geometry, not final art.
- TP connections are intentional and match the modular-map design.
- scenario weights, map size, secret mission chance, and initial-event count are development defaults in balance.yml.
- final building/NBT assets can replace development room shells later.
- HUD intentionally exposes only the minimal public information defined by the design.
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

# PROJECT STATE

## Repository
- Hushkisses/space-social-survival
- Default branch: main

## Current main HEAD
- e02684ddc2eb8b7387462fb975dc154decb71337

## Parent validation dependency
- PR #23: PT-011~015 Playtest Hardening
- Branch: dev/PT-011-015-playtest-hardening
- Status: IMPLEMENTED / VALIDATION_PENDING
- GitHub Actions full test/build: SUCCESS (Build #585)
- Windows/Paper integrated validation: pending
- Do not mark PT-011~015 COMPLETE or merge it without that validation.

## Current milestone
Player Comprehension / Playability

## Last completed
- DEV-001 through DEV-059 gameplay + vertical slice — COMPLETE
- PT-001 through PT-005 Playtest Integration — COMPLETE
- PT-006 through PT-010 First Multiplayer Playtest Build — COMPLETE_FOR_PLAYTEST

## Current batch
- PX-005 through PX-007 World Readability
- Status: IMPLEMENTED / VALIDATION_PENDING
- Development branch: dev/PX-005-007-world-readability
- Base branch: dev/PX-001-004-player-comprehension
- PR #25
- Detail: docs/PX/PX-005-007-world-readability.md

## Parent PX dependency
- PR #24: PX-001~004 Player Comprehension Core
- Branch: dev/PX-001-004-player-comprehension
- Status: IMPLEMENTED / VALIDATION_PENDING
- Do not merge PX-005~007 ahead of the PX-001~004 dependency chain.

## PX-001 Game Start / Onboarding
- normal player flow no longer requires a start command:
  - online players are automatically enrolled into a physical waiting lobby
  - a visible green ready zone is provided in the ship world away from the generated ship area
  - when the configured minimum player count is met and every lobby player stands in the ready zone, a 10-second countdown starts
  - leaving the ready zone cancels the countdown
  - successful countdown automatically prepares the match with a random seed
- match setup now opens the role-selection GUI immediately instead of requiring an opening-briefing click-through
- public accident/shared-mission context is delivered by title/chat while the role GUI is opened
- an unselected player receives a protected hotbar Nether Star that reopens the role-selection GUI on right-click
- selecting a role removes the temporary selector and immediately opens private player information
- persistent right-click Crew PDA granted on activation
- PDA restored on participant respawn/reconnect and protected from normal dropping
- no hidden scenario truth or other-player private data exposed

## PX-002 Role Card / Personal Objective UX
- private PDA displays:
  - role and Korean capability explanations
  - required starter equipment / purpose / possession
  - base objective / progress
  - secret mission / progress
  - alive state
  - player-visible infection precision only
  - sanctions/restrictions
  - current room
  - recommended next action
- public-status and first-match help pages are accessible from the PDA

## PX-003 Common Objective HUD / Next Action Guidance
- added core PlayerGuidanceResolver
- PX-003.1 presentation polish:
  - persistent guidance moved from center ActionBar to a compact right-side scoreboard
  - scoreboard shows return stage, crisis, current room, one public priority, target facility and need
  - CRISIS/COLLAPSE uses a top boss bar for urgent ship-wide attention
  - ActionBar is reserved for short action-result feedback instead of permanent HUD text
  - scoreboard/bossbar are removed when the match runtime is absent
- public status GUI exposes broader ship/facility state without hidden actor/cause data

## PX-004 Facility Work UX
- existing FacilityActionExecutor remains authoritative
- facility GUI now exposes:
  - facility status/description
  - relevant ship metrics
  - current public problem
  - shared resources
  - carried relevant resources
  - role equipment possession
  - resource requirements
  - advanced-role requirement
  - explicit denial reasons
  - safe predicted effects

## PX-005 Incident Presentation / Response Feedback
- small incidents use Korean chat + ActionBar + short sound
- large incidents use title/subtitle + alarm + incident BossBar
- alerts include public target location, need and recommended action
- forced admin incidents use the same presentation/world-consequence path
- trackable public consequences announce stabilization when actually resolved
- hidden actor/infection/scenario truth remains hidden

## PX-006 Ship Navigation / Signage / Map
- room-specific visual accent markers
- floating room names
- portal destination labels
- existing gold/yellow/red connection-state language preserved
- Crew PDA ship map shows current room, priority target, route, adjacency and connection state
- no live player-position map

## PX-007 Item / Resource Usability
- functional role equipment has Korean purpose/role/use-location/loss lore
- physical resources have Korean purpose/use-location/Cargo-deposit lore
- resource pickup and functional-equipment pickup/drop feedback
- visible resource-cache labels
- Cargo deposit reports deposited amount, updated shared stock and objective delta when applicable
- ItemsAdder remains optional; vanilla fallback retains the same gameplay metadata

## Validation status
- PX-001~004 GitHub Actions full test/build: SUCCESS (Build #595, Build #597)
- PX-003.1 HUD presentation polish CI: SUCCESS (Build #599)
- PX-001.1 automatic lobby / immediate role-selection flow CI: SUCCESS (Build #613)
- PX-005~007 GitHub Actions Test/Build: SUCCESS (Build #639)
- PX-005~007 Windows/Paper integrated validation: pending
- PX-001~004 Windows/Paper integrated validation: pending
- PT-011~015 Windows/Paper integrated validation: still pending
- first blind 1~3 player 20-minute playability gate: not yet attempted
- real 6~10 player balance test remains blocked on the PX playability gate

## Next batch after validation
- PX-008 through PX-010 Social / Terminal-State / Playability Gate
- do not expand roles/objectives/events/scenarios for content volume before the PX exit gate

## Environment
- Java 25.0.4.1
- Gradle 9.7.1 Wrapper
- Paper 26.2 build 123
- PostgreSQL available later if aggregate telemetry becomes necessary

## Source of Truth
1. GitHub main latest source
2. docs/PROJECT-STATE.md
3. docs/PX/PX-001-010-player-experience-roadmap.md
4. docs/game-design-v1.0.md
5. docs/development-plan-v1.0.md
6. related actual source/tests

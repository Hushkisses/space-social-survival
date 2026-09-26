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
- PX-001 through PX-004 Player Comprehension Core
- Status: IMPLEMENTED / VALIDATION_PENDING
- Development branch: dev/PX-001-004-player-comprehension
- Base branch: dev/PT-011-015-playtest-hardening
- Detail: docs/PX/PX-001-004-player-comprehension.md

## PX-001 Game Start / Onboarding
- staged public accident/shared-mission/first-action briefing
- role selection remains the existing production flow
- role selection immediately opens private player information
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

## Validation status
- PX-001~004 GitHub Actions full test/build: SUCCESS (Build #595, Build #597)
- PX-003.1 HUD presentation polish CI: pending
- PX-001~004 Windows/Paper integrated validation: pending
- PT-011~015 Windows/Paper integrated validation: still pending
- first blind 1~3 player 20-minute playability gate: not yet attempted
- real 6~10 player balance test remains blocked on the PX playability gate

## Next batch after validation
- PX-005 through PX-007 World Readability
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

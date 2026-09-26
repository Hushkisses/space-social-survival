# PX-001~004 Player Comprehension Core

## Goal

Turn the existing technically connected vertical slice into a game that a first-time player can understand without developer narration.

This batch intentionally does not add new roles, objectives, incidents, scenarios, or hidden-information shortcuts.

## Base

- Parent branch: `dev/PT-011-015-playtest-hardening`
- PR #23 remains unmerged and PT-011~015 remains validation pending.
- PX work therefore preserves all PT-011~015 code without marking it COMPLETE.

## PX-001 Game Start / Onboarding

Implemented:

- staged opening briefing with:
  - public accident situation
  - shared return mission
  - public initial-problem count
  - current starting location
  - explicit first action sequence
  - role-selection entry point
- selecting a role immediately opens the player's private PDA view.
- when the match activates:
  - role starter equipment is granted through the existing StarterKitService.
  - a persistent Crew PDA item is granted.
  - the player receives the current highest-priority public action.
- Crew PDA is:
  - right-click accessible
  - protected from normal dropping
  - removed from death drops
  - restored on respawn/reconnect for participants

Hidden scenario truth and other players' private information are never displayed.

## PX-002 Role Card / Personal Objective UX

Crew PDA private view displays only the viewing player's information:

- public accident briefing and shared mission
- selected role and Korean gameplay descriptions of advanced capabilities
- required starter equipment, purpose, and current possession
- base personal objective and progress
- secret mission and progress when assigned
- alive/dead state
- infection information only at player-visible precision
  - exposed/latent infection is not revealed by the PDA
  - symptomatic or medically suppressed state may be shown
- current sanctions/restrictions
- current room
- current recommended team action

The PDA also provides:

- detailed public ship status
- compact first-match help

## PX-003 Common Objective HUD / Next Action Guidance

Added a pure-core `PlayerGuidanceResolver`.

The resolver derives one truthful public priority from authoritative core state:

1. unusable critical facility
2. survival metric below return requirement
3. other unusable/damaged facility
4. current return-stage action

The action bar now communicates:

- current return stage
- crisis stage
- current room
- highest-priority public problem
- target facility
- publicly knowable requirement

It does not expose hidden actor/cause information.

The PDA public-status page shows the broader state:

- ship metrics
- return stage
- crisis stage
- shared resource summary
- all facility statuses

## PX-004 Facility Work UX

Existing facility actions and execution services remain authoritative.

The facility GUI now shows:

- facility status and description
- relevant ship metrics
- whether this facility is the current public priority
- relevant shared resources
- relevant physical resources currently carried by the player
- required role equipment and possession state
- basic vs advanced action distinction
- Korean role/capability names
- known shared-resource requirements
- predicted public action effect
- concrete unavailable reason

No facility behavior was duplicated. The UI delegates execution to the existing `FacilityActionExecutor`.

## Automated tests

Added `PlayerGuidanceResolverTest` covering:

- low-power routing to Engineering with Power Cells
- unusable Engineering taking priority over metric repair
- Navigation stage routing to Bridge

GitHub Actions full test/build is required before this batch is considered implemented.

## Manual Windows/Paper validation

Do not mark COMPLETE until the representative Windows/Paper flow is verified:

1. start a development match
2. confirm the opening briefing explains accident/shared mission/first action
3. select a role and verify the private PDA opens
4. verify base objective and secret mission visibility is private and correct
5. verify starter equipment purpose/possession updates after activation
6. right-click PDA to reopen it
7. verify HUD shows stage/crisis/current room/one public priority/target facility/need
8. force or create a low-power or damaged-facility condition
9. verify HUD and PDA public state update without hidden-info leakage
10. open each facility console and verify:
    - status
    - relevant metrics
    - shared/carried resources
    - equipment requirement
    - resource requirement
    - denial reason
    - effect preview
11. execute representative basic and advanced actions and confirm existing behavior still works
12. die/respawn and verify PDA is not a recoverable loot item and is restored
13. reconnect and verify PDA remains available
14. verify PT-011~015 physical objective progress, sanctions, death loot, result GUI and operator tools still function

## Completion state

- Code: IMPLEMENTED after CI success
- Windows/Paper: VALIDATION_PENDING
- COMPLETE only after user-reported integration validation

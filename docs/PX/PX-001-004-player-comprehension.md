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

### PX-001.1 Physical waiting lobby / automatic start

- normal players are automatically enrolled into the lobby on join while no match is active.
- a physical waiting platform is rendered in the ship world at a safe negative-X location, outside the generated ship clear/build area.
- the waiting platform contains a visible green ready zone.
- when:
  - the configured minimum player count is met,
  - every lobby participant is online, and
  - every lobby participant stands inside the green ready zone,
  a 10-second ready countdown begins.
- leaving the ready zone cancels that countdown.
- a successful countdown automatically calls normal match preparation with a random seed.
- /space match devstart remains available only as an operator/development bypass; it is not required for the normal player flow.
- pre-match quit removes the player from the waiting lobby; quit after match creation keeps reconnect state.
- reset returns retained participants to the waiting lobby.

### Immediate role selection

- successful match preparation no longer forces the staged briefing GUI before role choice.
- public accident/shared-mission context is delivered by title/chat while the role-selection GUI is opened immediately.
- each unselected player receives a temporary protected Nether Star:
  - right-click reopens the role-selection GUI
  - normal dropping is blocked
  - it is removed as soon as a role is selected
- closing the role-selection GUI without choosing a role gives an ActionBar reminder explaining how to reopen it.
- selecting a role immediately opens the player's private PDA view.

The previous opening briefing GUI remains available as a support/debug surface, but is no longer a mandatory click-through in the normal start path.

When the match activates:
  - role starter equipment is granted through the existing StarterKitService.
  - a persistent Crew PDA item is granted.
  - the player receives the current highest-priority public action.
- Crew PDA is:
  - right-click accessible
  - protected from normal dropping
  - removed from death drops
  - restored on respawn/reconnect for participants

Hidden scenario truth and other players' private information are never displayed.

### PX-001.2 Mandatory opening recovery

Playtest feedback showed that the ship could begin close enough to the first return requirement that players could advance almost immediately.

The opening now applies a deliberately damaged ship state from `balance.yml` before initial incidents:

- power: 35
- oxygen: 40
- hull: 38
- reactor: 30

These are explicitly playability-phase tuning defaults, not final balance.

A small configurable shared emergency reserve is also seeded so random initial incidents cannot make the mandatory opening recovery impossible:

- Repair Parts: 4
- Power Cells: 2
- Fuel: 2
- Medical Supplies: 2

The default starting state does not satisfy `ReturnRequirements.developmentDefaults()`, so players must perform real recovery work before the first return-stage transition. No artificial time lock is used.

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

### PX-003.1 HUD presentation polish

The first Paper validation showed that a permanent center ActionBar was technically functional but not sufficiently readable during normal play.

The presentation layer is therefore split by information lifetime:

- **Right-side scoreboard — persistent player/action context**
  - current room
  - viewing player's base personal objective
  - personal-objective progress or completion/failure state
  - private indication that a secret mission exists, without exposing its content
  - one highest-priority public problem
  - target facility
  - publicly knowable requirement
  - return-stage and crisis text are intentionally omitted from the normal sidebar
- **Top boss bar — urgent ship-wide crisis only**
  - shown only during CRISIS/COLLAPSE
  - communicates the public priority and target facility
  - does not invent a countdown when no authoritative timed-event state exists
- **ActionBar — transient action feedback**
  - no longer carries the permanent HUD
  - representative facility action success/failure is shown briefly
- **PDA — detailed state**
  - retains the full public and private detail pages

The HUD does not expose hidden actor/cause information.

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

GitHub Actions full test/build SUCCESS (Build #595). HUD polish passed Build #599. Automatic lobby / immediate role-selection flow passed Build #613. Opening recovery / personal-objective HUD passed Build #740.

## Manual Windows/Paper validation

Do not mark COMPLETE until the representative Windows/Paper flow is verified:

1. start the server with no active match and confirm joining players are automatically moved into the physical waiting lobby
2. confirm the green ready zone is visually clear and the ActionBar reports lobby/ready counts
3. with the normal configured minimum met, move every lobby participant into the ready zone
4. confirm the 10-second boss-bar countdown starts, and confirm one player stepping out cancels it
5. complete the countdown and confirm the match starts automatically without a start command
6. confirm the role-selection GUI opens immediately for every participant
7. close the role-selection GUI without choosing; confirm the ActionBar explains reopening and the hotbar Nether Star reopens the GUI on right-click
8. select a role; confirm the temporary selector is removed and the private PDA opens
9. verify base objective and secret mission visibility is private and correct
10. verify starter equipment purpose/possession updates after activation
11. right-click PDA to reopen it
12. verify the right-side scoreboard shows current room, the player's base personal objective/progress, public urgent objective, target facility and need
13. if a secret mission exists, confirm the sidebar only indicates its existence while its actual content remains in the private PDA
14. confirm stage/crisis lines are absent from the normal sidebar and the center ActionBar is no longer permanently occupied
15. confirm a fresh match starts below the first return requirement and cannot immediately advance the return flow
16. restore the required ship metrics through normal facility actions and confirm the first return-stage transition only becomes available afterward
17. force or create a low-power or damaged-facility condition and verify the scoreboard/PDA update
18. force enough crisis pressure to reach CRISIS/COLLAPSE and verify a top boss bar appears without exposing hidden cause information
19. open each facility console and verify:
    - status
    - relevant metrics
    - shared/carried resources
    - equipment requirement
    - resource requirement
    - denial reason
    - effect preview
20. execute representative basic and advanced actions and confirm:
    - existing behavior still works
    - the action result appears as short-lived ActionBar feedback
21. die/respawn and verify PDA is not a recoverable loot item and is restored
22. reconnect and verify PDA remains available
23. run /space match reset and confirm retained participants return to the waiting lobby in Adventure mode
24. verify PT-011~015 physical objective progress, sanctions, death loot, result GUI and operator tools still function

## Completion state

- Code: IMPLEMENTED after CI success
- Windows/Paper: VALIDATION_PENDING
- COMPLETE only after user-reported integration validation

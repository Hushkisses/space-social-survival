# PX-001~010 Player Experience / Playability Roadmap

## 1. Why this phase exists

The project has reached a technically connected vertical slice:

- match setup and role selection
- random logical/physical ship generation
- facilities, resources, incidents and scenarios
- meetings and sanctions
- death / infected post-death
- return flow, winners and MVP
- functional role items
- physical resource pickup/deposit
- telemetry
- playtest operator tools

However, the current build is still difficult for a first-time player to understand without developer explanation.

The problem is no longer primarily missing backend systems. The problem is that existing systems are not yet translated into a clear player experience.

This phase therefore pauses feature/content expansion and focuses on making the existing game understandable, navigable and playable.

## 2. Product goal

Before inviting 6-10 players to a real full match, reach the following gate:

> Three first-time players should be able to enter the server, understand what they should do, navigate the ship, respond to incidents, use facilities, understand their role/personal objective, and continue playing for at least 20 minutes with no out-of-game explanation beyond joining the server.

Admin/debug commands may be used to force test conditions, but ordinary gameplay actions must not require admin intervention.

## 3. Development principle

Until the playability gate is reached:

- do not add more roles only to increase content volume.
- do not add more objectives only to increase content volume.
- do not add more events/scenarios only to increase content volume.
- do not spend significant effort on final balance.
- do not require final NBT art assets to continue development.
- preserve hidden information and social deduction uncertainty.
- all important player-facing output is Korean.
- keep core game state authoritative; GUI/HUD/items are presentation and interaction layers.
- preserve current logical-map coordinate independence.
- retain vanilla fallbacks for optional external plugins/content.

Reserved expansion ranges (DEV-060+, 070+, 080+, 090+, 100+, 110+, 120+) remain reserved until playtest evidence justifies expansion.

---

# PX-001 Game Start / Onboarding Flow

## Goal

A first-time player understands the situation and immediate next action within the first 30-60 seconds.

## Implement

- Replace the current single briefing experience with a short staged onboarding flow.
- Clearly show:
  - accident/public situation
  - common mission
  - role selection
  - personal role description
  - base personal objective
  - secret mission, if any
  - starting equipment and what it does
  - where the player currently is
  - what the first recommended action is
- Create an always-accessible personal information surface.
  - preferred form: crew PDA / personal menu item or equivalent GUI.
  - must not require remembering a debug/admin command.
- Provide a compact first-match help page:
  - move through gold/yellow/red connection pads
  - facility consoles
  - resource caches
  - meetings
  - death communication rule
  - role equipment
- Do not reveal hidden scenario truth, other players' private objectives, infection identity, or other forbidden information.

## Acceptance criteria

A new player can answer without external explanation:

1. What is happening?
2. What is my role?
3. What is my personal objective?
4. What is the shared objective?
5. What item did I receive and why?
6. What should I do next?

---

# PX-002 Role Card / Personal Objective UX

## Goal

Private information becomes usable gameplay information rather than data visible only through commands.

## Implement

Create a persistent personal-status GUI containing:

- role name
- role specialty
- advanced actions the role can perform
- required role equipment
- equipment currently possessed / lost
- base personal objective
- base objective progress
- secret mission, when assigned
- secret mission progress
- survival/alive state
- infection information only to the precision the game design allows
- current sanctions/restrictions affecting the player

Role text should explain gameplay value, not only internal capability enum names.

Example:

Engineer
- Repairs ship systems efficiently.
- Can perform precise engineering diagnosis.
- Can redistribute power.
- Requires Engineer Multitool for advanced engineering actions.

## Acceptance criteria

A player can inspect their private information at any time without admin help and without leaking it to other players.

---

# PX-003 Common Objective HUD / Next Action Guidance

## Goal

The game continuously answers: "What should our team do now?"

## Implement

Upgrade the current public HUD from raw values into actionable state.

Public display should communicate:

- current return stage
- critical ship metrics
- crisis stage
- current room
- highest-priority public problem
- target facility for that problem
- required resource/action when this can be truthfully known
- optional countdown for timed critical events

Example:

POWER CRITICAL — 43%
Go to Engineering.
Need: Power Cell
Current stage: Restore survival systems

The system must distinguish:

- objective state
- incident response
- general ship status

Do not expose hidden cause/actor information.

## Priority rule

When multiple problems exist, show one high-priority next action and provide a separate detailed public-status GUI for the rest.

## Acceptance criteria

During a forced ship problem, a first-time player can identify the required facility and response without an admin explaining the system.

---

# PX-004 Facility Work UX

## Goal

Facility interaction feels like doing a job, not clicking a debug menu.

## Implement

Improve each facility GUI to show:

- facility name/status
- relevant ship metrics
- current active problem
- resource requirements
- shared resource availability
- personal carried relevant resources
- basic actions
- advanced role actions
- required equipment
- clear reason when unavailable
- predicted effect where safe to reveal

Examples:

Engineering
- Power: 43%
- Reactor: 71%
- Shared Power Cells: 2
- Carried Power Cells: 1
- [Insert Power Cell]
- [Precise Diagnosis] Engineer + Multitool
- [Redistribute Power] Engineer + Multitool

Medical
- target/player selector where necessary
- injury/infection test precision clearly differentiated
- medical supply requirements

Cargo
- obvious physical-item deposit workflow
- shared stock summary
- processing recipes with input/output preview

Bridge
- return stage
- meeting status
- navigation/return controls
- public ship summary

## Acceptance criteria

A player can infer why an action is unavailable and what they need to make it available.

---

# PX-005 Incident Presentation / Response Feedback

## Goal

An incident produces understandable urgency and a playable response loop.

## Implement

For incidents, add presentation proportional to severity:

Small:
- chat/actionbar alert
- short sound
- affected system/facility
- response hint

Major:
- title/subtitle or strong equivalent
- alarm sound
- bossbar/countdown when timed
- public affected systems
- recommended facility/action
- persistent HUD priority until stabilized

Resolution:
- clearly announce that the problem was resolved/stabilized
- remove obsolete warnings
- show remaining consequences if any

Do not expose:
- hidden saboteur identity
- hidden infection identity
- secret scenario truth

## Acceptance criteria

A new player can experience one forced small incident and one forced major incident and understand:
- something happened,
- where to go,
- what resource/action is relevant,
- when it has been resolved.

---

# PX-006 Ship Navigation / Signage / Map

## Goal

Players can build a mental map of the ship and intentionally reach facilities.

## Implement

Even while final NBT art is unavailable:

- give every module a strong visual identity.
- facility-specific accent blocks/signs.
- room name at entry.
- connection destination indication where design permits.
- central ship-map GUI or map board.
- show room graph / discovered rooms without showing live player positions.
- provide clear return-to-Bridge orientation.
- visually distinguish:
  - core facility
  - corridor
  - junction
  - airlock
  - auxiliary room
- connection state color must remain obvious:
  - open
  - conditional
  - blocked

No global live player-position map.

## Acceptance criteria

A new player starting at Bridge can find Engineering and Cargo using only in-game information.

---

# PX-007 Item / Resource Usability

## Goal

Players understand items by looking at them and receiving normal gameplay feedback.

## Implement

For all functional and resource items:

- Korean display name
- concise lore explaining purpose
- role/equipment requirement if relevant
- where the item is normally used
- pickup feedback for important items
- loss/drop feedback for unique role equipment
- clear distinction between:
  - physical carried resource
  - shared stored resource
  - functional equipment

Improve resource-cache readability:
- cache label
- loot significance
- avoid visually confusing fallback items

Consider quick-deposit feedback at Cargo:
- what was deposited
- updated shared stock
- objective progress caused by deposit

## Acceptance criteria

A first-time player can explain the purpose of a Power Cell, Repair Parts, their role tool, and a Security Keycard without external documentation.

---

# PX-008 Meeting UX

## Goal

A meeting feels like a social-deduction phase rather than a sequence of inventory screens.

## Implement

- clear meeting start transition.
- meeting type/reason.
- discussion countdown.
- show participant list.
- show voting phase separately from discussion phase.
- visible vote completion count, not vote content.
- target selection.
- confirmation before severe sanctions when useful.
- result presentation.
- transition back to gameplay.
- preserve sanction execution prerequisites.
- communications failures must interact with meeting availability according to existing rules.

Do not magically execute actions that existing design says require facilities/roles/prerequisites.

## Acceptance criteria

Players understand:
1. a meeting has started,
2. when discussion ends,
3. when voting begins,
4. whether their vote was recorded,
5. what sanction won,
6. whether execution succeeded.

---

# PX-009 Death / Detention / Ejection UX

## Goal

Terminal states are understandable and do not create rule confusion.

## Implement

Death:
- clear death state feedback.
- explicitly communicate living/dead communication restrictions.
- normal dead -> spectator/dead-state experience.
- infected dead -> infected post-death gameplay explanation.
- dropped equipment/resources remain recoverable.

Detention:
- clear reason and duration/state.
- detention area presentation.
- what actions remain available.

Access restriction:
- clear feedback when blocked.

Ejection:
- strong transition/feedback.
- move to airlock/ejection presentation.
- spectator/dead-state behavior consistent with design.

## Acceptance criteria

A player who is killed, detained, access-restricted, or ejected understands what happened and what they are still allowed to do.

---

# PX-010 1-3 Player End-to-End Playability Harness

## Goal

Prove the game is understandable before recruiting a full 6-10 player group.

## Implement

Create a non-production internal playability mode that can exercise the real loop with 1-3 players.

It may:
- bypass minimum players explicitly.
- provide operator forcing for scenario/incidents.
- accelerate timers.
- provide test resources.

It must not:
- change production balance/config defaults.
- bypass ordinary player interaction once a test starts.
- replace facility/resource/objective gameplay with admin commands.

Add an operator checklist command/report that validates:

- onboarding shown
- role selected
- private objective available
- starter equipment granted
- ship/navigation usable
- resource cache populated
- facility console usable
- automatic/forced incident response
- meeting flow
- death/sanction transition
- return/result flow
- telemetry saved

## Exit gate for PX phase

Do not move to the first real 6-10 player full-match balance test until:

1. A first-time player identifies their next action within 60 seconds.
2. They can find Engineering and Cargo without external directions.
3. They can explain their role tool and at least two resources.
4. A forced small incident is resolved using only in-game cues.
5. A forced major incident produces an understandable response loop.
6. A meeting can be completed without admin explanation.
7. A death/sanction state is understandable.
8. 1-3 players can remain in normal gameplay for 20 minutes without an admin intervening to explain mechanics.
9. Match reset/rematch remains stable.
10. No critical gameplay blocker is present.

Only after this gate:
- run the first real 6-10 player full match,
- review telemetry and qualitative feedback,
- then select DEV-060+ / 070+ / 080+ / other expansion work from evidence.

---

# Recommended implementation batches

## Batch A — PX-001~004: Player Comprehension Core

Implement together:
- onboarding
- persistent personal role/objective GUI
- actionable common-objective HUD
- facility GUI information architecture

This is the highest-priority batch.

## Batch B — PX-005~007: World Readability

Implement together:
- incident presentation
- ship navigation/signage/map
- item/resource usability

## Batch C — PX-008~010: Social / Terminal-State / Playability Gate

Implement together:
- meeting UX
- death/detention/ejection UX
- 1-3 player end-to-end playability harness

---

# Validation policy

For every batch:

1. Read latest GitHub source first.
2. Preserve existing architecture and services.
3. Add automated tests where rules can be tested without Paper.
4. Run GitHub Actions full test/build.
5. Do not mark COMPLETE until Windows/Paper validation is reported by the user.
6. Record status in docs/PROJECT-STATE.md.
7. Keep player-facing output Korean.
8. Use existing debug/operator commands only for validation, not as the normal gameplay path.

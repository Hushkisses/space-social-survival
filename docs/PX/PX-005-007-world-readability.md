# PX-005~007 World Readability

## Goal

Make the existing game readable without adding content volume. This batch focuses on incident response, ship navigation, and item/resource comprehension while keeping core state authoritative and preserving hidden information.

## PX-005 Incident Presentation / Response Feedback

Implemented:

- Small incidents now present as:
  - Korean ship alert chat
  - short ActionBar response cue
  - short alert sound
  - affected area/facility
  - recommended response
- Major incidents now present as:
  - strong title/subtitle
  - alarm sound
  - temporary top boss bar
  - affected area/facility
  - recommended response
- Major presentation does not invent an authoritative event countdown where none exists.
- Existing common-objective HUD remains authoritative for persistent next-action guidance.
- Detectable resolution conditions announce `안정화 완료` and remove the tracked incident state.
- Presentation never exposes hidden saboteur identity, hidden infection identity, or secret scenario truth.

Representative response mapping covers:
- reactor runaway
- total power failure
- hull breach
- mass infection
- alien intrusion
- door fault
- communications noise
- cargo damage
- medical contamination
- local oxygen drop
- small fire
- power-cell depletion

## PX-005.1 Playtest correction — concrete recovery actions

First Paper validation exposed two comprehension/blocker issues:

- guidance used vague phrases such as `시설 점검` even when no such GUI action existed.
- oxygen loss had no explicit normal-play recovery action.

Corrections:

- damaged Cargo now exposes `화물실 설비 복구` in its facility GUI.
  - cost: shared Repair Parts x1
  - success returns Cargo to NORMAL.
- damaged/contaminated Medical now exposes `의료실 오염 제거`.
  - cost: shared Medical Supplies x1
  - success returns Medical to NORMAL and clears the contamination flag.
- Engineering now exposes `산소 계통 복구`.
  - cost: shared Repair Parts x1
  - success restores oxygen by 15.
- Player guidance and incident hints now name the exact action button and exact required resource instead of telling the player to merely inspect a facility.

## PX-006 Ship Navigation / Signage / Map

Implemented:

- Every generated module receives a floating room label.
- Visual label accent follows room category:
  - core
  - corridor
  - junction
  - airlock
  - auxiliary
- Every physical connection pad receives a floating destination label.
- Existing connection pad block colors remain authoritative:
  - gold/open
  - yellow/conditional
  - red/blocked
- Entering a different module gives current-room navigation feedback and a subtle sound.
- A shared route-guidance service calculates a player-usable shortest path toward the current public target facility.
- The right-side HUD continuously shows the next room on that route.
- Room-entry feedback shows current room, target facility and immediate next room.
- If no fully traversable route exists, the route can still show the topological path while clearly flagging blocked segments.
- Crew PDA now includes a ship-map page.
- PDA map shows:
  - all generated rooms
  - room category
  - current room
  - graph distance from Bridge
  - directly connected rooms
  - current connection state for each edge
  - recommended full route from the player's current room to the current public target facility
  - warning when that route contains a currently unavailable connection
- PDA map does not show live player positions.

## PX-007 Item / Resource Usability

Implemented:

- Physical resources now always contain Korean gameplay lore:
  - purpose
  - normal use location
  - physical-carried-resource identity
  - Cargo deposit explanation
- Functional role equipment now always contains:
  - purpose
  - normal use location
  - functional-equipment identity
  - loss/theft warning
- Important pickup feedback:
  - resource name + normal use location
  - functional equipment name + normal use location
- Dropping functional role equipment produces an explicit loss warning.
- Generated emergency resource caches receive a visible floating label.
- Cargo deposit feedback now reports:
  - deposited total
  - deposited resource breakdown
  - resulting shared quantity for those resources
  - personal-objective progress delta when the deposit advanced an objective
- ItemsAdder remains optional:
  - custom assets are used when available
  - vanilla fallback items retain all gameplay metadata and Korean lore

## Automated validation

- GitHub Actions Build #667: SUCCESS
- Playtest correction Build #707: SUCCESS
- Test: SUCCESS
- Build: SUCCESS

## Windows / Paper validation checklist

Do not mark this batch COMPLETE until representative integrated validation is reported.

### PX-005

1. Start a one-player development match with `/space match devstart <seed>`.
2. Force a small incident.
3. Confirm:
   - chat alert
   - ActionBar cue
   - sound
   - affected area
   - recommended response
4. Force a major incident.
5. Confirm:
   - title/subtitle
   - alarm
   - temporary boss bar
   - HUD remains readable
6. Resolve a supported incident such as door fault or communications noise and confirm `안정화 완료` feedback.
7. Force Cargo damage and confirm the HUD/incident text says `화물실 설비 복구`, not vague facility inspection.
8. Confirm Cargo GUI exposes that action and requires Repair Parts x1.
9. Force Medical contamination and confirm `의료실 오염 제거` is available with Medical Supplies x1.
10. Lower oxygen and confirm Engineering exposes `산소 계통 복구` with Repair Parts x1.
11. Confirm no hidden scenario actor/identity is revealed.

### PX-006

1. Walk through multiple generated rooms.
2. Confirm room labels are readable and do not obstruct interaction.
3. Confirm portal pads display destination room names.
4. Confirm entering a new room gives brief current-area feedback.
5. Open Crew PDA -> 함선 지도.
6. Confirm:
   - current room indication
   - room categories
   - Bridge distance
   - adjacency
   - open/conditional/blocked connection state
7. Starting at Bridge, locate Engineering and Cargo using only in-game guidance.
8. While standing in a corridor/junction, confirm the HUD displays the next room toward the current target facility.
9. Open PDA -> 함선 지도 and confirm the recommended route lists the room sequence.
10. Lock or disable a route connection and confirm blocked-route feedback is understandable.

### PX-007

1. Open a generated emergency resource cache.
2. Confirm the cache has a visible label.
3. Inspect Power Cell, Repair Parts, Medical Supplies, and at least one special resource.
4. Confirm each item explains purpose, use location, and physical-resource status.
5. Pick up resources and verify pickup feedback.
6. Inspect the current role starter tool and confirm its purpose/use location.
7. Drop that role tool and verify the loss warning.
8. Deposit mixed resources at Cargo.
9. Confirm the result reports deposit breakdown, new shared quantity, and objective progress when applicable.

## Completion state

- Code: IMPLEMENTED
- GitHub Actions: SUCCESS
- Windows/Paper integrated validation: PENDING
- COMPLETE only after user-reported integrated validation.

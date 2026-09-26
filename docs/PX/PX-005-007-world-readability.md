# PX-005~007 World Readability

## Goal

Make the existing game readable through in-world cues instead of requiring the developer to explain incidents, navigation, or items.

This batch does not add new roles, objectives, scenarios, or balance content.

## Base / dependency

- Parent branch: `dev/PX-001-004-player-comprehension`
- PR #24 remains unmerged / validation pending.
- Development branch: `dev/PX-005-007-world-readability`
- PR #25: PX-005~007 World Readability

## PX-005 Incident Presentation / Response Feedback

Implemented:

- automatic and forced incidents now use one player-facing presentation path.
- small incidents:
  - Korean chat alert
  - short ActionBar response summary
  - short alert sound
  - public response location
  - publicly knowable required resource/equipment
  - recommended action
- large incidents:
  - title/subtitle
  - stronger alarm sound
  - dedicated incident BossBar
  - public response location / need / action
- trackable incident consequences are monitored once per second.
- when a public consequence is actually stabilized:
  - incident state is cleared where appropriate
  - obsolete large-event BossBar is removed
  - players receive a Korean stabilization message and confirmation sound.
- forced `/space event trigger` and `/space event random` now go through the same world consequence + presentation path as automatic incidents.
- presentation does not reveal hidden saboteur identity, hidden infection identity, or secret scenario truth.

Known design limitation preserved from existing gameplay:
- some legacy event flags/consequences do not yet have a normal player action that can clear them. The presentation layer does not invent a fake resolution for those incidents.

## PX-006 Ship Navigation / Signage / Map

Implemented:

### Physical room identity

Every generated module receives:

- room-specific accent markers
- floating room name
- persistent visual distinction between:
  - Bridge
  - Engineering
  - Medical
  - Research
  - Cargo
  - Habitation
  - corridor
  - junction
  - airlock
  - auxiliary rooms

This identity layer is applied even when an NBT module structure exists.

### Portal destination signage

Every physical connection pad now has a floating destination label:

- `→ 기관실`
- `→ 화물실`
- etc.

Existing connection-state block language remains authoritative:

- gold = open
- yellow = conditional
- red = blocked

### PDA ship map

The Crew PDA now contains a **함선 지도** button.

The map GUI shows:

- current room
- current highest-priority target room
- recommended shortest topological route
- all generated rooms
- room category
- direct neighboring rooms
- current connection state for each adjacency
- current-room marker
- priority-target marker

The map intentionally does not display other players' live positions.

## PX-007 Item / Resource Usability

Implemented:

### Functional equipment

Role equipment now always has Korean player-facing metadata even when the physical base item is supplied by ItemsAdder:

- Korean display name
- gameplay purpose
- responsible role
- main use location
- explanation that the physical item is required for advanced actions
- loss/recovery warning

Starter-kit grant also explains purpose and main use location.

Functional equipment pickup/drop now produces short ActionBar feedback.

### Physical resources

Every resource item now shows:

- Korean name
- `물리 자원 · 현재 소지 중`
- gameplay purpose
- main use facility
- explicit instruction that Cargo deposit converts it into shared stock

Important resource pickup shows an ActionBar message with:

- resource name
- quantity
- reminder that it is still a physical carried resource
- reminder to deposit it at Cargo

ItemsAdder custom IDs remain optional. When ItemsAdder is unavailable, the existing vanilla fallback items retain the same Korean metadata and gameplay identity.

### Resource caches

Generated emergency resource caches now have:

- clearer Korean container name
- visible floating `비상 보급 상자 / 물리 자원` label

### Cargo deposit feedback

Cargo deposit now reports:

- what was deposited
- quantities
- updated shared stock for the deposited resource types
- personal objective progress increase when the deposit actually advanced an objective

## Automated validation

GitHub Actions Build #639: SUCCESS

- Test: SUCCESS
- Build: SUCCESS

## Windows / Paper integrated validation

Do not mark COMPLETE until the following representative flow is verified:

### A. Navigation

1. Start a solo test:
   `/space match devstart 1004`
2. Select a role and enter ACTIVE.
3. Confirm every room has a readable floating name and visible identity accent.
4. At a connection pad, confirm the destination room name is visible.
5. Confirm pad state remains visually distinct:
   - gold open
   - yellow conditional
   - red blocked
6. Open Crew PDA -> 함선 지도.
7. Confirm:
   - current room is correct
   - priority target matches HUD
   - recommended route is plausible
   - adjacency/connection states are readable
   - no other-player live position is exposed.
8. Starting at Bridge, find Engineering and Cargo using only in-game information.

### B. Incident presentation

1. Force a small incident:
   `/space event trigger door_fault`
2. Confirm:
   - chat alert
   - sound
   - ActionBar response guidance
   - target/need/action are understandable.
3. Repair the fault through normal facility gameplay where possible.
4. Confirm stabilization feedback appears and obsolete warning disappears.
5. Force a major incident:
   `/space event trigger hull_breach`
6. Confirm:
   - title/subtitle
   - alarm
   - incident BossBar
   - target/need/action
   - persistent public HUD priority.
7. Restore relevant public ship state and confirm stabilization feedback.

### C. Items / resources

1. Open a generated emergency cache.
2. Confirm the cache is visibly labeled before opening.
3. Pick up at least:
   - Power Cell
   - Repair Parts
4. Inspect item lore and confirm purpose/use location/Cargo-deposit rule are understandable.
5. Confirm pickup ActionBar feedback.
6. Inspect the selected role's starter equipment.
7. Confirm role/purpose/use-location/loss information is clear.
8. Drop and recover the functional equipment and confirm feedback.
9. Deposit physical resources at Cargo.
10. Confirm:
    - deposited types/amounts
    - new shared stock
    - objective progress delta when applicable.

## Completion state

- Code: IMPLEMENTED
- GitHub Actions: SUCCESS (Build #639)
- Windows/Paper integrated validation: PENDING
- COMPLETE only after user-reported integrated validation

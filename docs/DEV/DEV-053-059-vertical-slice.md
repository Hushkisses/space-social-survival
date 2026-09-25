# DEV-053 through DEV-059 Vertical Slice Integration

## Goal

Turn the completed MVP gameplay systems into a repeatable, physically traversable match flow suitable for real playtesting.

## Included

- DEV-053 Match Setup Pipeline
- DEV-054 Automatic Match Activation
- DEV-055 Production MVP Tile Catalog
- DEV-056 Physical Dev Ship
- DEV-057 Live HUD
- DEV-058 Live Briefing GUI
- DEV-059 Playtest Harness / Rematch Reset

## Flow

1. Players join the lobby.
2. Admin runs /space match start <seed>.
3. For smoke testing only, /space match devstart <seed> bypasses the production minimum-player rule.
4. Match setup creates:
   - role candidates
   - objective conflicts and base objectives
   - optional secret missions
   - hidden scenario
   - initial small incidents
   - constrained random logical map
   - physical development ship
5. Players are teleported to the bridge and shown the public briefing.
6. Each player chooses one of three role candidates.
7. When the final role is selected, the match automatically becomes ACTIVE and the runtime timer starts.
8. The action-bar HUD continuously shows minimal public state.
9. Portal pads allow physical traversal of the generated logical topology.
10. /space match reset returns mutable systems to a new-match state while retaining lobby membership.

## Non-goals

- final ship art or Structure Block/NBT assets
- final ItemsAdder item art
- final MythicMobs / ModelEngine content
- final balance values
- revealing hidden scenario truth or private objectives in HUD
- replacing the logical map with hardcoded coordinates

## Balance configuration

balance.yml owns:
- map tile range
- map dead-end/core-distance constraints
- secret mission probability
- initial small-event count
- scenario weights

These values are development defaults and must remain playtest-tunable.

## Validation

1. GitHub Actions full test/build.
2. Windows test/build/deploy.
3. One-player devstart.
4. Physical portal traversal.
5. Briefing -> role selection -> automatic ACTIVE.
6. HUD verification.
7. Reset and second devstart without server restart.

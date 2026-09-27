# MAP-V4 Dense Single-Deck Ship Prototype

## Status

- Experimental branch: `dev/physical-ship-layout-v2`
- Draft PR: #27
- Purpose: make the ship readable and dense enough for 6-10 players
- GitHub Actions: SUCCESS (Build #885)
- Windows/Paper playtest: pending

## Reset from V3

MAP-V3 was still too hard to navigate:

- upper/lower decks added cognitive cost without enough gameplay value
- route holograms were not intuitive
- repeated junction/deck spaces made the ship feel larger than necessary
- traversal still felt like learning a generated graph instead of learning a spacecraft

MAP-V4 removes those assumptions.

## Physical scale

Single deck only.

Fixed physical envelope:

- X: 0 through 49
- Z: -19 through 19
- Y: one gameplay deck at 80

Room sizes:

- normal core rooms: 9x9 or 11x11
- Cargo / Engineering: 11x11
- optional rooms: 9x9

The expected farthest normal core-facility trip is intentionally short enough for a 6-10 player social game.

## Unique core facilities

Exactly one of each core facility is generated:

- Bridge
- Engineering
- Medical
- Research
- Cargo
- Habitation

There is one central hub only:

- `junction_1`

The following are not generated in MAP-V4:

- `junction_2`
- `junction_3`
- corridor_1 through corridor_5
- airlock_2

No duplicate core facility exists in the topology.

## Compact skeleton

The central hub is the primary orientation landmark.

From the hub:

- west: Bridge
- north: Habitation, then Cargo
- south: Medical, then Research
- east: Engineering

Two short side loops connect:

- Habitation -> Cargo -> Engineering
- Medical -> Research -> Engineering

This gives alternate movement without making a maze.

## Randomness

The ship skeleton and core-facility geography stay stable between matches.

Each match adds exactly three optional rooms selected from:

- Maintenance Room
- Emergency Storage
- Communications Auxiliary
- Isolation Room
- Main Airlock

Randomness is therefore moved toward:

- which auxiliary rooms exist
- event/damage locations
- resource locations
- connection faults / locks

rather than randomizing the basic geography players need to learn.

## Wayfinding

Floating route/destination holograms were removed.

Permanent physical signs are used instead:

Central hub:

- ← Bridge
- Engineering →
- ↑ Habitation / Cargo
- ↓ Medical / Research

Every room also receives a physical sign with its room name.

The current urgent target is represented only by a small lime floor marker at recommended doorways. The HUD still states the target facility.

The goal is:

- HUD tells the player **what** facility is needed.
- fixed ship geography/signs tell the player **where** it is.
- dynamic floor markers only help at a doorway; they are not the primary navigation system.

## Lighting

Power-linked lighting remains, but emergency lighting is brighter than V3:

- 70-100: white, light 15
- 40-69: yellow, light 13
- 15-39: red emergency, light 10
- 0-14: near-blackout, light 4

Starting power 35 therefore remains visibly damaged while still being navigable.

## World isolation / cleanup

MAP-V4 uses:

`space_ship_compact_v4`

Legacy prototype worlds do not overlap with it.

Before each generation, the full compact ship envelope is cleared. Legacy 15x15 NBT modules are intentionally not loaded until the final footprint is accepted.

## Automated validation

Build #885 passes:

- full project test/build
- 100 generated seeds
- exactly 10 meaningful rooms per match
- exactly one of each core facility
- no corridor rooms
- no second hub
- connected topology
- Bridge -> Engineering graph distance <= 2
- all physical room slots stay in the single-deck compact envelope
- no room overlap

## Playtest questions

Evaluate these before architecture polish:

1. Can you understand the whole ship after one walk around?
2. Is the central hub immediately recognizable?
3. Can you find Bridge / Engineering / Medical without relying on a hologram?
4. Is the ship now dense enough for 6-10 players to meet naturally?
5. Are the two small side loops useful, or should the map be even simpler?
6. Are three optional rooms too many, too few, or appropriate?

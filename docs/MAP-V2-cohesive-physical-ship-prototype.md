# MAP-V5 Dense Large-Room Ship Prototype

## Status

- Experimental branch: `dev/physical-ship-layout-v2`
- Draft PR: #27
- Purpose: preserve large usable rooms while removing wasted corridor distance
- GitHub Actions: SUCCESS (Build #900)
- Windows/Paper playtest: pending

## Design correction from V4

MAP-V4 made the whole ship smaller by shrinking rooms.

Playtest feedback clarified that room size was not the problem.

The actual problem was:

- too much empty travel distance between rooms
- navigation depended too much on abstract signs/holograms
- the ship needed a more readable route language

MAP-V5 therefore restores room size and compresses only the connections between them.

## Physical scale

Single deck remains.

Core room sizes:

- Bridge: 15x15
- Engineering: 15x15
- Habitation: 13x13
- Cargo: 13x13
- Medical: 13x13
- Research: 13x13
- Central hub: 11x11

Optional rooms are 11x11.

Core connected-room gaps are generally 1-3 blocks.

The full optional-slot envelope is approximately:

- X: 0 through 60
- Z: -23 through 23

The usable ship feels denser than that envelope because the core rooms occupy most of the footprint instead of leaving long corridors.

## Stable core geography

Exactly one of each core facility remains:

- Bridge
- Engineering
- Habitation
- Cargo
- Medical
- Research

One central hub remains.

Primary layout:

```text
             Habitation --- Cargo
                  |           |
Bridge --- Central Hub --- Engineering
                  |           |
               Medical --- Research
```

Optional rooms attach directly to the outside of those large rooms.

## Permanent floor routes

Floating destination holograms are not the primary navigation system.

Permanent floor route colors are now used:

- cyan/light blue: Bridge
- red: Engineering
- lime: Habitation
- orange: Cargo
- pink: Medical
- purple: Research

The central hub floor contains four colored rays:

- west -> Bridge
- east -> Engineering
- north -> Habitation / Cargo
- south -> Medical / Research

Habitation carries the orange branch toward Cargo.

Medical carries the purple branch toward Research.

The physical corridor center line uses the same color whenever it is part of a primary route.

Emergency guidance still uses a small temporary lime doorway marker, but that marker is secondary to permanent navigation.

## PDA schematic map

The PDA map is no longer a sequential list of generated graph nodes.

It is a fixed schematic whose item positions mirror the physical ship:

- Habitation / Cargo on the north side
- Bridge / Hub / Engineering in the center
- Medical / Research on the south side
- optional rooms appear only when generated

Markers:

- green dot: current room
- yellow diamond: current urgent target

The same facility colors used on the physical floor are used in the PDA.

The map still exposes connection state details without showing hidden scenario truth.

## Randomness

The core geography remains stable so players can learn the ship.

Each match still varies:

- three optional rooms
- damage / incident locations
- hull breach location
- resources
- connection faults / locks
- scenario state

## Lighting

Power-linked lighting remains:

- 70-100: white, light 15
- 40-69: yellow, light 13
- 15-39: red emergency, light 10
- 0-14: near-blackout, light 4

## Runtime isolation

MAP-V5 uses:

`space_ship_compact_v5`

This prevents old V2/V3/V4 prototype blocks from visually overlapping the current test.

Legacy large NBT modules remain disabled until physical scale is accepted.

## Automated validation

Build #900 passes:

- full project test/build
- 100 generated seeds
- exactly one of every core facility
- no independent corridor rooms
- no second hub
- connected topology
- large core-room minimum sizes are enforced
- every primary core connection gap is <= 3 blocks
- no physical room overlap

## Playtest questions

Evaluate these first:

1. Do the restored room sizes feel comfortable again?
2. Are the gaps between rooms now short enough?
3. Can you navigate by floor color without reading every sign?
4. Does the PDA map immediately explain the ship's shape?
5. Does the ship feel dense enough for 6-10 players without feeling cramped?
6. Should the central hub remain, or should rooms connect even more directly?

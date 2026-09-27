# MAP-V3 Compact Ship Prototype

## Status

- Experimental branch: `dev/physical-ship-layout-v2`
- Draft PR: #27
- Purpose: rebuild the physical ship scale for a 6-10 player match
- GitHub Actions: SUCCESS (Build #869)
- Do not merge until playtest feedback accepts the direction.

## Why this is a reset

The previous prototypes were too large and reused the same physical world, which allowed older room shells to visually overlap newer layouts.

MAP-V3 resets those assumptions:

- new runtime world: `space_ship_compact_v3`
- legacy 15x15 NBT room structures are disabled for this prototype
- the complete compact ship envelope is cleared before every generation
- corridor tiles are no longer spawned as independent rooms
- physical corridors are only connection space between meaningful rooms

## Target scale

Designed specifically for 6-10 players.

Physical envelope:

- X: 0 through 56
- Z: -24 through 24
- two decks
- room sizes: 9x9, 11x11, or 13x13 depending on function
- generated logical module count: 10-12

The goal is frequent player encounters without making role movement trivial.

## Layout

Upper deck:

- Bridge
- central atrium / stair core
- Habitation
- Medical
- Research
- selected auxiliary room / airlock

Lower deck:

- central atrium / stair core
- Cargo
- Engineering
- selected maintenance / auxiliary room / airlock

`junction_1` and `junction_2` share the same X/Z footprint and form the only mandatory vertical spine.

## Topology

The logical graph was simplified to match the compact physical layout.

Removed from generated room selection:

- corridor_1 through corridor_5
- junction_3

Required meaningful spaces:

- Bridge
- Engineering
- Medical
- Research
- Cargo
- Habitation
- upper atrium
- lower atrium

Each seed adds only 2-4 optional auxiliary/airlock spaces.

This prevents room count from inflating the physical footprint.

## Lighting

Power-linked lighting remains:

- 70-100: normal white / light 15
- 40-69: reduced yellow / light 11
- 15-39: red emergency / light 7
- 0-14: near-blackout / light 2

Opening power 35 therefore starts under emergency lighting.

## Automated checks

Build #869 passes:

- full project tests/build
- 100-seed compact topology generation
- no independent corridor rooms
- 10-12 generated modules
- one mandatory cross-deck spine
- no reused logical connection points
- all physical rooms stay inside the compact envelope
- rooms on the same deck do not overlap

## Playtest questions

Focus only on scale and movement first:

1. Is the ship now small enough for 6-10 players?
2. Do players encounter each other often enough?
3. Is Bridge -> Engineering travel short enough?
4. Does two-deck movement add space without making navigation annoying?
5. Are 9x9-13x13 rooms large enough for facility gameplay?
6. Does the central atrium feel like a useful landmark?
7. Should optional rooms be even smaller or fewer?
8. After the scale is accepted, should the next pass focus on architecture/visual identity?

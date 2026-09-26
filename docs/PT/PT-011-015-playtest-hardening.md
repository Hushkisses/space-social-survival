# PT-011 through PT-015 Playtest Hardening

## Goal
Close the gap between a technically playable vertical slice and a multiplayer session that produces useful gameplay and balance evidence.

## PT-011 Objective Progress
Personal objectives must advance from normal gameplay instead of admin progress commands. Facility actions, deposits and end-state checks feed the existing ObjectiveEngine.

## PT-012 Physical Sanctions
Meeting results have tangible consequences in the world: weapon loss, movement detention, access restrictions and ejection.

## PT-013 Death Loot
Functional equipment and carried resources are recoverable after death through normal Minecraft drops. Death therefore transfers strategic tools/resources to whoever reaches the body location.

## PT-014 Result GUI
A completed or failed return automatically evaluates results and opens a shared final-result GUI. Private objectives are revealed only here.

## PT-015 Operator Workflow
Preflight prevents avoidable multiplayer-session setup mistakes. Postmatch summarizes results and telemetry so balance observations can be reviewed immediately.

## Validation
1. GitHub Actions full test/build.
2. Windows build/deploy/Paper startup.
3. single-player smoke:
   - physical objective progress
   - access restriction
   - death item drop/recovery
   - result GUI
   - preflight/postmatch
4. first real 6–10 player session remains empirical balance validation.

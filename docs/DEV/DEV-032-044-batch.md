# DEV-032 through DEV-044 Social / Communication / Scenario Batch

This batch groups the social layer, communication rules, and scenario layer so they can be validated as one integrated gameplay slice.

## Included
- DEV-032 Meeting System
- DEV-033 Emergency Meeting
- DEV-034 Sanction Voting
- DEV-035 Sanction Execution
- DEV-036 Conditional PvP
- DEV-037 Voice Chat Integration
- DEV-038 Radio System
- DEV-039 Dead Communication
- DEV-040 Scenario Engine
- DEV-041 Accident Scenario
- DEV-042 Sabotage Scenario
- DEV-043 Infection System
- DEV-044 Infection Scenario

## Important boundaries
- voting does not magically execute a sanction
- sanction execution checks required infrastructure/authority
- human PvP is denied unless a defined condition allows it
- system information is truthful; low-precision infection tests may be inconclusive rather than false
- public scenario briefing and hidden scenario truth are separate
- infection conversion after death remains DEV-046
- voice chat is an optional external adapter and never game-state authority

## Validation
1. GitHub Actions full tests and build
2. Windows full tests and build
3. Paper startup
4. integrated social / communication / scenario smoke test
5. only then mark DEV-032 through DEV-044 COMPLETE and merge

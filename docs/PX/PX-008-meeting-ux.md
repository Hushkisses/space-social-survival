# PX-008 Meeting UX

## Goal

Make meetings feel like a distinct social-deduction phase instead of an immediate inventory vote.

PX-008 preserves the existing authoritative core rules:

- `MeetingService` decides whether a meeting can start.
- `SanctionVoteService` records and resolves votes.
- `SanctionExecutor` decides whether the winning sanction can actually be executed.
- `PhysicalSanctionService` applies the existing physical consequence.

The Paper layer only adds presentation, timing and interaction flow.

## Implemented flow

### 1. Meeting transition

When a regular or emergency meeting starts:

- current facility inventory is closed.
- all online participants receive a strong title/subtitle.
- meeting type and reason are shown.
- the participant list is broadcast.
- a meeting BossBar appears.
- a bell sound marks the phase transition.

Regular meeting reason:

- crew-requested regular meeting

Emergency reasons retain the existing public reason:

- body found
- infection alert
- reactor critical
- security alert
- special event

### 2. Discussion phase

Default playtest timing:

- 60 seconds

During discussion:

- BossBar shows discussion phase, public reason and remaining time.
- voting UI is not available yet.
- facility consoles are temporarily unavailable.
- other existing meeting-start prerequisites remain authoritative.

The timings are configurable in `balance.yml`:

```yaml
meeting:
  discussion-seconds: 60
  voting-seconds: 45
```

Existing local balance files are migrated with these defaults without overwriting existing values.

### 3. Voting phase

After discussion:

- title/sound announces voting.
- every online participant receives the sanction-choice GUI.
- BossBar shows:
  - voting phase
  - completed vote count
  - eligible online voter count
  - remaining time
- individual vote contents are never broadcast.
- closing the vote GUI before voting reopens it after a short delay.

Available sanctions remain unchanged:

- no action
- medical check
- disarm
- detain
- access restrict
- eject

The GUI also explains execution prerequisites where they are already part of the existing rules.

### 4. Target selection / confirmation

For sanctions requiring a target:

- a player-head target selector is shown.

For higher-impact sanctions:

- detain
- access restrict
- eject

an additional confirmation screen is required before the vote is recorded.

The confirmation explicitly states that winning the vote does not bypass existing execution prerequisites.

### 5. Vote completion and timeout

- only the count of completed votes is public.
- if every currently online eligible participant votes, the meeting resolves immediately.
- if the voting timer expires first, the existing vote resolver resolves the votes that were actually cast.
- an empty vote still resolves through the existing core behavior.

### 6. Result and execution feedback

After resolution:

- winning sanction and target are publicly announced.
- tie behavior remains the existing `NO_ACTION` fallback.
- actual sanction execution still goes through `SanctionExecutor`.
- execution success or failure is announced separately.
- execution-failure reasons are presented in Korean.
- participants receive a result title and short result BossBar.
- normal gameplay presentation resumes after the result presentation.

No vote magically bypasses:

- medical availability
- security authority
- detention availability
- airlock availability
- valid target requirements

## Communications / facility rules

Regular meeting start retains the existing core communication/power rule.

Emergency meetings retain the existing emergency start path.

During an active discussion or voting phase, facility console work is paused so the meeting reads as a separate game phase.

## Automated validation

- configuration migration coverage includes the meeting timing defaults.
- full GitHub Actions test/build required.
- Windows/Paper integrated validation required before COMPLETE.

## Manual Windows/Paper validation

1. deploy the branch and start a one-player development match.
2. use the Bridge console to call a regular meeting.
3. confirm the facility GUI closes and a meeting title appears.
4. confirm the public reason and participant list are shown.
5. confirm the discussion BossBar counts down.
6. during discussion, try another facility console and confirm it is refused with Korean feedback.
7. confirm no voting GUI appears before discussion ends.
8. when voting begins, confirm the sanction-choice GUI opens.
9. close the voting GUI without voting and confirm it reopens.
10. confirm the BossBar shows only vote completion count, not vote contents.
11. choose a target-based low-impact sanction and confirm target selection.
12. choose DETAIN / ACCESS_RESTRICT / EJECT and confirm the extra confirmation step appears.
13. cast a vote and confirm it is recorded.
14. in a one-player meeting, confirm the meeting resolves immediately after that vote.
15. confirm winning sanction/target and execution result are visually distinct.
16. test at least one execution failure condition and confirm its reason is Korean and understandable.
17. after the result presentation, confirm facility gameplay can resume.
18. confirm meeting cooldown and existing start-denial rules still work.
19. reset the match and confirm no stale meeting BossBar/GUI remains.

## Completion state

- Code: IMPLEMENTED
- CI: pending until the current branch run completes
- Windows/Paper: VALIDATION_PENDING
- COMPLETE: not yet

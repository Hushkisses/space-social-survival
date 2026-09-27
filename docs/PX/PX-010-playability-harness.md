# PX-010 1–3 Player End-to-End Playability Harness

## Status

- Branch: `dev/PX-010-playability-harness`
- Base: validated PX-009 branch head
- Draft PR: #29
- Status: IMPLEMENTED / VALIDATION_PENDING
- GitHub Actions full test/build: SUCCESS
- Windows/Paper integrated validation: pending

## Goal

Prove that the existing game can be exercised end-to-end with 1–3 players before inviting a full 6–10 player group.

This ticket does not add content volume or replace normal gameplay with admin commands.

## Internal playability mode

Use:

```text
/space playability start [seed]
```

Rules:

- only 1–3 current lobby participants are accepted,
- normal minimum-player requirements are bypassed only for this internal mode,
- role selection, facility work, resources, incidents, meetings, sanctions, death and return remain the real gameplay path,
- production balance/config defaults are not changed.

Reset and rematch:

```text
/space playability reset
/space playability start [seed]
```

The runtime records development start/reset counts so the operator report can show whether reset → rematch has actually been exercised in the current server process.

## Operator checklist

Use:

```text
/space playability check
```

The report gathers evidence from existing authoritative runtime state and telemetry.

Automatically evidenced items:

- onboarding shown,
- role selection completed,
- private base objective available,
- starter role equipment granted,
- physical/logical ship navigation generated,
- resource caches populated,
- at least one successful facility action,
- at least one runtime incident experienced,
- meeting flow resolved,
- death or meaningful sanction state experienced,
- return/result produced,
- telemetry file saved,
- at least 20 minutes of match runtime,
- reset followed by another development start.

The report intentionally does **not** auto-pass qualitative blind-playtest observations:

- first-time player finds a next action within 60 seconds,
- first-time player finds Engineering and Cargo without external directions,
- player can explain their role equipment and two resource types,
- player understands incident / meeting / terminal-state feedback without developer explanation.

These require human observation.

## Existing operator tools reused

PX-010 deliberately reuses existing tools instead of duplicating them.

Force a runtime incident:

```text
/space director force small
/space director force large
```

Provide test resources:

```text
/space resource add <shared|self> <type> <amount>
```

Shorten only the final-hold validation timer when needed:

```text
/space return reset <holdSeconds>
```

Save telemetry manually:

```text
/space playability save
```

These commands may create test conditions, but the response itself should still be completed through normal player interaction.

## Telemetry additions

PX-010 adds evidence markers without changing game rules:

- `playability.onboarding.shown`
- `playability.role.selected`
- `playability.private_objective.available`
- `playability.starter.granted`

The telemetry snapshot now retains `endedAt` so a completed session can still be checked against the 20-minute gate.

Existing evidence remains authoritative for:

- `resource.cache.count`
- `facility.action.success`
- `incident.total`
- `meeting.resolved`
- `death.total`
- `sanction.physical.total`
- final result runtime
- telemetry saved-file path

## Non-goals

- no new roles,
- no new objectives,
- no new scenarios,
- no new event content,
- no production balance changes,
- no admin command replacing facility/resource/objective gameplay,
- no automatic claim that qualitative UX is good.

## Windows / Paper validation

1. stop the server,
2. switch/pull `dev/PX-010-playability-harness`,
3. run `dev-server\quick-deploy.bat`,
4. run `dev-server\start-dev.bat`,
5. start with `/space playability start [seed]`,
6. select roles normally,
7. use the PDA and navigation normally,
8. perform at least one real facility action,
9. force a small and/or large incident only if natural timing is inconvenient,
10. resolve the incident through normal facility/resource play,
11. complete a meeting,
12. experience a death or meaningful sanction,
13. progress the return/result path,
14. save/check telemetry,
15. run `/space playability check`,
16. reset and start a second match to exercise rematch stability.

PX-010 remains `VALIDATION_PENDING` until the real Windows/Paper flow is reported working.


## PX-010.1 Left Mission HUD prototype

Player-facing game branding is now `람몽어스` in the active onboarding/HUD surfaces.

When ItemsAdder is available, the normal opening/recovery loop uses a left-side custom HUD:

- role not selected -> `직업을 선택하세요`
- own role selected while other players are still choosing -> `승무원 선택 대기`
- low power -> `전력 계통 복구 → 기관실 · 전력 셀 필요`
- low hull -> `선체 균열 수리 · 수리 부품 확보 후 표시된 균열`
- low reactor -> `원자로 안정화 → 기관실 · 연료 필요`
- low oxygen -> `산소 계통 복구 → 생활구역 · 수리 부품`
- navigation / return preparation -> compact `귀환 절차 진행` guidance
- final hold -> `귀환 상태 유지`

The right scoreboard is reduced to location / ship metrics / personal objective / PDA hint while the custom left mission HUD is active.

Fallback policy:

- ItemsAdder unavailable -> existing right-side urgent objective remains
- required HUD image unavailable -> existing right-side urgent objective remains
- complex facility-damage states currently stay on the vanilla right-side fallback rather than showing an inaccurate generic image

The HUD uses ItemsAdder `CUSTOM` HUD API through the existing reflection-only bridge, so ItemsAdder remains optional.

Because HUD assets changed, Windows validation requires `/iazip` after deploy/start.


## PX-010.2 HUD visual correction

Windows screenshot feedback showed the first left-HUD prototype was oversized, pushed too high, and competed with the right sidebar/chat.

Correction:

- ItemsAdder mission HUD render scale reduced from 240 to 150
- vertical font-image position reduced from 225 to 150
- custom HUD X offset moved from -145 to -220
- right sidebar compressed to:
  - current location
  - two compact ship-metric rows
  - one compact personal-objective row
  - optional secret-mission indicator
  - PDA hint
- role-selection confirmation reduced to one chat line
- starter-equipment duplicate chat line removed
- mission activation guidance reduced to two chat lines
- pre-role briefing reduced to situation + one start instruction
- duplicate all-crew activation broadcast removed

The left mission HUD remains the primary immediate-action surface; right sidebar and chat are now secondary.

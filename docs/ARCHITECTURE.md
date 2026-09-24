# Architecture

## Principles

- `core` owns game-domain rules and should avoid Paper API dependencies where practical.
- `paper-plugin` adapts Paper/Bukkit events, players, inventories, UI, teleportation, scheduling, and damage into the core.
- External plugins are accessed through adapter interfaces and are not the source of truth for game state.
- Runtime match state lives in memory. Persistence stores only account/meta progression and configuration.
- Content such as objectives, events, scenarios, balance values, and map metadata should be data-driven where practical.

## Planned modules

```text
space-social-survival/
├─ core/
├─ paper-plugin/
├─ integrations/
│  ├─ itemsadder/
│  ├─ voicechat/
│  ├─ mythicmobs/
│  └─ modelengine/
├─ dev-server/
├─ docs/
└─ tools/
```

## Dependency direction

```text
paper-plugin ───────► core
integrations/* ─────► integration interfaces
paper-plugin ───────► integrations/*

core ─X─► Paper / ItemsAdder / MythicMobs / ModelEngine
```

## Source of Truth

The latest GitHub `main` branch and `docs/PROJECT-STATE.md` take precedence over old chat context or assumptions.

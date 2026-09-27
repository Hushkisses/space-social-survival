# Development Server

`dev-server/server/` is intentionally ignored by Git.

## Required local files

Place the Paper server in:

```text
dev-server/server/paper.jar
```

The runtime plugin folder is:

```text
dev-server/server/plugins/
```

Licensed/proprietary plugin jars such as ItemsAdder stay local and are not committed.

For the current development setup, place these jars under the runtime plugin folder:

```text
dev-server/server/plugins/SpaceSurvival.jar
dev-server/server/plugins/ItemsAdder_4.x.x.jar
dev-server/server/plugins/ProtocolLib.jar
```

## ItemsAdder content

The SpaceSurvival-owned ItemsAdder content pack is tracked in Git:

```text
dev-server/itemsadder-content/spacesurvival/
```

Do not edit the runtime copy as the source of truth:

```text
dev-server/server/plugins/ItemsAdder/contents/spacesurvival/
```

`quick-deploy.bat` replaces that runtime namespace with the tracked source pack on every deploy.

After pulling changes:

```powershell
dev-server\quick-deploy.bat
dev-server\start-dev.bat
```

When textures/models changed, run this once after the server starts:

```text
/iazip
```

Representative test:

```text
/iaget spacesurvival:repair_parts
```

## DEV validation

From the repository root on Windows:

```powershell
dev-server\quick-deploy.bat
dev-server\start-dev.bat
```

Then run in game or console:

```text
space status
```

Expected player-facing output is Korean.

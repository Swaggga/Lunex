# LunexBeta

Private Lunex client base for Minecraft 1.21.4 on Fabric.

## Requirements

- Java 21
- Gradle wrapper pinned to Gradle 8.11.1

## Build

```bash
./gradlew build
```

The remapped mod jar is written to `build/libs/`.

## Run client

```bash
./gradlew runClient
```

## Controls

- Right Shift — open the Lunex ClickGUI
- Modules are toggled from the ClickGUI.

## Project layout

- `src/main/java` — shared Fabric entrypoint.
- `src/client/java` — client-only Lunex bootstrap, modules, config, rendering, keybinds.
- `src/main/resources/fabric.mod.json` — Fabric mod metadata.

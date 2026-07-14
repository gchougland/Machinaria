# Changelog

All notable changes to Machinaria are documented in this file.

## [0.3.0] - 7/13/2026

### Added

- **Machinaria creative tab** — gear-icon tab in the creative library listing all mod items and blocks
- **Aetherhaven integration (optional)** — Rivet Cogsworth mechanic villager: oil a rusty robot to free them, unlock them at the inn, shop and house quests, schedule, gifts, quest board fetches, journal guide page, and shop stock for vanilla metal sets plus Machinaria blocks
- **Rusty Robot** block that drops itself, and **Oil** fluid used to wake Rivet (later husks become scrap once rescued)

## [0.2.1] - 2026-07-03

### Fixed

- **Mechanic's Workstation** — added `BenchBlock` block entity component so the bench opens the crafting UI on Hytale 0.5 (break and replace existing placed benches)

## [0.2.0] - 2026-05-26

### Requirements

- **Hytale** `^0.5.0` (stable 0.5.0; manifest uses Semver range instead of legacy dated `ServerVersion`)

### Changed

- **Build / manifest** — `server_version` is `^0.5.0`; added `runServerNoSync` and Windows `runServer` JVM arg workaround (hytale-mod 0.7.x)
- **0.5 API migration** — inventory via `InventoryComponent.getCombined`; processing bench acceleration via `ProcessingBenchBlock.advanceProcessing`; positions use `org.joml.Vector3d` / `Vector3i`

### Fixed

- **Wood_Gear_Trunk** — removed obsolete `Wood_Lightwood_Softwood*` resource types (Update 4); added missing `server.lang` description key
- **Golem_Clockwork** — attack interaction updated to `Root_NPC_Golem_Crystal_Attack` (vanilla golem root rename)

## [0.1.2] - 2/18/2026

### Fixed
- **Update 3 Compatibility:** Updated Rusted Expanse World Structure file to match new format for update 3.

## [0.1.1] - 2/8/2026

### Fixed

- **Endgame & QoL compatibility:** When playing with Endgame & QoL (including the Void Golem Update), entering the Rusted Expanse fragment could leave players unable to break blocks and mobs frozen in place. Machinaria now uses a dedicated GameplayConfig (`MachinariaPortal`) for the Rusted Expanse instead of the shared "Portal" config, so the dimension is no longer affected by Endgame & QoL’s fragment changes. Block breaking and NPC behavior in the Rusted Expanse now work correctly with both mods installed.

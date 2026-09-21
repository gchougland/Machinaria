# Changelog

All notable changes to Machinaria are documented in this file.

## [0.4.2] - 9/21/2026

### Added

- **Robot Villager Voice** Added a unique robot voice to the robot villagers for Aetherhaven.

## [0.4.1] - 8/27/2026 (Hytale Update 6)

### Added

- **Stormwind Mechanic's Shop** — Stormwind-style variant of the Mechanic's Shop for towns using the Stormwind building set (requires Aetherhaven).
- **HStats** — integrated anonymous server metrics via [hstats.dev](https://hstats.dev) (same pattern as Aetherhaven)
- **Rivet (Aetherhaven crossmod)** — calendar birthday (Winter 18), birthday greetings and gift reactions, speech voice, holiday gift preferences, and Wintertide gift/incoming dialogue when Aetherhaven is installed

### Changed

- **Hytale Update 6** — targets `>=0.6.0-pre.0 <0.7.0` (pre-release channel); chunk/block lookups use section refs and `BlockStateInfo.getSectionRef()` instead of deprecated column accessors
- **Rusted Expanse Shard Recipe** - Simplified the recipe as not to require very rare flora.
- **Rusty robot oil immersion** — removed debug log spam; fluid checks are throttled and section lookups cached so idle rusty robot drops no longer probe oil every tick
- **Rivet's shop quest (Aetherhaven)** — grants the bronze mechanics shop plot token directly instead of a blueprint learn step

### Fixed

- **Clockfin spawning** — shallow-oil dive motion via `Template_Machinaria_Swimming_Passive`; spawns scoped to `Env_Machinaria_RustedExpanse` instead of all Zone 1 plains
- **Ancient Pocket Watch** — migrated interactions and bench acceleration off deprecated chunk APIs to `BlockSection` / `BlockComponentSection` / `BlockStateInfo.getSectionRef()` (Update 6)
- **Robot villager emotes** — `Machinaria_Robot_Base` now defines `PonderDismissive`, `Yawn`, and `Laugh` using player emote animations so Aetherhaven leisure/shop visuals work on Rivet and other robot NPCs

## [0.4.0] - 7/22/2026

### Added

- **Robot townsfolk (optional Aetherhaven)** — Reginald Volt and Copper Pin join the townsfolk pool when Machinaria and Aetherhaven are both installed. They use the robot body with player cosmetic attachments.
- **CozyTales Fishing (optional, assets-only)** — Oil is fishable when CozyTales Fishing is present; **Clockfin** can be caught in oil pools (journal entry). No hard dependency on CozyTales; `Server/CozyTalesFishing/*` JSON is inert without that mod.
- **Clockfin wildlife** — Passive swimmers spawn in **Oil** (same world spawn pattern as vanilla fish in water). Killing them drops scrap metal (sometimes iron ore). Processing the caught fish item at a **Fish Processor** (CozyTales) yields scrap and iron ore.

### Changed

- **Robot appearance** — Rivet and the robot body mesh no longer bake in the antenna. Antennas use a model attachment instead. The rusty robot block model matches (no antenna on the placed block).

### Fixed

- **Rivet's house quest** — quest objectives now use Aetherhaven progression kinds (`plot_token_received`, `construction_built`, `dialogue_turn_in`) so dialogue turn-in completes the quest after Rivet is assigned to a house
- **Rivet's shop quest** — same progression-kind fix for blueprint learn, build, and dialogue turn-in

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

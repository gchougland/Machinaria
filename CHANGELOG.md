# Changelog

All notable changes to Machinaria are documented in this file.

## [0.1.2] - 2/18/2026

### Fixed
- **Update 3 Compatibility:** Updated Rusted Expanse World Structure file to match new format for update 3.

## [0.1.1] - 2/8/2026

### Fixed

- **Endgame & QoL compatibility:** When playing with Endgame & QoL (including the Void Golem Update), entering the Rusted Expanse fragment could leave players unable to break blocks and mobs frozen in place. Machinaria now uses a dedicated GameplayConfig (`MachinariaPortal`) for the Rusted Expanse instead of the shared "Portal" config, so the dimension is no longer affected by Endgame & QoL’s fragment changes. Block breaking and NPC behavior in the Rusted Expanse now work correctly with both mods installed.

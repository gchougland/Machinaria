package com.hexvane.machinaria.aetherhaven;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.machinaria.MachinariaConstants;
import com.hexvane.machinaria.MachinariaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Soft Aetherhaven hooks for oil immersion after rescue (scrap vs spawn NPC).
 *
 * <p>Rescue dialogue uses stock Aetherhaven {@code complete_quest} + {@code despawn_npc} (cross-world town
 * lookup and default vanish FX). No custom dialogue action registration.
 */
public final class AetherhavenIntegration {
    private static boolean enabled;
    private static boolean loggedMissing;

    private AetherhavenIntegration() {}

    /** Resolves Aetherhaven on plugin setup; no-ops when the plugin is not loaded. */
    public static void setup() {
        tryEnable();
    }

    /**
     * Retries soft-enable if Machinaria setup ran before Aetherhaven finished constructing.
     * Safe to call from ticking systems.
     */
    public static void tryEnable() {
        if (enabled) {
            return;
        }
        try {
            AetherhavenPlugin ah = AetherhavenPlugin.get();
            if (ah == null) {
                logMissing("Aetherhaven not present; oil immersion will always try to spawn rescue NPC.");
                return;
            }
            enabled = true;
            MachinariaPlugin plugin = MachinariaPlugin.get();
            if (plugin != null) {
                plugin.getLogger().atInfo().log("Aetherhaven soft integration enabled.");
            }
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            logMissing("Aetherhaven classes unavailable; oil immersion will always try to spawn rescue NPC.");
        }
    }

    public static void shutdown() {
        enabled = false;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether the player’s town has finished {@code q_mechanic_rescue}. Used so later oil immersions drop scrap
     * instead of another rescue NPC. Searches loaded worlds (portal instances do not own towns).
     */
    public static boolean hasCompletedRescue(@NonNull UUID playerUuid) {
        tryEnable();
        if (!enabled) {
            return false;
        }
        try {
            TownRecord town = findPlayerTown(playerUuid);
            return town != null && town.hasQuestCompleted(MachinariaConstants.QUEST_MECHANIC_RESCUE);
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            return false;
        }
    }

    @Nullable
    private static TownRecord findPlayerTown(@NonNull UUID playerUuid) {
        AetherhavenPlugin ah = AetherhavenPlugin.get();
        if (ah == null) {
            return null;
        }
        Universe universe = Universe.get();
        if (universe == null) {
            return null;
        }
        World defaultWorld = universe.getDefaultWorld();
        if (defaultWorld != null) {
            TownRecord fromDefault = findInWorldManager(defaultWorld, ah, playerUuid);
            if (fromDefault != null) {
                return fromDefault;
            }
        }
        for (World world : universe.getWorlds().values()) {
            if (defaultWorld != null && world.getName().equals(defaultWorld.getName())) {
                continue;
            }
            TownRecord town = findInWorldManager(world, ah, playerUuid);
            if (town != null) {
                return town;
            }
        }
        for (World world : universe.getWorlds().values()) {
            TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, ah);
            for (TownRecord town : tm.allTowns()) {
                if (town.getOwnerUuid().equals(playerUuid) || town.isMemberPlayer(playerUuid)) {
                    return town;
                }
            }
        }
        return null;
    }

    @Nullable
    private static TownRecord findInWorldManager(
        @NonNull World world,
        @NonNull AetherhavenPlugin ah,
        @NonNull UUID playerUuid
    ) {
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, ah);
        return tm.findTownForPlayerInWorld(playerUuid);
    }

    private static void logMissing(@NonNull String message) {
        if (loggedMissing) {
            return;
        }
        loggedMissing = true;
        MachinariaPlugin plugin = MachinariaPlugin.get();
        if (plugin != null) {
            plugin.getLogger().atInfo().log(message);
        }
    }
}

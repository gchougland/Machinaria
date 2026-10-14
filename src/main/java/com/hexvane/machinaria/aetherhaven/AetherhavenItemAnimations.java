package com.hexvane.machinaria.aetherhaven;

import com.hexvane.machinaria.MachinariaPlugin;
import com.hypixel.hytale.assetstore.AssetLoadResult;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.AssetPackRegisterEvent;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.asset.type.itemanimation.config.ItemPlayerAnimations;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;

/**
 * Loads the robot life-action tables only when Aetherhaven is present.
 *
 * <p>Those tables point at animation files that only Aetherhaven ships. Under {@code Server/Item/Animations} the
 * server would validate them with the rest of the pack and refuse all of Machinaria when Aetherhaven is missing.
 * They live under {@link #PATH} instead, which the server does not scan, and go into the same asset store from
 * here (the way Aetherhaven loads {@code Server/Aetherhaven/NpcModels}).
 */
public final class AetherhavenItemAnimations {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Relative to the pack root. Not a folder the server scans. */
    public static final String PATH = "Server/Aetherhaven/ItemAnimations";

    /** Straight after every pack's own assets, and Aetherhaven's animation files, have loaded. */
    private static final short PRIORITY_LOAD = LoadAssetEvent.PRIORITY_LOAD_REGISTRY + 1;

    private AetherhavenItemAnimations() {}

    public static void register(@NonNull MachinariaPlugin plugin) {
        plugin.getEventRegistry().register(PRIORITY_LOAD, LoadAssetEvent.class, event -> load(plugin));
        // A pack registered again later is reloaded from the scanned folders only.
        plugin.getEventRegistry().register(AssetPackRegisterEvent.class, event -> {
            if (plugin.getIdentifier().toString().equals(event.getAssetPack().getName())) {
                load(plugin);
            }
        });
    }

    private static void load(@NonNull MachinariaPlugin plugin) {
        AetherhavenIntegration.tryEnable();
        if (!AetherhavenIntegration.isEnabled()) {
            return;
        }
        AssetModule assetModule = AssetModule.get();
        AssetPack pack = assetModule == null ? null : assetModule.getAssetPack(plugin.getIdentifier().toString());
        if (pack == null) {
            return;
        }
        Path directory = pack.getRoot().resolve(PATH);
        if (!Files.isDirectory(directory)) {
            return;
        }
        try {
            AssetLoadResult<String, ItemPlayerAnimations> result =
                ItemPlayerAnimations.getAssetStore().loadAssetsFromDirectory(pack.getName(), directory);
            if (result.hasFailed()) {
                LOGGER.atSevere().log(
                    "Aetherhaven item animation tables failed to load from %s: %s",
                    PATH,
                    result.getFailedToLoadKeys()
                );
            }
            LOGGER.atInfo().log(
                "Loaded %s Aetherhaven item animation table(s) from %s: %s",
                result.getLoadedAssets().size(),
                PATH,
                result.getLoadedAssets().keySet()
            );
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Could not read %s", PATH);
        }
    }
}

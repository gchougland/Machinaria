package com.hexvane.machinaria;

import com.hypixel.hytale.builtin.crafting.state.ProcessingBenchState;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.GrowthModifierAsset;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.meta.BlockStateModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public class MachinariaPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    protected static MachinariaPlugin instance;
    private ComponentType<ChunkStore, TimeAccelerationOverlay> timeAccelerationOverlayComponentType;
    private ComponentType<EntityStore, PocketwatchStoredTimeComponent> pocketwatchStoredTimeComponentComponentType;
    private PocketwatchAccumulationSystem pocketwatchAccumulationSystem;

    public static MachinariaPlugin get() {
        return instance;
    }

    public MachinariaPlugin(@NonNull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        instance = this;
        timeAccelerationOverlayComponentType =
                this.getChunkStoreRegistry().registerComponent(
                        TimeAccelerationOverlay.class, "TimeAcceleration", TimeAccelerationOverlay.CODEC
                );
        this.getCodecRegistry(Interaction.CODEC).register(
                "AccelerateTime",
                AccelerateTimeInteraction.class,
                AccelerateTimeInteraction.CODEC
        );
        pocketwatchStoredTimeComponentComponentType = getEntityStoreRegistry().registerComponent(
                PocketwatchStoredTimeComponent.class,
                "AncientPocketWatchTime",
                PocketwatchStoredTimeComponent.CODEC);
        pocketwatchAccumulationSystem = new PocketwatchAccumulationSystem(Player.getComponentType());
        this.getEntityStoreRegistry().registerSystem(pocketwatchAccumulationSystem);
        TimeAccelerationExpirySystem expirySystem
                = new TimeAccelerationExpirySystem(timeAccelerationOverlayComponentType);
        this.getChunkStoreRegistry().registerSystem(expirySystem);
        TimeAccelerationOverlayParticleSystem particleSystem =
                new TimeAccelerationOverlayParticleSystem(timeAccelerationOverlayComponentType);
        this.getChunkStoreRegistry().registerSystem(particleSystem);
        getCodecRegistry(GrowthModifierAsset.CODEC).register(
                "Fertilizer",
                TimeAccelerationGrowthModifierAsset.class,
                TimeAccelerationGrowthModifierAsset.CODEC);
        ProcessingBenchAccelerationSystem benchAccelerationSystem = new ProcessingBenchAccelerationSystem(
                timeAccelerationOverlayComponentType
        );
        this.getChunkStoreRegistry().registerSystem(benchAccelerationSystem);
    }

    @Override
    protected void shutdown() {
        super.shutdown();
    }

    public ComponentType<ChunkStore, TimeAccelerationOverlay> getTimeAccelerationOverlayComponentType() {
        return this.timeAccelerationOverlayComponentType;
    }

    public ComponentType<EntityStore, PocketwatchStoredTimeComponent> getPocketwatchStoredTimeComponentComponentType() {
        return this.pocketwatchStoredTimeComponentComponentType;
    }
}

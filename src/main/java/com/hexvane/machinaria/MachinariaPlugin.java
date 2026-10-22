package com.hexvane.machinaria;

import com.hexvane.machinaria.aetherhaven.AetherhavenIntegration;
import com.hexvane.machinaria.aetherhaven.AetherhavenItemAnimations;
import com.hexvane.machinaria.aetherhaven.RustyRobotOilImmersionSystem;
import com.hexvane.machinaria.generated.HstatsBuildMetadata;
import com.hexvane.machinaria.heartworks.MovingPlatformComponent;
import com.hexvane.machinaria.heartworks.MovingPlatformSystem;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.GrowthModifierAsset;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import com.hexvane.machinaria.command.MachinariaPrefabsCommand;
import com.hexvane.machinaria.heartworks.PendulumPassthroughSystem;
import com.hexvane.machinaria.heartworks.CameraShotEffect;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;

public class MachinariaPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    protected static MachinariaPlugin instance;
    private ComponentType<ChunkStore, TimeAccelerationOverlay> timeAccelerationOverlayComponentType;
    private ComponentType<EntityStore, PocketwatchStoredTimeComponent> pocketwatchStoredTimeComponentComponentType;
    private PocketwatchAccumulationSystem pocketwatchAccumulationSystem;
    private ComponentType<EntityStore, MovingPlatformComponent> movingPlatformComponentType;

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
        logHstats(this);
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
        this.getEntityStoreRegistry().registerSystem(new RustyRobotOilImmersionSystem());
        // The Heartworks' moving platforms, lifts, turning cogs and pendulum (prop entities placed by prefabs).
        movingPlatformComponentType = getEntityStoreRegistry().registerComponent(
                MovingPlatformComponent.class,
                "MachinariaMovingPlatform",
                MovingPlatformComponent.CODEC);
        this.getEntityStoreRegistry().registerSystem(new MovingPlatformSystem(movingPlatformComponentType));
        this.getEntityStoreRegistry().registerSystem(new PendulumPassthroughSystem());
        // The Heartworks' cutscene shots (the Heart Gate and the vault door opening): a trigger volume effect type.
        this.getCodecRegistry(TriggerEffect.CODEC).register(CameraShotEffect.TYPE, CameraShotEffect.class, CameraShotEffect.CODEC);
        // /machinariaprefabs: the vanilla prefab editor on the Machinaria prefabs.
        this.getCommandRegistry().registerCommand(new MachinariaPrefabsCommand());
        AetherhavenIntegration.setup();
        AetherhavenItemAnimations.register(this);
    }

    @Override
    protected void start() {
        // Aetherhaven may finish constructing after our setup(); retry soft enable here.
        AetherhavenIntegration.tryEnable();
    }

    @Override
    protected void shutdown() {
        AetherhavenIntegration.shutdown();
        super.shutdown();
    }

    public HytaleLogger getLogger() {
        return LOGGER;
    }

    public ComponentType<ChunkStore, TimeAccelerationOverlay> getTimeAccelerationOverlayComponentType() {
        return this.timeAccelerationOverlayComponentType;
    }

    public ComponentType<EntityStore, PocketwatchStoredTimeComponent> getPocketwatchStoredTimeComponentComponentType() {
        return this.pocketwatchStoredTimeComponentComponentType;
    }

    private static void logHstats(@NonNull MachinariaPlugin plugin) {
        String hstatsModUuid = HstatsBuildMetadata.HSTATS_MOD_UUID;
        String modVersion = plugin.getManifest().getVersion().toString();
        if (!hstatsModUuid.isBlank()) {
            new HStats(hstatsModUuid, modVersion);
            LOGGER.atInfo().log("HStats metrics enabled for Machinaria v%s.", modVersion);
        } else {
            LOGGER.atInfo().log(
                "HStats metrics disabled: set MACHINARIA_HSTATS_MOD_UUID when building, "
                    + "or pass -Phstats_mod_uuid=... to Gradle."
            );
        }
    }

    public ComponentType<EntityStore, MovingPlatformComponent> getMovingPlatformComponentType() {
        return movingPlatformComponentType;
    }
}

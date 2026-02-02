package com.hexvane.machinaria;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TimeAccelerationOverlayParticleSystem extends EntityTickingSystem<ChunkStore> {

    private static final int PARTICLE_UPDATE_INTERVAL = 10;
    private static final double PARTICLE_RADIUS = 48.0;
    private static final String PARTICLE_SYSTEM_ID = "TimeAcceleration_Overlay";

    private final ComponentType<ChunkStore, TimeAccelerationOverlay> overlayType;
    private ComponentType<ChunkStore, BlockModule.BlockStateInfo> blockStateInfoType;
    private int tickCounter;

    public TimeAccelerationOverlayParticleSystem(
            ComponentType<ChunkStore, TimeAccelerationOverlay> overlayType
    ) {
        this.overlayType = overlayType;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @NonNull ArchetypeChunk<ChunkStore> archetypeChunk,
            @NonNull Store<ChunkStore> store,
            @NonNull CommandBuffer<ChunkStore> commandBuffer
    ) {
        TimeAccelerationOverlay overlay = archetypeChunk.getComponent(index, this.overlayType);
        if (overlay == null) {
            return;
        }
        if (this.blockStateInfoType == null) {
            this.blockStateInfoType = BlockModule.BlockStateInfo.getComponentType();
        }
        if (this.blockStateInfoType == null) {
            return;
        }
        BlockModule.BlockStateInfo blockStateInfo = archetypeChunk.getComponent(index, this.blockStateInfoType);
        if (blockStateInfo == null) {
            return;
        }

        Ref<ChunkStore> chunkRef = blockStateInfo.getChunkRef();
        if (!chunkRef.isValid()) {
            return;
        }

        WorldChunk worldChunk = store.getComponent(chunkRef, WorldChunk.getComponentType());
        if (worldChunk == null) {
            return;
        }

        int localX = ChunkUtil.xFromBlockInColumn(blockStateInfo.getIndex());
        int localY = ChunkUtil.yFromBlockInColumn(blockStateInfo.getIndex());
        int localZ = ChunkUtil.zFromBlockInColumn(blockStateInfo.getIndex());
        int worldX = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getX(), localX);
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getZ(), localZ);

        Vector3d blockCenter = new Vector3d(worldX + 0.5, localY + 0.5, worldZ + 0.5);

        tickCounter++;
        if (tickCounter < PARTICLE_UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;

        World world = store.getExternalData().getWorld();
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource =
                entityStore.getResource(EntityModule.get().getPlayerSpatialResourceType());
        ObjectList<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
        playerSpatialResource.getSpatialStructure().collect(blockCenter, PARTICLE_RADIUS, playerRefs);

        if (playerRefs.isEmpty()) {
            return;
        }

        ParticleUtil.spawnParticleEffect(PARTICLE_SYSTEM_ID, blockCenter, playerRefs, entityStore);
        playerRefs.clear();
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return this.overlayType;
    }
}

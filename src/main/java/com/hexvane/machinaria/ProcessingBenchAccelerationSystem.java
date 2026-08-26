package com.hexvane.machinaria;

import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ProcessingBenchAccelerationSystem extends EntityTickingSystem<ChunkStore> {
    private static final float SPEED_BOOST_PER_LEVEL = 2.0f;

    private final ComponentType<ChunkStore, TimeAccelerationOverlay> overlayType;
    private final ComponentType<ChunkStore, ProcessingBenchBlock> processingBenchBlockType;
    private final ComponentType<ChunkStore, BenchBlock> benchBlockType;
    private final ComponentType<ChunkStore, BlockModule.BlockStateInfo> blockStateInfoType;
    private final Query<ChunkStore> query;

    public ProcessingBenchAccelerationSystem(ComponentType<ChunkStore, TimeAccelerationOverlay> overlayType) {
        this.overlayType = overlayType;
        this.processingBenchBlockType = ProcessingBenchBlock.getComponentType();
        this.benchBlockType = BenchBlock.getComponentType();
        this.blockStateInfoType = BlockModule.BlockStateInfo.getComponentType();
        this.query = Query.and(overlayType, processingBenchBlockType);
    }

    @Override
    public void tick(
            float dt,
            int index,
            @NonNull ArchetypeChunk<ChunkStore> archetypeChunk,
            @NonNull Store<ChunkStore> store,
            @NonNull CommandBuffer<ChunkStore> commandBuffer
    ) {
        TimeAccelerationOverlay overlay = archetypeChunk.getComponent(index, overlayType);
        if (overlay == null || System.currentTimeMillis() >= overlay.getExpiresAtEpochMillis()) {
            return;
        }

        ProcessingBenchBlock processingBenchBlock = archetypeChunk.getComponent(index, processingBenchBlockType);
        BenchBlock benchBlock = archetypeChunk.getComponent(index, benchBlockType);
        BlockModule.BlockStateInfo blockStateInfo = archetypeChunk.getComponent(index, blockStateInfoType);
        if (processingBenchBlock == null || benchBlock == null || blockStateInfo == null) {
            return;
        }

        Ref<ChunkStore> chunkRef = blockStateInfo.getChunkRef();
        if (!chunkRef.isValid()) {
            return;
        }

        BlockChunk blockChunk = store.getComponent(chunkRef, BlockChunk.getComponentType());
        if (blockChunk == null) {
            return;
        }

        int blockIndex = blockStateInfo.getIndex();
        int localX = ChunkUtil.xFromBlockInColumn(blockIndex);
        int localY = ChunkUtil.yFromBlockInColumn(blockIndex);
        int localZ = ChunkUtil.zFromBlockInColumn(blockIndex);
        int blockX = ChunkUtil.worldCoordFromLocalCoord(blockChunk.getX(), localX);
        int blockZ = ChunkUtil.worldCoordFromLocalCoord(blockChunk.getZ(), localZ);
        int blockId = blockChunk.getBlock(localX, localY, localZ);
        BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
        if (blockType == null || processingBenchBlock.getProcessingBench() == null) {
            return;
        }

        int rotationIndex = rotationIndexAt(store, blockX, localY, blockZ, blockIndex);
        World world = store.getExternalData().getWorld();
        Store<EntityStore> entityStore = world.getEntityStore().getStore();

        float bonusDt = dt * SPEED_BOOST_PER_LEVEL * overlay.getLevel();
        if (bonusDt > 0.0f) {
            processingBenchBlock.advanceProcessing(
                    bonusDt, entityStore, benchBlock, blockStateInfo, blockX, localY, blockZ, blockType, rotationIndex
            );
        }
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return query;
    }

    /** Reads block rotation via the chunk-section entity for this block column. */
    private static int rotationIndexAt(
            @NonNull Store<ChunkStore> store,
            int worldBlockX,
            int localY,
            int worldBlockZ,
            int blockIndex
    ) {
        World world = store.getExternalData().getWorld();
        Ref<ChunkStore> sectionRef =
                world.getChunkStore().getChunkSectionReferenceAtBlock(worldBlockX, localY, worldBlockZ);
        if (sectionRef == null || !sectionRef.isValid()) {
            return 0;
        }
        BlockSection blockSection = store.getComponent(sectionRef, BlockSection.getComponentType());
        if (blockSection == null) {
            return 0;
        }
        return blockSection.getRotationIndex(blockIndex);
    }
}

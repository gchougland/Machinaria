package com.hexvane.machinaria;

import com.hypixel.hytale.builtin.crafting.state.ProcessingBenchState;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;

public class ProcessingBenchAccelerationSystem extends EntityTickingSystem<ChunkStore> {
    private final Query<ChunkStore> query;
    private final ComponentType<ChunkStore, TimeAccelerationOverlay> overlayType;
    private ComponentType<ChunkStore, ProcessingBenchState> processingBenchStateType;

    private static final float speedBoostPerLevel = 2.0f;

    public ProcessingBenchAccelerationSystem(
            ComponentType<ChunkStore, TimeAccelerationOverlay> overlayType
    ) {
        this.processingBenchStateType = null;
        this.overlayType = overlayType;
        this.query = overlayType;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @NonNull ArchetypeChunk<ChunkStore> archetypeChunk,
            @NonNull Store<ChunkStore> store,
            @NonNull CommandBuffer<ChunkStore> commandBuffer
    ) {
        if (processingBenchStateType == null) {
            processingBenchStateType = resolveProcessingBenchStateType();
            if (processingBenchStateType == null) return;
        }
        ProcessingBenchState benchState = archetypeChunk.getComponent(index, this.processingBenchStateType);
        TimeAccelerationOverlay overlay = archetypeChunk.getComponent(index, this.overlayType);
        if (overlay == null) return;
        if (System.currentTimeMillis() >= overlay.getExpiresAtEpochMillis()) return;
        double multiplier = 1.0 + speedBoostPerLevel * overlay.getLevel();
        if (benchState != null) {
            benchState.tick((float) (dt * (multiplier - 1.0)), index, archetypeChunk, store, commandBuffer);
        }
    }

    @SuppressWarnings("unchecked")
    private ComponentType<ChunkStore, ProcessingBenchState> resolveProcessingBenchStateType() {
        return (ComponentType<ChunkStore, ProcessingBenchState>)
                ChunkStore.REGISTRY.getData().getComponentType("processingBench");
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return this.query;
    }
}

package com.hexvane.machinaria;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TimeAccelerationExpirySystem extends EntityTickingSystem<ChunkStore> {
    private final ComponentType<ChunkStore, TimeAccelerationOverlay> overlayType;

    public TimeAccelerationExpirySystem(ComponentType<ChunkStore, TimeAccelerationOverlay> overlayType) {
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
        Ref<ChunkStore> ref = archetypeChunk.getReferenceTo(index);
        TimeAccelerationOverlay overlay = archetypeChunk.getComponent(index, this.overlayType);
        if (overlay == null) return;
        long now = System.currentTimeMillis();
        if (now >= overlay.getExpiresAtEpochMillis()) {
            commandBuffer.removeComponent(ref, this.overlayType);
        }
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return this.overlayType;
    }
}

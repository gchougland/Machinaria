package com.hexvane.machinaria;

import com.hypixel.hytale.builtin.adventure.farming.config.modifiers.FertilizerGrowthModifierAsset;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.GrowthModifierAsset;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class TimeAccelerationGrowthModifierAsset extends FertilizerGrowthModifierAsset {
    public static final BuilderCodec<TimeAccelerationGrowthModifierAsset> CODEC = BuilderCodec.builder(
            TimeAccelerationGrowthModifierAsset.class,
            TimeAccelerationGrowthModifierAsset::new,
            FertilizerGrowthModifierAsset.CODEC
    ).build();

    private static final float speedBoostPerLevel = 5.0f;

    @Override
    public double getCurrentGrowthMultiplier(
            CommandBuffer<ChunkStore> commandBuffer,
            Ref<ChunkStore> sectionRef,
            Ref<ChunkStore> blockRef,
            int x,
            int y,
            int z,
            boolean initialTick
    ) {
        double fertilizerMultiplier = super.getCurrentGrowthMultiplier(
                commandBuffer, sectionRef, blockRef, x, y, z, initialTick
        );

        ComponentType<ChunkStore, TimeAccelerationOverlay> overlayType
                = MachinariaPlugin.get().getTimeAccelerationOverlayComponentType();
        TimeAccelerationOverlay overlay = commandBuffer.getComponent(blockRef, overlayType);

        if (overlay != null && System.currentTimeMillis() < overlay.getExpiresAtEpochMillis()) {
            double overlayMultiplier = 1 + speedBoostPerLevel * overlay.getLevel();
            return fertilizerMultiplier * overlayMultiplier;
        }

        return fertilizerMultiplier;
    }
}

package com.hexvane.machinaria;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AccelerateTimeInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<AccelerateTimeInteraction> CODEC = BuilderCodec.builder(
            AccelerateTimeInteraction.class, AccelerateTimeInteraction::new, SimpleBlockInteraction.CODEC
    )
            .documentation("Apply or upgrade time acceleration overlay on a block.")
            .append(
                    new KeyedCodec<>("CostPerLevel", Codec.FLOAT),
                    (i, v) -> i.costPerLevel = v,
                    i -> i.costPerLevel
            )
            .add()
            .append(
                    new KeyedCodec<>("MaxLevel", Codec.INTEGER),
                    (i, v) -> i.maxLevel = v,
                    i -> i.maxLevel
            )
            .add()
            .build();
    protected Float costPerLevel = 10.f;
    protected Integer maxLevel = 5;


    @Override
    public @NonNull WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Server;
    }

    @Override
    protected void interactWithBlock(
            @Nonnull World world,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nullable ItemStack itemInHand,
            @Nonnull Vector3i targetBlock,
            @Nonnull CooldownHandler cooldownHandler
    ) {
        int x = targetBlock.getX();
        int z = targetBlock.getZ();
        WorldChunk worldChunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        Ref<ChunkStore> blockRef = null;
        if (worldChunk != null) {
            blockRef = worldChunk.getBlockComponentEntity(x, targetBlock.getY(), z);
        }

        if (blockRef == null) {
            context.getState().state = InteractionState.Failed;
        } else {
            Ref<EntityStore> playerRef = context.getEntity();
            PlayerRef playerRefComponent = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
            PocketwatchStoredTimeComponent pocketwatchTime = commandBuffer.getComponent(
                    playerRef,
                    MachinariaPlugin.get().getPocketwatchStoredTimeComponentComponentType()
            );
            if (pocketwatchTime == null) {
                pocketwatchTime = new PocketwatchStoredTimeComponent(0);
                commandBuffer.addComponent(playerRef, MachinariaPlugin.get().getPocketwatchStoredTimeComponentComponentType(), pocketwatchTime);
            }
            float stored = pocketwatchTime.getStoredTime();
            Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
            ComponentType<ChunkStore, TimeAccelerationOverlay> overlayType
                    = MachinariaPlugin.get().getTimeAccelerationOverlayComponentType();
            TimeAccelerationOverlay overlay = chunkStore.getComponent(
                    blockRef,
                    overlayType
            );
            Float cost = 0f;
            long duration = System.currentTimeMillis() + 30_000L;
            // If no existing overlay, create new one at level 1
            if (overlay == null) {
                cost = costPerLevel;
                if (stored < cost) {
                    context.getState().state = InteractionState.Failed;
                    if(playerRefComponent != null) {
                        NotificationUtil.sendNotification(
                                playerRefComponent.getPacketHandler(),
                                Message.translation("server.items.Ancient_Pocket_Watch.notEnoughTime")
                                        .param("time", formatStoredTime(stored)));
                    }
                    return;
                } else {
                    chunkStore.addComponent(blockRef, overlayType, new TimeAccelerationOverlay(1, duration));
                    float newStored = stored - cost;
                    pocketwatchTime.setStoredTime(newStored);
                    worldChunk.setTicking(x, targetBlock.getY(), z, true);
                    if(playerRefComponent != null) {
                        NotificationUtil.sendNotification(
                                playerRefComponent.getPacketHandler(),
                                Message.translation("server.items.Ancient_Pocket_Watch.storedTime")
                                        .param("time", formatStoredTime(stored)));
                    }
                }
            } else { // If already has overlay, increase level by one if not maxed out
                int newLevel = Math.min(overlay.getLevel() + 1, maxLevel);
                if (newLevel > overlay.getLevel()) {
                    cost = costPerLevel * newLevel;
                    if (stored < cost) {
                        context.getState().state = InteractionState.Failed;
                        if(playerRefComponent != null) {
                            NotificationUtil.sendNotification(
                                    playerRefComponent.getPacketHandler(),
                                    Message.translation("server.items.Ancient_Pocket_Watch.notEnoughTime")
                                            .param("time", formatStoredTime(stored)));
                        }
                    } else {
                        overlay.setLevel(newLevel);
                        overlay.setExpiresAtEpochMillis(duration);
                        float newStored = stored - cost;
                        pocketwatchTime.setStoredTime(newStored);
                        worldChunk.setTicking(x, targetBlock.getY(), z, true);
                        if(playerRefComponent != null) {
                            NotificationUtil.sendNotification(
                                    playerRefComponent.getPacketHandler(),
                                    Message.translation("server.items.Ancient_Pocket_Watch.storedTime")
                                            .param("time", formatStoredTime(stored)));
                        }
                    }
                }
            }

        }
    }

    public static String formatStoredTime(float totalSeconds) {
        int secs = Math.max(0, (int) totalSeconds);
        int hours = secs / 3600;
        int minutes = (secs % 3600) / 60;
        int seconds = secs % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void simulateInteractWithBlock(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @org.jspecify.annotations.Nullable ItemStack itemStack, @NonNull World world, @NonNull Vector3i vector3i) {

    }
}

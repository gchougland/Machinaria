package com.hexvane.machinaria;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;

public class PocketwatchAccumulationSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    private final Query<EntityStore> query;
    @Nonnull
    private final ComponentType<EntityStore, Player> componentType;
    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> refComponentType = PlayerRef.getComponentType();
    @Nonnull
    private final ComponentType<EntityStore, PocketwatchStoredTimeComponent> pocketwatchTimeType;

    private static final String POCKETWATCH_ITEM_ID = "Ancient_Pocket_Watch";

    public PocketwatchAccumulationSystem(@NonNull ComponentType<EntityStore,Player> componentType) {
        this.componentType = componentType;
        this.pocketwatchTimeType = MachinariaPlugin.get().getPocketwatchStoredTimeComponentComponentType();
        this.query = Query.and(componentType, refComponentType);
    }

    @Override
    public void tick(
            float dt,
            int index,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> store,
            @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        Player player = archetypeChunk.getComponent(index, componentType);
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        CombinedItemContainer container = inventory.getCombinedBackpackStorageHotbar();
        short capacity = container.getCapacity();
        boolean hasPocketwatch = false;
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack != null && !ItemStack.isEmpty(stack) && POCKETWATCH_ITEM_ID.equals(stack.getItemId())) {
                hasPocketwatch = true;
                break;
            }
        }
        if (!hasPocketwatch) return;
        PocketwatchStoredTimeComponent pocketwatchTime = commandBuffer.getComponent(playerRef, pocketwatchTimeType);
        if (pocketwatchTime == null) {
            pocketwatchTime = new PocketwatchStoredTimeComponent(0);
            commandBuffer.addComponent(playerRef, pocketwatchTimeType, pocketwatchTime);
        }
        float current = pocketwatchTime.getStoredTime();
        float max = pocketwatchTime.getMaxStoredTime();
        float next = Math.min(current + dt, max);
        pocketwatchTime.setStoredTime(next);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }
}

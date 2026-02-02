package com.hexvane.machinaria;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public class PocketwatchStoredTimeComponent implements Component<EntityStore> {

    public static final BuilderCodec<PocketwatchStoredTimeComponent> CODEC = BuilderCodec.builder(
            PocketwatchStoredTimeComponent.class, PocketwatchStoredTimeComponent::new
    )
            .append(
                    new KeyedCodec<>("StoredTime", Codec.FLOAT),
                    (state, v) -> state.storedTime = v,
                    state -> state.storedTime
            )
            .add()
            .append(
                    new KeyedCodec<>("MaxStoredTime", Codec.FLOAT),
                    (state, v) -> state.maxStoredTime = v,
                    state -> state.maxStoredTime
            )
            .add()
            .build();

    protected float storedTime = 0;
    protected float maxStoredTime = 5000;

    public PocketwatchStoredTimeComponent() {
    }

    public PocketwatchStoredTimeComponent(float storedTime) {
        this.storedTime = storedTime;
    }

    public PocketwatchStoredTimeComponent(float storedTime, float maxStoredTime) {
        this.storedTime = storedTime;
        this.maxStoredTime = maxStoredTime;
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new PocketwatchStoredTimeComponent(this.storedTime, this.maxStoredTime);
    }

    public float getStoredTime() {
        return storedTime;
    }

    public void setStoredTime(float storedTime) {
        this.storedTime = storedTime;
    }

    public float getMaxStoredTime() {
        return maxStoredTime;
    }
}

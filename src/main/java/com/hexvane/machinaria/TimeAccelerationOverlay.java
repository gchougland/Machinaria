package com.hexvane.machinaria;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.Nullable;

public class TimeAccelerationOverlay implements Component<ChunkStore> {
    public static final BuilderCodec<TimeAccelerationOverlay> CODEC = BuilderCodec.builder(
            TimeAccelerationOverlay.class, TimeAccelerationOverlay::new
    )
            .append(
                    new KeyedCodec<>("Level", Codec.INTEGER),
                    (overlay, level) -> overlay.level = level,
                    overlay -> overlay.level == 1 ? null : overlay.level
            )
            .add()
            .append(
                    new KeyedCodec<>("ExpiresAtEpochMillis", Codec.LONG),
                    (overlay, v) -> overlay.expiresAtEpochMillis = v,
                    overlay -> overlay.expiresAtEpochMillis
            )
            .add()
            .build();
    protected int level;

    protected long expiresAtEpochMillis;

    public TimeAccelerationOverlay() {
    }

    public TimeAccelerationOverlay(int level, long expiresAtEpochMillis) {
        this.level = level;
        this.expiresAtEpochMillis = expiresAtEpochMillis;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return this.level;
    }

    public long getExpiresAtEpochMillis() {
        return expiresAtEpochMillis;
    }

    public void setExpiresAtEpochMillis(long expiresAtEpochMillis) {
        this.expiresAtEpochMillis = expiresAtEpochMillis;
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        return new TimeAccelerationOverlay(this.level, this.expiresAtEpochMillis);
    }
}

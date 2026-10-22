package com.hexvane.machinaria.heartworks;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

/**
 * Drives every {@link MovingPlatformComponent} each tick by setting its entity's transform; the engine's transform
 * system sends the change to clients, and players standing on the platform's Hard hitbox ride along.
 */
public final class MovingPlatformSystem extends EntityTickingSystem<EntityStore> {

    private static final double TWO_PI = Math.PI * 2;

    private final ComponentType<EntityStore, MovingPlatformComponent> platformType;
    private final ComponentType<EntityStore, TransformComponent> transformType;
    private final Query<EntityStore> query;

    public MovingPlatformSystem(@NonNull ComponentType<EntityStore, MovingPlatformComponent> platformType) {
        this.platformType = platformType;
        this.transformType = TransformComponent.getComponentType();
        this.query = Query.and(platformType, transformType);
    }

    @NonNull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> store,
            @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        MovingPlatformComponent platform = archetypeChunk.getComponent(index, platformType);
        TransformComponent transform = archetypeChunk.getComponent(index, transformType);
        if (platform == null || transform == null) {
            return;
        }
        if (!platform.anchored) {
            platform.origin.set(transform.getPosition());
            platform.baseRotation.set(transform.getRotation());
            platform.anchored = true;
        }
        platform.time += dt;

        Rotation3f rotation = new Rotation3f(platform.baseRotation);
        switch (platform.mode) {
            case "Turn" -> {
                double yaw = platform.baseRotation.yaw() + Math.toRadians(platform.degreesPerSecond) * platform.time;
                rotation.setYaw((float) wrap(yaw));
            }
            case "Swing" -> {
                double cycle = Math.max(0.1, platform.period);
                double angle = Math.toRadians(platform.amplitude) * Math.sin(TWO_PI * (platform.time / cycle + platform.phase));
                switch (platform.axis) {
                    case "Pitch" -> rotation.setPitch((float) (platform.baseRotation.pitch() + angle));
                    case "Yaw" -> rotation.setYaw((float) (platform.baseRotation.yaw() + angle));
                    default -> rotation.setRoll((float) (platform.baseRotation.roll() + angle));
                }
            }
            default -> {
                double s = slideFraction(platform);
                Vector3d target = new Vector3d(platform.offset).mul(s).add(platform.origin);
                transform.setPosition(target);
            }
        }
        if (!"Slide".equals(platform.mode)) {
            transform.setRotation(rotation);
            HeadRotation head = archetypeChunk.getComponent(index, HeadRotation.getComponentType());
            if (head != null) {
                head.setRotation(rotation);
            }
        }
    }

    /** 0 at the origin, 1 at the far end: wait, ease out, wait, ease back, over one period. */
    static double slideFraction(@NonNull MovingPlatformComponent platform) {
        double cycle = Math.max(0.1, platform.period);
        double pause = Math.max(0, Math.min(platform.pause, cycle / 2 - 0.05));
        double travel = (cycle - 2 * pause) / 2;
        double u = ((platform.time / cycle + platform.phase) % 1.0 + 1.0) % 1.0 * cycle;
        if (u < pause) {
            return 0;
        }
        if (u < pause + travel) {
            return smooth((u - pause) / travel);
        }
        if (u < 2 * pause + travel) {
            return 1;
        }
        return 1 - smooth((u - 2 * pause - travel) / travel);
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static double wrap(double radians) {
        double r = radians % TWO_PI;
        return r > Math.PI ? r - TWO_PI : r < -Math.PI ? r + TWO_PI : r;
    }
}

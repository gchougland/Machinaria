package com.hexvane.machinaria.heartworks;

import com.hexvane.machinaria.MachinariaPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

/**
 * Moves a prop entity on a fixed loop, so the Heartworks can have sliding platforms, lifts, turning cogs and a pendulum.
 * The entity carries a model and a Hard {@code HitboxCollision} that lets players stand on it and be carried
 * ({@code AllowEntityAnchoring}); this component only drives its transform. Written into prefabs as
 * {@code "MachinariaMovingPlatform": { "Mode": "Slide", "Offset": {"X": 0, "Y": 0, "Z": 8}, "Period": 8, "Pause": 1 }}.
 *
 * <ul>
 *   <li><b>Slide</b>: from where it was placed to {@code Offset} and back, eased, waiting {@code Pause} seconds at each
 *       end; one full cycle takes {@code Period} seconds.</li>
 *   <li><b>Turn</b>: turns about its vertical axis at {@code DegreesPerSecond} (negative turns the other way).</li>
 *   <li><b>Swing</b>: swings {@code Amplitude} degrees either side about {@code Axis} (Roll, Pitch or Yaw) once every
 *       {@code Period} seconds.</li>
 * </ul>
 * {@code Phase} (0 to 1) starts the loop part way through, so neighbours can move out of step.
 */
public class MovingPlatformComponent implements Component<EntityStore> {

    public static final BuilderCodec<MovingPlatformComponent> CODEC = BuilderCodec.builder(
            MovingPlatformComponent.class, MovingPlatformComponent::new
    )
            .append(new KeyedCodec<>("Mode", Codec.STRING), (c, v) -> c.mode = v, c -> c.mode)
            .add()
            .append(new KeyedCodec<>("Offset", Vector3dUtil.CODEC), (c, v) -> c.offset.set(v), c -> c.offset)
            .add()
            .append(new KeyedCodec<>("Period", Codec.FLOAT), (c, v) -> c.period = v, c -> c.period)
            .add()
            .append(new KeyedCodec<>("Pause", Codec.FLOAT), (c, v) -> c.pause = v, c -> c.pause)
            .add()
            .append(new KeyedCodec<>("Phase", Codec.FLOAT), (c, v) -> c.phase = v, c -> c.phase)
            .add()
            .append(new KeyedCodec<>("DegreesPerSecond", Codec.FLOAT), (c, v) -> c.degreesPerSecond = v, c -> c.degreesPerSecond)
            .add()
            .append(new KeyedCodec<>("Amplitude", Codec.FLOAT), (c, v) -> c.amplitude = v, c -> c.amplitude)
            .add()
            .append(new KeyedCodec<>("Axis", Codec.STRING), (c, v) -> c.axis = v, c -> c.axis)
            .add()
            // Where the loop is anchored, captured on the first tick and saved, so a reload resumes the same loop.
            .append(new KeyedCodec<>("Anchored", Codec.BOOLEAN), (c, v) -> c.anchored = v, c -> c.anchored)
            .add()
            .append(new KeyedCodec<>("Origin", Vector3dUtil.CODEC), (c, v) -> c.origin.set(v), c -> c.origin)
            .add()
            .append(new KeyedCodec<>("BaseRotation", Rotation3f.CODEC), (c, v) -> c.baseRotation.set(v), c -> c.baseRotation)
            .add()
            .append(new KeyedCodec<>("Time", Codec.FLOAT), (c, v) -> c.time = v, c -> c.time)
            .add()
            .build();

    public static ComponentType<EntityStore, MovingPlatformComponent> getComponentType() {
        return MachinariaPlugin.get().getMovingPlatformComponentType();
    }

    String mode = "Slide";
    final Vector3d offset = new Vector3d();
    float period = 8f;
    float pause = 1f;
    float phase = 0f;
    float degreesPerSecond = 20f;
    float amplitude = 30f;
    String axis = "Roll";
    boolean anchored;
    final Vector3d origin = new Vector3d();
    final Rotation3f baseRotation = new Rotation3f();
    float time;

    public MovingPlatformComponent() {
    }

    @Override
    public @NonNull MovingPlatformComponent clone() {
        MovingPlatformComponent copy = new MovingPlatformComponent();
        copy.mode = mode;
        copy.offset.set(offset);
        copy.period = period;
        copy.pause = pause;
        copy.phase = phase;
        copy.degreesPerSecond = degreesPerSecond;
        copy.amplitude = amplitude;
        copy.axis = axis;
        copy.anchored = anchored;
        copy.origin.set(origin);
        copy.baseRotation.set(baseRotation);
        copy.time = time;
        return copy;
    }
}

package com.hexvane.machinaria.heartworks;

import com.hypixel.hytale.builtin.triggervolumes.YawRotation;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.PositionDistanceOffsetType;
import com.hypixel.hytale.protocol.PositionType;
import com.hypixel.hytale.protocol.RotationType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.entity.entities.player.CameraManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A short cutscene shot, as a trigger volume effect: for {@code Duration} seconds every player within {@code Radius}
 * of the volume (or every player in the world when Radius is 0) sees the scene from a fixed camera that glides from
 * {@code From} to {@code To} while looking at {@code LookAt}, with their movement held; then their own camera comes
 * back. Positions are offsets from the volume's origin and turn with the prefab, like the built-in effects' volume
 * anchors. Written into a prefab's trigger volume as
 * {@code {"Type": "MachinariaCameraShot", "Event": "SIGNAL_RECEIVED", "Delay": 2, "From": {"X": 1, "Y": 2.4, "Z": 7.5},
 * "To": {...}, "LookAt": {...}, "Duration": 4, "Glide": 0.03, "Radius": 128}}.
 *
 * <p>Vanilla has no camera move in data (its camera effects are shakes only), so this uses the server camera packet the
 * engine's own camera commands send ({@code SetServerCamera} with a custom position and rotation), and resets it as
 * {@link CameraManager#resetCamera} does.
 */
public class CameraShotEffect extends TriggerEffect {

    public static final String TYPE = "MachinariaCameraShot";

    @NonNull
    public static final BuilderCodec<CameraShotEffect> CODEC = BuilderCodec.builder(CameraShotEffect.class, CameraShotEffect::new, BASE_CODEC)
            .append(new KeyedCodec<>("From", Vector3dUtil.CODEC, false), (e, v) -> e.from = v, e -> e.from)
            .add()
            .append(new KeyedCodec<>("To", Vector3dUtil.CODEC, false), (e, v) -> e.to = v, e -> e.to)
            .add()
            .append(new KeyedCodec<>("LookAt", Vector3dUtil.CODEC, false), (e, v) -> e.lookAt = v, e -> e.lookAt)
            .add()
            .append(new KeyedCodec<>("Duration", Codec.FLOAT, false), (e, v) -> e.duration = v, e -> e.duration)
            .add()
            .append(new KeyedCodec<>("Glide", Codec.FLOAT, false), (e, v) -> e.glide = v, e -> e.glide)
            .add()
            .append(new KeyedCodec<>("Radius", Codec.FLOAT, false), (e, v) -> e.radius = v, e -> e.radius)
            .add()
            .append(new KeyedCodec<>("HoldMovement", Codec.BOOLEAN, false), (e, v) -> e.holdMovement = v, e -> e.holdMovement)
            .add()
            .build();

    /** Seconds after the cut before the glide to {@code To} starts, so the client has settled on {@code From}. */
    private static final long GLIDE_START_MS = 150;

    private Vector3d from = new Vector3d(0, 3, 6);
    private Vector3d to = null;
    private Vector3d lookAt = new Vector3d();
    private float duration = 3f;
    private float glide = 0.03f;
    private float radius = 0f;
    private boolean holdMovement = true;

    @Override
    public void execute(@NonNull TriggerContext context) {
        if (from == null || lookAt == null) return;
        Vector3d origin = context.getVolume().getPosition();
        Vector3d start = new Vector3d(origin).add(from);
        Vector3d end = to != null ? new Vector3d(origin).add(to) : new Vector3d(start);
        Vector3d target = new Vector3d(origin).add(lookAt);
        World world = context.getStore().getExternalData().getWorld();

        List<PlayerRef> players = new ArrayList<>();
        double r2 = (double) radius * radius;
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            if (playerRef == null || !playerRef.isValid()) continue;
            if (radius > 0 && playerRef.getTransform().getPosition().distanceSquared(origin) > r2) continue;
            players.add(playerRef);
        }
        if (players.isEmpty()) return;

        // The cut: straight to the first camera position (lerp 1), then a slow glide toward the second.
        ServerCameraSettings cut = shot(start, target, 1f);
        ServerCameraSettings push = shot(end, target, Math.max(0.001f, glide));
        for (PlayerRef playerRef : players) {
            playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, cut));
        }
        world.scheduleAfter(() -> {
            for (PlayerRef playerRef : players) {
                if (playerRef.isValid()) playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, push));
            }
        }, GLIDE_START_MS, TimeUnit.MILLISECONDS);
        world.scheduleAfter(() -> {
            for (PlayerRef playerRef : players) {
                if (!playerRef.isValid()) continue;
                Ref<EntityStore> ref = playerRef.getReference();
                CameraManager cameraManager = ref != null && ref.isValid() ? ref.getStore().getComponent(ref, CameraManager.getComponentType()) : null;
                if (cameraManager != null) {
                    cameraManager.resetCamera(playerRef);
                } else {
                    playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, false, null));
                }
            }
        }, Math.max(250L, (long) (duration * 1000f)), TimeUnit.MILLISECONDS);
    }

    @NonNull
    private ServerCameraSettings shot(@NonNull Vector3d at, @NonNull Vector3d target, float lerp) {
        ServerCameraSettings s = new ServerCameraSettings();
        s.positionLerpSpeed = lerp;
        s.rotationLerpSpeed = lerp;
        s.isFirstPerson = false;
        s.hideHeldItem = true;
        s.displayReticle = false;
        s.displayCursor = false;
        s.eyeOffset = false;
        s.distance = 0f;
        s.positionDistanceOffsetType = PositionDistanceOffsetType.None;
        s.positionType = PositionType.Custom;
        s.position = new Position(at.x, at.y, at.z);
        s.rotationType = RotationType.Custom;
        s.rotation = look(at, target);
        if (holdMovement) s.movementMultiplier = new Vector3f(0f, 0f, 0f);
        return s;
    }

    /** Yaw 0 looks toward -Z and yaw turns toward -X (the entity convention); pitch is up positive, in radians. */
    @NonNull
    static Direction look(@NonNull Vector3d from, @NonNull Vector3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        float yaw = (float) Math.atan2(-dx, -dz);
        float pitch = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        return new Direction(yaw, pitch, 0f);
    }

    @Override
    public void rotateInPlace(float yawRadians, @NonNull Vector3d volumeOrigin) {
        if (from != null) YawRotation.rotate(from, yawRadians);
        if (to != null) YawRotation.rotate(to, yawRadians);
        if (lookAt != null) YawRotation.rotate(lookAt, yawRadians);
    }
}

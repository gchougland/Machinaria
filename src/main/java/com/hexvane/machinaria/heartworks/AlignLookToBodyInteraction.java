package com.hexvane.machinaria.heartworks;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Turns the user's look (head yaw) to its body yaw, keeping the look's pitch.
 *
 * <p>An NPC fires projectiles along its look, but particles attached to its model turn with its body. The
 * Heartwinder's Gear Toss draws its lanes on the body and throws along the look, so a body lagging the look put the
 * gears off the lanes. The toss runs this just before the throw, so the gears go exactly where the lanes were shown.
 */
public final class AlignLookToBodyInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<AlignLookToBodyInteraction> CODEC = BuilderCodec.builder(
            AlignLookToBodyInteraction.class, AlignLookToBodyInteraction::new, SimpleInstantInteraction.CODEC
    )
            .documentation("Turn the user's look to its body yaw, so a projectile fired next follows the body.")
            .build();

    @Override
    protected void firstRun(
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> ref = context.getEntity();
        if (commandBuffer == null || ref == null || !ref.isValid()) {
            return;
        }
        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation head = commandBuffer.getComponent(ref, HeadRotation.getComponentType());
        if (transform == null || head == null) {
            return;
        }
        Rotation3f look = new Rotation3f(head.getRotation());
        look.setYaw(transform.getRotation().yaw());
        head.setRotation(look);
    }
}

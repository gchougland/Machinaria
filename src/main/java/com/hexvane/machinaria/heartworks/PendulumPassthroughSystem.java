package com.hexvane.machinaria.heartworks;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ProjectilePassthrough;
import com.hypixel.hytale.server.core.modules.entity.system.ModelSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * The Winding Chamber's pendulums: a small box for the server, a box round the whole swing for the client.
 *
 * <p>The client culls an entity by the hitbox of the model the server sends it (a bounding sphere on that box, tested
 * against the view frustum). The pendulum's origin is on the floor and its blade swings 9 blocks out, so a small
 * hitbox made the blade vanish when the floor point left the view. The spawn test, though, needs the model asset's
 * hitbox free of blocks, and a box round the whole swing fails it in the arena.
 *
 * <p>So the model asset keeps a small hitbox (spawn test, server bounding box, motion controller, projectiles), and on
 * every add (spawn and chunk load) this swaps the entity's model for the same model with {@link #CLIENT_BOX}, which
 * only the client reads. The server bounding box is put back to the asset's small box. Projectiles fly through the
 * pendulum as through a vanilla tornado ability entity.
 */
public final class PendulumPassthroughSystem extends HolderSystem<EntityStore> {

    private static final String PENDULUM_ROLE = "Machinaria_Heartworks_Heartwinder_Pendulum";

    /**
     * The whole swing in model space: the blade stays within x 0.5, y 1.3 to 21.4 and z 9.2 at either end of
     * PendulumSwing. Sent to the client only.
     */
    private static final Box CLIENT_BOX = new Box(-0.5, 0, -9.4, 0.5, 21.5, 9.4);

    @Nonnull
    private final Query<EntityStore> query = Query.and(NPCEntity.getComponentType(), ModelComponent.getComponentType());

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = Set.of(new SystemDependency<>(Order.AFTER, ModelSystems.ModelSpawned.class));

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void onEntityAdd(@Nonnull Holder<EntityStore> holder, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store) {
        final NPCEntity npc = holder.getComponent(NPCEntity.getComponentType());
        if (npc == null || !PENDULUM_ROLE.equals(npc.getRoleName())) {
            return;
        }
        holder.ensureComponent(ProjectilePassthrough.getComponentType());

        final ModelComponent modelComponent = holder.getComponent(ModelComponent.getComponentType());
        if (modelComponent == null) {
            return;
        }
        final Model model = modelComponent.getModel();
        final Box serverBox = model != null ? model.getBoundingBox() : null;
        // Already the client box (taller than any box the asset spawns with): nothing to swap.
        if (serverBox == null || serverBox.height() >= CLIENT_BOX.height()) {
            return;
        }
        final ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(model.getModelAssetId());
        if (modelAsset == null) {
            return;
        }

        final Model clientModel = Model.createScaledModel(modelAsset, model.getScale(), model.getRandomAttachmentIds(), new Box(CLIENT_BOX));
        holder.putComponent(ModelComponent.getComponentType(), new ModelComponent(clientModel));

        final BoundingBox boundingBox = holder.getComponent(BoundingBox.getComponentType());
        if (boundingBox != null) {
            boundingBox.setBoundingBox(serverBox);
            boundingBox.setBaseModelBox(serverBox);
        }
    }

    @Override
    public void onEntityRemoved(@Nonnull Holder<EntityStore> holder, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store) {
    }
}

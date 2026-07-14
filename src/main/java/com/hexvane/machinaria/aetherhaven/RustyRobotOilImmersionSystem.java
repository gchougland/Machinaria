package com.hexvane.machinaria.aetherhaven;

import com.hexvane.machinaria.MachinariaConstants;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/**
 * When a rusty robot item entity sits in Oil, either frees Rivet (first time) or turns into scrap
 * after the rescue quest is complete. Mutations go through {@link CommandBuffer} / {@code world.execute}.
 *
 * <p>Oil detection does <b>not</b> require Aetherhaven soft-integration to be enabled. Soft AH is only
 * used to know whether rescue was already completed (scrap vs NPC).
 */
public final class RustyRobotOilImmersionSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final double PLAYER_SEARCH_RADIUS = 32.0;
    private static final double RESCUE_NPC_NEARBY_RADIUS = 16.0;
    /** Scan a tall column: drops often float slightly above the fluid surface. */
    private static final int FLUID_SCAN_Y_ABOVE = 2;
    private static final int FLUID_SCAN_Y_BELOW = 4;
    private static final long DEBUG_INTERVAL_MS = 3000L;

    private final Query<EntityStore> query;
    private final ComponentTypeHolder types;

    private int oilIndex = Integer.MIN_VALUE;
    private int oilSourceIndex = Integer.MIN_VALUE;
    private boolean fluidIndexesResolved;
    private long lastDebugLogMs;
    private boolean loggedRegistered;

    public RustyRobotOilImmersionSystem() {
        this.types = new ComponentTypeHolder();
        this.query = Query.and(types.itemType, types.transformType);
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!loggedRegistered) {
            loggedRegistered = true;
            LOGGER.atInfo().log("RustyRobotOilImmersionSystem is ticking ItemEntities.");
        }

        // Soft AH is optional here — immersion must work even if get()/soft-enable fails.
        AetherhavenIntegration.tryEnable();

        ItemComponent itemComponent = archetypeChunk.getComponent(index, types.itemType);
        TransformComponent transform = archetypeChunk.getComponent(index, types.transformType);
        if (itemComponent == null || transform == null) {
            return;
        }
        ItemStack stack = itemComponent.getItemStack();
        if (stack == null || stack.isEmpty() || !MachinariaConstants.ITEM_RUSTY_ROBOT.equals(stack.getItemId())) {
            return;
        }

        Vector3d position = transform.getPosition();
        World world = store.getExternalData().getWorld();
        boolean inOil = isInOil(world, position);
        maybeDebug(world, position, inOil, stack.getItemId());

        if (!inOil) {
            return;
        }

        Ref<EntityStore> itemRef = archetypeChunk.getReferenceTo(index);
        Vector3d spawnPos = new Vector3d(position);
        NearestPlayer nearest = findNearestPlayer(store, spawnPos);

        boolean rescued = nearest != null && AetherhavenIntegration.hasCompletedRescue(nearest.playerUuid());
        if (!rescued && hasNearbyRescueNpc(store, spawnPos)) {
            LOGGER.atFine().log("Skipping oil immersion; rescue NPC already nearby.");
            return;
        }

        int quantity = Math.max(1, stack.getQuantity());
        Rotation3f facePlayer = nearest != null ? yawToward(spawnPos, nearest.position()) : new Rotation3f();
        commandBuffer.removeEntity(itemRef, RemoveReason.REMOVE);
        LOGGER.atInfo().log(
            "Rusty robot immersed in oil at %.1f,%.1f,%.1f (rescuedQuest=%s ah=%s)",
            spawnPos.x,
            spawnPos.y,
            spawnPos.z,
            rescued,
            AetherhavenIntegration.isEnabled()
        );

        if (rescued) {
            world.execute(() -> spawnScrap(world, spawnPos, quantity));
            return;
        }
        world.execute(() -> spawnRescueNpc(world, spawnPos, facePlayer));
    }

    private void maybeDebug(@Nonnull World world, @Nonnull Vector3d position, boolean inOil, @Nonnull String itemId) {
        long now = System.currentTimeMillis();
        if (now - lastDebugLogMs < DEBUG_INTERVAL_MS) {
            return;
        }
        lastDebugLogMs = now;
        int bx = MathUtil.floor(position.x);
        int by = MathUtil.floor(position.y);
        int bz = MathUtil.floor(position.z);
        int fluidHere = fluidIdAt(world, bx, by, bz);
        int fluidBelow = fluidIdAt(world, bx, by - 1, bz);
        LOGGER.atInfo().log(
            "Rusty robot drop seen itemId=%s pos=%.2f,%.2f,%.2f inOil=%s fluid@feet=%s(%s) fluid@-1=%s(%s) ahEnabled=%s",
            itemId,
            position.x,
            position.y,
            position.z,
            inOil,
            fluidHere,
            fluidName(fluidHere),
            fluidBelow,
            fluidName(fluidBelow),
            AetherhavenIntegration.isEnabled()
        );
    }

    private boolean isInOil(@Nonnull World world, @Nonnull Vector3d position) {
        resolveFluidIndexes();
        int bx = MathUtil.floor(position.x);
        int by = MathUtil.floor(position.y);
        int bz = MathUtil.floor(position.z);
        for (int dy = FLUID_SCAN_Y_ABOVE; dy >= -FLUID_SCAN_Y_BELOW; dy--) {
            if (isOilFluidId(fluidIdAt(world, bx, by + dy, bz))) {
                return true;
            }
        }
        // Neighbor columns: item can sit against the lip of a fluid source.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                for (int dy = 1; dy >= -2; dy--) {
                    if (isOilFluidId(fluidIdAt(world, bx + dx, by + dy, bz + dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Same chunk-section probe used by CozyTales fishing ({@code FishFluidHelper}).
     */
    private static int fluidIdAt(@Nonnull World world, int x, int y, int z) {
        var sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null) {
            return Fluid.EMPTY_ID;
        }
        FluidSection fluidSection =
            world.getChunkStore().getStore().getComponentConcurrent(sectionRef, FluidSection.getComponentType());
        if (fluidSection == null) {
            return Fluid.EMPTY_ID;
        }
        return fluidSection.getFluidId(x, y, z);
    }

    @Nonnull
    private static String fluidName(int fluidId) {
        if (fluidId == Fluid.EMPTY_ID || fluidId == Integer.MIN_VALUE) {
            return "empty";
        }
        Fluid fluid = Fluid.getAssetMap().getAsset(fluidId);
        return fluid != null && fluid.getId() != null ? fluid.getId() : ("#" + fluidId);
    }

    private boolean isOilFluidId(int fluidId) {
        if (fluidId == Fluid.EMPTY_ID || fluidId == Integer.MIN_VALUE) {
            return false;
        }
        if (fluidId == oilIndex || fluidId == oilSourceIndex) {
            return true;
        }
        Fluid fluid = Fluid.getAssetMap().getAsset(fluidId);
        if (fluid == null) {
            return false;
        }
        String id = fluid.getId();
        if (id == null) {
            return false;
        }
        if (MachinariaConstants.FLUID_OIL.equals(id) || MachinariaConstants.FLUID_OIL_SOURCE.equals(id)) {
            return true;
        }
        String lower = id.toLowerCase();
        return lower.equals("oil") || lower.equals("oil_source") || lower.startsWith("oil_");
    }

    private void resolveFluidIndexes() {
        if (fluidIndexesResolved) {
            return;
        }
        try {
            oilIndex = Fluid.getAssetMap().getIndex(MachinariaConstants.FLUID_OIL);
            oilSourceIndex = Fluid.getAssetMap().getIndex(MachinariaConstants.FLUID_OIL_SOURCE);
            if (oilIndex != Integer.MIN_VALUE || oilSourceIndex != Integer.MIN_VALUE) {
                fluidIndexesResolved = true;
                LOGGER.atInfo().log(
                    "Resolved oil fluid indexes Oil=%s Oil_Source=%s",
                    oilIndex,
                    oilSourceIndex
                );
            }
        } catch (RuntimeException e) {
            oilIndex = Integer.MIN_VALUE;
            oilSourceIndex = Integer.MIN_VALUE;
            LOGGER.atWarning().withCause(e).log("Failed to resolve Oil fluid indexes");
        }
    }

    @Nullable
    private static NearestPlayer findNearestPlayer(@Nonnull Store<EntityStore> store, @Nonnull Vector3d position) {
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial =
            store.getResource(EntityModule.get().getPlayerSpatialResourceType());
        List<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
        playerSpatial.getSpatialStructure().collect(position, PLAYER_SEARCH_RADIUS, playerRefs);
        double bestDistSq = Double.MAX_VALUE;
        NearestPlayer best = null;
        for (Ref<EntityStore> ref : playerRefs) {
            if (ref == null || !ref.isValid()) {
                continue;
            }
            if (store.getComponent(ref, Player.getComponentType()) == null) {
                continue;
            }
            UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
            TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
            if (uuid == null || tc == null) {
                continue;
            }
            double distSq = tc.getPosition().distanceSquared(position);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = new NearestPlayer(uuid.getUuid(), new Vector3d(tc.getPosition()));
            }
        }
        return best;
    }

    private static boolean hasNearbyRescueNpc(@Nonnull Store<EntityStore> store, @Nonnull Vector3d position) {
        AtomicBoolean found = new AtomicBoolean(false);
        store.forEachEntityParallel(NPCEntity.getComponentType(), (i, chunk, buffer) -> {
            if (found.get()) {
                return;
            }
            NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());
            if (npc == null || npc.getRoleName() == null) {
                return;
            }
            if (!MachinariaConstants.NPC_MECHANIC_RESCUE.equals(npc.getRoleName().trim())) {
                return;
            }
            TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
            if (tc == null) {
                return;
            }
            if (tc.getPosition().distanceSquared(position) <= RESCUE_NPC_NEARBY_RADIUS * RESCUE_NPC_NEARBY_RADIUS) {
                found.set(true);
            }
        });
        return found.get();
    }

    private static void spawnScrap(@Nonnull World world, @Nonnull Vector3d position, int quantity) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        Holder<EntityStore> holder = ItemComponent.generateItemDrop(
            store,
            new ItemStack(MachinariaConstants.ITEM_SCRAP_METAL, quantity),
            new Vector3d(position),
            new Rotation3f(),
            0f,
            0.15f,
            0f
        );
        if (holder != null) {
            store.addEntity(holder, AddReason.SPAWN);
            LOGGER.atInfo().log("Converted immersed rusty robot into scrap x%s", quantity);
        }
    }

    private static void spawnRescueNpc(
        @Nonnull World world,
        @Nonnull Vector3d position,
        @Nonnull Rotation3f rotation
    ) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        if (hasNearbyRescueNpc(store, position)) {
            return;
        }
        NPCPlugin npc = NPCPlugin.get();
        if (npc == null) {
            LOGGER.atWarning().log("NPCPlugin missing; cannot spawn Machinaria_Mechanic_Rescue");
            return;
        }
        float yaw = rotation.yaw();
        if (!Float.isFinite(yaw)) {
            yaw = 0f;
        }
        Rotation3f safeRotation = new Rotation3f(0f, yaw, 0f);
        var pair = npc.spawnNPC(store, MachinariaConstants.NPC_MECHANIC_RESCUE, null, position, safeRotation);
        if (pair == null) {
            LOGGER.atWarning().log(
                "Failed to spawn Machinaria_Mechanic_Rescue at %s,%s,%s (role missing or not spawnable?)",
                position.x,
                position.y,
                position.z
            );
            return;
        }
        Ref<EntityStore> ref = pair.first();
        NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity != null) {
            npcEntity.getLeashPoint().set(position);
            npcEntity.setLeashHeading(safeRotation.yaw());
            npcEntity.setLeashPitch(0f);
        }
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc != null) {
            tc.getPosition().set(position);
            tc.getRotation().set(0f, safeRotation.yaw(), 0f);
            store.putComponent(ref, TransformComponent.getComponentType(), tc);
        }
        LOGGER.atInfo().log("Spawned Machinaria_Mechanic_Rescue after oil immersion");
    }

    @Nonnull
    private static Rotation3f yawToward(@Nonnull Vector3d from, @Nonnull Vector3d to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        if (dx * dx + dz * dz < 1.0e-8) {
            return new Rotation3f();
        }
        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        if (!Float.isFinite(yaw)) {
            return new Rotation3f();
        }
        return new Rotation3f(0f, yaw, 0f);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    private record NearestPlayer(@Nonnull UUID playerUuid, @Nonnull Vector3d position) {}

    private static final class ComponentTypeHolder {
        private final ComponentType<EntityStore, ItemComponent> itemType = ItemComponent.getComponentType();
        private final ComponentType<EntityStore, TransformComponent> transformType =
            TransformComponent.getComponentType();
    }
}

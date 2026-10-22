package com.hexvane.machinaria.command;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.prefabeditor.PrefabEditSessionManager;
import com.hypixel.hytale.builtin.buildertools.prefabeditor.PrefabEditorCreationSettings;
import com.hypixel.hytale.builtin.buildertools.prefabeditor.commands.PrefabEditLoadCommand;
import com.hypixel.hytale.builtin.buildertools.prefabeditor.enums.PrefabRootDirectory;
import com.hypixel.hytale.builtin.buildertools.prefabeditor.enums.PrefabRowSplitMode;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Opens the vanilla prefab editor (/pedit load) on every Machinaria prefab, or on one folder of them, one row per
 * folder. Saving there writes back into the Machinaria asset pack.
 */
public final class MachinariaPrefabsCommand extends AbstractAsyncPlayerCommand {

    private static final String PACK_NAME = "Machinaria";
    private static final Pattern SAFE_FOLDER = Pattern.compile("[A-Za-z0-9_]+(/[A-Za-z0-9_]+)*");

    private final RequiredArg<String> folderArg = withRequiredArg("folder",
        "A folder under the Machinaria prefabs, such as Heartworks, Heartworks/A2 or Pumpworks", ArgTypes.STRING);

    public MachinariaPrefabsCommand() {
        super("machinariaprefabs", "Open the prefab editor with the Machinaria prefabs in one folder");
        addAliases("mprefabs");
        setPermissionGroups("hytale:WorldEditor");
        // With no folder: every Machinaria prefab.
        addUsageVariant(new AbstractAsyncPlayerCommand("Open the prefab editor with every Machinaria prefab") {
            @Nonnull
            @Override
            protected CompletableFuture<Void> executeAsync(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                                                           @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef,
                                                           @Nonnull final World world) {
                return open(context, store, ref, playerRef, "");
            }
        });
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                                                   @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef,
                                                   @Nonnull final World world) {
        return open(context, store, ref, playerRef, folderArg.get(context));
    }

    @Nonnull
    private static CompletableFuture<Void> open(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                                                @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef,
                                                @Nonnull final String folderInput) {
        final var sessions = BuilderToolsPlugin.get().getPrefabEditSessionManager();
        if (sessions.isEditingAPrefab(playerRef.getUuid())) {
            context.sendMessage(Message.raw("You already have a prefab editor open. Leave it with /pedit exit first."));
            return CompletableFuture.completedFuture(null);
        }

        final String packName = findPackName();
        if (packName == null) {
            context.sendMessage(Message.raw("The Machinaria prefabs could not be found."));
            return CompletableFuture.completedFuture(null);
        }

        String folder = folderInput.trim().replace('\\', '/');
        while (folder.endsWith("/")) folder = folder.substring(0, folder.length() - 1);
        if (!folder.isEmpty() && !SAFE_FOLDER.matcher(folder).matches()) {
            context.sendMessage(Message.raw("Give a folder name like Heartworks or Heartworks/A2."));
            return CompletableFuture.completedFuture(null);
        }
        final String path = packName + "/" + (folder.isEmpty() ? "" : folder + "/");

        final var settings = new PrefabEditorCreationSettings(
            PrefabRootDirectory.ASSET,
            List.of(path),
            PrefabEditLoadCommand.DEFAULT_PASTE_LEVEL_GOAL,
            PrefabEditLoadCommand.DEFAULT_BLOCKS_BETWEEN_MULTI_PREFABS,
            PrefabEditLoadCommand.DEFAULT_WORLD_GEN_TYPE,
            PrefabEditLoadCommand.DEFAULT_BLOCKS_ABOVE_SURFACE,
            PrefabEditLoadCommand.DEFAULT_PREFAB_STACKING_AXIS,
            PrefabEditLoadCommand.DEFAULT_PREFAB_ALIGNMENT,
            true,   // recursive: every subfolder
            false,  // loadChildren
            true,   // loadEntities: keep spawn markers and trigger volumes, so saving does not drop them
            false,  // enableWorldTicking: no block updates or triggers firing while editing
            PrefabRowSplitMode.BY_ALL_SUBFOLDERS,
            PrefabEditSessionManager.DEFAULT_CHUNK_ENVIRONMENT,
            PrefabEditSessionManager.DEFAULT_GRASS_TINT_HEX
        );

        final var player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return CompletableFuture.completedFuture(null);

        context.sendMessage(Message.raw("Loading the Machinaria prefabs" + (folder.isEmpty() ? "" : " in " + folder) + "..."));
        final var result = sessions.loadPrefabAndCreateEditSession(ref, player, settings, store);
        if (result == null) {
            final var reason = settings.getFailureMessage();
            context.sendMessage(reason != null ? reason : Message.raw("The prefab editor could not open."));
            return CompletableFuture.completedFuture(null);
        }
        return result;
    }

    /** The name the prefab editor knows the Machinaria asset pack by. */
    private static String findPackName() {
        for (final var packPath : PrefabStore.get().getAllAssetPrefabPaths()) {
            if (!packPath.isFromAssetPack()) continue;
            final String display = packPath.getDisplayName();
            if (display.equals(PACK_NAME) || packPath.getPackName().endsWith(":" + PACK_NAME)) return display;
        }
        return null;
    }
}

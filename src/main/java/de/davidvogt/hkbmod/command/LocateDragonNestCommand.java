package de.davidvogt.hkbmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.davidvogt.hkbmod.block.ModBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to locate the nearest dragon nest.
 * Usage: /locate_dragonnest [radius]
 *
 * Searches for dragon nest blocks in a spiral pattern around the player.
 * Default search radius is 5000 blocks, can be specified up to 10000.
 */
public class LocateDragonNestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("locate_dragonnest")
                        .requires(source -> source.hasPermission(2)) // Require OP level 2

                        // /locate_dragonnest (with default radius)
                        .executes(context -> locateNest(context, 5000))

                        // /locate_dragonnest <radius>
                        .then(Commands.argument("radius", IntegerArgumentType.integer(100, 10000))
                                .executes(context -> locateNest(context, IntegerArgumentType.getInteger(context, "radius"))))
        );
    }

    private static int locateNest(CommandContext<CommandSourceStack> context, int searchRadius) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos playerPos = BlockPos.containing(source.getPosition());

        source.sendSuccess(
                () -> Component.literal("Suche nach Drachennestern im Umkreis von " + searchRadius + " Blöcken..."),
                false
        );

        // Search in a spiral pattern for efficiency
        List<BlockPos> foundNests = new ArrayList<>();
        int chunksSearchedCount = 0;
        long startTime = System.currentTimeMillis();

        // Search in chunks (16x16 areas) in a spiral pattern
        int maxChunkRadius = searchRadius / 16;

        outerLoop:
        for (int radius = 0; radius <= maxChunkRadius; radius++) {
            // Search the square ring at this radius
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Skip interior points (we already checked those)
                    if (Math.abs(dx) < radius && Math.abs(dz) < radius) {
                        continue;
                    }

                    int chunkX = (playerPos.getX() >> 4) + dx;
                    int chunkZ = (playerPos.getZ() >> 4) + dz;

                    // Search this chunk for dragon nests
                    BlockPos nestPos = searchChunkForNest(level, chunkX, chunkZ);
                    if (nestPos != null) {
                        double distance = Math.sqrt(playerPos.distSqr(nestPos));
                        if (distance <= searchRadius) {
                            foundNests.add(nestPos);
                        }
                    }

                    chunksSearchedCount++;

                    // If we found at least 3 nests, we can stop early
                    if (foundNests.size() >= 3) {
                        break outerLoop;
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        final int chunksSearched = chunksSearchedCount;

        if (foundNests.isEmpty()) {
            source.sendFailure(Component.literal(
                    "Kein Drachennest im Umkreis von " + searchRadius + " Blöcken gefunden. " +
                    "(" + chunksSearched + " Chunks durchsucht in " + duration + "ms)"
            ));
            return 0;
        }

        // Find the closest nest
        BlockPos closestNest = null;
        double closestDistance = Double.MAX_VALUE;

        for (BlockPos nest : foundNests) {
            double distance = Math.sqrt(playerPos.distSqr(nest));
            if (distance < closestDistance) {
                closestDistance = distance;
                closestNest = nest;
            }
        }

        final BlockPos finalNest = closestNest;
        final double finalDistance = closestDistance;

        // Send coordinates
        source.sendSuccess(() -> Component.literal(
                String.format("Nächstes Drachennest gefunden bei: [%d, %d, %d] (%.1f Blöcke entfernt)",
                        finalNest.getX(), finalNest.getY(), finalNest.getZ(), finalDistance)
        ), true);

        source.sendSuccess(() -> Component.literal(
                String.format("Teleport-Befehl: /tp @s %d %d %d",
                        finalNest.getX(), finalNest.getY(), finalNest.getZ())
        ), false);

        // If multiple nests were found, list them
        if (foundNests.size() > 1) {
            source.sendSuccess(
                    () -> Component.literal("Gefunden: " + foundNests.size() + " Drachennester in " +
                            chunksSearched + " Chunks (" + duration + "ms)"),
                    false
            );
        }

        return 1;
    }

    /**
     * Searches a single chunk for dragon nest blocks
     * @return The position of a dragon nest block, or null if none found
     */
    private static BlockPos searchChunkForNest(ServerLevel level, int chunkX, int chunkZ) {
        // Only search if the chunk is loaded or can be loaded
        if (!level.hasChunk(chunkX, chunkZ)) {
            return null;
        }

        // Search the chunk area for dragon nest blocks
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;

        // Dragon nests spawn on mountains (y=100-256)
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                for (int y = 100; y <= 256; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.is(ModBlocks.DRAGON_NEST.get())) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }
}

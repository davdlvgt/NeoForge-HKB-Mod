package de.davidvogt.hkbmod.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Utility class for handling client/server side operations safely.
 * This helps prevent common issues in multiplayer environments.
 */
public class SideUtils {

    /**
     * Check if we're running on the client side.
     * @return true if on client, false if on server
     */
    public static boolean isClientSide() {
        return FMLEnvironment.dist.isClient();
    }

    /**
     * Check if we're running on the server side.
     * @return true if on server, false if on client
     */
    public static boolean isServerSide() {
        return !FMLEnvironment.dist.isClient();
    }

    /**
     * Check if a level is on the server side.
     * This is useful for logic that should only run on the server.
     * @param level The level to check
     * @return true if the level is server-side
     */
    public static boolean isServerLevel(Level level) {
        return level != null && !level.isClientSide;
    }

    /**
     * Check if a level is on the client side.
     * This is useful for client-only rendering or UI logic.
     * @param level The level to check
     * @return true if the level is client-side
     */
    public static boolean isClientLevel(Level level) {
        return level != null && level.isClientSide;
    }

    /**
     * Safely cast a player to ServerPlayer if on server side.
     * @param player The player to cast
     * @return ServerPlayer if on server, null otherwise
     */
    public static ServerPlayer asServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer && isServerLevel(player.level())) {
            return serverPlayer;
        }
        return null;
    }

    /**
     * Check if the current environment is a dedicated server.
     * @return true if running on dedicated server
     */
    public static boolean isDedicatedServer() {
        return isServerSide();
    }

    /**
     * Check if the current environment is integrated server (singleplayer).
     * This requires additional context and should be used carefully.
     * @return true if likely running integrated server
     */
    public static boolean isIntegratedServer() {
        return isClientSide(); // In integrated server, client side exists
    }
}
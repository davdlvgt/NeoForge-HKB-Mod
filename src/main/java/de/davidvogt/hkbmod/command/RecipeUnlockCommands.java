package de.davidvogt.hkbmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.attachment.ModAttachments;
import de.davidvogt.hkbmod.network.SyncUnlockedRecipesPacket;
import de.davidvogt.hkbmod.research.PlayerResearchHelper;
import de.davidvogt.hkbmod.util.RecipeHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Commands for managing recipe unlocks for testing and administration.
 * Commands:
 * - /hkb unlock <player> <recipe> - Unlock a specific recipe
 * - /hkb lock <player> <recipe> - Lock a specific recipe
 * - /hkb unlockall <player> - Unlock all whitelisted recipes
 * - /hkb resetrecipes <player> - Clear all unlocked recipes
 * - /hkb listunlocked <player> - List all unlocked recipes
 */
public class RecipeUnlockCommands {

    // Suggestion provider for recipe IDs from RecipeHelper
    private static final SuggestionProvider<CommandSourceStack> RECIPE_SUGGESTIONS = (context, builder) -> {
        RecipeHelper.getAllowedRecipeIds().forEach(builder::suggest);
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("hkb")
                        .requires(source -> source.hasPermission(2)) // Require OP level 2

                        // /hkb unlock <player> <recipe>
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("recipe", StringArgumentType.string())
                                                .suggests(RECIPE_SUGGESTIONS)
                                                .executes(RecipeUnlockCommands::unlockRecipe))))

                        // /hkb lock <player> <recipe>
                        .then(Commands.literal("lock")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("recipe", StringArgumentType.string())
                                                .suggests(RECIPE_SUGGESTIONS)
                                                .executes(RecipeUnlockCommands::lockRecipe))))

                        // /hkb unlockall <player>
                        .then(Commands.literal("unlockall")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(RecipeUnlockCommands::unlockAllRecipes)))

                        // /hkb resetrecipes <player>
                        .then(Commands.literal("resetrecipes")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(RecipeUnlockCommands::resetRecipes)))

                        // /hkb listunlocked <player>
                        .then(Commands.literal("listunlocked")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(RecipeUnlockCommands::listUnlockedRecipes)))
        );
    }

    /**
     * Unlocks a specific recipe for a player
     */
    private static int unlockRecipe(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String recipeId = StringArgumentType.getString(context, "recipe");
            ResourceLocation recipeLocation = ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, recipeId);

            boolean wasUnlocked = PlayerResearchHelper.unlockRecipe(player, recipeLocation);

            if (wasUnlocked) {
                // Sync to client
                player.connection.send(SyncUnlockedRecipesPacket.fromSet(
                        player.getData(ModAttachments.PLAYER_RESEARCH).getUnlockedRecipes()));

                context.getSource().sendSuccess(
                        () -> Component.literal("Unlocked recipe '" + recipeId + "' for " + player.getName().getString()),
                        true
                );
                return 1;
            } else {
                context.getSource().sendFailure(
                        Component.literal("Recipe '" + recipeId + "' was already unlocked for " + player.getName().getString())
                );
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Locks a specific recipe for a player
     */
    private static int lockRecipe(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String recipeId = StringArgumentType.getString(context, "recipe");
            ResourceLocation recipeLocation = ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, recipeId);

            boolean wasLocked = PlayerResearchHelper.lockRecipe(player, recipeLocation);

            if (wasLocked) {
                // Sync to client
                player.connection.send(SyncUnlockedRecipesPacket.fromSet(
                        player.getData(ModAttachments.PLAYER_RESEARCH).getUnlockedRecipes()));

                context.getSource().sendSuccess(
                        () -> Component.literal("Locked recipe '" + recipeId + "' for " + player.getName().getString()),
                        true
                );
                return 1;
            } else {
                context.getSource().sendFailure(
                        Component.literal("Recipe '" + recipeId + "' was not unlocked for " + player.getName().getString())
                );
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Unlocks all whitelisted recipes for a player
     */
    private static int unlockAllRecipes(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");

            int count = 0;
            for (String recipeId : RecipeHelper.getAllowedRecipeIds()) {
                ResourceLocation recipeLocation = ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, recipeId);
                if (PlayerResearchHelper.unlockRecipe(player, recipeLocation)) {
                    count++;
                }
            }

            // Sync to client
            player.connection.send(SyncUnlockedRecipesPacket.fromSet(
                    player.getData(ModAttachments.PLAYER_RESEARCH).getUnlockedRecipes()));

            int finalCount = count;
            context.getSource().sendSuccess(
                    () -> Component.literal("Unlocked " + finalCount + " recipes for " + player.getName().getString()),
                    true
            );
            return count;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Clears all unlocked recipes for a player
     */
    private static int resetRecipes(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            int count = PlayerResearchHelper.getUnlockedRecipeCount(player);

            PlayerResearchHelper.clearUnlockedRecipes(player);

            // Sync to client
            player.connection.send(SyncUnlockedRecipesPacket.fromSet(
                    player.getData(ModAttachments.PLAYER_RESEARCH).getUnlockedRecipes()));

            int finalCount = count;
            context.getSource().sendSuccess(
                    () -> Component.literal("Reset " + finalCount + " unlocked recipes for " + player.getName().getString()),
                    true
            );
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Lists all unlocked recipes for a player
     */
    private static int listUnlockedRecipes(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            Collection<ResourceLocation> unlockedRecipes = PlayerResearchHelper.getUnlockedRecipes(player);

            if (unlockedRecipes.isEmpty()) {
                context.getSource().sendSuccess(
                        () -> Component.literal(player.getName().getString() + " has no unlocked recipes."),
                        false
                );
                return 0;
            }

            context.getSource().sendSuccess(
                    () -> Component.literal(player.getName().getString() + " has " + unlockedRecipes.size() + " unlocked recipes:"),
                    false
            );

            for (ResourceLocation recipe : unlockedRecipes) {
                context.getSource().sendSuccess(
                        () -> Component.literal("  - " + recipe.toString()),
                        false
                );
            }

            return unlockedRecipes.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}

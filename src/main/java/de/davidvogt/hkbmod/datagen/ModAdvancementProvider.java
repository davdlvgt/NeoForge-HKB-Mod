package de.davidvogt.hkbmod.datagen;

import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.item.ModItems;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class ModAdvancementProvider implements AdvancementSubProvider {

    @Override
    public void generate(@Nonnull HolderLookup.Provider registries, @Nonnull Consumer<AdvancementHolder> saver) {
        // Echtes Root-Advancement ohne Parent und ohne Rezept-Belohnung
        addRootAdvancement(saver, "research/root",
                Items.BOOK, ModBlocks.RESEARCH_TABLE, AdvancementType.TASK, 0);

        // Weitere Advancements hier hinzufügen...
        // addPickupAdvancement(saver, "mining/first_ore", "research/root", Items.IRON_ORE, 50);
        // addCraftingAdvancement(saver, "crafting/workbench", "research/root", Items.CRAFTING_TABLE, "crafting_table");

        // Beispiel für Impossible-Advancement (für Forschungs-System):
        // ARCHER
        addImpossibleAdvancement(saver, "archer/level_0", "research/root", Items.BOW, AdvancementType.TASK);
        addCraftingAdvancement(saver, "archer/level_1", "archer/level_0", ModBlocks.ELASTIC_WOOD, "elastic_wood");
        addCraftingAdvancement(saver, "archer/level_2", "archer/level_1", ModItems.LONGBOW_STICK, "longbow_stick");
        addCraftingAdvancement(saver, "archer/level_3", "archer/level_2", ModItems.LONGBOW, "longbow");
        addImpossibleAdvancement(saver, "archer/level_4", "archer/level_3", Items.ARROW, AdvancementType.GOAL);
        addImpossibleAdvancement(saver, "archer/level_5", "archer/level_4", Items.GOLDEN_APPLE, AdvancementType.CHALLENGE);

        // CAVALIER
        addImpossibleAdvancement(saver, "cavalier/level_0", "research/root", Items.SADDLE, AdvancementType.TASK);
        addImpossibleAdvancement(saver, "cavalier/level_1", "cavalier/level_0", Items.IRON_HORSE_ARMOR, AdvancementType.TASK);
        addImpossibleAdvancement(saver, "cavalier/level_2", "cavalier/level_1", Items.DIAMOND_HORSE_ARMOR, AdvancementType.TASK);
        addImpossibleAdvancement(saver, "cavalier/level_3", "cavalier/level_2", Items.GOLDEN_HORSE_ARMOR, AdvancementType.TASK);
        addImpossibleAdvancement(saver, "cavalier/level_4", "cavalier/level_3", Items.BLACK_BANNER, AdvancementType.GOAL);
        addImpossibleAdvancement(saver, "cavalier/level_5", "cavalier/level_4", Items.TOTEM_OF_UNDYING, AdvancementType.CHALLENGE);

        // KNIGHT
        addImpossibleAdvancement(saver, "knight/level_0", "research/root", Items.IRON_SWORD, AdvancementType.TASK);
        addImpossibleAdvancement(saver, "knight/level_1", "knight/level_0", Items.DIAMOND_SWORD, AdvancementType.TASK);
        addCraftingAdvancement(saver, "knight/level_2", "knight/level_1", ModItems.EMERALD_SWORD, "emerald_sword");
        addImpossibleAdvancement(saver, "knight/level_3", "knight/level_2", Items.DIAMOND_CHESTPLATE, AdvancementType.TASK);
        addImpossibleAdvancement(saver, "knight/level_4", "knight/level_3", Items.NETHERITE_SWORD, AdvancementType.GOAL);
        addImpossibleAdvancement(saver, "knight/level_5", "knight/level_4", Items.NETHERITE_CHESTPLATE, AdvancementType.CHALLENGE);

        // MAGICIAN
        addImpossibleAdvancement(saver, "magician/level_0", "research/root", Items.ENCHANTED_BOOK, AdvancementType.TASK);
        addImpossibleAdvancement(saver, "magician/level_1", "magician/level_0", Items.BLAZE_ROD, AdvancementType.TASK);
        addCraftingAdvancement(saver, "magician/level_2", "magician/level_1", ModItems.TIME_SETTER, "time_setter");
        addImpossibleAdvancement(saver, "magician/level_3", "magician/level_2", Items.NETHER_STAR, AdvancementType.TASK);
        addImpossibleAdvancement(saver, "magician/level_4", "magician/level_3", Items.DRAGON_EGG, AdvancementType.GOAL);
        addImpossibleAdvancement(saver, "magician/level_5", "magician/level_4", Items.END_CRYSTAL, AdvancementType.CHALLENGE);

        // MINER
        addImpossibleAdvancement(saver, "miner/level_0", "research/root", Items.STONE_PICKAXE, AdvancementType.TASK);
        addImpossibleAdvancement(saver, "miner/level_1", "miner/level_0", Items.IRON_PICKAXE, AdvancementType.TASK);
        addImpossibleAdvancement(saver, "miner/level_2", "miner/level_1", Items.RAIL, AdvancementType.TASK);
        addCraftingAdvancement(saver, "miner/level_3", "miner/level_2", ModItems.EMERALD_PICKAXE, "emerald_pickaxe");
        addImpossibleAdvancement(saver, "miner/level_4", "miner/level_3", Items.DRAGON_EGG, AdvancementType.GOAL);
        addCraftingAdvancement(saver, "miner/level_5", "miner/level_4", ModItems.MAGIC_PICKAXE, "magic_pickaxe");


    }

    // ========== EINFACHE METHODEN ZUM HINZUFÜGEN VON ADVANCEMENTS ==========

    /**
     * Fügt ein echtes Root-Advancement hinzu (komplett ohne Parent - für echte root.json)
     *
     * @param saver       Der Consumer für das Advancement
     * @param path        Pfad des Advancements (z.B. "research/root")
     * @param displayItem Item das im Advancement-Tab angezeigt wird
     * @param triggerItem Item das aufgehoben werden muss um das Advancement zu bekommen
     * @param type        Typ des Advancements (TASK, GOAL, CHALLENGE)
     * @param experience  XP-Belohnung (0 für keine)
     */
    public void addRootAdvancement(Consumer<AdvancementHolder> saver, String path,
                                   ItemLike displayItem, ItemLike triggerItem, AdvancementType type, int experience) {
        Advancement.Builder builder = Advancement.Builder.advancement();

        // KEIN Parent für echte Root-Advancements!

        // Display konfigurieren
        builder.display(new ItemStack(displayItem),
                Component.translatable("advancements.hkbmod." + path.replace("/", ".") + ".title"),
                Component.translatable("advancements.hkbmod." + path.replace("/", ".") + ".description"),
                null, type, true, true, false);

        // Kriterium hinzufügen
        builder.addCriterion("has_" + getItemName(triggerItem),
                InventoryChangeTrigger.TriggerInstance.hasItems(triggerItem));

        // Nur XP-Belohnung hinzufügen (keine Rezepte)
        if (experience > 0) {
            builder.rewards(AdvancementRewards.Builder.experience(experience));
        }

        // Speichern
        builder.save(saver, ResourceLocation.fromNamespaceAndPath("hkbmod", path));
    }

    /**
     * Fügt ein einfaches Advancement hinzu (nur mit XP-Belohnung)
     *
     * @param saver       Der Consumer für das Advancement
     * @param path        Pfad des Advancements (z.B. "mining/first_ore")
     * @param parent      Parent-Advancement (z.B. "minecraft:story/root")
     * @param displayItem Item das im Advancement-Tab angezeigt wird
     * @param triggerItem Item das aufgehoben werden muss um das Advancement zu bekommen
     * @param type        Typ des Advancements (TASK, GOAL, CHALLENGE)
     * @param experience  XP-Belohnung (0 für keine)
     */
    public void addSimpleAdvancement(Consumer<AdvancementHolder> saver, String path, String parent,
                                     ItemLike displayItem, ItemLike triggerItem, AdvancementType type, int experience) {
        addSimpleAdvancement(saver, path, parent, displayItem, triggerItem, type, experience, null);
    }

    /**
     * Fügt ein einfaches Advancement mit Rezept-Belohnung hinzu
     */
    public void addSimpleAdvancement(Consumer<AdvancementHolder> saver, String path, String parent,
                                     ItemLike displayItem, ItemLike triggerItem, AdvancementType type,
                                     int experience, String recipeReward) {
        Advancement.Builder builder = Advancement.Builder.advancement();

        // Parent setzen
        builder.parent(AdvancementSubProvider.createPlaceholder(parent.contains(":") ? parent : "hkbmod:" + parent));

        // Display konfigurieren
        builder.display(new ItemStack(displayItem),
                Component.translatable("advancements.hkbmod." + path.replace("/", ".") + ".title"),
                Component.translatable("advancements.hkbmod." + path.replace("/", ".") + ".description"),
                null, type, true, true, false);

        // Kriterium hinzufügen
        builder.addCriterion("has_" + getItemName(triggerItem),
                InventoryChangeTrigger.TriggerInstance.hasItems(triggerItem));

        // Rewards hinzufügen
        AdvancementRewards.Builder rewardsBuilder = AdvancementRewards.Builder.experience(experience);
        if (recipeReward != null) {
            rewardsBuilder.addRecipe(ResourceKey.create(
                    Registries.RECIPE, ResourceLocation.fromNamespaceAndPath("hkbmod", recipeReward)
            ));
        }
        if (experience > 0 || recipeReward != null) {
            builder.rewards(rewardsBuilder);
        }

        // Speichern
        builder.save(saver, ResourceLocation.fromNamespaceAndPath("hkbmod", path));
    }

    /**
     * Fügt ein Pickup-Advancement hinzu (Item aufheben)
     */
    public void addPickupAdvancement(Consumer<AdvancementHolder> saver, String path, String parent,
                                     ItemLike item, int experience) {
        addSimpleAdvancement(saver, path, parent, item, item, AdvancementType.TASK, experience);
    }

    /**
     * Fügt ein Crafting-Advancement mit Rezept-Belohnung hinzu
     */
    public void addCraftingAdvancement(Consumer<AdvancementHolder> saver, String path, String parent,
                                       ItemLike craftedItem, String recipeReward) {
        addSimpleAdvancement(saver, path, parent, craftedItem, craftedItem, AdvancementType.GOAL, 0, recipeReward);
    }

    /**
     * Fügt ein Challenge-Advancement hinzu (schwer zu erreichen)
     */
    public void addChallengeAdvancement(Consumer<AdvancementHolder> saver, String path, String parent,
                                        ItemLike displayItem, ItemLike triggerItem, int experience) {
        addSimpleAdvancement(saver, path, parent, displayItem, triggerItem, AdvancementType.CHALLENGE, experience);
    }

    // ========== ERWEITERTE METHODE FÜR KOMPLEXE ADVANCEMENTS ==========

    /**
     * Für komplexere Advancements mit benutzerdefinierten Kriterien
     */
    public void addCustomAdvancement(Consumer<AdvancementHolder> saver, String path, String parent,
                                     ItemStack displayItem, Component title, Component description,
                                     String criterionName, Criterion<?> criterion,
                                     AdvancementType type, AdvancementRewards rewards) {
        Advancement.Builder builder = Advancement.Builder.advancement();

        builder.parent(AdvancementSubProvider.createPlaceholder(parent.contains(":") ? parent : "hkbmod:" + parent));
        builder.display(displayItem, title, description, null, type, true, true, false);
        builder.addCriterion(criterionName, criterion);
        if (rewards != null) {
            builder.rewards(rewards);
        }
        builder.save(saver, ResourceLocation.fromNamespaceAndPath("hkbmod", path));
    }

    /**
     * Fügt ein Advancement mit dem Trigger "minecraft:impossible" hinzu
     * Diese Methode erstellt automatisch die Translation-Keys basierend auf dem Pfad
     */
    public void addImpossibleAdvancement(Consumer<AdvancementHolder> saver, String path, String parent,
                                         ItemLike displayItem, AdvancementType type) {
        addImpossibleAdvancement(saver, path, parent, displayItem, type, null);
    }

    /**
     * Fügt ein Advancement mit dem Trigger "minecraft:impossible" hinzu (mit Belohnungen)
     */
    public void addImpossibleAdvancement(Consumer<AdvancementHolder> saver, String path, String parent,
                                         ItemLike displayItem, AdvancementType type, AdvancementRewards rewards) {
        Advancement.Builder builder = Advancement.Builder.advancement();

        // Parent setzen
        builder.parent(AdvancementSubProvider.createPlaceholder(parent.contains(":") ? parent : "hkbmod:" + parent));

        // Display konfigurieren mit ItemStack um die gewünschte JSON-Struktur zu erhalten
        builder.display(
                new ItemStack(displayItem), // ItemStack verwenden für korrekte JSON-Struktur
                Component.translatable("advancements.hkbmod." + path.replace("/", ".") + ".title"),
                Component.translatable("advancements.hkbmod." + path.replace("/", ".") + ".description"),
                null, // background
                type, // frame
                true, // show_toast
                true, // announce_to_chat
                false); // hidden

        // Impossible-Kriterium hinzufügen mit dem Namen "research_completed"
        builder.addCriterion("research_completed", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()));

        if (rewards != null) {
            builder.rewards(rewards);
        }

        // Speichern
        builder.save(saver, ResourceLocation.fromNamespaceAndPath("hkbmod", path));
    }

    // ========== HELPER-METHODEN ==========

    /**
     * Extrahiert einen sauberen Namen aus einem Item
     */
    private String getItemName(ItemLike item) {
        return item.asItem().toString().replace(":", "_");
    }
}

package de.davidvogt.hkbmod.datagen;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.item.ModItems;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Generates language files (translations) for the mod.
 * This creates the en_us.json file with English translations.
 */
public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, HKBMod.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // Creative Tab
        add("itemGroup.hkbmod", "HKB Mod");

        // Items


        // Tools
        add(ModItems.EMERALD_AXE.get(), "Emerald Axe");
        add(ModItems.EMERALD_PICKAXE.get(), "Emerald Pickaxe");
        add(ModItems.EMERALD_SWORD.get(), "Emerald Sword");
        add(ModItems.EMERALD_SHOVEL.get(), "Emerald Shovel");
        add(ModItems.EMERALD_HOE.get(), "Emerald Hoe");
        add(ModItems.MAGIC_PICKAXE.get(), "Magic Pickaxe");

        // Blocks
        add(ModBlocks.TEST_BLOCK.get(), "Test Block");
        add(ModBlocks.CUSTOM_TEST_BLOCK.get(), "Custom Test Block");
        add(ModBlocks.TEST_LAMP.get(), "Test Lamp");
        add(ModBlocks.RESEARCH_TABLE.get(), "Research Table");

        // Advancements - Root
        add("advancements.hkbmod.root.title", "Research Classes");
        add("advancements.hkbmod.root.description", "Master different combat and magic classes");

        // Knight Advancements
        add("advancements.hkbmod.knight.level_0.title", "Knight: Squire");
        add("advancements.hkbmod.knight.level_0.description", "Begin your journey as a Knight");
        add("advancements.hkbmod.knight.level_1.title", "Knight: Man-at-Arms");
        add("advancements.hkbmod.knight.level_1.description", "Advance your Knight training");
        add("advancements.hkbmod.knight.level_2.title", "Knight: Warrior");
        add("advancements.hkbmod.knight.level_2.description", "Become a skilled Knight warrior");
        add("advancements.hkbmod.knight.level_3.title", "Knight: Veteran");
        add("advancements.hkbmod.knight.level_3.description", "Achieve veteran Knight status");
        add("advancements.hkbmod.knight.level_4.title", "Knight: Champion");
        add("advancements.hkbmod.knight.level_4.description", "Rise to Knight champion");
        add("advancements.hkbmod.knight.level_5.title", "Knight: Grandmaster");
        add("advancements.hkbmod.knight.level_5.description", "Master the Knight class completely");

        // Archer Advancements
        add("advancements.hkbmod.archer.level_0.title", "Archer: Novice");
        add("advancements.hkbmod.archer.level_0.description", "Begin your journey as an Archer");
        add("advancements.hkbmod.archer.level_1.title", "Archer: Hunter");
        add("advancements.hkbmod.archer.level_1.description", "Advance your Archer skills");
        add("advancements.hkbmod.archer.level_2.title", "Archer: Marksman");
        add("advancements.hkbmod.archer.level_2.description", "Become a skilled Marksman");
        add("advancements.hkbmod.archer.level_3.title", "Archer: Sharpshooter");
        add("advancements.hkbmod.archer.level_3.description", "Achieve Sharpshooter status");
        add("advancements.hkbmod.archer.level_4.title", "Archer: Deadeye");
        add("advancements.hkbmod.archer.level_4.description", "Rise to Deadeye rank");
        add("advancements.hkbmod.archer.level_5.title", "Archer: Grandmaster");
        add("advancements.hkbmod.archer.level_5.description", "Master the Archer class completely");

        // Cavalier Advancements
        add("advancements.hkbmod.cavalier.level_0.title", "Cavalier: Rider");
        add("advancements.hkbmod.cavalier.level_0.description", "Begin your journey as a Cavalier");
        add("advancements.hkbmod.cavalier.level_1.title", "Cavalier: Scout");
        add("advancements.hkbmod.cavalier.level_1.description", "Advance your Cavalier training");
        add("advancements.hkbmod.cavalier.level_2.title", "Cavalier: Lancer");
        add("advancements.hkbmod.cavalier.level_2.description", "Become a skilled Lancer");
        add("advancements.hkbmod.cavalier.level_3.title", "Cavalier: Knight-Errant");
        add("advancements.hkbmod.cavalier.level_3.description", "Achieve Knight-Errant status");
        add("advancements.hkbmod.cavalier.level_4.title", "Cavalier: Paladin");
        add("advancements.hkbmod.cavalier.level_4.description", "Rise to Paladin rank");
        add("advancements.hkbmod.cavalier.level_5.title", "Cavalier: Grandmaster");
        add("advancements.hkbmod.cavalier.level_5.description", "Master the Cavalier class completely");

        // Magician Advancements
        add("advancements.hkbmod.magician.level_0.title", "Magician: Apprentice");
        add("advancements.hkbmod.magician.level_0.description", "Begin your journey as a Magician");
        add("advancements.hkbmod.magician.level_1.title", "Magician: Adept");
        add("advancements.hkbmod.magician.level_1.description", "Advance your magical studies");
        add("advancements.hkbmod.magician.level_2.title", "Magician: Sorcerer");
        add("advancements.hkbmod.magician.level_2.description", "Become a skilled Sorcerer");
        add("advancements.hkbmod.magician.level_3.title", "Magician: Warlock");
        add("advancements.hkbmod.magician.level_3.description", "Achieve Warlock status");
        add("advancements.hkbmod.magician.level_4.title", "Magician: Archmage");
        add("advancements.hkbmod.magician.level_4.description", "Rise to Archmage rank");
        add("advancements.hkbmod.magician.level_5.title", "Magician: Grandmaster");
        add("advancements.hkbmod.magician.level_5.description", "Master the Magician class completely");

        // Tooltips (optional)
    }
}
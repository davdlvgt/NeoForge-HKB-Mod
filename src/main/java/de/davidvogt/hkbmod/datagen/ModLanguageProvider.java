package de.davidvogt.hkbmod.datagen;

import de.davidvogt.hkbmod.HKBMod;
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


        // Tooltips (optional)
    }
}
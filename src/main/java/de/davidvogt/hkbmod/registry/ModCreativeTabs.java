package de.davidvogt.hkbmod.registry;

import de.davidvogt.hkbmod.HKBMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for custom creative mode tabs.
 */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HKBMod.MODID);

}
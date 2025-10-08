package de.davidvogt.hkbmod.worldgen.tree;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.worldgen.ModConfiguredFeatures;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

public class ModTreeGrowers {
    public static final TreeGrower ROBINIA = new TreeGrower(HKBMod.MODID + ":robinia",
            Optional.empty(), Optional.of(ModConfiguredFeatures.ROBINIA_KEY), Optional.empty());

}

package de.davidvogt.hkbmod.item;

import de.davidvogt.hkbmod.util.ModTags;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.common.Tags;

public class ModMaterials {
    public static final ToolMaterial EMERALD_MATERIAL = new ToolMaterial(
            ModTags.Blocks.INCORRECT_FOR_EMERALD_TOOL,
            1750,
            8.5F,
            3.5F,
            17,
            Tags.Items.GEMS_EMERALD
    );
}

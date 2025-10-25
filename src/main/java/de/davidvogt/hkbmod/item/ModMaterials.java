package de.davidvogt.hkbmod.item;

import com.google.common.collect.Maps;
import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.util.ModTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.common.Tags;

import java.util.Map;

public class ModMaterials {
    public static final ToolMaterial EMERALD_MATERIAL = new ToolMaterial(
            ModTags.Blocks.INCORRECT_FOR_EMERALD_TOOL,
            1750,
            8.5F,
            3.5F,
            17,
            Tags.Items.GEMS_EMERALD
    );

    public static final ArmorMaterial EMERALD_ARMOR_MATERIAL = new ArmorMaterial(
            35,
            makeDefense(3, 6, 8, 3, 11),
            13,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            2.5F,
            0.1F,
            ModTags.Items.REPAIRS_EMERALD_ARMOR,
            ResourceKey.create(
                    ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("equipment_asset")),
                    ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "emerald")
            )
    );

    private static Map<ArmorType, Integer> makeDefense(int boots, int leggings, int chestplate, int helmet, int body) {
        return Maps.newEnumMap(
                Map.of(
                        ArmorType.BOOTS,
                        boots,
                        ArmorType.LEGGINGS,
                        leggings,
                        ArmorType.CHESTPLATE,
                        chestplate,
                        ArmorType.HELMET,
                        helmet,
                        ArmorType.BODY,
                        body
                )
        );
    }
}

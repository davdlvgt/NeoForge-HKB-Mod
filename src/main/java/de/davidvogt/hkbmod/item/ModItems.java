package de.davidvogt.hkbmod.item;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.item.custom.MagicPickaxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for all custom items in the mod.
 * This demonstrates various item types and their properties.
 */
@EventBusSubscriber(modid = HKBMod.MODID)
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HKBMod.MODID);

    // Temporarily register as simple item until we can resolve ToolMaterials API
    public static final DeferredItem<Item> EMERALD_AXE = ITEMS.registerItem(
            "emerald_axe",
            props -> new Item(
                    props.axe(
                            ModMaterials.EMERALD_MATERIAL,   // Material to use
                            3,                                  // Type-specific attack damage bonus
                            -2.4F                               // Type-specific attack speed modifier
                    )
            )
    );

    public static final DeferredItem<Item> EMERALD_PICKAXE = ITEMS.registerItem(
            "emerald_pickaxe",
            props -> new Item(
                    props.pickaxe(
                            ModMaterials.EMERALD_MATERIAL,   // Material to use
                            1,                                  // Type-specific attack damage bonus
                            -2.8F                               // Type-specific attack speed modifier
                    )
            )
    );

    public static final DeferredItem<Item> EMERALD_SWORD = ITEMS.registerItem(
            "emerald_sword",
            props -> new Item(
                    props.sword(
                            ModMaterials.EMERALD_MATERIAL,   // Material to use
                            3.5F,                                  // Type-specific attack damage bonus
                            -2.5F                               // Type-specific attack speed modifier
                    )
            )
    );

    public static final DeferredItem<MagicPickaxeItem> MAGIC_PICKAXE = ITEMS.registerItem(
            "magic_pickaxe",
            (properties) -> new MagicPickaxeItem(
                    properties.pickaxe(ModMaterials.EMERALD_MATERIAL, 7F, -3.5F)
            )
    );

    public static final DeferredItem<Item> EMERALD_SHOVEL = ITEMS.registerItem(
            "emerald_shovel",
            props -> new Item(
                    props.shovel(
                            ModMaterials.EMERALD_MATERIAL,   // Material to use
                            1.5F,                                  // Type-specific attack damage bonus
                            -3.0F                               // Type-specific attack speed modifier
                    )
            )
    );

    public static final DeferredItem<Item> EMERALD_HOE = ITEMS.registerItem(
            "emerald_hoe",
            props -> new Item(
                    props.hoe(
                            ModMaterials.EMERALD_MATERIAL,   // Material to use
                            -3.0F,                                  // Type-specific attack damage bonus
                            0.0F                               // Type-specific attack speed modifier
                    )
            )
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
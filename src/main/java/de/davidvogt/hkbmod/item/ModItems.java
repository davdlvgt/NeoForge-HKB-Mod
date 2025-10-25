package de.davidvogt.hkbmod.item;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.item.custom.LongbowArrowItem;
import de.davidvogt.hkbmod.item.custom.LongbowItem;
import de.davidvogt.hkbmod.item.custom.MagicPickaxeItem;
import de.davidvogt.hkbmod.item.custom.TimeSetterItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
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
                            -2.0F                               // Type-specific attack speed modifier
                    )
            )
    );

    public static final DeferredItem<MagicPickaxeItem> MAGIC_PICKAXE = ITEMS.registerItem(
            "magic_pickaxe",
            (props) -> new MagicPickaxeItem(
                    props.pickaxe(ModMaterials.EMERALD_MATERIAL, 7F, -3.5F)
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

    public static final DeferredItem<Item> EMERALD_HELMET = ITEMS.registerItem(
            "emerald_helmet",
            props -> new Item(
                    props.humanoidArmor(
                            ModMaterials.EMERALD_ARMOR_MATERIAL,
                            ArmorType.HELMET
                    )
            )
    );

    public static final DeferredItem<Item> EMERALD_CHESTPLATE = ITEMS.registerItem(
            "emerald_chestplate",
            props -> new Item(
                    props.humanoidArmor(
                            ModMaterials.EMERALD_ARMOR_MATERIAL,
                            ArmorType.CHESTPLATE
                    )
            )
    );

    public static final DeferredItem<Item> EMERALD_LEGGINGS = ITEMS.registerItem(
            "emerald_leggings",
            props -> new Item(
                    props.humanoidArmor(
                            ModMaterials.EMERALD_ARMOR_MATERIAL,
                            ArmorType.LEGGINGS
                    )
            )
    );

    public static final DeferredItem<Item> EMERALD_BOOTS = ITEMS.registerItem(
            "emerald_boots",
            props -> new Item(
                    props.humanoidArmor(
                            ModMaterials.EMERALD_ARMOR_MATERIAL,
                            ArmorType.BOOTS
                    )
            )
    );

    public static final DeferredItem<TimeSetterItem> TIME_SETTER = ITEMS.registerItem(
            "time_setter",
            (props) -> new TimeSetterItem(props.stacksTo(1).durability(3))
    );

    public static final DeferredItem<Item> LONG_STICK = ITEMS.registerItem(
            "long_stick",
            (props) -> new Item(props.stacksTo(32))
    );

    public static final DeferredItem<Item> LONG_STRING = ITEMS.registerItem(
            "long_string",
            (props) -> new Item(props.stacksTo(32))
    );

    public static final DeferredItem<Item> LONGBOW_STICK = ITEMS.registerItem(
            "longbow_stick",
            (props) -> new Item(props.stacksTo(32))
    );

    public static final DeferredItem<LongbowArrowItem> LONGBOW_ARROW = ITEMS.registerItem(
            "longbow_arrow",
            (props) -> new LongbowArrowItem(props.stacksTo(32))
    );

    public static final DeferredItem<LongbowItem> LONGBOW = ITEMS.registerItem(
            "longbow",
            (props) -> new LongbowItem(props.durability(500),
                    stack -> stack.is(ModItems.LONGBOW_ARROW.get()))
    );

    // Deer drops
    public static final DeferredItem<Item> DEER_BEEF = ITEMS.registerSimpleItem(
            "deer_beef",
            new Item.Properties().food(net.minecraft.world.food.Foods.BEEF)
    );

    public static final DeferredItem<Item> COOKED_DEER_BEEF = ITEMS.registerSimpleItem(
            "cooked_deer_beef",
            new Item.Properties().food(net.minecraft.world.food.Foods.COOKED_BEEF)
    );

    public static final DeferredItem<Item> DEER_ANTLERS = ITEMS.registerSimpleItem(
            "deer_antlers",
            new Item.Properties()
    );

    // Dragon drops
    public static final DeferredItem<Item> DRAGON_SKIN = ITEMS.registerSimpleItem(
            "dragon_skin",
            new Item.Properties()
    );

    public static final DeferredItem<Item> DRAGON_MATERIAL = ITEMS.registerSimpleItem(
            "dragon_material",
            new Item.Properties()
    );

    public static final DeferredItem<Item> DRAGON_SADDLE = ITEMS.registerSimpleItem(
            "dragon_saddle",
            new Item.Properties()
    );

    // TODO: Add deer spawn egg when SpawnEggItem API is clarified for NeoForge 21.8
    // Spawn eggs require special handling in 1.21.8
    // For now, use /summon hkbmod:deer to spawn deer

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
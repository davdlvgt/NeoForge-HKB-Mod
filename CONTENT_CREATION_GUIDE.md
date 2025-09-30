# Content Creation Guide for HKB Mod

This guide shows you how to create your own items, blocks, and use data generation in the HKB mod.

## Table of Contents
1. [Creating Items](#creating-items)
2. [Creating Blocks](#creating-blocks)
3. [Creative Tabs](#creative-tabs)
4. [Data Generation](#data-generation)
5. [Asset Structure](#asset-structure)
6. [Testing Your Content](#testing-your-content)

## Creating Items

All items are registered in `src/main/java/de/davidvogt/hkbmod/registry/ModItems.java`.

### Basic Items

```java
// Simple item
public static final DeferredItem<Item> MY_ITEM = ITEMS.registerSimpleItem("my_item");

// Item with custom stack size
public static final DeferredItem<Item> MY_STACKABLE = ITEMS.registerItem("my_stackable",
    properties -> new Item(properties.stacksTo(16)));
```

### Tools

```java
// Create a custom tool tier
public static final Tier MY_TIER = new Tier() {
    @Override public int getUses() { return 500; }
    @Override public float getSpeed() { return 8.0F; }
    @Override public float getAttackDamageBonus() { return 3.0F; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_IRON_TOOL;
    }
    @Override public int getEnchantmentValue() { return 15; }
    @Override public Ingredient getRepairIngredient() {
        return Ingredient.of(MY_MATERIAL.get());
    }
};

// Create tools using the tier
public static final DeferredItem<SwordItem> MY_SWORD = ITEMS.registerItem("my_sword",
    properties -> new SwordItem(MY_TIER, new Item.Properties()
        .attributes(SwordItem.createAttributes(MY_TIER, 3, -2.4F))));
```

### Armor

```java
// Create armor material
public static final ArmorMaterial MY_ARMOR_MATERIAL = new ArmorMaterial(
    Map.of(
        ArmorItem.Type.BOOTS, 2,
        ArmorItem.Type.LEGGINGS, 5,
        ArmorItem.Type.CHESTPLATE, 6,
        ArmorItem.Type.HELMET, 2
    ),
    15, // enchantability
    SoundEvents.ARMOR_EQUIP_IRON,
    () -> Ingredient.of(MY_MATERIAL.get()),
    List.of(new ArmorMaterial.Layer(
        ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "my_armor")
    )),
    0.0F, // toughness
    0.0F  // knockback resistance
);

// Create armor pieces
public static final DeferredItem<ArmorItem> MY_HELMET = ITEMS.registerItem("my_helmet",
    properties -> new ArmorItem(MY_ARMOR_MATERIAL, ArmorItem.Type.HELMET, properties));
```

### Food Items

```java
// Basic food
public static final DeferredItem<Item> MY_FOOD = ITEMS.registerItem("my_food",
    properties -> new Item(properties.food(new FoodProperties.Builder()
        .nutrition(4)
        .saturationModifier(0.3F)
        .build())));

// Food with effects
public static final DeferredItem<Item> SPECIAL_FOOD = ITEMS.registerItem("special_food",
    properties -> new Item(properties.food(new FoodProperties.Builder()
        .nutrition(4)
        .saturationModifier(1.2F)
        .alwaysEdible()
        .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 1), 1.0F)
        .build())
        .rarity(Rarity.RARE)));
```

### Custom Behavior Items

```java
public static final DeferredItem<Item> CUSTOM_ITEM = ITEMS.registerItem("custom_item",
    properties -> new Item(properties.stacksTo(1)) {
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("Custom item used!"));
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
        }
    });
```

## Creating Blocks

All blocks are registered in `src/main/java/de/davidvogt/hkbmod/registry/ModBlocks.java`.

### Basic Blocks

```java
// Simple decorative block
public static final DeferredBlock<Block> MY_BLOCK = BLOCKS.registerSimpleBlock("my_block",
    BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_RED)
        .instrument(NoteBlockInstrument.CHIME)
        .requiresCorrectToolForDrops()
        .strength(5.0F, 6.0F)
        .sound(SoundType.METAL));

// Always create a corresponding block item
public static final DeferredItem<BlockItem> MY_BLOCK_ITEM =
    ModItems.ITEMS.registerSimpleBlockItem("my_block", MY_BLOCK);
```

### Ore Blocks

```java
// Ore that drops items and XP
public static final DeferredBlock<DropExperienceBlock> MY_ORE = BLOCKS.register("my_ore",
    () -> new DropExperienceBlock(UniformInt.of(3, 7),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 3.0F)));
```

### Functional Blocks

```java
// Slab
public static final DeferredBlock<SlabBlock> MY_SLAB = BLOCKS.register("my_slab",
    () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(MY_BLOCK.get())));

// Stairs
public static final DeferredBlock<StairBlock> MY_STAIRS = BLOCKS.register("my_stairs",
    () -> new StairBlock(MY_BLOCK.get().defaultBlockState(),
        BlockBehaviour.Properties.ofFullCopy(MY_BLOCK.get())));

// Door
public static final DeferredBlock<DoorBlock> MY_DOOR = BLOCKS.register("my_door",
    () -> new DoorBlock(BlockSetType.IRON,
        BlockBehaviour.Properties.of()
            .strength(5.0F)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)));
```

### Special Properties

```java
// Glass block
public static final DeferredBlock<Block> MY_GLASS = BLOCKS.registerSimpleBlock("my_glass",
    BlockBehaviour.Properties.of()
        .strength(0.3F)
        .sound(SoundType.GLASS)
        .noOcclusion()
        .isValidSpawn((state, level, pos, entityType) -> false)
        .isRedstoneConductor((state, level, pos) -> false)
        .isSuffocating((state, level, pos) -> false)
        .isViewBlocking((state, level, pos) -> false));

// Light-emitting block
public static final DeferredBlock<Block> MY_LAMP = BLOCKS.registerSimpleBlock("my_lamp",
    BlockBehaviour.Properties.of()
        .strength(0.3F)
        .lightLevel(state -> 15)); // Full light level
```

## Creative Tabs

Add your items to creative tabs in `ModCreativeTabs.java`:

```java
.displayItems((parameters, output) -> {
    // Add your items here
    output.accept(ModItems.MY_ITEM.get());
    output.accept(ModBlocks.MY_BLOCK_ITEM.get());
}).build());
```

## Data Generation

### Setting Up Data Generation

**Status for NeoForge 21.8**: ✅ **Working** - Data generation is successfully implemented and tested!

**What's Working**:
- ✅ Data generation framework using `GatherDataEvent.Client`
- ✅ Language provider (`ModLanguageProvider.java`) - generates translations automatically
- ✅ Successfully runs with `./gradlew runData` and generates `en_us.json`
- ✅ Block items properly registered in `ModItems.java` to avoid circular dependencies

**Current Status**:
- ✅ Language files: Fully working and tested
- ❌ Recipe providers: API changed in NeoForge 21.8, temporarily disabled
- ❌ Loot table providers: API changed in NeoForge 21.8, temporarily disabled
- ❌ Model providers: Missing classes in NeoForge 21.8, temporarily disabled
- ❌ Tag providers: API compatibility issues, temporarily disabled

**Current limitations**:
- DropExperienceBlock ore blocks cause initialization issues and are temporarily disabled
- Other data providers need API updates for NeoForge 21.8 compatibility

**Manual assets still needed**:
- `src/main/resources/assets/hkbmod/textures/item/` - Item textures (16x16 PNG)
- `src/main/resources/assets/hkbmod/textures/block/` - Block textures (16x16 PNG)

### Adding Recipes (`ModRecipeProvider.java`)

```java
// Shaped recipe
ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MY_TOOL.get())
    .pattern("XXX")
    .pattern(" S ")
    .pattern(" S ")
    .define('X', ModItems.MY_MATERIAL.get())
    .define('S', Items.STICK)
    .unlockedBy("has_material", has(ModItems.MY_MATERIAL.get()))
    .save(recipeOutput);

// Shapeless recipe
ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MY_ITEM.get(), 9)
    .requires(ModBlocks.MY_BLOCK.get())
    .unlockedBy("has_block", has(ModBlocks.MY_BLOCK.get()))
    .save(recipeOutput);

// Smelting recipe
SimpleCookingRecipeBuilder.smelting(
    Ingredient.of(ModBlocks.MY_ORE.get()),
    RecipeCategory.MISC,
    ModItems.MY_MATERIAL.get(),
    0.7f,
    200
).unlockedBy("has_ore", has(ModBlocks.MY_ORE.get()))
 .save(recipeOutput);
```

### Adding Loot Tables (`ModLootTableProvider.java`)

```java
// In ModBlockLootSubProvider.generate()

// Simple block drop
this.dropSelf(ModBlocks.MY_BLOCK.get());

// Ore with fortune
this.add(ModBlocks.MY_ORE.get(), createOreLikeDrops(ModBlocks.MY_ORE.get(), ModItems.MY_MATERIAL.get()));
```

### Adding Tags

In `ModBlockTagProvider.java`:
```java
// Mining tool tags
this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
    .add(ModBlocks.MY_BLOCK.get());

// Mining level tags
this.tag(BlockTags.NEEDS_IRON_TOOL)
    .add(ModBlocks.MY_BLOCK.get());
```

In `ModItemTagProvider.java`:
```java
// Tool tags
this.tag(ItemTags.SWORDS)
    .add(ModItems.MY_SWORD.get());
```

### Adding Translations (`ModLanguageProvider.java`)

```java
// In addTranslations()
add(ModItems.MY_ITEM.get(), "My Item");
add(ModBlocks.MY_BLOCK.get(), "My Block");
add("tooltip.hkbmod.my_item", "This is my custom item");
```

## Asset Structure

After running data generation, you need to add textures:

```
src/main/resources/
├── assets/hkbmod/
│   ├── textures/
│   │   ├── item/
│   │   │   ├── my_item.png
│   │   │   ├── my_sword.png
│   │   │   └── ...
│   │   └── block/
│   │       ├── my_block.png
│   │       ├── my_ore.png
│   │       └── ...
│   ├── models/ (auto-generated)
│   ├── blockstates/ (auto-generated)
│   └── lang/ (auto-generated)
└── data/hkbmod/ (auto-generated)
    ├── recipes/
    ├── loot_tables/
    ├── tags/
    └── ...
```

## Testing Your Content

1. **Build the mod**: `./gradlew build`
2. **Run client**: `./gradlew runClient`
3. **Run data generation**: `./gradlew runData`
4. **Check creative tabs**: Your items should appear in the mod's creative tabs
5. **Test recipes**: Try crafting your items in the crafting table
6. **Test blocks**: Place and break your blocks to verify loot tables

## Tips

1. **Always run data generation** after adding new content
2. **Add textures** to the appropriate folders (16x16 PNG files)
3. **Test in both creative and survival** modes
4. **Check the logs** for any registration errors
5. **Use existing examples** as templates for new content
6. **Follow naming conventions**: lowercase with underscores
7. **Add translations** for all your content

## Common Issues

- **Missing textures**: Add PNG files to `textures/item/` or `textures/block/`
- **No recipes**: Make sure you added them in `ModRecipeProvider.java` and ran data generation
- **Items not in creative tab**: Add them to `ModCreativeTabs.java`
- **Blocks don't drop items**: Check your loot tables in `ModLootTableProvider.java`
- **Tools don't mine blocks**: Add proper tags in `ModBlockTagProvider.java`
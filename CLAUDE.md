# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Building and Running
- **Build the mod**: `./gradlew build` (Linux/Mac) or `.\gradlew.bat build` (Windows)
- **Run Minecraft client with mod**: `./gradlew runClient`
- **Run dedicated server with mod**: `./gradlew runServer`
- **Run data generation**: `./gradlew runData`
- **Run game test server**: `./gradlew runGameTestServer`
- **Clean build artifacts**: `./gradlew clean`
- **Refresh dependencies**: `./gradlew --refresh-dependencies`

### Development Workflow
- The project uses Gradle with the NeoForged ModDev plugin
- Java 21 is required (as specified in build.gradle)
- The mod targets Minecraft 1.21.8 with NeoForge 21.8.47
- Recommended IDEs: IntelliJ IDEA or Eclipse

## Resources and Documentation

### Official Documentation
- **NeoForged Documentation**: https://docs.neoforged.net/
- **NeoForged Discord**: https://discord.neoforged.net/
- **Mojang Mappings License**: https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

### Troubleshooting
- If missing libraries in IDE: run `./gradlew --refresh-dependencies`
- If encountering build issues: run `./gradlew clean` then rebuild

## Project Architecture

### Core Structure
This is a NeoForge Minecraft mod with the following key components:

- **Mod ID**: `hkbmod` (defined in `HKBMod.MODID`)
- **Package**: `de.davidvogt.hkbmod`
- **Main Classes**:
  - `HKBMod.java` - Main mod class with common logic, registries, and event handling
  - `HKBModClient.java` - Client-side only code (annotated with `@Mod(dist = Dist.CLIENT)`)
  - `Config.java` - Configuration system using NeoForge's ModConfigSpec

### Registry System
The mod uses NeoForge's DeferredRegister system for registration:
- **Blocks**: `HKBMod.BLOCKS` - DeferredRegister for blocks
- **Items**: `HKBMod.ITEMS` - DeferredRegister for items
- **Creative Tabs**: `HKBMod.CREATIVE_MODE_TABS` - DeferredRegister for creative mode tabs

### Configuration
- Configuration is handled through `Config.java` using ModConfigSpec
- Config file is registered in the main mod constructor
- Examples include boolean, integer, string, and list configurations
- Client-side config screen is available through the mods menu

### Resource Structure
- **Assets**: `src/main/resources/assets/hkbmod/` - Client-side resources
- **Data**: Data generation outputs to `src/generated/resources/`
- **Templates**: `src/main/templates/` contains templated files like `neoforge.mods.toml`
- **Language files**: Located in `assets/hkbmod/lang/`

### Build Process
- The build system uses property substitution from `gradle.properties` into templated files
- Generated resources are automatically included in the build
- The mod metadata is generated from template files during build

### Mapping Names
- The project uses official Mojang mapping names for methods and fields
- These mappings are covered by Mojang's license (see link above)
- Parchment mappings are used for better parameter names and javadocs

### Key Patterns
- Use DeferredRegister for all registry objects (blocks, items, etc.)
- Separate client and common code using appropriate annotations
- Event handling through both mod event bus (`modEventBus`) and NeoForge event bus (`NeoForge.EVENT_BUS`)
- Configuration should use ModConfigSpec for type safety and validation

### Content Creation Patterns

#### Items (`ModItems.java`)
- **Basic Items**: Use `ITEMS.registerSimpleItem("name")` for simple items
- **Tools**: Create custom `Tier` and use specific tool classes (`SwordItem`, `PickaxeItem`, etc.)
- **Armor**: Create custom `ArmorMaterial` and use `ArmorItem` with types
- **Food**: Use `FoodProperties.Builder()` for nutrition and effects
- **Custom Behavior**: Override methods in anonymous classes for special functionality

#### Blocks (`ModBlocks.java`)
- **Simple Blocks**: Use `BLOCKS.registerSimpleBlock("name", properties)`
- **Ore Blocks**: Use `DropExperienceBlock` with `UniformInt` for XP drops
- **Functional Blocks**: Use specific classes (`SlabBlock`, `StairBlock`, `DoorBlock`, etc.)
- **Block Items**: Always create corresponding `BlockItem` entries for inventory
- **Properties**: Use `BlockBehaviour.Properties` for hardness, sounds, light levels, etc.

#### Creative Tabs (`ModCreativeTabs.java`)
- Organize items logically in tabs
- Use `Component.translatable()` for tab titles
- Add items through `displayItems()` method

#### Data Generation
- **Framework**: ✅ Uses `GatherDataEvent.Client` in NeoForge 21.8 (working)
- **Language Provider**: ✅ `ModLanguageProvider.java` generates `en_us.json` translations
- **Status**: Basic data generation system is working and tested
- **Working Providers**: Language files automatically generated
- **API Issues**: Recipe, loot, model, and tag providers need NeoForge 21.8 API updates
- **Known Issues**: DropExperienceBlock ore blocks cause initialization problems and are disabled
- **Block Items**: Must be registered in `ModItems.java` to avoid circular dependencies
- Run with `./gradlew runData` to generate working data files (currently language only)

### Development Notes
- The project includes comprehensive examples of items, blocks, tools, armor, and food
- Data generation automatically creates recipes, loot tables, models, and language files
- All content follows NeoForge 21.8 patterns and best practices
- Logging is done through SLF4J logger (`HKBMod.LOGGER`)
- The mod uses Parchment mappings for better parameter names
- CI/CD is configured through GitHub Actions (`.github/workflows/build.yml`)
- I use Minecraft Verison 1.21.8 with NeoForge Version 21.8.47. Consider using the documentation which are linked in the CLAUDE.md
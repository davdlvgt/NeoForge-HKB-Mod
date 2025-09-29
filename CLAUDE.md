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

### Development Notes
- The project includes example implementations of blocks, items, and creative tabs
- Logging is done through SLF4J logger (`HKBMod.LOGGER`)
- The mod uses Parchment mappings for better parameter names
- CI/CD is configured through GitHub Actions (`.github/workflows/build.yml`)
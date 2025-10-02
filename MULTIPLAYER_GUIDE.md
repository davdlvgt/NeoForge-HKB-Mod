# Multiplayer Development Guide

This guide covers the configuration and patterns for ensuring your HKB mod works properly in both Singleplayer and Multiplayer environments.

## Current Configuration Status

✅ **Your mod is already configured for Singleplayer and Multiplayer compatibility!**

The following configurations are in place:

### 1. Mod Metadata Configuration
- **neoforge.mods.toml**: Dependencies are configured with `side="BOTH"` for NeoForge and Minecraft
- This ensures the mod loads on both client and server

### 2. Proper Client/Server Separation
- **HKBMod.java**: Main mod class runs on both sides
- **HKBModClient.java**: Client-only code with `@Mod(dist = Dist.CLIENT)` annotation
- **SideUtils.java**: Utility class for safe side checking

### 3. Network System Ready
- **NetworkHandler.java**: Prepared for multiplayer packet communication
- Ready for CustomPacketPayload implementation when needed

## Important Development Patterns

### Side Safety
```java
// Always check which side you're on
if (SideUtils.isServerLevel(level)) {
    // Server-only logic here
}

if (SideUtils.isClientLevel(level)) {
    // Client-only logic here
}
```

### Event Handling
```java
// Server events (already implemented in HKBMod.java)
@SubscribeEvent
public void onServerStarting(ServerStartingEvent event) {
    // This runs only on the server side
}
```

### Configuration
- **Config.java** uses `ModConfig.Type.COMMON` - synced between client and server
- For client-only configs, use `ModConfig.Type.CLIENT`
- For server-only configs, use `ModConfig.Type.SERVER`

## Testing Your Mod

### Singleplayer Testing
```bash
./gradlew runClient
```

### Multiplayer Testing
1. **Start a dedicated server:**
   ```bash
   ./gradlew runServer
   ```

2. **Start a client and connect to localhost**

### What to Test
- [ ] Mod loads without errors on both client and server
- [ ] Items and blocks appear correctly
- [ ] Configuration syncs properly
- [ ] No client-only code runs on server
- [ ] No server-only code runs on client

## Adding Network Communication

When you need to add multiplayer features that require client-server communication:

1. **Create packet classes using CustomPacketPayload**
2. **Update NetworkHandler.java to register packets**
3. **Use the networking documentation**: https://docs.neoforged.net/docs/networking/

## Common Multiplayer Issues to Avoid

### ❌ Don't Do This
```java
// This crashes dedicated servers!
Minecraft.getInstance().player.sendMessage(...);
```

### ✅ Do This Instead
```java
// Safe server-side player messaging
if (player instanceof ServerPlayer serverPlayer) {
    serverPlayer.sendSystemMessage(Component.literal("Hello!"));
}
```

### ❌ Don't Do This
```java
// This causes sync issues
player.getInventory().add(new ItemStack(Items.DIAMOND));
```

### ✅ Do This Instead
```java
// Server authoritative
if (!level.isClientSide) {
    player.getInventory().add(new ItemStack(Items.DIAMOND));
}
```

## Key Classes Reference

- **HKBMod.java**: Main mod class, runs on both sides
- **HKBModClient.java**: Client-only initialization
- **Config.java**: Shared configuration
- **SideUtils.java**: Side checking utilities
- **NetworkHandler.java**: Network communication (when needed)

## Resources

- **NeoForge Docs**: https://docs.neoforged.net/
- **Networking Guide**: https://docs.neoforged.net/docs/networking/
- **NeoForged Discord**: https://discord.neoforged.net/
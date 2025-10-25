package de.davidvogt.hkbmod.block.entity;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.item.entity.ModEntities;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Dragon Nest Block Entity - Manages dragon spawning and nest behavior.
 * <p>
 * Features:
 * - Spawns 1-2 dragons when first created
 * - Tracks linked dragons by UUID
 * - Respawns dragons after they die (with cooldown)
 * - Dragons know this nest as their home
 */
public class DragonNestBlockEntity extends BlockEntity {

    // Configuration
    private static final int MIN_DRAGONS = 1;
    private static final int MAX_DRAGONS = 2;
    private static final int RESPAWN_COOLDOWN_TICKS = 12000; // 10 minutes (12000 ticks)
    private static final int SPAWN_RADIUS = 10; // Dragons spawn within 10 blocks
    private static final int CHECK_INTERVAL = 100; // Check dragon status every 5 seconds

    // State
    private final Set<UUID> linkedDragonUUIDs = new HashSet<>();
    private long lastRespawnTime = 0;
    private int tickCounter = 0;
    private boolean initialized = false;

    public DragonNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAGON_NEST_BE.get(), pos, state);
    }

    /**
     * Server-side tick method
     */
    public static void tick(ServerLevel level, BlockPos pos, BlockState state, DragonNestBlockEntity blockEntity) {
        blockEntity.tickCounter++;

        // First-time initialization: spawn initial dragons
        if (!blockEntity.initialized) {
            blockEntity.spawnInitialDragons(level);
            blockEntity.initialized = true;
            blockEntity.setChanged();
        }

        // Periodically check dragon status and respawn if needed
        if (blockEntity.tickCounter % CHECK_INTERVAL == 0) {
            blockEntity.checkAndMaintainDragons(level);
        }
    }

    /**
     * Spawns the initial set of dragons when the nest is first created
     */
    private void spawnInitialDragons(ServerLevel level) {
        int dragonCount = MIN_DRAGONS + level.random.nextInt(MAX_DRAGONS - MIN_DRAGONS + 1);
        HKBMod.LOGGER.info("[DragonNest] Spawning {} initial dragons at nest position {}", dragonCount, worldPosition);

        for (int i = 0; i < dragonCount; i++) {
            spawnDragon(level);
        }
    }

    /**
     * Spawns a single dragon near the nest
     */
    private void spawnDragon(ServerLevel level) {
        // Find a safe spawn position near the nest
        BlockPos spawnPos = findSafeSpawnPosition(level);
        if (spawnPos == null) {
            HKBMod.LOGGER.warn("[DragonNest] Could not find safe spawn position for dragon near {}", worldPosition);
            return;
        }

        // Create and spawn the dragon
        DragonEntity dragon = ModEntities.DRAGON.get().create(level, EntitySpawnReason.STRUCTURE);
        if (dragon != null) {
            dragon.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5);
            dragon.setNestPosition(worldPosition);
            dragon.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.STRUCTURE, null);

            if (level.addFreshEntity(dragon)) {
                linkedDragonUUIDs.add(dragon.getUUID());
                HKBMod.LOGGER.info("[DragonNest] Spawned dragon {} at position {}", dragon.getUUID(), spawnPos);
                setChanged();
            } else {
                HKBMod.LOGGER.error("[DragonNest] Failed to add dragon entity to world");
            }
        }
    }

    /**
     * Finds a safe position to spawn a dragon near the nest
     */
    private BlockPos findSafeSpawnPosition(ServerLevel level) {
        RandomSource random = level.getRandom();

        // Try 10 times to find a good spawn position
        for (int attempt = 0; attempt < 10; attempt++) {
            int xOffset = random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
            int zOffset = random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
            int yOffset = random.nextInt(5) - 2; // Slight vertical variance

            BlockPos testPos = worldPosition.offset(xOffset, yOffset, zOffset);

            // Check if position is safe (not inside blocks, has air space)
            if (level.getBlockState(testPos).isAir() &&
                    level.getBlockState(testPos.above()).isAir() &&
                    level.getBlockState(testPos.above(2)).isAir()) {
                return testPos;
            }
        }

        // Fallback to nest position + some height
        return worldPosition.above(5);
    }

    /**
     * Checks if dragons are alive and respawns them if needed
     */
    private void checkAndMaintainDragons(ServerLevel level) {
        // Remove dead dragons from the set
        linkedDragonUUIDs.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            boolean isDead = entity == null || !entity.isAlive();
            if (isDead) {
                HKBMod.LOGGER.info("[DragonNest] Dragon {} is dead or missing, removing from nest", uuid);
            }
            return isDead;
        });

        // Count alive dragons
        int aliveDragons = linkedDragonUUIDs.size();

        // Check if we need to spawn more dragons
        if (aliveDragons < MIN_DRAGONS) {
            long currentTime = level.getGameTime();

            // Check respawn cooldown
            if (currentTime - lastRespawnTime >= RESPAWN_COOLDOWN_TICKS) {
                int dragonsToSpawn = MIN_DRAGONS - aliveDragons;
                HKBMod.LOGGER.info("[DragonNest] Respawning {} dragons (current alive: {})", dragonsToSpawn, aliveDragons);

                for (int i = 0; i < dragonsToSpawn; i++) {
                    spawnDragon(level);
                }

                lastRespawnTime = currentTime;
                setChanged();
            }
        }
    }

    /**
     * Registers a dragon with this nest
     */
    public void registerDragon(UUID dragonUUID) {
        if (linkedDragonUUIDs.add(dragonUUID)) {
            HKBMod.LOGGER.info("[DragonNest] Registered dragon {} with nest at {}", dragonUUID, worldPosition);
            setChanged();
        }
    }

    /**
     * Unregisters a dragon from this nest
     */
    public void unregisterDragon(UUID dragonUUID) {
        if (linkedDragonUUIDs.remove(dragonUUID)) {
            HKBMod.LOGGER.info("[DragonNest] Unregistered dragon {} from nest at {}", dragonUUID, worldPosition);
            setChanged();
        }
    }

    /**
     * Returns the set of linked dragon UUIDs
     */
    public Set<UUID> getLinkedDragons() {
        return Collections.unmodifiableSet(linkedDragonUUIDs);
    }

    /**
     * Called when the nest block is destroyed
     */
    public void onNestDestroyed() {
        if (level instanceof ServerLevel serverLevel) {
            // Notify all linked dragons that their nest is gone
            for (UUID uuid : linkedDragonUUIDs) {
                Entity entity = serverLevel.getEntity(uuid);
                if (entity instanceof DragonEntity dragon) {
                    dragon.setNestPosition(null);
                    HKBMod.LOGGER.info("[DragonNest] Removed nest link from dragon {} (nest destroyed)", uuid);
                }
            }
            linkedDragonUUIDs.clear();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        onNestDestroyed();
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        // Save linked dragon UUIDs as strings (more compatible)
        output.putInt("DragonCount", linkedDragonUUIDs.size());
        int index = 0;
        for (UUID uuid : linkedDragonUUIDs) {
            output.putString("Dragon" + index, uuid.toString());
            index++;
        }

        // Save other state
        output.putLong("LastRespawnTime", lastRespawnTime);
        output.putBoolean("Initialized", initialized);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // Load linked dragon UUIDs
        linkedDragonUUIDs.clear();
        int dragonCount = input.getIntOr("DragonCount", 0);
        for (int i = 0; i < dragonCount; i++) {
            String uuidString = input.getStringOr("Dragon" + i, "");
            if (!uuidString.isEmpty()) {
                try {
                    linkedDragonUUIDs.add(UUID.fromString(uuidString));
                } catch (IllegalArgumentException e) {
                    HKBMod.LOGGER.error("[DragonNest] Failed to parse UUID: {}", uuidString);
                }
            }
        }

        // Load other state
        lastRespawnTime = input.getLongOr("LastRespawnTime", 0L);
        initialized = input.getBooleanOr("Initialized", false);
    }
}

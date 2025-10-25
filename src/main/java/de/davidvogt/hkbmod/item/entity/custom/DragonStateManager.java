package de.davidvogt.hkbmod.item.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Manages all state for the DragonEntity, including synced data accessors and state transitions.
 * This consolidates state management that was scattered throughout DragonEntity.
 */
public class DragonStateManager {
    // Entity Data Accessors - synced to client
    public static final EntityDataAccessor<Boolean> DATA_IS_LANDED =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_LANDING_MODE =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_RESTING =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_TAMED =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_ATE_GOLDEN_APPLE =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> DATA_IS_SITTING =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_FLYING_MODE =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonStateManager.class);
    private final DragonEntity dragon;

    // Non-synced state
    private int landedTimer = 0;
    private int flyingTimer = 0;
    private int tamingTimer = 0;
    private int fireballCooldown = 0;
    private int fireBreathDuration = 0;
    private int fireBreathCooldown = 0;
    private int onGroundTimer = 0;
    private int inAirTimer = 0;
    private BlockPos nestPosition = null;

    public DragonStateManager(DragonEntity dragon) {
        this.dragon = dragon;
    }

    /**
     * Defines all synced data for the dragon
     */
    public static void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_IS_LANDED, false);
        builder.define(DATA_IS_LANDING_MODE, false);
        builder.define(DATA_IS_RESTING, false);
        builder.define(DATA_IS_TAMED, false);
        builder.define(DATA_ATE_GOLDEN_APPLE, false);
        builder.define(DATA_OWNER_UUID, "");
        builder.define(DATA_IS_SITTING, false);
        builder.define(DATA_IS_FLYING_MODE, false);
    }

    // === LANDED STATE ===
    public boolean isLanded() {
        return dragon.getEntityData().get(DATA_IS_LANDED);
    }

    public void setLanded(boolean landed) {
        boolean oldValue = dragon.getEntityData().get(DATA_IS_LANDED);
        if (oldValue != landed && !dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} landed state: {} -> {} at {}", dragon.getId(), oldValue, landed, dragon.blockPosition());
        }
        dragon.getEntityData().set(DATA_IS_LANDED, landed);
    }

    // === LANDING MODE STATE ===
    public boolean isLandingMode() {
        return dragon.getEntityData().get(DATA_IS_LANDING_MODE);
    }

    public void setLandingMode(boolean landingMode) {
        boolean oldValue = dragon.getEntityData().get(DATA_IS_LANDING_MODE);
        if (oldValue != landingMode && !dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} landing mode: {} -> {}", dragon.getId(), oldValue, landingMode);
        }
        dragon.getEntityData().set(DATA_IS_LANDING_MODE, landingMode);
    }

    // === RESTING STATE ===
    public boolean isResting() {
        return dragon.getEntityData().get(DATA_IS_RESTING);
    }

    public void setResting(boolean resting) {
        boolean oldValue = dragon.getEntityData().get(DATA_IS_RESTING);
        if (oldValue != resting && !dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} resting: {} -> {}", dragon.getId(), oldValue, resting);
        }
        dragon.getEntityData().set(DATA_IS_RESTING, resting);

        // Update pose based on resting state
        if (resting && !isSitting()) {
            dragon.setPose(Pose.SLEEPING);
        } else if (!isSitting()) {
            dragon.setPose(Pose.STANDING);
        }
    }

    // === TAMED STATE ===
    public boolean isTamed() {
        return dragon.getEntityData().get(DATA_IS_TAMED);
    }

    public void setTamed(boolean tamed) {
        boolean oldValue = dragon.getEntityData().get(DATA_IS_TAMED);
        if (oldValue != tamed && !dragon.level().isClientSide) {
            LOGGER.info("Dragon {} tamed: {} -> {}", dragon.getId(), oldValue, tamed);
        }
        dragon.getEntityData().set(DATA_IS_TAMED, tamed);
    }

    // === SITTING STATE ===
    public boolean isSitting() {
        return dragon.getEntityData().get(DATA_IS_SITTING);
    }

    public void setSitting(boolean sitting) {
        boolean oldValue = dragon.getEntityData().get(DATA_IS_SITTING);
        if (oldValue != sitting && !dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} sitting: {} -> {}", dragon.getId(), oldValue, sitting);
        }
        dragon.getEntityData().set(DATA_IS_SITTING, sitting);

        // Update pose
        if (sitting) {
            dragon.setPose(Pose.CROUCHING);
            if (isResting()) {
                setResting(false);
            }
        } else {
            dragon.setPose(Pose.STANDING);
        }
    }

    // === FLYING MODE STATE ===
    public boolean isFlyingMode() {
        return dragon.getEntityData().get(DATA_IS_FLYING_MODE);
    }

    public void setFlyingMode(boolean flyingMode) {
        boolean oldValue = dragon.getEntityData().get(DATA_IS_FLYING_MODE);
        if (oldValue != flyingMode && !dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} flying mode: {} -> {}", dragon.getId(), oldValue, flyingMode);
        }
        dragon.getEntityData().set(DATA_IS_FLYING_MODE, flyingMode);
    }

    // === GOLDEN APPLE STATE ===
    public boolean hasEatenGoldenApple() {
        return dragon.getEntityData().get(DATA_ATE_GOLDEN_APPLE);
    }

    public void setAteGoldenApple(boolean ateGoldenApple) {
        dragon.getEntityData().set(DATA_ATE_GOLDEN_APPLE, ateGoldenApple);
    }

    // === OWNER STATE ===
    public Optional<UUID> getOwnerUUID() {
        String uuidString = dragon.getEntityData().get(DATA_OWNER_UUID);
        if (uuidString == null || uuidString.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid owner UUID: {}", uuidString);
            return Optional.empty();
        }
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        dragon.getEntityData().set(DATA_OWNER_UUID, uuid == null ? "" : uuid.toString());
    }

    // === TIMERS ===
    public int getLandedTimer() {
        return landedTimer;
    }

    public void setLandedTimer(int timer) {
        this.landedTimer = timer;
    }

    public int getFlyingTimer() {
        return flyingTimer;
    }

    public void setFlyingTimer(int timer) {
        this.flyingTimer = timer;
    }

    public int getTamingTimer() {
        return tamingTimer;
    }

    public void setTamingTimer(int timer) {
        this.tamingTimer = timer;
    }

    public int getFireballCooldown() {
        return fireballCooldown;
    }

    public void setFireballCooldown(int cooldown) {
        this.fireballCooldown = cooldown;
    }

    public boolean canShootFireball() {
        return fireballCooldown <= 0;
    }

    public int getFireBreathDuration() {
        return fireBreathDuration;
    }

    public void incrementFireBreathDuration() {
        this.fireBreathDuration++;
    }

    public int getFireBreathCooldown() {
        return fireBreathCooldown;
    }

    public boolean canBreatheFireOnGround() {
        return fireBreathCooldown <= 0;
    }

    public void startFireBreathCooldown() {
        this.fireBreathCooldown = DragonConstants.FIRE_BREATH_COOLDOWN_TICKS;
        this.fireBreathDuration = 0;
    }

    public int getOnGroundTimer() {
        return onGroundTimer;
    }

    public void setOnGroundTimer(int timer) {
        this.onGroundTimer = timer;
    }

    public int getInAirTimer() {
        return inAirTimer;
    }

    public void setInAirTimer(int timer) {
        this.inAirTimer = timer;
    }

    // === NEST POSITION ===
    @Nullable
    public BlockPos getNestPosition() {
        return nestPosition;
    }

    public void setNestPosition(@Nullable BlockPos pos) {
        this.nestPosition = pos;
        if (pos != null && !dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} nest set to {}", dragon.getId(), pos);
        }
    }

    public boolean hasNest() {
        return nestPosition != null;
    }

    /**
     * Ticks all cooldowns and timers
     */
    public void tickTimers() {
        if (dragon.level().isClientSide) {
            return;
        }

        if (tamingTimer > 0) {
            tamingTimer--;
            if (tamingTimer == 0 && hasEatenGoldenApple()) {
                setAteGoldenApple(false);
                LOGGER.debug("Dragon {} taming timer expired", dragon.getId());
            }
        }

        if (fireballCooldown > 0) {
            fireballCooldown--;
        }

        if (fireBreathCooldown > 0) {
            fireBreathCooldown--;
        }
    }
}

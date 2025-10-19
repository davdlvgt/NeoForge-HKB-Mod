package de.davidvogt.hkbmod.item.entity.custom;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.item.ModItems;
import de.davidvogt.hkbmod.item.entity.ai.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom Dragon entity that resembles a smaller version of the Ender Dragon.
 * This dragon spawns rarely on mountains and has neutral behavior by default.
 *
 * Dragons defend their nests by shooting explosive fireballs at players who approach within 30 blocks.
 */
public class DragonEntity extends Monster {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonEntity.class);

    // Synced data accessors - these automatically sync to client
    private static final EntityDataAccessor<Boolean> DATA_IS_LANDED =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_LANDING_MODE =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_RESTING =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_TAMED =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ATE_GOLDEN_APPLE =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_IS_SITTING =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_FLYING_MODE =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    // Per-entity scale so you can experiment with different sizes per dragon
    private static final EntityDataAccessor<Float> DATA_DRAGON_SCALE =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);

    private final DragonFlightHistory flightHistory = new DragonFlightHistory();
    private int landedTimer = 0;
    private int flyingTimer = 0;
    private int tamingTimer = 0; // Timer for 3-second window after eating golden apple
    private int lastJumpTime = 0; // Timer for double-tap detection (spacebar)
    private int fireballCooldown = 0; // Cooldown for fireball attacks
    private int fireBreathDuration = 0; // How long dragon has been breathing fire
    private int fireBreathCooldown = 0; // Cooldown after breathing fire

    // Stable ground detection with hysteresis
    private int onGroundTimer = 0; // Counts ticks dragon has been on ground
    private int inAirTimer = 0; // Counts ticks dragon has been in air

    private boolean isBreathingFire = false; // Whether dragon is actively breathing fire

    // Nest position tracking
    @Nullable
    private BlockPos nestPosition = null;

    public DragonEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        // Enable flying capability - dragons should fly!
        this.setNoGravity(true);
        // Use custom move control that doesn't interfere with rotation
        this.moveControl = new DragonMoveControl(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IS_LANDED, false);
        builder.define(DATA_IS_LANDING_MODE, false);
        builder.define(DATA_IS_RESTING, false);
        builder.define(DATA_IS_TAMED, false);
        builder.define(DATA_ATE_GOLDEN_APPLE, false);
        builder.define(DATA_OWNER_UUID, "");
        builder.define(DATA_IS_SITTING, false);
        builder.define(DATA_IS_FLYING_MODE, false);
        // Initialize per-entity scale with the default from DragonConstants
        builder.define(DATA_DRAGON_SCALE, DragonConstants.DRAGON_SCALE);
    }

    public boolean isLanded() {
        return this.entityData.get(DATA_IS_LANDED);
    }

    public void setLanded(boolean landed) {
        boolean oldValue = this.entityData.get(DATA_IS_LANDED);
        if (oldValue != landed && !this.level().isClientSide) {
            LOGGER.debug("Dragon {} state change - isLanded: {} -> {} at position: {}, {}, {}",
                this.getId(), oldValue, landed, String.format("%.2f", this.getX()),
                String.format("%.2f", this.getY()), String.format("%.2f", this.getZ()));
        }
        this.entityData.set(DATA_IS_LANDED, landed);
    }

    public boolean isLandingMode() {
        return this.entityData.get(DATA_IS_LANDING_MODE);
    }

    public void setLandingMode(boolean landingMode) {
        boolean oldValue = this.entityData.get(DATA_IS_LANDING_MODE);
        if (oldValue != landingMode && !this.level().isClientSide) {
            LOGGER.debug("Dragon {} state change - isLandingMode: {} -> {} at position: {}, {}, {}",
                this.getId(), oldValue, landingMode, String.format("%.2f", this.getX()),
                String.format("%.2f", this.getY()), String.format("%.2f", this.getZ()));
        }
        this.entityData.set(DATA_IS_LANDING_MODE, landingMode);
    }

    /**
     * Checks if the dragon is resting (curled up like a polar fox)
     */
    public boolean isResting() {
        return this.entityData.get(DATA_IS_RESTING);
    }

    /**
     * Sets the resting state for the dragon
     */
    public void setResting(boolean resting) {
        boolean oldValue = this.entityData.get(DATA_IS_RESTING);
        if (oldValue != resting && !this.level().isClientSide) {
            LOGGER.debug("Dragon {} state change - isResting: {} -> {} at position: {}, {}, {}",
                this.getId(), oldValue, resting, String.format("%.2f", this.getX()),
                String.format("%.2f", this.getY()), String.format("%.2f", this.getZ()));
        }
        this.entityData.set(DATA_IS_RESTING, resting);

        // Set the pose to SLEEPING when resting (like polar fox curling up)
        // BUT: Don't change pose if dragon is sitting (sitting has priority)
        if (resting) {
            if (!this.isSitting()) {
                this.setPose(net.minecraft.world.entity.Pose.SLEEPING);
            }
        } else {
            if (!this.isSitting()) {
                this.setPose(net.minecraft.world.entity.Pose.STANDING);
            }
        }
    }

    public boolean isTamed() {
        return this.entityData.get(DATA_IS_TAMED);
    }

    public void setTamed(boolean tamed) {
        boolean oldValue = this.entityData.get(DATA_IS_TAMED);
        if (oldValue != tamed && !this.level().isClientSide) {
            LOGGER.info("Dragon {} state change - isTamed: {} -> {} at position: {}, {}, {}",
                this.getId(), oldValue, tamed, String.format("%.2f", this.getX()),
                String.format("%.2f", this.getY()), String.format("%.2f", this.getZ()));
        }
        this.entityData.set(DATA_IS_TAMED, tamed);
    }

    /**
     * Checks if the dragon is sitting (waiting for owner's command)
     */
    public boolean isSitting() {
        return this.entityData.get(DATA_IS_SITTING);
    }

    /**
     * Sets the sitting state for the dragon
     */
    public void setSitting(boolean sitting) {
        boolean oldValue = this.entityData.get(DATA_IS_SITTING);
        if (oldValue != sitting && !this.level().isClientSide) {
            LOGGER.info("Dragon {} state change - isSitting: {} -> {} at position: {}, {}, {}",
                this.getId(), oldValue, sitting, String.format("%.2f", this.getX()),
                String.format("%.2f", this.getY()), String.format("%.2f", this.getZ()));
        }
        this.entityData.set(DATA_IS_SITTING, sitting);

        // Set the pose to CROUCHING when sitting (SLEEPING blocks interactions!)
        if (sitting) {
            this.setPose(net.minecraft.world.entity.Pose.CROUCHING);
            // Also disable resting if sitting (they are mutually exclusive)
            if (this.isResting()) {
                this.setResting(false);
            }
        } else {
            this.setPose(net.minecraft.world.entity.Pose.STANDING);
            LOGGER.debug("Dragon {} pose set to STANDING, dragon should be able to move now", this.getId());
        }
    }

    public boolean hasEatenGoldenApple() {
        return this.entityData.get(DATA_ATE_GOLDEN_APPLE);
    }

    public void setAteGoldenApple(boolean ateGoldenApple) {
        boolean oldValue = this.entityData.get(DATA_ATE_GOLDEN_APPLE);
        if (oldValue != ateGoldenApple && !this.level().isClientSide) {
            LOGGER.debug("Dragon {} state change - ateGoldenApple: {} -> {} at position: {}, {}, {}",
                this.getId(), oldValue, ateGoldenApple, String.format("%.2f", this.getX()),
                String.format("%.2f", this.getY()), String.format("%.2f", this.getZ()));
        }
        this.entityData.set(DATA_ATE_GOLDEN_APPLE, ateGoldenApple);
    }

    /**
     * Checks if the dragon is in flying mode (when being ridden)
     */
    public boolean isFlyingMode() {
        return this.entityData.get(DATA_IS_FLYING_MODE);
    }

    /**
     * Sets the flying mode for the dragon (when being ridden)
     */
    public void setFlyingMode(boolean flyingMode) {
        boolean oldValue = this.entityData.get(DATA_IS_FLYING_MODE);
        if (oldValue != flyingMode && !this.level().isClientSide) {
            LOGGER.debug("Dragon {} state change - isFlyingMode: {} -> {} at position: {}, {}, {}",
                this.getId(), oldValue, flyingMode, String.format("%.2f", this.getX()),
                String.format("%.2f", this.getY()), String.format("%.2f", this.getZ()));
        }
        this.entityData.set(DATA_IS_FLYING_MODE, flyingMode);
    }

    /**
     * Gets the UUID of the owner if the dragon is tamed
     */
    public Optional<UUID> getOwnerUUID() {
        String uuidString = this.entityData.get(DATA_OWNER_UUID);
        if (uuidString == null || uuidString.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Sets the UUID of the owner - called when the dragon is tamed
     */
    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, uuid == null ? "" : uuid.toString());
    }

    public int getLandedTimer() {
        return landedTimer;
    }

    public void setLandedTimer(int timer) {
        if (!this.level().isClientSide && timer != this.landedTimer) {
            LOGGER.debug("Dragon {} landedTimer set to: {} ticks ({} seconds)",
                this.getId(), timer, timer / 20.0);
        }
        this.landedTimer = timer;
    }

    public int getFlyingTimer() {
        return flyingTimer;
    }

    public void setFlyingTimer(int timer) {
        this.flyingTimer = timer;
    }

    /**
     * Sets the nest position for this dragon
     */
    public void setNestPosition(@Nullable BlockPos pos) {
        this.nestPosition = pos;
        if (!this.level().isClientSide && pos != null) {
            LOGGER.info("Dragon {} nest position set to: {}", this.getId(), pos.toShortString());
        }
    }

    /**
     * Gets the nest position for this dragon
     */
    @Nullable
    public BlockPos getNestPosition() {
        return this.nestPosition;
    }

    /**
     * Checks if this dragon has a nest
     */
    public boolean hasNest() {
        return this.nestPosition != null;
    }

    @Override
    protected void registerGoals() {
        // Priority 0: Retaliate when dragon is attacked (highest priority)
        this.goalSelector.addGoal(0, new DragonRetaliateAttackGoal(this));

        // Priority 1: Follow owner if tamed
        this.goalSelector.addGoal(1, new FollowOwnerGoal(this, 0.6D, 6.0F, 15.0F));

        // Priority 2: Protect owner - breathes fire at monsters near the owner (6 block radius)
        this.goalSelector.addGoal(2, new DragonProtectOwnerGoal(this, 6.0D));

        // Priority 3: Return to nest when too far away or health is low (only for wild dragons)
        this.goalSelector.addGoal(3, new ReturnToNestGoal(this));

        // Priority 4: Defend nest from nearby players (only for wild dragons)
        this.goalSelector.addGoal(4, new DefendNestGoal(this));

        // Priority 5: Rest occasionally (lie down and curl up like polar fox) (only for wild dragons)
        this.goalSelector.addGoal(5, new DragonRestGoal(this));

        // Priority 6: Custom flying behavior - handles all movement AND rotation (only for wild dragons)
        this.goalSelector.addGoal(6, new DragonFlyingGoal(this));

        // Targeting: react to being hurt
        this.targetSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));

        // Removed LookAtPlayerGoal - it was interfering with flight direction
        // The dragon should look where it's flying, not at players

        // TODO: Add fireball attack goal when player attacks
        // this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Custom goal to shoot fireballs at attackers will be added here
    }

    /**
     * Shoots an explosive fireball at the target
     */
    public void shootExplosiveFireball(LivingEntity target) {
        if (this.level().isClientSide) {
            return;
        }

        // Calculate direction to target
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5) - this.getY(0.5);
        double dz = target.getZ() - this.getZ();

        // Create a large fireball (like Ghast fireballs) with TNT-like explosion
        ExplosiveFireballEntity fireball = new ExplosiveFireballEntity(this.level(), this, dx, dy, dz);

        // Position the fireball in front of the dragon's mouth
        Vec3 lookVec = this.getViewVector(1.0F);
        double spawnDistance = 2.0;
        fireball.setPos(
            this.getX() + lookVec.x * spawnDistance,
            this.getY(0.5) + 0.5,
            this.getZ() + lookVec.z * spawnDistance
        );

        // Add the fireball to the world
        this.level().addFreshEntity(fireball);

        // Play fireball shooting sound
        this.playSound(SoundEvents.GHAST_SHOOT, 1.0F, 1.0F);
    }

    /**
     * Shoots a fireball in the direction the rider is looking (used when player commands the dragon in the air)
     * Can be called when player is riding the dragon in the air and presses the fire key
     */
    public void shootFireballInDirection(Vec3 direction) {
        if (this.level().isClientSide) {
            LOGGER.debug("Dragon {} shootFireballInDirection called on CLIENT - returning", this.getId());
            return;
        }

        LOGGER.debug("Dragon {} shootFireballInDirection executing on SERVER", this.getId());

        // Normalize direction
        Vec3 normalizedDir = direction.normalize();

        // Create a large fireball (like Ghast fireballs) with TNT-like explosion
        ExplosiveFireballEntity fireball = new ExplosiveFireballEntity(
            this.level(),
            this,
            normalizedDir.x,
            normalizedDir.y,
            normalizedDir.z
        );

        // Position the fireball in front of the dragon's mouth
        Vec3 lookVec = this.getViewVector(1.0F);
        double spawnDistance = 2.0;
        fireball.setPos(
            this.getX() + lookVec.x * spawnDistance,
            this.getY(0.5) + 0.5,
            this.getZ() + lookVec.z * spawnDistance
        );

        // Add the fireball to the world
        this.level().addFreshEntity(fireball);

        // Play fireball shooting sound
        this.playSound(SoundEvents.GHAST_SHOOT, 1.0F, 1.0F);

        LOGGER.debug("Dragon {} fireball shot in direction: {}", this.getId(), normalizedDir);
    }

    /**
     * Checks if the dragon can shoot a fireball (cooldown check)
     */
    public boolean canShootFireball() {
        return this.fireballCooldown <= 0;
    }

    /**
     * Gets the current fireball cooldown in ticks
     */
    public int getFireballCooldown() {
        return this.fireballCooldown;
    }

    /**
     * Sets the fireball cooldown to a specific value
     */
    public void setFireballCooldown(int ticks) {
        this.fireballCooldown = ticks;
    }

    /**
     * Checks if the dragon can breathe fire on ground (cooldown check)
     */
    public boolean canBreatheFireOnGround() {
        return this.fireBreathCooldown <= 0;
    }

    /**
     * Gets the current fire breath cooldown in ticks
     */
    public int getFireBreathCooldown() {
        return this.fireBreathCooldown;
    }

    /**
     * Gets the current fire breath duration in ticks
     */
    public int getFireBreathDuration() {
        return this.fireBreathDuration;
    }

    /**
     * Increments the fire breath duration by 1 tick
     */
    public void incrementFireBreathDuration() {
        this.fireBreathDuration++;
    }

    /**
     * Starts the fire breath cooldown (2 seconds = 40 ticks) and resets duration
     */
    public void startFireBreathCooldown() {
        this.fireBreathCooldown = 40; // 2 seconds
        this.fireBreathDuration = 0;
    }

    /**
     * Breathes fire at close range, creating a cone of fire particles and damaging entities
     * Used when dragon is close to the target (< 10 blocks)
     */
    public void breatheFire(LivingEntity target) {
        if (this.level().isClientSide) {
            return;
        }

        // Calculate direction to target
        Vec3 lookVec = this.getViewVector(1.0F);
        Vec3 startPos = new Vec3(this.getX(), this.getY(0.5) + 0.5, this.getZ()).add(lookVec.scale(6.0));

        // Create a cone of fire in front of the dragon
        // Fire breath extends 8 blocks forward
        double breathRange = 8.0D;

        // Check multiple points in a cone shape for entities to damage
        for (double distance = 1.0D; distance <= breathRange; distance += 0.5D) {
            // Create a cone by checking slightly offset positions
            for (double offset = -0.5D; offset <= 0.5D; offset += 0.25D) {
                Vec3 perpendicular = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
                Vec3 checkPos = startPos.add(
                    lookVec.x * distance + perpendicular.x * offset * distance * 0.3,
                    lookVec.y * distance,
                    lookVec.z * distance + perpendicular.z * offset * distance * 0.3
                );

                // Spawn fire particles on client side via packet
                if (!this.level().isClientSide) {
                    // Server: Send particle packet to clients
                    ((net.minecraft.server.level.ServerLevel)this.level()).sendParticles(
                        ParticleTypes.FLAME,
                        checkPos.x, checkPos.y, checkPos.z,
                        2, // particle count
                        0.1, 0.1, 0.1, // random offset
                        0.01 // speed
                    );

                    // Also add some smoke
                    if (distance > 2.0D && this.random.nextFloat() < 0.3F) {
                        ((net.minecraft.server.level.ServerLevel)this.level()).sendParticles(
                            ParticleTypes.LARGE_SMOKE,
                            checkPos.x, checkPos.y, checkPos.z,
                            1,
                            0.15, 0.15, 0.15,
                            0.01
                        );
                    }
                }

                // Check for entities to damage in a small radius around each point
                AABB damageBox = new AABB(checkPos.x - 0.5, checkPos.y - 0.5, checkPos.z - 0.5,
                                          checkPos.x + 0.5, checkPos.y + 0.5, checkPos.z + 0.5);

                List<LivingEntity> entities = this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    damageBox,
                    entity -> entity != this && entity.isAlive()
                );

                for (LivingEntity entity : entities) {
                    // Deal fire damage
                    entity.hurt(this.damageSources().mobAttack(this), 3.0F);
                    // Set entity on fire for 5 seconds (100 ticks)
                    entity.setRemainingFireTicks(100);
                }
            }
        }

        // Play dragon fire sound
        this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 1.0F, 0.8F);

        if (!this.level().isClientSide) {
            LOGGER.debug("Dragon {} breathing fire at close range", this.getId());
        }
    }

    /**
     * Breathes fire in a specific direction (used when player commands the dragon)
     * Can be called when player is riding the dragon and right-clicks with empty main hand
     */
    public void breatheFireInDirection(Vec3 direction) {
        if (this.level().isClientSide) {
            LOGGER.debug("Dragon {} breatheFireInDirection called on CLIENT - returning", this.getId());
            return;
        }

        LOGGER.debug("Dragon {} breatheFireInDirection executing on SERVER", this.getId());
        LOGGER.debug("Dragon {} position: {}, {}, {}", this.getId(), this.getX(), this.getY(), this.getZ());
        LOGGER.debug("Dragon {} direction: {}", this.getId(), direction);

        Vec3 normalizedDir = direction.normalize();
        Vec3 startPos = new Vec3(this.getX(), this.getY(0.5) + 0.5, this.getZ()).add(normalizedDir.scale(2.0));


        LOGGER.debug("Dragon {} start position: {}", this.getId(), startPos);
        LOGGER.debug("Dragon {} normalized direction: {}", this.getId(), normalizedDir);

        // Create a cone of fire in front of the dragon
        // Fire breath extends 10 blocks forward (slightly longer than auto-breath)
        double breathRange = 6.0D;

        int particleCount = 0;
        int entityHitCount = 0;

        // Check multiple points in a cone shape for entities to damage
        for (double distance = 1.0D; distance <= breathRange; distance += 0.5D) {
            // Create a cone by checking slightly offset positions
            for (double offset = -0.5D; offset <= 0.5D; offset += 0.25D) {
                Vec3 perpendicular = new Vec3(-normalizedDir.z, 0, normalizedDir.x).normalize();
                Vec3 checkPos = startPos.add(
                    normalizedDir.x * distance + perpendicular.x * offset * distance * 0.3,
                    normalizedDir.y * distance,
                    normalizedDir.z * distance + perpendicular.z * offset * distance * 0.3
                );

                // Spawn fire particles
                ((ServerLevel)this.level()).sendParticles(
                    ParticleTypes.FLAME,
                    checkPos.x, checkPos.y, checkPos.z,
                    5, // More particles for visibility
                    0.2, 0.2, 0.2,
                    0.03
                );
                particleCount++;

                // Add smoke
                if (distance > 2.0D && this.random.nextFloat() < 0.5F) {
                    ((ServerLevel)this.level()).sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        checkPos.x, checkPos.y, checkPos.z,
                        3,
                        0.25, 0.25, 0.25,
                        0.02
                    );
                }


                // Check for entities to damage
                AABB damageBox = new AABB(checkPos.x - 0.6, checkPos.y - 0.6, checkPos.z - 0.6,
                                          checkPos.x + 0.6, checkPos.y + 0.6, checkPos.z + 0.6);

                List<LivingEntity> entities = this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    damageBox,
                    entity -> entity != this && entity.isAlive() && !entity.equals(this.getControllingPassenger())
                );

                for (LivingEntity entity : entities) {
                    // Deal fire damage (slightly more damage than auto-breath)
                    entity.hurt(this.damageSources().mobAttack(this), 4.0F);
                    // Set entity on fire for 6 seconds (120 ticks)
                    entity.setRemainingFireTicks(120);
                    entityHitCount++;
                    LOGGER.debug("Dragon {} hit entity: {}", this.getId(), entity.getName().getString());
                }
            }
        }

        // Play dragon fire sound
        this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 1.5F, 0.8F);

        LOGGER.debug("Dragon {} spawned {} particle groups", this.getId(), particleCount);
        LOGGER.debug("Dragon {} hit {} entities", this.getId(), entityHitCount);
        LOGGER.debug("Dragon {} breathing fire in commanded direction - complete", this.getId());
    }

    /**
     * Get the flight history for rendering animations
     */
    public DragonFlightHistory getFlightHistory() {
        return this.flightHistory;
    }

    /**
     * Define the attributes for the dragon entity
     * These are scaled down from the Ender Dragon's attributes
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DragonConstants.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, DragonConstants.MOVEMENT_SPEED)
                .add(Attributes.FLYING_SPEED, DragonConstants.FLYING_SPEED)
                .add(Attributes.FOLLOW_RANGE, DragonConstants.FOLLOW_RANGE)
                .add(Attributes.ATTACK_DAMAGE, DragonConstants.ATTACK_DAMAGE)
                .add(Attributes.ARMOR, DragonConstants.ARMOR)
                .add(Attributes.KNOCKBACK_RESISTANCE, DragonConstants.KNOCKBACK_RESISTANCE);
    }

    /**
     * Get the scale factor for this dragon compared to the Ender Dragon.
     * The Ender Dragon model will be scaled down to this size in the renderer.
     *
     * @return Scale factor (0.35 = 35% of original Ender Dragon size)
     */
    public float getDragonScale() {
        return this.entityData.get(DATA_DRAGON_SCALE);
    }

    /**
     * Set the per-entity dragon scale. This is synced to clients.
     * Use values >0.0f. Values too large may cause clipping or unexpected behavior.
     */
    public void setDragonScale(float scale) {
        float old = this.entityData.get(DATA_DRAGON_SCALE);
        if (Float.compare(old, scale) != 0) {
            if (!this.level().isClientSide) {
                LOGGER.info("Dragon {} scale changed: {} -> {}", this.getId(), old, scale);
            }
            this.entityData.set(DATA_DRAGON_SCALE, scale);
        }
    }

    /**
     * Override to prevent the dragon from being affected by certain effects
     */
    @Override
    public boolean fireImmune() {
        return true; // Dragons are immune to fire
    }

    /**
     * Dragons don't take fall damage - they can land from any height
     */
    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
        // Don't call super - no fall damage for dragons
        // Dragons can land from any height safely
    }

    /**
     * Dragons can push through blocks when landing - prevents suffocation
     */
    @Override
    public boolean isPushable() {
        return false; // Can't be pushed by entities or blocks
    }

    /**
     * Override to ensure dragon can be interacted with even when sitting
     * (CROUCHING pose should allow interactions, but we override this to be safe)
     */
    @Override
    public boolean isPickable() {
        return true; // Always allow interactions with the dragon
    }

    /**
     * Allow players to ride the dragon if it's tamed
     */
    @Override
    protected boolean canAddPassenger(net.minecraft.world.entity.Entity passenger) {
        // Only allow one passenger (the owner)
        return this.getPassengers().isEmpty() && this.isTamed();
    }

    /**
     * Position the rider on the dragon's back
     */
    @Override
    protected void positionRider(net.minecraft.world.entity.Entity passenger, net.minecraft.world.entity.Entity.MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            // Position the rider on the dragon's back, scaled appropriately
            float scale = this.getDragonScale();
            double xOffset = 0.0D;
            double yOffset = 1.2D * scale; // Adjust height based on dragon scale
            double zOffset = scale -1.0D; // Slightly behind of center

            // Apply rotation to offset
            float yaw = this.getYRot() * ((float)Math.PI / 180F);
            double rotatedX = xOffset * Math.cos(yaw) - zOffset * Math.sin(yaw);
            double rotatedZ = xOffset * Math.sin(yaw) + zOffset * Math.cos(yaw);

            moveFunction.accept(passenger, this.getX() + rotatedX, this.getY() + yOffset, this.getZ() + rotatedZ);
        }
    }

    /**
     * Get the Y offset for passengers riding this dragon
     */
    public double getPassengersRidingOffset() {
        return this.getBbHeight() * 0.75D;
    }

    /**
     * Get the entity that is controlling this dragon (the rider)
     */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity firstPassenger = this.getFirstPassenger();
        if (firstPassenger instanceof Player player) {
            // Only the owner can control the dragon
            if (this.getOwnerUUID().isPresent() && this.getOwnerUUID().get().equals(player.getUUID())) {
                return player;
            }
        }
        return null;
    }

    /**
     * Handle movement when a player is riding the dragon
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.isAlive()) {
            LivingEntity rider = this.getControllingPassenger();

            if (rider != null && this.isVehicle()) {
                // The dragon is being ridden - use player's input

                // Set rotation to match rider's look direction
                this.setYRot(rider.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(rider.getXRot() * 0.5F); // Reduce pitch influence for smoother flight
                this.setRot(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();

                // Get player's movement input
                float strafe = rider.xxa * 0.5F; // A/D keys (left/right)
                float forward = rider.zza; // W/S keys (forward/backward)

                // Flight control: Only fly when spacebar is held
                boolean isJumping = rider.isJumping();

                if (isJumping) {
                    // FLIGHT MODE - Spacebar is held

                    float movementSpeed = (float)this.getAttributeValue(Attributes.FLYING_SPEED) * DragonConstants.RIDING_FLYING_SPEED_MULTIPLIER;

                    // W key increases speed (boost effect)
                    float speedMultiplier = DragonConstants.RIDING_GROUND_SPEED_MULTIPLIER;
                    if (forward > 0) {
                        // W pressed = fly faster (1.5x to 2.0x speed)
                        speedMultiplier = DragonConstants.RIDING_GROUND_SPEED_MULTIPLIER + (forward * DragonConstants.BOOST_SPEED_MULTIPLIER);
                    } else if (forward < 0) {
                        // S pressed = slower / backwards (0.5x speed)
                        speedMultiplier = 0.5F;
                    }

                    // Enable flying
                    this.setNoGravity(true);
                    this.setLanded(false); // Only set to false in active flight mode
                    this.setFlyingMode(true);

                    // Mouse controls flight direction (look vector)
                    Vec3 lookVec = rider.getLookAngle();

                    // Movement in look direction with speed multiplier
                    Vec3 forwardMovement = new Vec3(
                        lookVec.x * movementSpeed * speedMultiplier,
                        lookVec.y * movementSpeed * speedMultiplier, // Full vertical control via mouse
                        lookVec.z * movementSpeed * speedMultiplier
                    );

                    // Strafe movement (A/D for sideways movement)
                    Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
                    Vec3 strafeMovement = new Vec3(
                        rightVec.x * strafe * movementSpeed * DragonConstants.STRAFE_SPEED_MULTIPLIER,
                        0,
                        rightVec.z * strafe * movementSpeed * DragonConstants.STRAFE_SPEED_MULTIPLIER
                    );

                    // Optional: Shift for targeted descent (independent of mouse look direction)
                    Vec3 verticalAdjustment = Vec3.ZERO;
                    if (rider.isShiftKeyDown()) {
                        verticalAdjustment = new Vec3(0, -movementSpeed * DragonConstants.DESCENT_SPEED, 0);
                    }

                    // Combine all movements
                    Vec3 totalMovement = forwardMovement.add(strafeMovement).add(verticalAdjustment);

                    // Apply movement
                    this.setDeltaMovement(
                        totalMovement.x,
                        totalMovement.y,
                        totalMovement.z
                    );

                    // Play flying sound occasionally
                    if (this.tickCount % 20 == 0) {
                        this.playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.5F, 1.0F);
                    }

                    // Move the entity
                    this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                } else {
                    // LANDING MODE - Spacebar not pressed, dragon glides back to ground
                    this.setFlyingMode(false);

                    // Check if dragon is touching ground
                    boolean isOnGround = this.onGround();

                    // DEBUG: Show current status
                    if (!this.level().isClientSide && this.tickCount % 5 == 0) {
                        LOGGER.debug("Dragon {} travel - onGround: {}, isLanded: {}, flyingMode: {}",
                            this.getId(), isOnGround, this.isLanded(), this.isFlyingMode());
                    }

                    if (!isOnGround) {
                        // Dragon still in air - gentle gliding to ground
                        this.setNoGravity(false); // Activate gravity for gentle descent

                        float movementSpeed = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.5F;

                        // Horizontal movement still possible during gliding
                        Vec3 lookVec = rider.getLookAngle();
                        Vec3 forwardMovement = new Vec3(
                            lookVec.x * forward * movementSpeed,
                            0,
                            lookVec.z * forward * movementSpeed
                        );

                        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
                        Vec3 strafeMovement = new Vec3(
                            rightVec.x * strafe * movementSpeed,
                            0,
                            rightVec.z * strafe * movementSpeed
                        );

                        // Gentle descent - gravity + small braking
                        Vec3 currentMovement = this.getDeltaMovement();
                        Vec3 totalMovement = forwardMovement.add(strafeMovement);

                        this.setDeltaMovement(
                            totalMovement.x,
                            currentMovement.y * DragonConstants.GLIDE_DESCENT_MULTIPLIER - DragonConstants.GLIDE_DESCENT_BASE, // Gentle descent
                            totalMovement.z
                        );

                        // Move the entity
                        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

                        // Check again if dragon has now landed
                        if (this.onGround()) {
                            if (!this.isLanded()) {
                                this.playSound(SoundEvents.HORSE_LAND, 1.0F, 1.0F);
                                LOGGER.info("Dragon {} landed on ground - setting isLanded to TRUE", this.getId());
                            }
                            this.setLanded(true);
                        }
                    } else {
                        // WALKING MODE - Dragon is on ground and walking
                        this.setNoGravity(false);

                        // DEBUG: Show when we set isLanded
                        if (!this.level().isClientSide && !this.isLanded()) {
                            LOGGER.debug("Dragon {} is on ground in walk mode - setting isLanded to TRUE", this.getId());
                        }
                        this.setLanded(true); // IMPORTANT: Set landed to true!

                        // Use normal ground movement speed
                        float movementSpeed = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);

                        // Set the movement vector for normal walking
                        this.setSpeed(movementSpeed);

                        // Let the default travel logic handle ground movement
                        super.travel(new Vec3(strafe * 2.0F, travelVector.y, forward * 2.0F));
                        return;
                    }
                }

                // Don't call super.travel() for flying/gliding modes
                return;
            }
        }

        // Default movement when not being ridden
        super.travel(travelVector);
    }

    /**
     * Dragons can fly
     */
    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        net.minecraft.world.entity.ai.navigation.FlyingPathNavigation flyingpathnavigation = new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this, level);
        flyingpathnavigation.setCanOpenDoors(false);
        flyingpathnavigation.setCanFloat(true);
        return flyingpathnavigation;
    }

    /**
     * Override to prevent dragons from despawning when far from players.
     * Dragons should persist in the world like other important entities.
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // Dragons never despawn due to distance
    }

    /**
     * Override to make dragons persistent like named mobs or villagers.
     * This prevents them from being removed by the game's entity cleanup.
     */
    @Override
    public boolean requiresCustomPersistence() {
        return true; // Dragons always require custom persistence - never despawn
    }

    /**
     * Ambient sound - using Ender Dragon sounds for now
     */
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return net.minecraft.sounds.SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    /**
     * Sound when the dragon takes damage
     */
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return net.minecraft.sounds.SoundEvents.ENDER_DRAGON_HURT;
    }

    /**
     * Sound when the dragon dies
     */
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.ENDER_DRAGON_DEATH;
    }

    /**
     * Volume of the dragon's sounds (quieter than Ender Dragon)
     */
    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    /**
     * Override die to log death for debugging
     */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        if (!this.level().isClientSide) {
            /*System.out.println("[DRAGON-DEATH] Dragon died from: " + source.getMsgId() +
                " at position: " + String.format("%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()) +
                ", State: isLanded=" + isLanded() + ", isLandingMode=" + isLandingMode());*/
        }
        super.die(source);
    }

    @Override
    public void tick() {
        super.tick();

        // NEW STABLE GROUND DETECTION WITH HYSTERESIS AND TOLERANCE
        if (!this.level().isClientSide) {
            // Grant flight abilities to rider to prevent "Flying is not enabled" kick
            LivingEntity rider = this.getControllingPassenger();
            if (rider instanceof Player player) {
                // Enable flight while riding the dragon
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
            }

            // If dragon has a rider
            if (this.isVehicle() && rider != null) {
                // If flight mode is active (spacebar pressed), set isLanded immediately to false
                if (this.isFlyingMode()) {
                    this.onGroundTimer = 0;
                    this.inAirTimer = 0;
                    if (this.isLanded()) {
                        this.setLanded(false);
                    }
                } else {
                    // NOT in flight mode: Use hysteresis logic with tolerance check
                    boolean currentlyOnGround = this.onGround();

                    // ADDITIONAL CHECK: Is dragon within 0.1 blocks above ground?
                    boolean isNearGround = false;
                    if (!currentlyOnGround) {
                        // Check vertical distance to ground
                        BlockPos posBelow = this.blockPosition().below();
                        double distanceToGround = this.getY() - posBelow.getY() - 1.0; // -1.0 because posBelow is already 1 block below dragon

                        // If dragon is hovering less than 0.1 blocks above ground, consider it "on ground"
                        if (distanceToGround <= DragonConstants.GROUND_TOLERANCE_DISTANCE) {
                            isNearGround = true;
                        }
                    }

                    // Dragon is considered "on ground" if onGround() is true OR it's very close to ground
                    boolean effectivelyOnGround = currentlyOnGround || isNearGround;

                    if (effectivelyOnGround) {
                        // Dragon touching ground or very close
                        this.onGroundTimer++;
                        this.inAirTimer = 0;

                        // Only when STABLE on ground (10+ ticks), set isLanded to true
                        if (this.onGroundTimer >= DragonConstants.GROUND_STABILITY_THRESHOLD_TICKS && !this.isLanded()) {
                            this.setLanded(true);
                            LOGGER.debug("Dragon {} stable on ground for {} ticks (tolerance check: {}) - setting isLanded to TRUE",
                                this.getId(), this.onGroundTimer, isNearGround);
                        }
                    } else {
                        // Dragon is in the air
                        this.inAirTimer++;
                        this.onGroundTimer = 0;

                        // Only when STABLE in air (10+ ticks), set isLanded to false
                        if (this.inAirTimer >= DragonConstants.AIR_STABILITY_THRESHOLD_TICKS && this.isLanded()) {
                            this.setLanded(false);
                            LOGGER.debug("Dragon {} stable in air for {} ticks - setting isLanded to FALSE",
                                this.getId(), this.inAirTimer);
                        }
                    }
                }
            } else {
                // No rider: Reset timers
                this.onGroundTimer = 0;
                this.inAirTimer = 0;
            }
        }

        // Handle taming timer countdown
        if (!this.level().isClientSide && this.tamingTimer > 0) {
            this.tamingTimer--;
            if (this.tamingTimer == 0 && this.hasEatenGoldenApple()) {
                // Timer expired - reset the golden apple state
                this.setAteGoldenApple(false);
                LOGGER.info("Dragon {} taming timer expired, resetting golden apple state", this.getId());
            }
        }

        // Handle fireball cooldown countdown
        if (!this.level().isClientSide && this.fireballCooldown > 0) {
            this.fireballCooldown--;
        }

        // Handle fire breath cooldown countdown
        if (!this.level().isClientSide && this.fireBreathCooldown > 0) {
            this.fireBreathCooldown--;
        }

        // Update rotation to match movement direction every tick
        Vec3 deltaMovement = this.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(deltaMovement.x * deltaMovement.x + deltaMovement.z * deltaMovement.z);

        // Debug output every 20 ticks (once per second)
        if (this.tickCount % 20 == 0 && !this.level().isClientSide) {
            /*System.out.println("[DRAGON-TICK] State: isLanded=" + isLanded() + ", isLandingMode=" + isLandingMode() +
                ", flyingTimer=" + flyingTimer + "/" + 400 + " (" + (flyingTimer / 20.0) + "s/" + (400 / 20.0) + "s)" +
                ", landedTimer=" + landedTimer + "/" + 200 + " (" + (landedTimer / 20.0) + "s/" + (200 / 20.0) + "s)");
            System.out.println("[DRAGON-TICK] Position: " + String.format("%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()) +
                ", Speed: " + String.format("%.3f", horizontalSpeed) + ", NoGravity: " + this.isNoGravity());*/
        }

        if (horizontalSpeed > 0.001D) {
            // Calculate yaw (horizontal rotation) from movement direction
            // In Minecraft: Yaw 0 = South (+Z), Yaw 90 = West (-X), Yaw 180 = North (-Z), Yaw 270 = East (+X)
            // atan2 gives us the angle, but we need to convert it to Minecraft's coordinate system
            float targetYaw = (float)(Mth.atan2(deltaMovement.z, deltaMovement.x) * (180.0 / Math.PI)) - 90.0F;

            // Debug output
            if (this.tickCount % 20 == 0 && !this.level().isClientSide) {
                LOGGER.debug("Dragon {} target yaw: {}", this.getId(), String.format("%.2f", targetYaw));
            }

            // Smooth rotation transition - use rotLerp to handle angle wrapping correctly
            float currentYaw = this.getYRot();
            // Increase interpolation factor for faster rotation
            float newYaw = Mth.rotLerp(0.98F, currentYaw, targetYaw);

            // Set yaw rotation
            this.setYRot(newYaw);
            this.yBodyRot = newYaw;
            this.yHeadRot = newYaw;

            // Calculate pitch (vertical rotation) from movement direction
            float targetPitch = (float)(-Mth.atan2(deltaMovement.y, horizontalSpeed) * (180.0 / Math.PI));
            targetPitch = Mth.clamp(targetPitch, -60.0F, 60.0F);

            // Smooth pitch transition
            float currentPitch = this.getXRot();
            float newPitch = Mth.rotLerp(0.9F, currentPitch, targetPitch);
            this.setXRot(newPitch);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        // Remove flight ability when player dismounts
        if (passenger instanceof Player player && !this.level().isClientSide) {
            // Only remove mayfly if the player is not in creative mode
            if (!player.getAbilities().instabuild) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
        super.removePassenger(passenger);
    }

    /**
     * Handles player interaction with the dragon (taming system)
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.CONSUME;
        }

        // Only process MAIN_HAND to avoid double-processing
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        // Check if player is already the owner (for sitting/standing commands)
        if (this.isTamed() && this.getOwnerUUID().isPresent() && this.getOwnerUUID().get().equals(player.getUUID())) {
            LOGGER.debug("Dragon {} is tamed - checking interaction with owner {}", this.getId(), player.getName().getString());

            // --- FEEDING LOGIC: owner may feed dragon with golden apples ---
            // Accept enchanted golden apple or normal golden apple from either hand
            ItemStack feedStack = ItemStack.EMPTY;
            boolean feedFromMain = false;
            if (mainHandItem.is(Items.ENCHANTED_GOLDEN_APPLE) || mainHandItem.is(Items.GOLDEN_APPLE)) {
                feedStack = mainHandItem;
                feedFromMain = true;
            } else if (offHandItem.is(Items.ENCHANTED_GOLDEN_APPLE) || offHandItem.is(Items.GOLDEN_APPLE)) {
                feedStack = offHandItem;
                feedFromMain = false;
            }

            if (!feedStack.isEmpty()) {
                // Only owner can feed (we're already in owner branch, but double-check)
                if (this.getOwnerUUID().isPresent() && this.getOwnerUUID().get().equals(player.getUUID())) {
                    // Enchanted golden apple: full heal
                    if (feedStack.is(Items.ENCHANTED_GOLDEN_APPLE) && this.getHealth() != this.getMaxHealth()) {
                        if (!player.isCreative()) {
                            feedStack.shrink(1);
                        }
                        this.setHealth(this.getMaxHealth());
                        this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
                        this.level().broadcastEntityEvent(this, (byte) 7); // hearts
                        LOGGER.info("Dragon {} fully healed by enchanted golden apple from owner {}", this.getId(), player.getName().getString());
                        return InteractionResult.SUCCESS;
                    }

                    // Normal golden apple: heal 1/3 of max health
                    if (feedStack.is(Items.GOLDEN_APPLE) && this.getHealth() != this.getMaxHealth()) {
                        if (!player.isCreative()) {
                            feedStack.shrink(1);
                        }
                        float healAmount = this.getMaxHealth() / 3.0F;
                        this.heal(healAmount);
                        this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
                        this.level().broadcastEntityEvent(this, (byte) 7); // hearts
                        LOGGER.info("Dragon {} healed by {} HP from golden apple by owner {}", this.getId(), healAmount, player.getName().getString());
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            // --- end feeding logic ---

            // Check if player has a lead in offhand and dragon is not sitting
            boolean hasLeadInOffhand = offHandItem.is(Items.LEAD);

            if (hasLeadInOffhand && !this.isSitting()) {
                // Player wants to ride the dragon
                if (player.startRiding(this)) {
                    LOGGER.info("Dragon {} now being ridden by player {}", this.getId(), player.getName().getString());
                    this.playSound(SoundEvents.HORSE_SADDLE.value(), 1.0F, 1.0F);
                    return InteractionResult.SUCCESS;
                } else {
                    LOGGER.warn("Dragon {} failed to mount for player {}", this.getId(), player.getName().getString());
                    return InteractionResult.FAIL;
                }
            }

            // Toggle sitting/following with BOTH HANDS EMPTY
            // This prevents accidental toggling when holding items
            boolean hasBothHandsEmpty = mainHandItem.isEmpty() && offHandItem.isEmpty();

            LOGGER.debug("Dragon {} interaction - MainHand empty: {}, OffHand empty: {}, Both hands empty: {}",
                this.getId(), mainHandItem.isEmpty(), offHandItem.isEmpty(), hasBothHandsEmpty);

            if (hasBothHandsEmpty) {
                boolean wasSitting = this.entityData.get(DATA_IS_SITTING);

                LOGGER.debug("Dragon {} player clicked with empty hands - wasSitting: {}, toggling to: {}",
                    this.getId(), wasSitting, !wasSitting);

                // Toggle between sitting and following
                this.setSitting(!wasSitting);

                LOGGER.debug("Dragon {} after setSitting - isSitting: {}", this.getId(), this.isSitting());

                if (this.isSitting()) {
                    // Dragon sits down
                    this.getNavigation().stop(); // Stop any movement
                    this.setLanded(true); // Ensure dragon is on ground
                    this.setNoGravity(true); // Prevent falling through blocks
                    this.playSound(SoundEvents.WOLF_STEP, 1.0F, 1.0F);
                    LOGGER.info("Dragon {} is now sitting and waiting - Status: isSitting: {}, isLanded: {}, noGravity: {}",
                        this.getId(), this.isSitting(), this.isLanded(), this.isNoGravity());
                } else {
                    // Dragon stands up and follows
                    LOGGER.debug("Dragon {} standing up - Before: isSitting: {}, isLanded: {}, noGravity: {}",
                        this.getId(), this.isSitting(), this.isLanded(), this.isNoGravity());

                    this.setLanded(true); // Set to landed so it walks
                    this.setLandingMode(false); // Not in landing mode
                    this.setNoGravity(false); // Enable gravity for walking
                    this.setResting(false); // Make sure not resting

                    LOGGER.debug("Dragon {} standing up - After: isSitting: {}, isLanded: {}, noGravity: {}, isResting: {}, pose: {}",
                        this.getId(), this.isSitting(), this.isLanded(), this.isNoGravity(), this.isResting(), this.getPose());

                    this.playSound(SoundEvents.WOLF_STEP, 1.0F, 1.0F);
                    LOGGER.info("Dragon {} is now following owner - ready to walk", this.getId());
                }

                return InteractionResult.SUCCESS;
            } else {
                LOGGER.debug("Dragon {} interaction - hands not empty, not toggling sitting state", this.getId());
            }
        }

        // Check if dragon is not yet tamed
        if (!this.isTamed()) {
            // Step 1: Player needs enchanted golden apple in left hand (offhand) and dragon saddle in right hand (mainhand)
            boolean hasEnchantedAppleInOffhand = offHandItem.is(Items.ENCHANTED_GOLDEN_APPLE);
            boolean hasDragonSaddleInMainhand = mainHandItem.is(ModItems.DRAGON_SADDLE.get());

            if (hasEnchantedAppleInOffhand && !this.hasEatenGoldenApple()) {
                // Dragon eats the enchanted golden apple
                if (!player.isCreative()) {
                    offHandItem.shrink(1);
                }

                this.setAteGoldenApple(true);
                this.tamingTimer = 60; // 3 seconds = 60 ticks

                // Play eating sound and show hearts
                this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
                this.level().broadcastEntityEvent(this, (byte) 7); // Heart particles

                LOGGER.info("Dragon {} ate enchanted golden apple from player {} - 3 seconds to apply saddle",
                    this.getId(), player.getName().getString());
                return InteractionResult.SUCCESS;
            } else if (this.hasEatenGoldenApple() && hasDragonSaddleInMainhand && this.tamingTimer > 0) {
                // Step 2: Player applies dragon saddle within 3 seconds
                if (!player.isCreative()) {
                    mainHandItem.shrink(1);
                }

                // Dragon is now tamed!
                this.setTamed(true);
                this.setOwnerUUID(player.getUUID());
                this.setAteGoldenApple(false);
                this.tamingTimer = 0;

                // Play success sound and show hearts
                this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
                this.level().broadcastEntityEvent(this, (byte) 7); // Heart particles

                // Heal the dragon to full health
                this.setHealth(this.getMaxHealth());

                LOGGER.info("Dragon {} successfully tamed by player {}", this.getId(), player.getName().getString());
                return InteractionResult.SUCCESS;
            }
        }

        return super.mobInteract(player, hand);
    }

    /**
     * Checks if a player is holding an enchanted golden apple in either hand
     */
    public boolean isPlayerHoldingEnchantedGoldenApple(Player player) {
        return player.getMainHandItem().is(Items.ENCHANTED_GOLDEN_APPLE) ||
                player.getOffhandItem().is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        // Save nest position
        if (this.nestPosition != null) {
            output.putInt("NestX", this.nestPosition.getX());
            output.putInt("NestY", this.nestPosition.getY());
            output.putInt("NestZ", this.nestPosition.getZ());
            output.putBoolean("HasNest", true);
        } else {
            output.putBoolean("HasNest", false);
        }

        // Save timers
        output.putInt("LandedTimer", this.landedTimer);
        output.putInt("FlyingTimer", this.flyingTimer);
        output.putInt("TamingTimer", this.tamingTimer);

        // Save per-entity scale so it persists between world saves
        output.putFloat("DragonScale", this.getDragonScale());

        // Save taming data
        output.putBoolean("IsTamed", this.isTamed());
        output.putBoolean("IsSitting", this.isSitting());
        output.putBoolean("AteGoldenApple", this.hasEatenGoldenApple());
        if (this.getOwnerUUID().isPresent()) {
            output.putString("OwnerUUID", this.getOwnerUUID().get().toString());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        // Load nest position
        if (input.getBooleanOr("HasNest", false)) {
            int x = input.getIntOr("NestX", 0);
            int y = input.getIntOr("NestY", 0);
            int z = input.getIntOr("NestZ", 0);
            this.nestPosition = new BlockPos(x, y, z);
            if (!this.level().isClientSide) {
                LOGGER.info("Dragon {} loaded nest position: {}", this.getId(), this.nestPosition.toShortString());
            }
        }

        // Load timers
        this.landedTimer = input.getIntOr("LandedTimer", 0);
        this.flyingTimer = input.getIntOr("FlyingTimer", 0);
        this.tamingTimer = input.getIntOr("TamingTimer", 0);

        // Load per-entity scale; default to constant if not present
        float loadedScale = input.getFloatOr("DragonScale", DragonConstants.DRAGON_SCALE);
        this.entityData.set(DATA_DRAGON_SCALE, loadedScale);

        // Load taming data
        this.setTamed(input.getBooleanOr("IsTamed", false));
        this.setSitting(input.getBooleanOr("IsSitting", false));
        this.setAteGoldenApple(input.getBooleanOr("AteGoldenApple", false));
        String ownerUuidString = input.getStringOr("OwnerUUID", "");
        if (!ownerUuidString.isEmpty()) {
            try {
                this.setOwnerUUID(UUID.fromString(ownerUuidString));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Dragon {} failed to parse owner UUID: {}", this.getId(), ownerUuidString);
            }
        }
    }

    /**
     * Drops custom death loot, such as dragon skin
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);

        // Drop 1-3 dragon skin items on death
        int skinCount = 1 + this.random.nextInt(3); // 1, 2, or 3 skins
        for (int i = 0; i < skinCount; i++) {
            this.spawnAtLocation(level, new ItemStack(ModItems.DRAGON_SKIN.get()));
        }

        // TODO: Add chance to drop rare items like dragon eggs or special weapons
    }
}

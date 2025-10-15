package de.davidvogt.hkbmod.item.entity.custom;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.item.ModItems;
import de.davidvogt.hkbmod.item.entity.ai.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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

    private final DragonFlightHistory flightHistory = new DragonFlightHistory();
    private int landedTimer = 0;
    private int flyingTimer = 0;
    private int tamingTimer = 0; // Timer für die 3-Sekunden-Zeitspanne nach dem Essen des goldenen Apfels

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
    }

    public boolean isLanded() {
        return this.entityData.get(DATA_IS_LANDED);
    }

    public void setLanded(boolean landed) {
        boolean oldValue = this.entityData.get(DATA_IS_LANDED);
        if (oldValue != landed && !this.level().isClientSide) {
            System.out.println("[DRAGON] State change - isLanded: " + oldValue + " -> " + landed +
                " at position: " + String.format("%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
        }
        this.entityData.set(DATA_IS_LANDED, landed);
    }

    public boolean isLandingMode() {
        return this.entityData.get(DATA_IS_LANDING_MODE);
    }

    public void setLandingMode(boolean landingMode) {
        boolean oldValue = this.entityData.get(DATA_IS_LANDING_MODE);
        if (oldValue != landingMode && !this.level().isClientSide) {
            System.out.println("[DRAGON] State change - isLandingMode: " + oldValue + " -> " + landingMode +
                " at position: " + String.format("%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
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
            System.out.println("[DRAGON] State change - isResting: " + oldValue + " -> " + resting +
                " at position: " + String.format("%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
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
            System.out.println("[DRAGON] State change - isTamed: " + oldValue + " -> " + tamed +
                    " at position: " + String.format("%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
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
            System.out.println("[DRAGON] State change - isSitting: " + oldValue + " -> " + sitting +
                    " at position: " + String.format("%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
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
            System.out.println("[DRAGON] Pose set to STANDING, dragon should be able to move now");
        }
    }

    public boolean hasEatenGoldenApple() {
        return this.entityData.get(DATA_ATE_GOLDEN_APPLE);
    }

    public void setAteGoldenApple(boolean ateGoldenApple) {
        boolean oldValue = this.entityData.get(DATA_ATE_GOLDEN_APPLE);
        if (oldValue != ateGoldenApple && !this.level().isClientSide) {
            System.out.println("[DRAGON] State change - ateGoldenApple: " + oldValue + " -> " + ateGoldenApple +
                    " at position: " + String.format("%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
        }
        this.entityData.set(DATA_ATE_GOLDEN_APPLE, ateGoldenApple);
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
            System.out.println("[DRAGON] landedTimer set to: " + timer + " ticks (" + (timer / 20.0) + " seconds)");
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
            System.out.println("[DRAGON] Nest position set to: " + pos.toShortString());
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
        // Priority 0: Follow owner if tamed (HIGHEST PRIORITY for tamed dragons)
        this.goalSelector.addGoal(0, new FollowOwnerGoal(this, 0.6D, 3.0F, 15.0F));

        // Priority 1: Return to nest when too far away or health is low (only for wild dragons)
        this.goalSelector.addGoal(1, new ReturnToNestGoal(this));

        // Priority 2: Defend nest from nearby players (only for wild dragons)
        this.goalSelector.addGoal(2, new DefendNestGoal(this));

        // Priority 3: Rest occasionally (lie down and curl up like polar fox) (only for wild dragons)
        this.goalSelector.addGoal(3, new DragonRestGoal(this));

        // Priority 4: Custom flying behavior - handles all movement AND rotation (only for wild dragons)
        this.goalSelector.addGoal(4, new DragonFlyingGoal(this));

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
     * Breathes fire at close range, creating a cone of fire particles and damaging entities
     * Used when dragon is close to the target (< 10 blocks)
     */
    public void breatheFire(LivingEntity target) {
        if (this.level().isClientSide) {
            return;
        }

        // Calculate direction to target
        Vec3 lookVec = this.getViewVector(1.0F);
        Vec3 startPos = new Vec3(this.getX(), this.getY(0.5) + 0.5, this.getZ());

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
                        net.minecraft.core.particles.ParticleTypes.FLAME,
                        checkPos.x, checkPos.y, checkPos.z,
                        2, // particle count
                        0.1, 0.1, 0.1, // random offset
                        0.01 // speed
                    );

                    // Also add some smoke
                    if (distance > 2.0D && this.random.nextFloat() < 0.3F) {
                        ((net.minecraft.server.level.ServerLevel)this.level()).sendParticles(
                            net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
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
            System.out.println("[DRAGON-FIRE-BREATH] Dragon breathing fire at close range!");
        }
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
                .add(Attributes.MAX_HEALTH, 100.0D)      // Less than Ender Dragon's 200
                .add(Attributes.MOVEMENT_SPEED, 0.5D)    // Moderate speed
                .add(Attributes.FLYING_SPEED, 4.5D)      // Fast flying speed
                .add(Attributes.FOLLOW_RANGE, 64.0D)     // Can notice entities from far away
                .add(Attributes.ATTACK_DAMAGE, 8.0D)     // Moderate damage if it attacks
                .add(Attributes.ARMOR, 4.0D)             // Some protection
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D); // Resistant to knockback
    }

    /**
     * Get the scale factor for this dragon compared to the Ender Dragon.
     * The Ender Dragon model will be scaled down to this size in the renderer.
     *
     * @return Scale factor (0.35 = 35% of original Ender Dragon size)
     */
    public float getDragonScale() {
        return 0.35F;
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

        // Handle taming timer countdown
        if (!this.level().isClientSide && this.tamingTimer > 0) {
            this.tamingTimer--;
            if (this.tamingTimer == 0 && this.hasEatenGoldenApple()) {
                // Timer expired - reset the golden apple state
                this.setAteGoldenApple(false);
                System.out.println("[DRAGON-TAMING] Taming timer expired, resetting golden apple state");
            }
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
                System.out.println("Target Yaw: " + targetYaw);
            }

            // Smooth rotation transition - use rotLerp to handle angle wrapping correctly
            float currentYaw = this.getYRot();
            // Erhöhe den Interpolationsfaktor für schnellere Drehung
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
            System.out.println("[DRAGON-INTERACT] Is tamed! Checking interaction...");
            // Toggle sitting/following with BOTH HANDS EMPTY
            // This prevents accidental toggling when holding items
            boolean hasBothHandsEmpty = mainHandItem.isEmpty() && offHandItem.isEmpty();

            System.out.println("[DRAGON-INTERACT] MainHand empty: " + mainHandItem.isEmpty() + ", OffHand empty: " + offHandItem.isEmpty());
            System.out.println("[DRAGON-INTERACT] Both hands empty: " + hasBothHandsEmpty);

            if (hasBothHandsEmpty) {
                boolean wasSitting = this.isSitting();

                System.out.println("[DRAGON-INTERACT] Player clicked dragon with empty hands - wasSitting=" + wasSitting);
                System.out.println("[DRAGON-INTERACT] About to toggle sitting to: " + !wasSitting);

                // Toggle between sitting and following
                this.setSitting(!wasSitting);

                System.out.println("[DRAGON-INTERACT] After setSitting - isSitting=" + this.isSitting());

                if (this.isSitting()) {
                    // Dragon sits down
                    this.getNavigation().stop(); // Stop any movement
                    this.setLanded(true); // Ensure dragon is on ground
                    this.setNoGravity(true); // Prevent falling through blocks
                    this.playSound(SoundEvents.WOLF_STEP, 1.0F, 1.0F);
                    System.out.println("[DRAGON-TAMED] Dragon is now sitting and waiting");
                    System.out.println("[DRAGON-TAMED] Status: isSitting=" + this.isSitting() + ", isLanded=" + this.isLanded() + ", noGravity=" + this.isNoGravity());
                } else {
                    // Dragon stands up and follows
                    System.out.println("[DRAGON-TAMED] Dragon standing up - Before status change:");
                    System.out.println("  isSitting=" + this.isSitting() + ", isLanded=" + this.isLanded() + ", noGravity=" + this.isNoGravity());

                    this.setLanded(true); // Set to landed so it walks
                    this.setLandingMode(false); // Not in landing mode
                    this.setNoGravity(false); // Enable gravity for walking
                    this.setResting(false); // Make sure not resting

                    System.out.println("[DRAGON-TAMED] Dragon standing up - After status change:");
                    System.out.println("  isSitting=" + this.isSitting() + ", isLanded=" + this.isLanded() + ", noGravity=" + this.isNoGravity());
                    System.out.println("  isResting=" + this.isResting() + ", pose=" + this.getPose());

                    this.playSound(SoundEvents.WOLF_STEP, 1.0F, 1.0F);
                    System.out.println("[DRAGON-TAMED] Dragon is now following owner - ready to walk");
                }

                return InteractionResult.SUCCESS;
            } else {
                System.out.println("[DRAGON-INTERACT] Hands not empty, not toggling sitting state");
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

                System.out.println("[DRAGON-TAMING] Dragon ate enchanted golden apple! Player has 3 seconds to apply saddle.");
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

                System.out.println("[DRAGON-TAMING] Dragon successfully tamed by " + player.getName().getString() + "!");
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
                System.out.println("[DRAGON] Loaded nest position: " + this.nestPosition.toShortString());
            }
        }

        // Load timers
        this.landedTimer = input.getIntOr("LandedTimer", 0);
        this.flyingTimer = input.getIntOr("FlyingTimer", 0);
        this.tamingTimer = input.getIntOr("TamingTimer", 0);

        // Load taming data
        this.setTamed(input.getBooleanOr("IsTamed", false));
        this.setSitting(input.getBooleanOr("IsSitting", false));
        this.setAteGoldenApple(input.getBooleanOr("AteGoldenApple", false));
        String ownerUuidString = input.getStringOr("OwnerUUID", "");
        if (!ownerUuidString.isEmpty()) {
            try {
                this.setOwnerUUID(UUID.fromString(ownerUuidString));
            } catch (IllegalArgumentException e) {
                System.out.println("[DRAGON] Failed to parse owner UUID: " + ownerUuidString);
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

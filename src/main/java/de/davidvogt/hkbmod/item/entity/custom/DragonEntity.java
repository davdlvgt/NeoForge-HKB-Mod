package de.davidvogt.hkbmod.item.entity.custom;

import de.davidvogt.hkbmod.item.entity.ai.DragonFlyingGoal;
import de.davidvogt.hkbmod.item.entity.ai.DragonMoveControl;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Custom Dragon entity that resembles a smaller version of the Ender Dragon.
 * This dragon spawns rarely on mountains and has neutral behavior by default.
 *
 * TODO: Implement fireball attack when the dragon is attacked by a player
 */
public class DragonEntity extends Monster {

    private final DragonFlightHistory flightHistory = new DragonFlightHistory();

    public DragonEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        // Enable flying capability - dragons should fly!
        this.setNoGravity(true);
        // Use custom move control that doesn't interfere with rotation
        this.moveControl = new DragonMoveControl(this);
    }

    @Override
    protected void registerGoals() {
        // Priority 1: Custom flying behavior - handles all movement AND rotation
        this.goalSelector.addGoal(1, new DragonFlyingGoal(this));

        // Removed LookAtPlayerGoal - it was interfering with flight direction
        // The dragon should look where it's flying, not at players

        // TODO: Add fireball attack goal when player attacks
        // this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Custom goal to shoot fireballs at attackers will be added here
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
                .add(Attributes.FLYING_SPEED, 0.6D)      // Fast flying speed
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

    @Override
    public void tick() {
        super.tick();

        // Update rotation to match movement direction every tick
        Vec3 deltaMovement = this.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(deltaMovement.x * deltaMovement.x + deltaMovement.z * deltaMovement.z);

        // Debug output every 20 ticks (once per second)
        if (this.tickCount % 20 == 0 && !this.level().isClientSide) {
            System.out.println("Dragon Movement - X: " + deltaMovement.x + ", Z: " + deltaMovement.z + ", Speed: " + horizontalSpeed);
            System.out.println("Dragon Rotation - Current Yaw: " + this.getYRot());
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
            float newYaw = Mth.rotLerp(0.9F, currentYaw, targetYaw);

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
}

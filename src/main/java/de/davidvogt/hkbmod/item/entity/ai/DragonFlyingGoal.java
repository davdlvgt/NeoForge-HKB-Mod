package de.davidvogt.hkbmod.item.entity.ai;

import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Custom flying goal for the dragon that ensures smooth, continuous flight
 * with proper turning and height management.
 */
public class DragonFlyingGoal extends Goal {
    private final DragonEntity dragon;
    private double targetX;
    private double targetY;
    private double targetZ;
    private int flyingTimer = 0;
    private int pauseTimer = 0;
    private boolean isPaused = false;

    public DragonFlyingGoal(DragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return true; // Always active
    }

    @Override
    public boolean canContinueToUse() {
        return true; // Always active
    }

    @Override
    public void start() {
        pickNewTarget();
    }

    @Override
    public void tick() {
        flyingTimer++;

        // Handle pausing (0-3 seconds occasionally)
        if (isPaused) {
            pauseTimer--;
            if (pauseTimer <= 0) {
                isPaused = false;
                pickNewTarget();
            }
            // Slow down during pause but maintain altitude
            Vec3 motion = dragon.getDeltaMovement();
            dragon.setDeltaMovement(motion.x * 0.95, motion.y, motion.z * 0.95);
            maintainAltitude();
            return;
        }

        // Randomly pause (5% chance every second, up to 3 seconds)
        if (flyingTimer % 20 == 0 && dragon.getRandom().nextFloat() < 0.05F) {
            isPaused = true;
            pauseTimer = dragon.getRandom().nextInt(60); // 0-3 seconds
            return;
        }

        // Pick new target every 5-10 seconds
        if (flyingTimer > 100 + dragon.getRandom().nextInt(100)) {
            pickNewTarget();
            flyingTimer = 0;
        }

        // Get current position and direction TO TARGET
        Vec3 currentPos = dragon.position();
        Vec3 targetVec = new Vec3(targetX, targetY, targetZ);
        Vec3 directionToTarget = targetVec.subtract(currentPos);
        double distance = directionToTarget.length();

        // If close to target, pick new one
        if (distance < 5.0D) {
            pickNewTarget();
            return;
        }

        // Normalize direction to target
        Vec3 normalizedDirection = directionToTarget.normalize();

        // Calculate desired velocity - move TOWARDS THE TARGET
        double speed = 0.5D; // About 2x ground mob speed

        // Move directly towards target (horizontal only, Y handled separately)
        double desiredVelX = normalizedDirection.x * speed;
        double desiredVelZ = normalizedDirection.z * speed;

        // Get current velocity
        Vec3 currentVelocity = dragon.getDeltaMovement();

        // Smooth acceleration - interpolate between current and desired velocity
        double smoothFactor = 0.15D; // Higher value = faster response
        double newVelX = Mth.lerp(smoothFactor, currentVelocity.x, desiredVelX);
        double newVelZ = Mth.lerp(smoothFactor, currentVelocity.z, desiredVelZ);

        // Smooth altitude adjustment
        maintainAltitude();
        double newVelY = calculateVerticalVelocity();

        // Apply the new velocity
        dragon.setDeltaMovement(newVelX, newVelY, newVelZ);

        // Rotation is now handled in DragonEntity.tick() method
    }

    private void pickNewTarget() {
        // Pick a point in front of the dragon in a random direction
        double currentX = dragon.getX();
        double currentZ = dragon.getZ();

        // Random angle for turning
        double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = 30.0D + dragon.getRandom().nextDouble() * 40.0D; // 30-70 blocks away

        targetX = currentX + Math.cos(angle) * distance;
        targetZ = currentZ + Math.sin(angle) * distance;

        // Pick height between 140 and 180
        targetY = 140.0D + dragon.getRandom().nextDouble() * 40.0D;
    }

    private void maintainAltitude() {
        // This method helps keep the dragon between 140-180
        // Called to ensure emergency corrections
        double currentY = dragon.getY();

        if (currentY < 135.0D) {
            // Emergency: too low, force upward
            dragon.setDeltaMovement(dragon.getDeltaMovement().x, 0.15D, dragon.getDeltaMovement().z);
        } else if (currentY > 185.0D) {
            // Emergency: too high, force downward
            dragon.setDeltaMovement(dragon.getDeltaMovement().x, -0.15D, dragon.getDeltaMovement().z);
        }
    }

    private double calculateVerticalVelocity() {
        double currentY = dragon.getY();
        double heightDiff = targetY - currentY;

        // Smooth vertical movement
        return Mth.clamp(heightDiff * 0.02D, -0.08D, 0.08D);
    }
}

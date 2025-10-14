package de.davidvogt.hkbmod.item.entity.ai;

import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI Goal that makes dragons return to their nest when they're too far away
 * or when their health is low.
 */
public class ReturnToNestGoal extends Goal {
    private final DragonEntity dragon;
    private BlockPos nestPosition;
    private int checkInterval = 0;

    // Configuration
    private static final double MAX_DISTANCE_FROM_NEST = 128.0D; // Max distance before returning
    private static final double NEST_ARRIVAL_DISTANCE = 10.0D; // How close to get to nest
    private static final double LOW_HEALTH_THRESHOLD = 0.3D; // Return at 30% health
    private static final int CHECK_EVERY_N_TICKS = 40; // Check every 2 seconds

    public ReturnToNestGoal(DragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only run if dragon has a nest
        if (!dragon.hasNest()) {
            return false;
        }

        // Check periodically to avoid too many distance calculations
        checkInterval++;
        if (checkInterval < CHECK_EVERY_N_TICKS) {
            return false;
        }
        checkInterval = 0;

        nestPosition = dragon.getNestPosition();
        if (nestPosition == null) {
            return false;
        }

        // Don't interrupt if already landed
        if (dragon.isLanded()) {
            return false;
        }

        // Check if health is low
        double healthPercent = dragon.getHealth() / dragon.getMaxHealth();
        if (healthPercent < LOW_HEALTH_THRESHOLD) {
            if (!dragon.level().isClientSide) {
                System.out.println("[RETURN-TO-NEST] Dragon health low (" +
                    String.format("%.0f", healthPercent * 100) + "%), returning to nest");
            }
            return true;
        }

        // Check if too far from nest
        double distanceToNest = dragon.position().distanceTo(Vec3.atCenterOf(nestPosition));
        if (distanceToNest > MAX_DISTANCE_FROM_NEST) {
            if (!dragon.level().isClientSide) {
                System.out.println("[RETURN-TO-NEST] Dragon too far from nest (" +
                    String.format("%.1f", distanceToNest) + " blocks), returning");
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if we've reached the nest
        if (nestPosition == null) {
            return false;
        }

        double distanceToNest = dragon.position().distanceTo(Vec3.atCenterOf(nestPosition));

        // Stop if we're close enough to nest
        if (distanceToNest < NEST_ARRIVAL_DISTANCE) {
            if (!dragon.level().isClientSide) {
                System.out.println("[RETURN-TO-NEST] Arrived at nest (distance: " +
                    String.format("%.1f", distanceToNest) + " blocks)");
            }
            return false;
        }

        // Continue returning
        return true;
    }

    @Override
    public void start() {
        if (!dragon.level().isClientSide) {
            System.out.println("[RETURN-TO-NEST] Starting return to nest at " + nestPosition);
        }
        // Ensure dragon is in flying mode
        dragon.setNoGravity(true);
        dragon.setLanded(false);
    }

    @Override
    public void stop() {
        if (!dragon.level().isClientSide) {
            System.out.println("[RETURN-TO-NEST] Stopped returning to nest");
        }
    }

    @Override
    public void tick() {
        if (nestPosition == null) {
            return;
        }

        // Get current position and target
        Vec3 currentPos = dragon.position();
        Vec3 nestVec = Vec3.atCenterOf(nestPosition).add(0, 5, 0); // Aim 5 blocks above nest
        Vec3 directionToNest = nestVec.subtract(currentPos);
        double distanceToNest = directionToNest.length();

        // Log progress every 2 seconds
        if (dragon.tickCount % 40 == 0 && !dragon.level().isClientSide) {
            System.out.println("[RETURN-TO-NEST] Distance to nest: " +
                String.format("%.1f", distanceToNest) + " blocks");
        }

        // Normalize direction
        Vec3 normalizedDirection = directionToNest.normalize();

        // Calculate flight speed (faster when further away)
        double baseSpeed = 1.2D;
        double speed = Math.min(baseSpeed, distanceToNest * 0.1D);

        // Calculate desired velocity
        double desiredVelX = normalizedDirection.x * speed;
        double desiredVelY = normalizedDirection.y * speed * 0.5D; // Slower vertical movement
        double desiredVelZ = normalizedDirection.z * speed;

        // Get current velocity
        Vec3 currentVelocity = dragon.getDeltaMovement();

        // Smooth acceleration
        double smoothFactor = 0.2D;
        double newVelX = Mth.lerp(smoothFactor, currentVelocity.x, desiredVelX);
        double newVelY = Mth.lerp(smoothFactor, currentVelocity.y, desiredVelY);
        double newVelZ = Mth.lerp(smoothFactor, currentVelocity.z, desiredVelZ);

        // Apply velocity
        dragon.setDeltaMovement(newVelX, newVelY, newVelZ);

        // If very close, start landing sequence
        if (distanceToNest < NEST_ARRIVAL_DISTANCE * 2) {
            // Slow down for landing
            dragon.setDeltaMovement(
                dragon.getDeltaMovement().scale(0.9)
            );
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}

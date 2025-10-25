package de.davidvogt.hkbmod.item.entity.ai;

import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.item.entity.custom.DragonConstants;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that makes dragons occasionally rest by lying down and curling up, similar to polar foxes.
 * The dragon will rest for a configured duration when this goal activates.
 * Dragons fly relaxed to their nest before resting.
 */
public class DragonRestGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonRestGoal.class);

    private final DragonEntity dragon;
    private int restTimeLeft = 0;
    private int timeSinceLastRest = 0;
    private int checkTimer = 0;
    private BlockPos targetNestPos = null;
    private boolean movingToNest = false;

    public DragonRestGoal(DragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // Only active for wild dragons, not tamed ones
        if (dragon.isTamed()) {
            return false;
        }

        // Increment timer every tick
        timeSinceLastRest++;

        // Check every tick if landed timer expired, otherwise use intervals
        boolean landedTimerExpired = dragon.isLanded() && dragon.getLandedTimer() <= 0;

        if (!landedTimerExpired) {
            checkTimer++;
            if (checkTimer < DragonConstants.CHECK_INTERVAL_TICKS) {
                return false;
            }
            checkTimer = 0;
        } else {
            checkTimer = 0;
        }

        // Debug log every 10 seconds or when timer expired
        if ((timeSinceLastRest % 200 == 0 || landedTimerExpired) && !dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} rest check - time since last: {:.1f}s, landed: {}, timer: {}, has target: {}",
                    dragon.getId(), timeSinceLastRest / 20.0, dragon.isLanded(),
                    dragon.getLandedTimer(), dragon.getTarget() != null);
        }

        // Can't rest if already resting
        if (dragon.isResting()) {
            return false;
        }

        // Can't rest if defending nest or has a target
        if (dragon.getTarget() != null) {
            return false;
        }

        // Find a nearby DRAGON_NEST block to rest on
        BlockPos nestPos = findNearbyNest();
        if (nestPos == null) {
            if (landedTimerExpired && !dragon.level().isClientSide) {
                LOGGER.debug("Dragon {} - no nest found at {}, will take off instead",
                        dragon.getId(), dragon.blockPosition());
            }
            return false;
        }

        // After landing (timer expired), randomly decide: 50% rest, 50% take off
        if (landedTimerExpired) {
            // Random decision: 50% chance to rest
            if (dragon.getRandom().nextFloat() < 0.5F) {
                if (!dragon.level().isClientSide) {
                    LOGGER.info("Dragon {} decided to fly to nest and rest at {}",
                            dragon.getId(), nestPos);
                }
                targetNestPos = nestPos;
                return true;
            } else {
                if (!dragon.level().isClientSide) {
                    LOGGER.debug("Dragon {} skipped resting, will take off",
                            dragon.getId());
                }
                return false;
            }
        }

        return false;
    }

    /**
     * Finds a nearby DRAGON_NEST block within reasonable range
     */
    private BlockPos findNearbyNest() {
        // First priority: Use saved nest position
        if (dragon.hasNest()) {
            BlockPos savedNestPos = dragon.getNestPosition();
            if (savedNestPos != null) {
                BlockState state = dragon.level().getBlockState(savedNestPos);
                if (state.is(ModBlocks.DRAGON_NEST.get())) {
                    if (!dragon.level().isClientSide) {
                        double distance = Math.sqrt(dragon.blockPosition().distSqr(savedNestPos));
                        LOGGER.debug("Dragon {} using saved nest at {} (distance: {:.1f} blocks)",
                                dragon.getId(), savedNestPos, distance);
                    }
                    return savedNestPos;
                }
            }
        }

        // Second priority: Search in area around dragon
        BlockPos dragonPos = dragon.blockPosition();
        int searchRadius = DragonConstants.NEST_SEARCH_RADIUS;

        if (!dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} searching for nest in {}x{}x10 area",
                    dragon.getId(), searchRadius * 2, searchRadius * 2);
        }

        // Check in configured area around the dragon
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -5; y <= 5; y++) {
                    BlockPos checkPos = dragonPos.offset(x, y, z);
                    BlockState state = dragon.level().getBlockState(checkPos);
                    if (state.is(ModBlocks.DRAGON_NEST.get())) {
                        if (!dragon.level().isClientSide) {
                            double distance = Math.sqrt(x * x + y * y + z * z);
                            LOGGER.debug("Dragon {} found nest at {} (distance: {:.1f} blocks)",
                                    dragon.getId(), checkPos, distance);
                        }
                        return checkPos;
                    }
                }
            }
        }

        if (!dragon.level().isClientSide) {
            LOGGER.warn("Dragon {} found no nest in search area", dragon.getId());
        }

        return null;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if we have a target (being attacked or defending)
        if (dragon.getTarget() != null) {
            if (!dragon.level().isClientSide) {
                LOGGER.info("Dragon {} rest interrupted by target", dragon.getId());
            }
            return false;
        }

        // If moving to nest, continue until we're there or resting started
        if (movingToNest && !dragon.isResting()) {
            return true;
        }

        // Continue resting until time is up
        return restTimeLeft > 0;
    }

    @Override
    public void start() {
        if (targetNestPos == null) {
            return;
        }

        // Dragon will fly relaxed to nest
        movingToNest = true;

        // Ensure dragon is in flying mode for smooth flight
        dragon.setLanded(false);
        dragon.setLandingMode(false);
        dragon.setNoGravity(true);

        if (!dragon.level().isClientSide) {
            double distanceToNest = dragon.position().distanceTo(
                    new Vec3(
                            targetNestPos.getX() + 0.5,
                            targetNestPos.getY() + 1.0,
                            targetNestPos.getZ() + 0.5
                    )
            );
            LOGGER.debug("Dragon {} flying to nest at {} (distance: {:.2f} blocks)",
                    dragon.getId(), targetNestPos, distanceToNest);
        }
    }

    private void startResting() {
        // Only set resting to true when dragon is actually on the nest
        dragon.setResting(true);
        dragon.setLanded(true);
        restTimeLeft = DragonConstants.REST_DURATION_TICKS;
        dragon.getNavigation().stop();
        movingToNest = false;

        if (!dragon.level().isClientSide) {
            LOGGER.info("Dragon {} started resting on nest for {:.1f} seconds",
                    dragon.getId(), DragonConstants.REST_DURATION_TICKS / 20.0);
        }
    }

    @Override
    public void stop() {
        dragon.setResting(false);
        restTimeLeft = 0;
        timeSinceLastRest = 0;
        targetNestPos = null;
        movingToNest = false;

        if (!dragon.level().isClientSide) {
            LOGGER.info("Dragon {} finished resting, preparing to take off", dragon.getId());
        }

        // After resting, dragon should fly again
        dragon.setLanded(false);
        dragon.setFlyingTimer(0);
        dragon.setNoGravity(true);
    }

    @Override
    public void tick() {
        if (movingToNest && targetNestPos != null) {
            // Fly relaxed towards the nest (similar to ReturnToNestGoal but slower and more relaxed)
            Vec3 currentPos = dragon.position();
            Vec3 nestVec = Vec3.atCenterOf(targetNestPos).add(0, 1.0, 0); // Aim slightly above nest
            Vec3 directionToNest = nestVec.subtract(currentPos);
            double distanceToNest = directionToNest.length();

            // Log progress every 2 seconds
            if (dragon.tickCount % 40 == 0 && !dragon.level().isClientSide) {
                LOGGER.debug("Dragon {} flying to nest, distance: {:.1f} blocks",
                        dragon.getId(), distanceToNest);
            }

            if (distanceToNest <= DragonConstants.NEST_POSITION_THRESHOLD) {
                // Reached the nest, position dragon directly on it
                Vec3 targetVec = new Vec3(
                        targetNestPos.getX() + 0.5,
                        targetNestPos.getY() + 0.5,
                        targetNestPos.getZ() + 0.5
                );
                dragon.setPos(targetVec.x, targetVec.y, targetVec.z);
                dragon.setDeltaMovement(Vec3.ZERO);

                if (!dragon.level().isClientSide) {
                    LOGGER.debug("Dragon {} reached nest, starting rest", dragon.getId());
                }

                startResting();
            } else {
                // Continue flying towards nest with relaxed, smooth movement
                Vec3 normalizedDirection = directionToNest.normalize();

                // Relaxed flight speed (slower than normal flying)
                double speed = DragonConstants.RELAXED_FLIGHT_SPEED;

                // Calculate desired velocity
                double desiredVelX = normalizedDirection.x * speed;
                double desiredVelY = normalizedDirection.y * speed * 0.4D; // Even slower vertical movement for relaxed descent
                double desiredVelZ = normalizedDirection.z * speed;

                // Get current velocity
                Vec3 currentVelocity = dragon.getDeltaMovement();

                // Very smooth acceleration for relaxed flight
                double smoothFactor = 0.15D; // Lower value = smoother, more gradual changes

                // Interpolate towards desired velocity
                double newVelX = currentVelocity.x + (desiredVelX - currentVelocity.x) * smoothFactor;
                double newVelY = currentVelocity.y + (desiredVelY - currentVelocity.y) * smoothFactor;
                double newVelZ = currentVelocity.z + (desiredVelZ - currentVelocity.z) * smoothFactor;

                // Apply new velocity
                dragon.setDeltaMovement(newVelX, newVelY, newVelZ);
            }
        } else if (dragon.isResting()) {
            restTimeLeft--;

            // Stop all movement while resting and keep position on nest
            dragon.getNavigation().stop();
            dragon.setDeltaMovement(Vec3.ZERO);

            // Keep dragon positioned on nest
            if (targetNestPos != null) {
                Vec3 nestCenter = new Vec3(
                        targetNestPos.getX() + 0.5,
                        targetNestPos.getY() + 0.5,
                        targetNestPos.getZ() + 0.5
                );

                // Gently pull dragon towards nest center if it drifts
                if (dragon.position().distanceTo(nestCenter) > 0.5) {
                    dragon.setPos(nestCenter.x, nestCenter.y, nestCenter.z);
                }
            }

            // Debug output every 2 seconds
            if (!dragon.level().isClientSide && restTimeLeft % 40 == 0) {
                LOGGER.debug("Dragon {} resting... {:.1f} seconds remaining",
                        dragon.getId(), restTimeLeft / 20.0);
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}

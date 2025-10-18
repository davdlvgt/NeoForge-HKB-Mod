package de.davidvogt.hkbmod.item.entity.ai;

import de.davidvogt.hkbmod.item.entity.custom.DragonConstants;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Custom flying goal for the dragon that ensures smooth, continuous flight
 * with proper turning and height management. Also handles landing behavior.
 */
public class DragonFlyingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonFlyingGoal.class);

    private final DragonEntity dragon;
    private double targetX;
    private double targetY;
    private double targetZ;
    private int flyingTimer = 0;
    private int pauseTimer = 0;
    private boolean isPaused = false;

    // Landing system - NOW WITH RANDOM DURATIONS
    private int currentFlyingDuration = 0; // Will be set randomly
    private int currentLandingDuration = 0; // Will be set randomly
    private BlockPos landingSpot = null;
    private boolean isLandingMode = false;

    // Collision detection - tracking for stuck detection
    private Vec3 lastPosition = Vec3.ZERO;
    private int stuckCounter = 0;

    // Emergency evasive maneuver
    private boolean isPerformingEmergencyManeuver = false;
    private int emergencyManeuverTimer = 0;
    private Vec3 emergencyDirection = Vec3.ZERO;

    public DragonFlyingGoal(DragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only active for wild dragons, not tamed ones
        if (dragon.isTamed()) {
            return false;
        }
        return true; // Always active for wild dragons
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if dragon gets tamed
        if (dragon.isTamed()) {
            return false;
        }
        return true; // Always active for wild dragons
    }

    @Override
    public void start() {
        pickNewTarget();
        lastPosition = dragon.position(); // Save start position for collision detection
        // Set random flying duration on start
        setRandomFlyingDuration();
    }

    @Override
    public void tick() {
        // Check if dragon is currently landed
        if (dragon.isLanded()) {
            handleLandedBehavior();
            return;
        }

        // Check if dragon is in landing mode
        if (isLandingMode) {
            handleLandingApproach();
            return;
        }

        // Normal flying behavior
        flyingTimer++;
        dragon.setFlyingTimer(dragon.getFlyingTimer() + 1);

        // Log flying progress every 10 seconds
        if (dragon.getFlyingTimer() % 200 == 0 && !dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} flying for {}s / {}s until considering landing",
                dragon.getId(), dragon.getFlyingTimer() / 20, currentFlyingDuration / 20);
        }

        // Random chance to land early (1% chance per second after minimum flight time)
        int minFlightTime = DragonConstants.MIN_FLYING_DURATION_TICKS / 2; // Can land after half minimum time
        if (dragon.getFlyingTimer() > minFlightTime && dragon.getFlyingTimer() % 20 == 0) {
            float landingChance = 0.01F; // 1% chance per second
            if (dragon.getRandom().nextFloat() < landingChance) {
                if (!dragon.level().isClientSide) {
                    LOGGER.info("Dragon {} random decision to land after {}s flying",
                        dragon.getId(), dragon.getFlyingTimer() / 20);
                }
                // Start landing sequence
                initiateRandomLanding();
                return;
            }
        }

        // Check if scheduled landing time reached
        if (dragon.getFlyingTimer() >= currentFlyingDuration) {
            double currentY = dragon.getY();

            // Start landing approach - first climb to minimum altitude
            if (currentY >= DragonConstants.MIN_FLIGHT_ALTITUDE) {
                if (!dragon.level().isClientSide) {
                    LOGGER.info("Dragon {} scheduled landing at altitude {} (flew for {}s)",
                        dragon.getId(), String.format("%.1f", currentY), dragon.getFlyingTimer() / 20);
                }
                startLandingSequence();
                return;
            } else {
                // Not high enough yet, continue climbing to minimum altitude
                if (!dragon.level().isClientSide && dragon.getFlyingTimer() % 40 == 0) {
                    LOGGER.debug("Dragon {} ready to land but climbing to {} blocks first (current: {})",
                        dragon.getId(), DragonConstants.MIN_FLIGHT_ALTITUDE, String.format("%.1f", currentY));
                }
                // Set target to minimum flight altitude
                targetY = DragonConstants.MIN_FLIGHT_ALTITUDE;
            }
        }

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
        double speed = DragonConstants.NORMAL_FLIGHT_SPEED; // Fast flying speed

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

        // Collision detection - check if dragon gets stuck
        checkForStuck();
    }

    /**
     * Initiates the landing sequence by finding a suitable landing spot
     * Searches for a spot 30-60 blocks away horizontally from current position
     */
    private void startLandingSequence() {
        isLandingMode = true;
        dragon.setLandingMode(true); // Notify the entity
        landingSpot = findLandingSpotAhead();

        if (landingSpot == null) {
            // No suitable landing spot found, pick a ground location nearby
            landingSpot = new BlockPos(
                (int) dragon.getX(),
                (int) dragon.level().getHeight() - 1,
                (int) dragon.getZ()
            );
            if (!dragon.level().isClientSide) {
                LOGGER.warn("Dragon {} no solid block found, using fallback: {}", dragon.getId(), landingSpot);
            }
        } else {
            if (!dragon.level().isClientSide) {
                double horizontalDistance = Math.sqrt(
                    Math.pow(landingSpot.getX() - dragon.getX(), 2) +
                    Math.pow(landingSpot.getZ() - dragon.getZ(), 2)
                );
                LOGGER.debug("Dragon {} landing spot found at {} (horizontal: {} blocks, vertical: {} blocks)",
                    dragon.getId(), landingSpot, String.format("%.2f", horizontalDistance),
                    String.format("%.2f", landingSpot.getY() - dragon.getY()));
            }
        }
    }

    /**
     * Finds a landing spot ahead of the dragon's current position
     * Searches in a cone in front of the dragon's facing direction
     */
    private BlockPos findLandingSpotAhead() {
        Vec3 dragonPos = dragon.position();

        // Pick a random distance within configured range
        double targetDistance = DragonConstants.LANDING_SPOT_MIN_DISTANCE +
            dragon.getRandom().nextDouble() * (DragonConstants.LANDING_SPOT_MAX_DISTANCE - DragonConstants.LANDING_SPOT_MIN_DISTANCE);

        // Get dragon's current yaw (facing direction)
        float yaw = dragon.getYRot();

        // Add some randomness to the angle (±30 degrees)
        double angleVariation = (dragon.getRandom().nextDouble() - 0.5) * 60.0; // -30 to +30 degrees
        double targetAngle = Math.toRadians(yaw + angleVariation);

        // Calculate target position ahead of dragon
        int targetX = (int)(dragonPos.x + Math.sin(targetAngle) * targetDistance);
        int targetZ = (int)(dragonPos.z + Math.cos(targetAngle) * targetDistance);

        if (!dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} searching for landing spot at X={}, Z={} (angle={}°, distance={} blocks)",
                dragon.getId(), targetX, targetZ, String.format("%.1f", Math.toDegrees(targetAngle)),
                String.format("%.1f", targetDistance));
        }

        // Search downward from dragon's current height for a solid block
        int startY = (int)dragonPos.y;
        for (int y = startY; y > dragon.level().getMinY(); y--) {
            BlockPos checkPos = new BlockPos(targetX, y, targetZ);
            BlockState state = dragon.level().getBlockState(checkPos);

            if (!state.isAir() && state.isSolid()) {
                // Found a solid block, return position one block above it
                BlockPos landingPos = checkPos.above();
                if (!dragon.level().isClientSide) {
                    LOGGER.debug("Dragon {} found solid ground at Y={}, landing at Y={}",
                        dragon.getId(), y, landingPos.getY());
                }
                return landingPos;
            }
        }

        if (!dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} no solid ground found in search column", dragon.getId());
        }
        return null;
    }

    /**
     * Handles the dragon's approach to the landing spot
     * Dragon maintains flight (noGravity=true) and glides down at ~45 degree angle
     */
    private void handleLandingApproach() {
        if (landingSpot == null) {
            isLandingMode = false;
            dragon.setLandingMode(false);
            if (!dragon.level().isClientSide) {
                LOGGER.warn("Dragon {} landing cancelled - no landing spot", dragon.getId());
            }
            return;
        }

        // Check if dragon has touched the ground during approach
        if (dragon.onGround()) {
            if (!dragon.level().isClientSide) {
                LOGGER.info("Dragon {} early ground contact - landing immediately", dragon.getId());
            }
            completeLanding();
            return;
        }

        // Check if there are obstacles in the flight path ahead
        if (checkForObstacleAhead()) {
            if (!dragon.level().isClientSide) {
                LOGGER.info("Dragon {} obstacle detected in flight path - landing at current position", dragon.getId());
            }
            // Find ground below current position and land there
            landingSpot = findGroundBelow();
            if (landingSpot != null && Math.abs(dragon.getY() - landingSpot.getY()) < 5.0D) {
                // Close to ground, land immediately
                completeLanding();
                return;
            }
            // Otherwise continue with adjusted landing spot
        }

        // Ensure dragon still flies during approach (no gravity)
        dragon.setNoGravity(true);

        Vec3 currentPos = dragon.position();
        Vec3 targetPos = Vec3.atBottomCenterOf(landingSpot);

        // Calculate horizontal distance (XZ plane)
        double horizontalDistance = Math.sqrt(
            Math.pow(targetPos.x - currentPos.x, 2) +
            Math.pow(targetPos.z - currentPos.z, 2)
        );
        double verticalDistance = targetPos.y - currentPos.y;

        // Log landing progress every 10 ticks (0.5 seconds)
        if (dragon.tickCount % 10 == 0 && !dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} landing approach - horizontal: {} blocks, vertical: {} blocks",
                dragon.getId(), String.format("%.2f", horizontalDistance), String.format("%.2f", verticalDistance));
        }

        // Check if close enough to land (within configured arrival distance)
        if (horizontalDistance < DragonConstants.LANDING_ARRIVAL_DISTANCE && Math.abs(verticalDistance) < 1.5D) {
            if (!dragon.level().isClientSide) {
                LOGGER.info("Dragon {} landing complete", dragon.getId());
            }
            completeLanding();
            return;
        }

        // Calculate glide angle approach
        // Horizontal movement at faster speed to match flying speed
        double horizontalSpeed = 0.8D;

        // Calculate desired horizontal direction
        Vec3 horizontalDirection = new Vec3(
            targetPos.x - currentPos.x,
            0,
            targetPos.z - currentPos.z
        ).normalize();

        // Vertical speed based on horizontal distance to maintain ~45 degree glide angle
        // If we're high above target, descend faster. If close to target height, slow down
        double desiredVerticalSpeed;
        if (verticalDistance < 0) {
            // We're above the target - calculate descent rate for smooth glide
            // At ~45 degrees: vertical speed ≈ horizontal speed
            desiredVerticalSpeed = -horizontalSpeed * 0.8; // Slightly less than 45 degrees for smooth landing

            // Slow down vertical descent when getting close to target
            if (Math.abs(verticalDistance) < 10.0D) {
                desiredVerticalSpeed *= Math.abs(verticalDistance) / 10.0D;
            }
        } else {
            // We're below target - ascend slightly
            desiredVerticalSpeed = 0.2D;
        }

        // Slow down as we get very close
        if (horizontalDistance < 5.0D) {
            horizontalSpeed *= horizontalDistance / 5.0D;
        }

        Vec3 currentVelocity = dragon.getDeltaMovement();

        // Smooth interpolation for natural gliding movement
        double newVelX = Mth.lerp(0.15D, currentVelocity.x, horizontalDirection.x * horizontalSpeed);
        double newVelY = Mth.lerp(0.15D, currentVelocity.y, desiredVerticalSpeed);
        double newVelZ = Mth.lerp(0.15D, currentVelocity.z, horizontalDirection.z * horizontalSpeed);

        dragon.setDeltaMovement(newVelX, newVelY, newVelZ);
    }

    /**
     * Completes the landing sequence and transitions to landed state
     */
    private void completeLanding() {
        dragon.setLanded(true);
        dragon.setLandingMode(false); // No longer in landing mode

        // Set random landing duration
        setRandomLandingDuration();
        dragon.setLandedTimer(currentLandingDuration);

        dragon.setDeltaMovement(0, 0, 0);
        dragon.setNoGravity(false); // Enable gravity while landed
        isLandingMode = false;
        landingSpot = null;
    }

    /**
     * Handles behavior while the dragon is landed
     */
    private void handleLandedBehavior() {
        // Countdown landing timer
        int timer = dragon.getLandedTimer() - 1;
        dragon.setLandedTimer(timer);

        // Log remaining time every 10 seconds
        if (timer % 200 == 0 && !dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} landed - remaining time: {}s / {}s, walking around",
                dragon.getId(), timer / 20, currentLandingDuration / 20);
        }

        // Check if it's time to take off again (after walking duration)
        if (timer <= 0) {
            if (!dragon.level().isClientSide) {
                LOGGER.info("Dragon {} walking time finished - deciding next action", dragon.getId());
            }
            // Don't take off directly - let DragonRestGoal take over
            // Just stop movement and wait for rest goal to activate
            dragon.setDeltaMovement(0, dragon.getDeltaMovement().y, 0);
            return;
        }

        // Walking behavior - pick a new walking target every 2-3 seconds
        if (landingSpot == null || timer % (40 + dragon.getRandom().nextInt(20)) == 0) {
            pickGroundWalkingTarget();
        }

        // Move towards walking target if we have one
        if (landingSpot != null) {
            Vec3 currentPos = dragon.position();
            Vec3 targetPos = Vec3.atBottomCenterOf(landingSpot);
            Vec3 direction = new Vec3(targetPos.x - currentPos.x, 0, targetPos.z - currentPos.z);
            double distance = direction.length();

            if (distance > 0.5D) {
                // Check for obstacles in front of the dragon
                boolean obstacleDetected = checkForObstacle(direction);

                if (obstacleDetected && dragon.onGround()) {
                    // Jump over the obstacle
                    dragon.setDeltaMovement(
                        dragon.getDeltaMovement().x,
                        0.5D, // Jump strength
                        dragon.getDeltaMovement().z
                    );
                    if (!dragon.level().isClientSide && dragon.tickCount % 20 == 0) {
                        LOGGER.debug("Dragon {} jumping over obstacle", dragon.getId());
                    }
                } else {
                    // Normalize and walk
                    Vec3 normalizedDirection = direction.normalize();
                    double walkSpeed = 0.15D; // Slow walking speed

                    dragon.setDeltaMovement(
                        normalizedDirection.x * walkSpeed,
                        dragon.getDeltaMovement().y,
                        normalizedDirection.z * walkSpeed
                    );
                }
            } else {
                // Close enough to target, stop
                dragon.setDeltaMovement(0, dragon.getDeltaMovement().y, 0);
            }
        }
    }

    /**
     * Checks if there are obstacles ahead during landing approach
     * Scans 5-10 blocks ahead in flight direction
     */
    private boolean checkForObstacleAhead() {
        Vec3 dragonPos = dragon.position();
        Vec3 velocity = dragon.getDeltaMovement();

        // Get normalized movement direction
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed < 0.01) {
            return false; // Not moving, no obstacle
        }

        Vec3 direction = new Vec3(velocity.x / horizontalSpeed, velocity.y, velocity.z / horizontalSpeed);

        // Check 5-10 blocks ahead
        for (int distance = 5; distance <= 10; distance++) {
            BlockPos checkPos = new BlockPos(
                (int)(dragonPos.x + direction.x * distance),
                (int)(dragonPos.y + direction.y * distance),
                (int)(dragonPos.z + direction.z * distance)
            );

            // Check the position and surrounding blocks
            for (int yOffset = -1; yOffset <= 2; yOffset++) {
                BlockPos offsetPos = checkPos.offset(0, yOffset, 0);
                BlockState state = dragon.level().getBlockState(offsetPos);

                if (!state.isAir() && state.isSolid()) {
                    return true; // Found obstacle
                }
            }
        }

        return false;
    }

    /**
     * Finds solid ground directly below the dragon's current position
     */
    private BlockPos findGroundBelow() {
        Vec3 dragonPos = dragon.position();
        int startY = (int)dragonPos.y;

        for (int y = startY; y > dragon.level().getMinY(); y--) {
            BlockPos checkPos = new BlockPos((int)dragonPos.x, y, (int)dragonPos.z);
            BlockState state = dragon.level().getBlockState(checkPos);

            if (!state.isAir() && state.isSolid()) {
                return checkPos.above();
            }
        }

        return null;
    }

    /**
     * Checks if there's an obstacle in front of the dragon (used for walking)
     */
    private boolean checkForObstacle(Vec3 direction) {
        Vec3 normalizedDir = direction.normalize();
        Vec3 dragonPos = dragon.position();

        // Check 1-2 blocks ahead at dragon's eye level
        for (int i = 1; i <= 2; i++) {
            BlockPos checkPos = new BlockPos(
                (int)(dragonPos.x + normalizedDir.x * i),
                (int)(dragonPos.y),
                (int)(dragonPos.z + normalizedDir.z * i)
            );

            // Check both at dragon's feet level and one block up
            BlockState blockAtFeet = dragon.level().getBlockState(checkPos);
            BlockState blockAbove = dragon.level().getBlockState(checkPos.above());

            // If there's a solid block at feet or head level, it's an obstacle
            if ((blockAtFeet.isSolid() && !blockAtFeet.isAir()) ||
                (blockAbove.isSolid() && !blockAbove.isAir())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Picks a random nearby ground position for walking
     */
    private void pickGroundWalkingTarget() {
        double currentX = dragon.getX();
        double currentZ = dragon.getZ();
        double currentY = dragon.getY();

        // Pick a random point 5-15 blocks away
        double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = 5.0D + dragon.getRandom().nextDouble() * 10.0D;

        int targetX = (int)(currentX + Math.cos(angle) * distance);
        int targetZ = (int)(currentZ + Math.sin(angle) * distance);

        // Try to find ground level at target position
        for (int y = (int)currentY + 5; y > dragon.level().getMinY(); y--) {
            BlockPos checkPos = new BlockPos(targetX, y, targetZ);
            BlockState state = dragon.level().getBlockState(checkPos);

            if (!state.isAir() && state.isSolid()) {
                landingSpot = checkPos.above();
                if (!dragon.level().isClientSide) {
                    LOGGER.debug("Dragon {} new walking target: {}", dragon.getId(), landingSpot);
                }
                return;
            }
        }

        // If no ground found, stay at current position
        landingSpot = dragon.blockPosition();
    }

    /**
     * Handles taking off after being landed
     */
    private void takeOff() {
        dragon.setLanded(false);
        dragon.setFlyingTimer(0);
        dragon.setNoGravity(true);

        // Set new random flying duration
        setRandomFlyingDuration();

        // Pick a target high in the sky for initial climb
        double currentX = dragon.getX();
        double currentZ = dragon.getZ();
        double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = 40.0D + dragon.getRandom().nextDouble() * 30.0D;

        targetX = currentX + Math.cos(angle) * distance;
        targetZ = currentZ + Math.sin(angle) * distance;
        targetY = DragonConstants.TARGET_FLIGHT_ALTITUDE_MIN +
            dragon.getRandom().nextDouble() * (DragonConstants.TARGET_FLIGHT_ALTITUDE_MAX - DragonConstants.TARGET_FLIGHT_ALTITUDE_MIN);

        flyingTimer = 0;
        if (!dragon.level().isClientSide) {
            LOGGER.info("Dragon {} taking off - climbing to altitude {} blocks",
                dragon.getId(), String.format("%.1f", targetY));
        }
    }

    private void pickNewTarget() {
        double currentX = dragon.getX();
        double currentZ = dragon.getZ();

        // If dragon has a nest, bias targets towards nest area
        if (dragon.hasNest()) {
            BlockPos nestPos = dragon.getNestPosition();
            if (nestPos != null) {
                double distanceToNest = Math.sqrt(
                    Math.pow(nestPos.getX() - currentX, 2) +
                    Math.pow(nestPos.getZ() - currentZ, 2)
                );

                // If far from nest (>100 blocks), pick targets closer to nest
                if (distanceToNest > 100.0D) {
                    // Pick a point between current position and nest
                    double towardsNestX = nestPos.getX() - currentX;
                    double towardsNestZ = nestPos.getZ() - currentZ;
                    double angle = Math.atan2(towardsNestZ, towardsNestX);

                    // Add some randomness (±45 degrees)
                    angle += (dragon.getRandom().nextDouble() - 0.5) * Math.PI / 2.0;

                    double distance = 30.0D + dragon.getRandom().nextDouble() * 40.0D;
                    targetX = currentX + Math.cos(angle) * distance;
                    targetZ = currentZ + Math.sin(angle) * distance;

                    if (!dragon.level().isClientSide) {
                        LOGGER.debug("Dragon {} far from nest, picking target towards nest", dragon.getId());
                    }
                } else {
                    // Normal random target, but keep it within nest area
                    double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0;
                    double distance = 30.0D + dragon.getRandom().nextDouble() * 40.0D;

                    targetX = currentX + Math.cos(angle) * distance;
                    targetZ = currentZ + Math.sin(angle) * distance;

                    // Clamp to nest radius (max 120 blocks from nest)
                    double newDistToNest = Math.sqrt(
                        Math.pow(nestPos.getX() - targetX, 2) +
                        Math.pow(nestPos.getZ() - targetZ, 2)
                    );

                    if (newDistToNest > 120.0D) {
                        // Pull target back towards nest
                        double scale = 120.0D / newDistToNest;
                        targetX = nestPos.getX() + (targetX - nestPos.getX()) * scale;
                        targetZ = nestPos.getZ() + (targetZ - nestPos.getZ()) * scale;
                    }
                }

                // Set target altitude within configured range
                targetY = DragonConstants.TARGET_FLIGHT_ALTITUDE_MIN +
                    dragon.getRandom().nextDouble() * (DragonConstants.TARGET_FLIGHT_ALTITUDE_MAX - DragonConstants.TARGET_FLIGHT_ALTITUDE_MIN);
                return;
            }
        }

        // No nest or nest-less behavior: completely random
        double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = 30.0D + dragon.getRandom().nextDouble() * 40.0D;

        targetX = currentX + Math.cos(angle) * distance;
        targetZ = currentZ + Math.sin(angle) * distance;
        // Set target altitude within configured range
        targetY = DragonConstants.TARGET_FLIGHT_ALTITUDE_MIN +
            dragon.getRandom().nextDouble() * (DragonConstants.TARGET_FLIGHT_ALTITUDE_MAX - DragonConstants.TARGET_FLIGHT_ALTITUDE_MIN);
    }

    private void maintainAltitude() {
        // Ensure dragon stays within configured altitude range
        // This ensures the dragon always flies above trees
        double currentY = dragon.getY();

        if (currentY < DragonConstants.MIN_FLIGHT_ALTITUDE) {
            // Emergency: too low, force upward faster
            dragon.setDeltaMovement(dragon.getDeltaMovement().x, 0.3D, dragon.getDeltaMovement().z);
        } else if (currentY > DragonConstants.MAX_FLIGHT_ALTITUDE) {
            // Emergency: too high, force downward faster
            dragon.setDeltaMovement(dragon.getDeltaMovement().x, -0.3D, dragon.getDeltaMovement().z);
        }
    }

    private double calculateVerticalVelocity() {
        double currentY = dragon.getY();
        double heightDiff = targetY - currentY;

        // Faster vertical movement - increased from 0.02 to 0.04, and doubled max speeds
        return Mth.clamp(heightDiff * 0.04D, -0.16D, 0.16D);
    }

    /**
     * Checks if dragon gets stuck and initiates evasive maneuver if needed
     */
    private void checkForStuck() {
        Vec3 currentPosition = dragon.position();
        double movementDistance = currentPosition.distanceTo(lastPosition);

        // Check if dragon has moved sufficiently
        if (movementDistance < DragonConstants.MIN_MOVEMENT_PER_TICK) {
            stuckCounter++;

            // Debug output every half second while stuck
            if (stuckCounter % 10 == 0 && !dragon.level().isClientSide) {
                LOGGER.warn("Dragon {} seems stuck! Counter: {}/{} (movement: {})",
                    dragon.getId(), stuckCounter, DragonConstants.STUCK_THRESHOLD_TICKS,
                    String.format("%.4f", movementDistance));
            }

            // If dragon stuck too long, initiate emergency maneuver
            if (stuckCounter >= DragonConstants.STUCK_THRESHOLD_TICKS) {
                initiateEmergencyManeuver();
                stuckCounter = 0; // Reset counter
            }
        } else {
            // Dragon moving normally, reset counter
            if (stuckCounter > 0) {
                stuckCounter = 0;
            }
        }

        lastPosition = currentPosition;
    }

    /**
     * Initiates emergency evasive maneuver when dragon gets stuck
     */
    private void initiateEmergencyManeuver() {
        if (!dragon.level().isClientSide) {
            LOGGER.warn("Dragon {} emergency maneuver initiated - dragon is stuck!", dragon.getId());
        }

        isPerformingEmergencyManeuver = true;
        emergencyManeuverTimer = DragonConstants.EMERGENCY_MANEUVER_DURATION_TICKS;

        // Strategy: First try direction change, then fly upward
        // Choose random new direction (180° rotated + random)
        float currentYaw = dragon.getYRot();
        float newYaw = currentYaw + 150.0F + dragon.getRandom().nextFloat() * 60.0F; // 150-210° rotation

        double angle = Math.toRadians(newYaw);
        double distance = 20.0D;

        // New direction with slight climb
        emergencyDirection = new Vec3(
            Math.sin(angle) * distance,
            5.0D, // Slightly upward
            Math.cos(angle) * distance
        ).normalize();

        if (!dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} emergency direction: X={}, Y={}, Z={}",
                dragon.getId(), String.format("%.2f", emergencyDirection.x),
                String.format("%.2f", emergencyDirection.y), String.format("%.2f", emergencyDirection.z));
        }
    }

    /**
     * Performs emergency evasive maneuver when dragon gets stuck
     */
    private void performEmergencyManeuver() {
        emergencyManeuverTimer--;

        if (emergencyManeuverTimer <= 0) {
            // Maneuver finished
            isPerformingEmergencyManeuver = false;
            pickNewTarget(); // Pick new target
            if (!dragon.level().isClientSide) {
                LOGGER.info("Dragon {} emergency maneuver completed, resuming normal flight", dragon.getId());
            }
            return;
        }

        // Check if still blocked
        if (checkForObstacleAhead() && emergencyManeuverTimer > 20) {
            // Still blocked after 1 second - Plan B: Fly upward!
            if (!dragon.level().isClientSide) {
                LOGGER.warn("Dragon {} still blocked - ascending as last resort!", dragon.getId());
            }

            // Strong upward flight
            Vec3 currentVel = dragon.getDeltaMovement();
            dragon.setDeltaMovement(currentVel.x * 0.5, 0.5D, currentVel.z * 0.5); // Strong climb
            emergencyManeuverTimer = 20; // Continue for 1 more second
            return;
        }

        // Evasive maneuver: Fly in new direction
        double speed = 0.8D;
        Vec3 currentVel = dragon.getDeltaMovement();

        // Smooth transition to emergency direction
        double newVelX = Mth.lerp(0.2D, currentVel.x, emergencyDirection.x * speed);
        double newVelY = Mth.lerp(0.2D, currentVel.y, emergencyDirection.y * speed);
        double newVelZ = Mth.lerp(0.2D, currentVel.z, emergencyDirection.z * speed);

        dragon.setDeltaMovement(newVelX, newVelY, newVelZ);
    }

    /**
     * Sets a random flying duration for variety
     */
    private void setRandomFlyingDuration() {
        currentFlyingDuration = DragonConstants.MIN_FLYING_DURATION_TICKS +
            dragon.getRandom().nextInt(DragonConstants.MAX_FLYING_DURATION_TICKS - DragonConstants.MIN_FLYING_DURATION_TICKS + 1);
        if (!dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} new random flying duration: {} seconds ({}min {}sec)",
                dragon.getId(), currentFlyingDuration / 20, currentFlyingDuration / 20 / 60,
                (currentFlyingDuration / 20) % 60);
        }
    }

    /**
     * Sets a random landing duration for variety
     */
    private void setRandomLandingDuration() {
        currentLandingDuration = DragonConstants.MIN_LANDING_DURATION_TICKS +
            dragon.getRandom().nextInt(DragonConstants.MAX_LANDING_DURATION_TICKS - DragonConstants.MIN_LANDING_DURATION_TICKS + 1);
        if (!dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} new random landing duration: {} seconds ({}min {}sec)",
                dragon.getId(), currentLandingDuration / 20, currentLandingDuration / 20 / 60,
                (currentLandingDuration / 20) % 60);
        }
    }

    /**
     * Initiates landing at current position (random decision)
     */
    private void initiateRandomLanding() {
        double currentY = dragon.getY();

        // If already high enough, start landing immediately
        if (currentY >= DragonConstants.MIN_FLIGHT_ALTITUDE) {
            startLandingSequence();
        } else {
            // Climb to minimum altitude first
            targetY = DragonConstants.MIN_FLIGHT_ALTITUDE;
            // Will trigger landing on next tick when high enough
            dragon.setFlyingTimer(currentFlyingDuration);
        }
    }
}

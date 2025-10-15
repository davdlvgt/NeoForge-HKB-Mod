package de.davidvogt.hkbmod.item.entity.ai;

import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Custom flying goal for the dragon that ensures smooth, continuous flight
 * with proper turning and height management. Also handles landing behavior.
 */
public class DragonFlyingGoal extends Goal {
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
    private static final int MIN_FLYING_DURATION = 200; // 30 seconds minimum
    private static final int MAX_FLYING_DURATION = 1000; // 120 seconds maximum
    private static final int MIN_LANDING_DURATION = 100; // 20 seconds minimum
    private static final int MAX_LANDING_DURATION = 800; // 90 seconds maximum
    private BlockPos landingSpot = null;
    private boolean isLandingMode = false;

    // Kollisionserkennung - Tracking für Hängenbleiben
    private Vec3 lastPosition = Vec3.ZERO;
    private int stuckCounter = 0;
    private static final int STUCK_THRESHOLD = 40; // 2 Sekunden ohne Bewegung = steckengeblieben
    private static final double MIN_MOVEMENT = 0.05; // Minimale Bewegung pro Tick

    // Notfall-Ausweichmanöver
    private boolean isPerformingEmergencyManeuver = false;
    private int emergencyManeuverTimer = 0;
    private Vec3 emergencyDirection = Vec3.ZERO;

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
        lastPosition = dragon.position(); // Startposition für Kollisionserkennung speichern
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
            System.out.println("[FLYING-GOAL] Flying for " + (dragon.getFlyingTimer() / 20) + "s / " +
                (currentFlyingDuration / 20) + "s until considering landing");
        }

        // Random chance to land early (1% chance per second after minimum flight time)
        int minFlightTime = MIN_FLYING_DURATION / 2; // Can land after half minimum time
        if (dragon.getFlyingTimer() > minFlightTime && dragon.getFlyingTimer() % 20 == 0) {
            float landingChance = 0.01F; // 1% chance per second
            if (dragon.getRandom().nextFloat() < landingChance) {
                if (!dragon.level().isClientSide) {
                    System.out.println("[FLYING-GOAL] *** RANDOM DECISION TO LAND *** (after " +
                        (dragon.getFlyingTimer() / 20) + "s flying)");
                }
                // Start landing sequence
                initiateRandomLanding();
                return;
            }
        }

        // Check if scheduled landing time reached
        if (dragon.getFlyingTimer() >= currentFlyingDuration) {
            double currentY = dragon.getY();

            // Landeanflug beginnen - zuerst auf 180 Blöcke hochfliegen
            if (currentY >= 180.0D) {
                if (!dragon.level().isClientSide) {
                    System.out.println("[FLYING-GOAL] *** SCHEDULED LANDING at 180+ altitude *** (flew for " +
                        (dragon.getFlyingTimer() / 20) + "s, altitude: " + String.format("%.1f", currentY) + " blocks)");
                }
                startLandingSequence();
                return;
            } else {
                // Noch nicht hoch genug, weiter steigen auf 180 Blöcke
                if (!dragon.level().isClientSide && dragon.getFlyingTimer() % 40 == 0) {
                    System.out.println("[FLYING-GOAL] Ready to land but climbing to 180 blocks first (current: " +
                        String.format("%.1f", currentY) + ")");
                }
                // Setze Ziel auf 180 Blöcke Höhe
                targetY = 180.0D;
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
        double speed = 1.0D; // Fast flying speed (doubled from 0.5)

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

        // Kollisionserkennung - Überprüfen ob der Drache stecken bleibt
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
                System.out.println("[FLYING-GOAL] No solid block found, using fallback: " + landingSpot);
            }
        } else {
            if (!dragon.level().isClientSide) {
                double horizontalDistance = Math.sqrt(
                    Math.pow(landingSpot.getX() - dragon.getX(), 2) +
                    Math.pow(landingSpot.getZ() - dragon.getZ(), 2)
                );
                System.out.println("[FLYING-GOAL] Landing spot found at: " + landingSpot +
                    " (horizontal distance: " + String.format("%.2f", horizontalDistance) +
                    " blocks, vertical: " + String.format("%.2f", landingSpot.getY() - dragon.getY()) + " blocks)");
            }
        }
    }

    /**
     * Finds a landing spot 80-150 blocks ahead of the dragon's current position
     * Searches in a cone in front of the dragon's facing direction
     */
    private BlockPos findLandingSpotAhead() {
        Vec3 dragonPos = dragon.position();

        // Pick a random distance between 80-150 blocks away
        double targetDistance = 80.0D + dragon.getRandom().nextDouble() * 70.0D;

        // Get dragon's current yaw (facing direction)
        float yaw = dragon.getYRot();

        // Add some randomness to the angle (±30 degrees)
        double angleVariation = (dragon.getRandom().nextDouble() - 0.5) * 60.0; // -30 to +30 degrees
        double targetAngle = Math.toRadians(yaw + angleVariation);

        // Calculate target position ahead of dragon
        int targetX = (int)(dragonPos.x + Math.sin(targetAngle) * targetDistance);
        int targetZ = (int)(dragonPos.z + Math.cos(targetAngle) * targetDistance);

        if (!dragon.level().isClientSide) {
            System.out.println("[FLYING-GOAL] Searching for landing spot at X=" + targetX + ", Z=" + targetZ +
                " (angle=" + String.format("%.1f", Math.toDegrees(targetAngle)) + "°, distance=" +
                String.format("%.1f", targetDistance) + " blocks)");
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
                    System.out.println("[FLYING-GOAL] Found solid ground at Y=" + y + ", landing at Y=" + landingPos.getY());
                }
                return landingPos;
            }
        }

        if (!dragon.level().isClientSide) {
            System.out.println("[FLYING-GOAL] No solid ground found in search column");
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
                System.out.println("[FLYING-GOAL] Landing cancelled - no landing spot");
            }
            return;
        }

        // Check if dragon has touched the ground during approach
        if (dragon.onGround()) {
            if (!dragon.level().isClientSide) {
                System.out.println("[FLYING-GOAL] *** EARLY GROUND CONTACT *** - Landing immediately");
            }
            completeLanding();
            return;
        }

        // Check if there are obstacles in the flight path ahead
        if (checkForObstacleAhead()) {
            if (!dragon.level().isClientSide) {
                System.out.println("[FLYING-GOAL] *** OBSTACLE DETECTED IN FLIGHT PATH *** - Landing at current position");
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
            System.out.println("[FLYING-GOAL] Landing approach - Horizontal dist: " +
                String.format("%.2f", horizontalDistance) + " blocks, Vertical dist: " +
                String.format("%.2f", verticalDistance) + " blocks");
        }

        // Check if close enough to land (within 2 blocks horizontally and 1 block vertically)
        if (horizontalDistance < 2.0D && Math.abs(verticalDistance) < 1.5D) {
            if (!dragon.level().isClientSide) {
                System.out.println("[FLYING-GOAL] *** LANDING COMPLETE ***");
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
            System.out.println("[FLYING-GOAL] Landed - remaining time: " + (timer / 20) + "s / " +
                (currentLandingDuration / 20) + "s, walking around");
        }

        // Check if it's time to take off again (after walking duration)
        if (timer <= 0) {
            if (!dragon.level().isClientSide) {
                System.out.println("[FLYING-GOAL] *** WALKING TIME FINISHED - Dragon deciding next action ***");
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
                        System.out.println("[FLYING-GOAL] Dragon jumping over obstacle!");
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
                    System.out.println("[FLYING-GOAL] New walking target: " + landingSpot);
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

        // Pick a target high in the sky (150-180 blocks) for initial climb
        double currentX = dragon.getX();
        double currentZ = dragon.getZ();
        double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = 40.0D + dragon.getRandom().nextDouble() * 30.0D;

        targetX = currentX + Math.cos(angle) * distance;
        targetZ = currentZ + Math.sin(angle) * distance;
        targetY = 150.0D + dragon.getRandom().nextDouble() * 30.0D; // 150-180 blocks high

        flyingTimer = 0;
        if (!dragon.level().isClientSide) {
            System.out.println("[FLYING-GOAL] *** TAKING OFF *** - Climbing to altitude " +
                String.format("%.1f", targetY) + " blocks");
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
                        System.out.println("[FLYING-GOAL] Far from nest, picking target towards nest");
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

                // ERHÖHTE MINDESTFLUGHÖHE: 180-210 Blöcke (vorher 170-200)
                targetY = 180.0D + dragon.getRandom().nextDouble() * 30.0D;
                return;
            }
        }

        // No nest or nest-less behavior: completely random
        double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = 30.0D + dragon.getRandom().nextDouble() * 40.0D;

        targetX = currentX + Math.cos(angle) * distance;
        targetZ = currentZ + Math.sin(angle) * distance;
        // ERHÖHTE MINDESTFLUGHÖHE: 180-210 Blöcke (vorher 170-200)
        targetY = 180.0D + dragon.getRandom().nextDouble() * 30.0D;
    }

    private void maintainAltitude() {
        // ERHÖHTE GRENZWERTE für Mindestflughöhe
        // Dieser Code stellt sicher dass der Drache IMMER über Bäumen fliegt
        double currentY = dragon.getY();

        if (currentY < 175.0D) {
            // Emergency: too low (unter 175 statt 165), force upward faster
            dragon.setDeltaMovement(dragon.getDeltaMovement().x, 0.3D, dragon.getDeltaMovement().z);
        } else if (currentY > 215.0D) {
            // Emergency: too high (über 215 statt 205), force downward faster
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
     * Überprüft ob der Drache stecken bleibt und führt ggf. ein Ausweichmanöver durch
     */
    private void checkForStuck() {
        Vec3 currentPosition = dragon.position();
        double movementDistance = currentPosition.distanceTo(lastPosition);

        // Prüfe ob Drache sich ausreichend bewegt hat
        if (movementDistance < MIN_MOVEMENT) {
            stuckCounter++;

            // Debug-Ausgabe alle halbe Sekunde während Steckenbleiben
            if (stuckCounter % 10 == 0 && !dragon.level().isClientSide) {
                System.out.println("[FLYING-GOAL] ⚠️ Dragon seems stuck! Counter: " + stuckCounter + "/" + STUCK_THRESHOLD +
                    " (movement: " + String.format("%.4f", movementDistance) + ")");
            }

            // Wenn Drache zu lange steckt, Notfallmanöver einleiten
            if (stuckCounter >= STUCK_THRESHOLD) {
                initiateEmergencyManeuver();
                stuckCounter = 0; // Reset counter
            }
        } else {
            // Drache bewegt sich normal, Counter zurücksetzen
            if (stuckCounter > 0) {
                stuckCounter = 0;
            }
        }

        lastPosition = currentPosition;
    }

    /**
     * Initiiert ein Notfall-Ausweichmanöver wenn der Drache steckengeblieben ist
     */
    private void initiateEmergencyManeuver() {
        if (!dragon.level().isClientSide) {
            System.out.println("[FLYING-GOAL] 🚨 EMERGENCY MANEUVER INITIATED - Dragon is stuck!");
        }

        isPerformingEmergencyManeuver = true;
        emergencyManeuverTimer = 60; // 3 Sekunden Ausweichmanöver

        // Strategie: Zuerst Richtungswechsel versuchen, dann nach oben fliegen
        // Wähle zufällige neue Richtung (180° gedreht + Zufall)
        float currentYaw = dragon.getYRot();
        float newYaw = currentYaw + 150.0F + dragon.getRandom().nextFloat() * 60.0F; // 150-210° Drehung

        double angle = Math.toRadians(newYaw);
        double distance = 20.0D;

        // Neue Richtung mit leichtem Steigflug
        emergencyDirection = new Vec3(
            Math.sin(angle) * distance,
            5.0D, // Leicht nach oben
            Math.cos(angle) * distance
        ).normalize();

        if (!dragon.level().isClientSide) {
            System.out.println("[FLYING-GOAL] Emergency direction: " +
                String.format("X=%.2f, Y=%.2f, Z=%.2f", emergencyDirection.x, emergencyDirection.y, emergencyDirection.z));
        }
    }

    /**
     * Führt ein Notfall-Ausweichmanöver durch, wenn der Drache stecken bleibt
     */
    private void performEmergencyManeuver() {
        emergencyManeuverTimer--;

        if (emergencyManeuverTimer <= 0) {
            // Manöver beendet
            isPerformingEmergencyManeuver = false;
            pickNewTarget(); // Neues Ziel wählen
            if (!dragon.level().isClientSide) {
                System.out.println("[FLYING-GOAL] ✅ Emergency maneuver completed, resuming normal flight");
            }
            return;
        }

        // Prüfe ob immer noch Hindernis im Weg ist
        if (checkForObstacleAhead() && emergencyManeuverTimer > 20) {
            // Immer noch blockiert nach 1 Sekunde - Plan B: Nach oben fliegen!
            if (!dragon.level().isClientSide) {
                System.out.println("[FLYING-GOAL] 🚁 Still blocked - ASCENDING as last resort!");
            }

            // Stark nach oben fliegen
            Vec3 currentVel = dragon.getDeltaMovement();
            dragon.setDeltaMovement(currentVel.x * 0.5, 0.5D, currentVel.z * 0.5); // Starker Aufstieg
            emergencyManeuverTimer = 20; // Noch 1 Sekunde weitermachen
            return;
        }

        // Ausweichmanöver: In neue Richtung fliegen
        double speed = 0.8D;
        Vec3 currentVel = dragon.getDeltaMovement();

        // Sanfter Übergang zur Notfallrichtung
        double newVelX = Mth.lerp(0.2D, currentVel.x, emergencyDirection.x * speed);
        double newVelY = Mth.lerp(0.2D, currentVel.y, emergencyDirection.y * speed);
        double newVelZ = Mth.lerp(0.2D, currentVel.z, emergencyDirection.z * speed);

        dragon.setDeltaMovement(newVelX, newVelY, newVelZ);
    }

    /**
     * Sets a random flying duration for variety
     */
    private void setRandomFlyingDuration() {
        currentFlyingDuration = MIN_FLYING_DURATION +
            dragon.getRandom().nextInt(MAX_FLYING_DURATION - MIN_FLYING_DURATION + 1);
        if (!dragon.level().isClientSide) {
            System.out.println("[FLYING-GOAL] New random flying duration: " +
                (currentFlyingDuration / 20) + " seconds (" +
                (currentFlyingDuration / 20 / 60) + " min " +
                ((currentFlyingDuration / 20) % 60) + " sec)");
        }
    }

    /**
     * Sets a random landing duration for variety
     */
    private void setRandomLandingDuration() {
        currentLandingDuration = MIN_LANDING_DURATION +
            dragon.getRandom().nextInt(MAX_LANDING_DURATION - MIN_LANDING_DURATION + 1);
        if (!dragon.level().isClientSide) {
            System.out.println("[FLYING-GOAL] New random landing duration: " +
                (currentLandingDuration / 20) + " seconds (" +
                (currentLandingDuration / 20 / 60) + " min " +
                ((currentLandingDuration / 20) % 60) + " sec)");
        }
    }

    /**
     * Initiates landing at current position (random decision)
     */
    private void initiateRandomLanding() {
        double currentY = dragon.getY();

        // If already high enough, start landing immediately
        if (currentY >= 180.0D) {
            startLandingSequence();
        } else {
            // Climb to 180 first
            targetY = 180.0D;
            // Will trigger landing on next tick when high enough
            dragon.setFlyingTimer(currentFlyingDuration);
        }
    }
}

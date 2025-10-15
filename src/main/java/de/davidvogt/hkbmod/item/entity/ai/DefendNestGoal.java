package de.davidvogt.hkbmod.item.entity.ai;

import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes dragons defend their nest by attacking players who come too close.
 * - When resting: smaller detection radius (10 blocks), wakes up and attacks from ground
 * - When landed: normal detection radius (20 blocks), attacks from ground without flying
 * - When flying: flies towards player to attack, retreats if too close (< 20 blocks), random chance to land
 */
public class DefendNestGoal extends Goal {
    private static final double NEST_DEFENSE_RADIUS = 50.0D; // Increased for aerial combat
    private static final double RESTING_DETECTION_RADIUS = 10.0D; // Smaller radius when resting
    private static final double MIN_ATTACK_DISTANCE = 20.0D; // Minimum distance for aerial attack
    private static final double MAX_ATTACK_DISTANCE = 40.0D; // Maximum distance for aerial attack
    private static final double FIRE_BREATH_DISTANCE = 10.0D; // Distance for fire breath attack
    private static final int FIREBALL_COOLDOWN = 40; // 2 seconds between fireballs
    private static final int FIRE_BREATH_COOLDOWN = 20; // 1 second between fire breaths (faster!)
    private static final int CHECK_INTERVAL = 20; // Check for intruders every second
    private static final float LAND_TO_ATTACK_CHANCE = 0.15F; // 15% chance to land and attack from ground

    private final DragonEntity dragon;
    private LivingEntity target;
    private int fireballCooldown = 0;
    private int fireBreathCooldown = 0;
    private int checkTimer = 0;

    // Aerial combat states
    private enum CombatState {
        APPROACHING,  // Flying towards player
        ATTACKING,    // In attack range, shooting
        RETREATING    // Too close, flying away
    }
    private CombatState combatState = CombatState.APPROACHING;
    private Vec3 retreatTarget = null;
    private boolean decidedToLandAttack = false;

    public DefendNestGoal(DragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET, Goal.Flag.LOOK, Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only defend if dragon has a nest
        if (!dragon.hasNest()) {
            return false;
        }

        // Check periodically to reduce performance impact
        checkTimer--;
        if (checkTimer > 0 && target != null && target.isAlive()) {
            return true; // Keep attacking current target
        }
        checkTimer = CHECK_INTERVAL;

        BlockPos nestPos = dragon.getNestPosition();
        if (nestPos == null) {
            return false;
        }

        // Use smaller radius when resting, normal radius otherwise
        double detectionRadius = dragon.isResting() ? RESTING_DETECTION_RADIUS : NEST_DEFENSE_RADIUS;

        // Find players near the nest
        AABB searchArea = new AABB(nestPos).inflate(detectionRadius);
        List<Player> nearbyPlayers = dragon.level().getEntitiesOfClass(
            Player.class,
            searchArea,
            player -> !player.isSpectator() && !player.isCreative() && player.isAlive()
        );

        if (!nearbyPlayers.isEmpty()) {
            // Target the closest player to the nest
            target = nearbyPlayers.stream()
                .min((p1, p2) -> Double.compare(
                    p1.distanceToSqr(nestPos.getX(), nestPos.getY(), nestPos.getZ()),
                    p2.distanceToSqr(nestPos.getX(), nestPos.getY(), nestPos.getZ())
                ))
                .orElse(null);
            return target != null;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Stop if player left the nest area
        BlockPos nestPos = dragon.getNestPosition();
        if (nestPos == null) {
            return false;
        }

        // Use smaller radius when resting, normal radius otherwise
        double detectionRadius = dragon.isResting() ? RESTING_DETECTION_RADIUS : NEST_DEFENSE_RADIUS;
        double distanceToNest = target.distanceToSqr(nestPos.getX(), nestPos.getY(), nestPos.getZ());
        return distanceToNest <= detectionRadius * detectionRadius;
    }

    @Override
    public void start() {
        dragon.setTarget(target);
        fireballCooldown = 0;
        combatState = CombatState.APPROACHING;
        decidedToLandAttack = false;

        // If dragon is resting, wake it up (but don't make it fly)
        if (dragon.isResting()) {
            dragon.setResting(false);
            if (!dragon.level().isClientSide) {
                System.out.println("[DRAGON-DEFENSE] Dragon woke up from rest to defend nest!");
            }
        }

        // If dragon is flying, random chance to decide to land and attack from ground
        if (!dragon.isLanded() && dragon.getRandom().nextFloat() < LAND_TO_ATTACK_CHANCE) {
            decidedToLandAttack = true;
            if (!dragon.level().isClientSide) {
                System.out.println("[DRAGON-DEFENSE] Dragon decided to LAND and attack from ground!");
            }
        }
    }

    @Override
    public void stop() {
        target = null;
        dragon.setTarget(null);
        combatState = CombatState.APPROACHING;
        retreatTarget = null;
        decidedToLandAttack = false;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }

        // Always look at the target
        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Handle ground-based combat
        if (dragon.isLanded()) {
            handleGroundCombat();
            return;
        }

        // Handle decision to land and attack
        if (decidedToLandAttack && !dragon.isLandingMode()) {
            initiateLandingForCombat();
            return;
        }

        // Handle aerial combat
        handleAerialCombat();
    }

    /**
     * Handles combat when dragon is on the ground
     */
    private void handleGroundCombat() {
        // Stop movement, just shoot
        dragon.setDeltaMovement(0, dragon.getDeltaMovement().y, 0);

        double distanceToTarget = dragon.distanceTo(target);

        // Use fire breath if very close, otherwise use fireballs
        if (distanceToTarget < FIRE_BREATH_DISTANCE) {
            // Fire breath for close combat
            fireBreathCooldown--;
            if (fireBreathCooldown <= 0) {
                if (dragon.hasLineOfSight(target)) {
                    dragon.breatheFire(target);
                    fireBreathCooldown = FIRE_BREATH_COOLDOWN;

                    if (!dragon.level().isClientSide) {
                        System.out.println("[DRAGON-DEFENSE] Breathing FIRE from GROUND at close range (" +
                            String.format("%.1f", distanceToTarget) + "m)!");
                    }
                }
            }
        } else {
            // Fireballs for medium range
            fireballCooldown--;
            if (fireballCooldown <= 0) {
                if (dragon.hasLineOfSight(target)) {
                    dragon.shootExplosiveFireball(target);
                    fireballCooldown = FIREBALL_COOLDOWN;

                    if (!dragon.level().isClientSide) {
                        System.out.println("[DRAGON-DEFENSE] Shooting fireball from GROUND at " + target.getName().getString());
                    }
                }
            }
        }
    }

    /**
     * Handles aerial combat - flying towards player, shooting, and retreating if too close
     */
    private void handleAerialCombat() {
        double distanceToTarget = dragon.distanceTo(target);

        // Determine combat state based on distance
        if (distanceToTarget < MIN_ATTACK_DISTANCE) {
            // Too close - retreat!
            if (combatState != CombatState.RETREATING) {
                combatState = CombatState.RETREATING;
                calculateRetreatTarget();
                if (!dragon.level().isClientSide) {
                    System.out.println("[DRAGON-DEFENSE] Too close (" + String.format("%.1f", distanceToTarget) +
                        "m) - RETREATING!");
                }
            }
        } else if (distanceToTarget > MAX_ATTACK_DISTANCE) {
            // Too far - approach!
            combatState = CombatState.APPROACHING;
        } else {
            // In attack range - circle and shoot
            combatState = CombatState.ATTACKING;
        }

        // Execute movement based on state
        switch (combatState) {
            case APPROACHING:
                flyTowardsTarget();
                break;
            case ATTACKING:
                circleAndShoot();
                break;
            case RETREATING:
                flyAwayFromTarget();
                break;
        }

        // Shoot fireballs when cooldown is ready and in attack state
        // Use fire breath if very close (< 10 blocks), otherwise use fireballs
        if (combatState == CombatState.ATTACKING || combatState == CombatState.APPROACHING) {
            if (distanceToTarget < FIRE_BREATH_DISTANCE) {
                // Very close - use fire breath!
                fireBreathCooldown--;
                if (fireBreathCooldown <= 0) {
                    if (dragon.hasLineOfSight(target)) {
                        dragon.breatheFire(target);
                        fireBreathCooldown = FIRE_BREATH_COOLDOWN;

                        if (!dragon.level().isClientSide) {
                            System.out.println("[DRAGON-DEFENSE] Breathing FIRE from AIR at " +
                                target.getName().getString() + " (" + String.format("%.1f", distanceToTarget) + "m away)");
                        }
                    }
                }
            } else {
                // Normal range - use fireballs
                fireballCooldown--;
                if (fireballCooldown <= 0) {
                    if (dragon.hasLineOfSight(target)) {
                        dragon.shootExplosiveFireball(target);
                        fireballCooldown = FIREBALL_COOLDOWN;

                        if (!dragon.level().isClientSide) {
                            System.out.println("[DRAGON-DEFENSE] Shooting fireball from AIR at " +
                                target.getName().getString() + " (" + String.format("%.1f", distanceToTarget) + "m away)");
                        }
                    }
                }
            }
        }
    }

    /**
     * Fly directly towards the target player
     */
    private void flyTowardsTarget() {
        Vec3 dragonPos = dragon.position();
        Vec3 targetPos = target.position();

        // Calculate direction to target
        Vec3 direction = targetPos.subtract(dragonPos).normalize();

        // Fly towards target at combat speed
        double speed = 0.8D;
        Vec3 currentVel = dragon.getDeltaMovement();

        // Smooth acceleration towards target
        double newVelX = Mth.lerp(0.2D, currentVel.x, direction.x * speed);
        double newVelY = Mth.lerp(0.2D, currentVel.y, direction.y * speed * 0.5D); // Slower vertical
        double newVelZ = Mth.lerp(0.2D, currentVel.z, direction.z * speed);

        dragon.setDeltaMovement(newVelX, newVelY, newVelZ);
    }

    /**
     * Circle around the target while maintaining attack distance
     */
    private void circleAndShoot() {
        Vec3 dragonPos = dragon.position();
        Vec3 targetPos = target.position();

        // Calculate direction to target
        Vec3 toTarget = targetPos.subtract(dragonPos);
        double distance = toTarget.length();
        Vec3 toTargetNorm = toTarget.normalize();

        // Calculate perpendicular vector for circling (cross product with up vector)
        Vec3 circleDir = new Vec3(-toTargetNorm.z, 0, toTargetNorm.x).normalize();

        // Mix forward movement with circling
        Vec3 desiredDir = toTargetNorm.scale(0.3).add(circleDir.scale(0.7)).normalize();

        double speed = 0.6D;
        Vec3 currentVel = dragon.getDeltaMovement();

        double newVelX = Mth.lerp(0.15D, currentVel.x, desiredDir.x * speed);
        double newVelY = Mth.lerp(0.15D, currentVel.y, 0); // Maintain altitude
        double newVelZ = Mth.lerp(0.15D, currentVel.z, desiredDir.z * speed);

        dragon.setDeltaMovement(newVelX, newVelY, newVelZ);
    }

    /**
     * Fly away from the target to create distance
     */
    private void flyAwayFromTarget() {
        if (retreatTarget == null) {
            calculateRetreatTarget();
        }

        Vec3 dragonPos = dragon.position();
        Vec3 direction = retreatTarget.subtract(dragonPos);

        // Check if reached retreat point
        if (direction.length() < 5.0D) {
            combatState = CombatState.APPROACHING; // Turn around and attack again
            retreatTarget = null;
            if (!dragon.level().isClientSide) {
                System.out.println("[DRAGON-DEFENSE] Retreat complete - turning around for another attack!");
            }
            return;
        }

        direction = direction.normalize();
        double speed = 1.0D; // Fast retreat
        Vec3 currentVel = dragon.getDeltaMovement();

        double newVelX = Mth.lerp(0.2D, currentVel.x, direction.x * speed);
        double newVelY = Mth.lerp(0.2D, currentVel.y, direction.y * speed * 0.5D);
        double newVelZ = Mth.lerp(0.2D, currentVel.z, direction.z * speed);

        dragon.setDeltaMovement(newVelX, newVelY, newVelZ);
    }

    /**
     * Calculate a retreat position away from the target
     */
    private void calculateRetreatTarget() {
        Vec3 dragonPos = dragon.position();
        Vec3 targetPos = target.position();

        // Calculate direction away from target
        Vec3 awayFromTarget = dragonPos.subtract(targetPos).normalize();

        // Add some randomness to avoid predictable movement
        double randomAngle = (dragon.getRandom().nextDouble() - 0.5) * Math.PI / 3; // ±30 degrees
        double cos = Math.cos(randomAngle);
        double sin = Math.sin(randomAngle);
        Vec3 rotated = new Vec3(
            awayFromTarget.x * cos - awayFromTarget.z * sin,
            awayFromTarget.y,
            awayFromTarget.x * sin + awayFromTarget.z * cos
        );

        // Retreat 50 blocks away
        double retreatDistance = 50.0D;
        retreatTarget = dragonPos.add(rotated.scale(retreatDistance));

        // Ensure retreat target is at good flying altitude
        retreatTarget = new Vec3(retreatTarget.x, Math.max(retreatTarget.y, 180.0D), retreatTarget.z);

        if (!dragon.level().isClientSide) {
            System.out.println("[DRAGON-DEFENSE] Retreat target set: " +
                String.format("%.1f, %.1f, %.1f", retreatTarget.x, retreatTarget.y, retreatTarget.z));
        }
    }

    /**
     * Initiates landing sequence for ground-based combat
     */
    private void initiateLandingForCombat() {
        // Override the FlyingGoal by setting landing mode
        dragon.setLandingMode(true);

        // Find ground near target
        Vec3 targetPos = target.position();
        BlockPos groundPos = findGroundNearTarget(targetPos);

        if (groundPos != null) {
            // Set dragon to glide towards ground
            Vec3 dragonPos = dragon.position();
            Vec3 direction = Vec3.atCenterOf(groundPos).subtract(dragonPos).normalize();

            double speed = 0.8D;
            dragon.setDeltaMovement(
                direction.x * speed,
                -0.3D, // Descend
                direction.z * speed
            );

            if (!dragon.level().isClientSide) {
                System.out.println("[DRAGON-DEFENSE] Gliding to ground at " + groundPos.toShortString() +
                    " to attack from there!");
            }
        }
    }

    /**
     * Find ground position near the target
     */
    private BlockPos findGroundNearTarget(Vec3 targetPos) {
        int startY = (int)targetPos.y + 10;
        int targetX = (int)targetPos.x;
        int targetZ = (int)targetPos.z;

        for (int y = startY; y > dragon.level().getMinY(); y--) {
            BlockPos checkPos = new BlockPos(targetX, y, targetZ);
            if (!dragon.level().getBlockState(checkPos).isAir() &&
                dragon.level().getBlockState(checkPos).isSolid()) {
                return checkPos.above();
            }
        }

        return null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}

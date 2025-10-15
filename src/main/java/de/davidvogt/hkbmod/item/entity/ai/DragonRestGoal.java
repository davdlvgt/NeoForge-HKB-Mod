package de.davidvogt.hkbmod.item.entity.ai;

import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Goal that makes dragons occasionally rest by lying down and curling up, similar to polar foxes.
 * The dragon will rest for 10 minutes when this goal activates.
 * Dragons fly relaxed to their nest before resting.
 */
public class DragonRestGoal extends Goal {
    private static final int REST_DURATION = 2000; // 2 Minuten (120 Sekunden) - FÜR TESTING
    private static final int MIN_TIME_BETWEEN_RESTS = 0; // Keine Wartezeit - Drache ruht direkt nach dem Landen
    private static final int CHECK_INTERVAL = 20; // Check every second
    private static final double NEST_POSITION_THRESHOLD = 3.0; // Must be within 3 blocks of nest center
    private static final double RELAXED_FLIGHT_SPEED = 0.6D; // Langsame, entspannte Fluggeschwindigkeit

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
        // Increment timer every tick
        timeSinceLastRest++;

        // WICHTIG: Wenn LandedTimer abgelaufen ist, prüfe JEDES Tick!
        boolean landedTimerExpired = dragon.isLanded() && dragon.getLandedTimer() <= 0;

        if (!landedTimerExpired) {
            // Nur wenn Timer nicht abgelaufen, verwende normale Prüf-Intervalle
            checkTimer++;
            if (checkTimer < CHECK_INTERVAL) {
                return false;
            }
            checkTimer = 0;
        } else {
            // Timer ist abgelaufen - prüfe jedes Tick!
            checkTimer = 0;
        }

        // Debug log every 10 seconds or when timer expired
        if ((timeSinceLastRest % 200 == 0 || landedTimerExpired) && !dragon.level().isClientSide) {
            System.out.println("[DRAGON-REST-DEBUG] Time since last rest: " + (timeSinceLastRest / 20.0) + "s, isLanded: " + dragon.isLanded() + ", landedTimer: " + dragon.getLandedTimer() + ", hasTarget: " + (dragon.getTarget() != null));
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
            // Debug log when timer expired
            if (landedTimerExpired && !dragon.level().isClientSide) {
                System.out.println("[DRAGON-REST-DEBUG] *** No nest block found nearby! Dragon will take off instead. Position: " + dragon.blockPosition().toShortString() + " ***");
            }
            return false;
        }

        // Nach dem Landen (wenn LandedTimer abgelaufen ist), ZUFÄLLIG entscheiden ob rasten oder abheben
        // 50% Chance zu rasten, 50% Chance direkt wieder abzuheben (wird von FlyingGoal übernommen)
        if (landedTimerExpired) {
            // Random decision: 50% chance to rest
            if (dragon.getRandom().nextFloat() < 0.5F) {
                if (!dragon.level().isClientSide) {
                    System.out.println("[DRAGON-REST-DEBUG] ★★★ RANDOM DECISION: Dragon will fly to nest and rest at " + nestPos.toShortString() + " ★★★");
                }
                targetNestPos = nestPos;
                return true;
            } else {
                if (!dragon.level().isClientSide) {
                    System.out.println("[DRAGON-REST-DEBUG] ★★★ RANDOM DECISION: Dragon skips resting and will take off again! ★★★");
                }
                // Don't rest, let the dragon take off instead (handled by FlyingGoal)
                return false;
            }
        }

        return false;
    }

    /**
     * Finds a nearby DRAGON_NEST block within reasonable range
     */
    private BlockPos findNearbyNest() {
        // ERSTE PRIORITÄT: Nutze die gespeicherte Nest-Position des Drachen
        if (dragon.hasNest()) {
            BlockPos savedNestPos = dragon.getNestPosition();
            if (savedNestPos != null) {
                // Prüfe ob das gespeicherte Nest noch existiert
                BlockState state = dragon.level().getBlockState(savedNestPos);
                if (state.is(ModBlocks.DRAGON_NEST.get())) {
                    if (!dragon.level().isClientSide) {
                        // Berechne Distanz zwischen den beiden BlockPos
                        double distance = Math.sqrt(dragon.blockPosition().distSqr(savedNestPos));
                        System.out.println("[DRAGON-REST-DEBUG] Using saved nest position at " + savedNestPos.toShortString() + " (distance: " + String.format("%.1f", distance) + " blocks)");
                    }
                    return savedNestPos;
                }
            }
        }

        // ZWEITE PRIORITÄT: Suche in größerem Bereich (30x30x10)
        BlockPos dragonPos = dragon.blockPosition();

        if (!dragon.level().isClientSide) {
            System.out.println("[DRAGON-REST-DEBUG] Searching for nest in 30x30x10 area around " + dragonPos.toShortString());
        }

        // Check in a 30x30x10 area around the dragon (erweitert von 10x10x5)
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                for (int y = -5; y <= 5; y++) {
                    BlockPos checkPos = dragonPos.offset(x, y, z);
                    BlockState state = dragon.level().getBlockState(checkPos);
                    if (state.is(ModBlocks.DRAGON_NEST.get())) {
                        if (!dragon.level().isClientSide) {
                            double distance = Math.sqrt(x*x + y*y + z*z);
                            System.out.println("[DRAGON-REST-DEBUG] Found nest at " + checkPos.toShortString() + " (distance: " + String.format("%.1f", distance) + " blocks)");
                        }
                        return checkPos;
                    }
                }
            }
        }

        if (!dragon.level().isClientSide) {
            System.out.println("[DRAGON-REST-DEBUG] *** NO NEST FOUND in 30x30x10 area! ***");
        }

        return null;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if we have a target (being attacked or defending)
        if (dragon.getTarget() != null) {
            if (!dragon.level().isClientSide) {
                System.out.println("[DRAGON-REST] Interrupted by target!");
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
            System.out.println("[DRAGON-REST] Dragon will fly relaxed to nest at " + targetNestPos.toShortString() + " (current distance: " + String.format("%.2f", distanceToNest) + ")");
        }
    }

    private void startResting() {
        // Only set resting to true when dragon is actually on the nest
        dragon.setResting(true);
        dragon.setLanded(true);
        restTimeLeft = REST_DURATION;
        dragon.getNavigation().stop();
        movingToNest = false;

        if (!dragon.level().isClientSide) {
            System.out.println("[DRAGON-REST] ★★★ Dragon is now ON NEST and started resting for " + (REST_DURATION / 20.0) + " seconds ★★★");
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
            System.out.println("[DRAGON-REST] ★★★ Dragon finished resting - preparing to take off ★★★");
        }

        // Nach dem Ruhen soll der Drache wieder fliegen
        // Setze isLanded auf false, damit DragonFlyingGoal den Drachen wieder abheben lässt
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
                System.out.println("[DRAGON-REST] Flying relaxed to nest, distance: " + String.format("%.1f", distanceToNest) + " blocks");
            }

            if (distanceToNest <= NEST_POSITION_THRESHOLD) {
                // Reached the nest, position dragon directly on it
                Vec3 targetVec = new Vec3(
                    targetNestPos.getX() + 0.5,
                    targetNestPos.getY() + 0.5,
                    targetNestPos.getZ() + 0.5
                );
                dragon.setPos(targetVec.x, targetVec.y, targetVec.z);
                dragon.setDeltaMovement(Vec3.ZERO);

                if (!dragon.level().isClientSide) {
                    System.out.println("[DRAGON-REST] Reached nest position, starting rest");
                }

                startResting();
            } else {
                // Continue flying towards nest with relaxed, smooth movement
                Vec3 normalizedDirection = directionToNest.normalize();

                // Relaxed flight speed (slower than normal flying)
                double speed = RELAXED_FLIGHT_SPEED;

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
                System.out.println("[DRAGON-REST] Resting... " + (restTimeLeft / 20.0) + " seconds remaining");
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}

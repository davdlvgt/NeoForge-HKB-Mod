package de.davidvogt.hkbmod.item.entity.ai;

import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes dragons defend their nest by attacking players who come too close.
 * When a player enters within 30 blocks of the nest, the dragon will shoot explosive fireballs.
 */
public class DefendNestGoal extends Goal {
    private static final double NEST_DEFENSE_RADIUS = 20.0D;
    private static final int FIREBALL_COOLDOWN = 40; // 2 seconds between fireballs
    private static final int CHECK_INTERVAL = 20; // Check for intruders every second

    private final DragonEntity dragon;
    private LivingEntity target;
    private int fireballCooldown = 0;
    private int checkTimer = 0;

    public DefendNestGoal(DragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET, Goal.Flag.LOOK));
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

        // Find players near the nest
        AABB searchArea = new AABB(nestPos).inflate(NEST_DEFENSE_RADIUS);
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

        double distanceToNest = target.distanceToSqr(nestPos.getX(), nestPos.getY(), nestPos.getZ());
        return distanceToNest <= NEST_DEFENSE_RADIUS * NEST_DEFENSE_RADIUS;
    }

    @Override
    public void start() {
        dragon.setTarget(target);
        fireballCooldown = 0;
    }

    @Override
    public void stop() {
        target = null;
        dragon.setTarget(null);
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }

        // Look at the target
        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Shoot fireballs when cooldown is ready
        fireballCooldown--;
        if (fireballCooldown <= 0) {
            // Check if dragon has line of sight to target
            if (dragon.hasLineOfSight(target)) {
                dragon.shootExplosiveFireball(target);
                fireballCooldown = FIREBALL_COOLDOWN;
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}


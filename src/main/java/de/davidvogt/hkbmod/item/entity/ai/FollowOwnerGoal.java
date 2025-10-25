package de.davidvogt.hkbmod.item.entity.ai;

import de.davidvogt.hkbmod.item.entity.custom.DragonConstants;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Goal that makes tamed dragons follow their owner by walking on the ground.
 * The dragon will walk towards the owner when they are too far away, and stop when close enough.
 */
public class FollowOwnerGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(FollowOwnerGoal.class);

    private final DragonEntity dragon;
    private final double speedModifier;
    private final float maxDistance;
    private final float minDistance;
    private LivingEntity owner;
    private int timeToRecalcPath;

    public FollowOwnerGoal(DragonEntity dragon, double speedModifier, float minDistance, float maxDistance) {
        this.dragon = dragon;
        this.speedModifier = speedModifier;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only follow if dragon is tamed and not sitting
        if (!this.dragon.isTamed()) {
            return false;
        }

        if (this.dragon.isSitting()) {
            if (!this.dragon.level().isClientSide) {
                //System.out.println("[FOLLOW-GOAL] Cannot follow - dragon is sitting");
            }
            return false;
        }

        // Get owner
        Optional<UUID> ownerUUID = this.dragon.getOwnerUUID();
        if (ownerUUID.isEmpty()) {
            return false;
        }

        // Find owner entity
        LivingEntity owner = this.dragon.level().getPlayerByUUID(ownerUUID.get());
        if (owner == null) {
            return false;
        }

        // Check if owner is too far away
        double distanceSq = this.dragon.distanceToSqr(owner);
        if (distanceSq < (double) (this.minDistance * this.minDistance)) {
            return false;
        }

        this.owner = owner;

        if (!this.dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} can follow owner - distance: {:.2f} blocks",
                    this.dragon.getId(), Math.sqrt(distanceSq));
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop following if dragon is now sitting or owner is too close
        if (this.dragon.isSitting()) {
            return false;
        }

        if (this.owner == null || !this.owner.isAlive()) {
            return false;
        }

        return this.dragon.distanceToSqr(this.owner) > (double) (this.minDistance * this.minDistance);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        // Disable flying when following owner - dragon should walk
        this.dragon.setNoGravity(false);
        // Ensure dragon is in landed state for walking animation
        this.dragon.setLanded(true);
        this.dragon.setLandingMode(false);

        if (!this.dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} started following owner (walking mode)", this.dragon.getId());
        }
    }

    @Override
    public void stop() {
        this.owner = null;
        this.dragon.getNavigation().stop();

        if (!this.dragon.level().isClientSide) {
            LOGGER.debug("Dragon {} stopped following owner", this.dragon.getId());
        }
    }

    @Override
    public void tick() {
        // Look at owner
        this.dragon.getLookControl().setLookAt(this.owner, 10.0F, (float) this.dragon.getMaxHeadXRot());

        // Ensure dragon stays landed while following
        if (!this.dragon.isLanded()) {
            this.dragon.setLanded(true);
        }

        // Recalculate path every 10 ticks (0.5 seconds)
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = DragonConstants.PATH_RECALC_INTERVAL_TICKS;

            // Check distance to owner
            double distanceSq = this.dragon.distanceToSqr(this.owner);

            // If too far, teleport to owner (like wolves do)
            if (distanceSq > (double) (this.maxDistance * this.maxDistance)) {
                this.teleportToOwner();
            } else {
                // Walk to owner
                this.dragon.getNavigation().moveTo(this.owner, this.speedModifier);
            }
        }
    }

    /**
     * Teleports the dragon to the owner if it's too far away (like wolves do)
     */
    private void teleportToOwner() {
        Vec3 ownerPos = this.owner.position();

        // Try to find a safe position near the owner
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = this.randomIntInclusive(-3, 3);
            int y = this.randomIntInclusive(-1, 1);
            int z = this.randomIntInclusive(-3, 3);

            if (this.maybeTeleportTo(
                    (int) ownerPos.x + x,
                    (int) ownerPos.y + y,
                    (int) ownerPos.z + z
            )) {
                if (!this.dragon.level().isClientSide) {
                    LOGGER.debug("Dragon {} teleported to owner", this.dragon.getId());
                }
                return;
            }
        }
    }

    /**
     * Attempts to teleport to a specific position if it's safe
     */
    private boolean maybeTeleportTo(int x, int y, int z) {
        if (!this.canTeleportTo(x, y, z)) {
            return false;
        }

        this.dragon.setPos((double) x + 0.5, y, (double) z + 0.5);
        this.dragon.setYRot(this.dragon.getYRot());
        this.dragon.setXRot(this.dragon.getXRot());
        this.dragon.getNavigation().stop();
        return true;
    }

    /**
     * Checks if the dragon can safely teleport to a position
     */
    private boolean canTeleportTo(int x, int y, int z) {
        // Simplified check - just verify position is valid
        BlockPos pos = new BlockPos(x, y, z);

        // Check if block below is solid (not air)
        BlockPos below = pos.below();
        if (this.dragon.level().getBlockState(below).isAir()) {
            return false;
        }

        // Check if position and above are not solid (space for dragon)
        return this.dragon.level().getBlockState(pos).isAir() &&
                this.dragon.level().getBlockState(pos.above()).isAir();
    }

    /**
     * Random integer between min and max (inclusive)
     */
    private int randomIntInclusive(int min, int max) {
        return min + this.dragon.getRandom().nextInt(max - min + 1);
    }
}

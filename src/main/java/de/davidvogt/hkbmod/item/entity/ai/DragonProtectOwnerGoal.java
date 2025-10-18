package de.davidvogt.hkbmod.item.entity.ai;

import de.davidvogt.hkbmod.item.entity.custom.DragonConstants;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Goal that makes a tamed dragon automatically protect its owner by breathing fire
 * at hostile monsters that come within a specified radius around the owner.
 *
 * Conditions:
 * - Dragon must be tamed
 * - Dragon must not be sitting
 * - Owner must exist and be alive
 * - Owner must not be riding the dragon (player not mounted)
 */
public class DragonProtectOwnerGoal extends Goal {
    private final DragonEntity dragon;
    private final double protectRadius;
    private LivingEntity owner;
    private int checkCooldown = 0; // ticks until next scan

    public DragonProtectOwnerGoal(DragonEntity dragon, double protectRadius) {
        this.dragon = dragon;
        this.protectRadius = protectRadius;
        // Only uses LOOK flag so it doesn't block movement goals like FollowOwner
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.dragon.level().isClientSide) {
            return false; // run only on server
        }

        if (!this.dragon.isTamed()) return false;
        if (this.dragon.isSitting()) return false;
        // Ensure owner is not riding the dragon
        if (this.dragon.getControllingPassenger() != null) return false;

        Optional<UUID> ownerUUID = this.dragon.getOwnerUUID();
        if (ownerUUID.isEmpty()) return false;

        LivingEntity owner = this.dragon.level().getPlayerByUUID(ownerUUID.get());
        if (owner == null || !owner.isAlive()) return false;

        // Owner must be reasonably close (within follow max distance)
        double maxFollow = DragonConstants.FOLLOW_MAX_DISTANCE;
        if (this.dragon.distanceToSqr(owner) > maxFollow * maxFollow) return false;

        // If dragon cannot breathe fire due to cooldown, don't start
        if (!this.dragon.canBreatheFireOnGround()) return false;

        // Check if any hostile monsters are within the protect radius around the owner
        AABB box = new AABB(
                owner.getX() - protectRadius, owner.getY() - protectRadius, owner.getZ() - protectRadius,
                owner.getX() + protectRadius, owner.getY() + protectRadius, owner.getZ() + protectRadius
        );

        List<Monster> monsters = this.dragon.level().getEntitiesOfClass(Monster.class, box, m -> m.isAlive());
        if (monsters.isEmpty()) return false;

        this.owner = owner;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.dragon.isSitting()) return false;
        if (this.owner == null || !this.owner.isAlive()) return false;
        if (this.dragon.getControllingPassenger() != null) return false;
        // Continue only while owner remains in follow range
        double maxFollow = DragonConstants.FOLLOW_MAX_DISTANCE;
        if (this.dragon.distanceToSqr(this.owner) > maxFollow * maxFollow) return false;

        // Also continue only while there are monsters nearby
        AABB box = new AABB(
                owner.getX() - protectRadius, owner.getY() - protectRadius, owner.getZ() - protectRadius,
                owner.getX() + protectRadius, owner.getY() + protectRadius, owner.getZ() + protectRadius
        );
        List<Monster> monsters = this.dragon.level().getEntitiesOfClass(Monster.class, box, m -> m.isAlive());
        return !monsters.isEmpty() && this.dragon.canBreatheFireOnGround();
    }

    @Override
    public void start() {
        this.checkCooldown = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
    }

    @Override
    public void tick() {
        if (this.owner == null) return;

        // Throttle scans to the configured check interval
        if (--this.checkCooldown > 0) return;
        this.checkCooldown = DragonConstants.CHECK_INTERVAL_TICKS;

        if (!this.dragon.canBreatheFireOnGround()) return;

        // Find nearest monster to the owner within radius
        AABB box = new AABB(
                owner.getX() - protectRadius, owner.getY() - protectRadius, owner.getZ() - protectRadius,
                owner.getX() + protectRadius, owner.getY() + protectRadius, owner.getZ() + protectRadius
        );

        List<Monster> monsters = this.dragon.level().getEntitiesOfClass(Monster.class, box, m -> m.isAlive());
        if (monsters.isEmpty()) return;

        Monster nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Monster m : monsters) {
            double d = m.distanceToSqr(this.owner);
            if (d < nearestDistSq) {
                nearestDistSq = d;
                nearest = m;
            }
        }

        if (nearest != null) {
            // Compute direction from dragon to target
            Vec3 toTarget = new Vec3(
                nearest.getX() - this.dragon.getX(),
                (nearest.getY(0.5) - this.dragon.getY(0.5)),
                nearest.getZ() - this.dragon.getZ()
            );

            // Avoid zero-length vectors (can cause particles at dragon center)
            if (toTarget.lengthSqr() < 1.0E-6) {
                return;
            }

            Vec3 dir = toTarget.normalize();

            // Make the dragon look at the target so particles/fire spawn from head direction
            this.dragon.getLookControl().setLookAt(nearest, 30.0F, (float) this.dragon.getMaxHeadXRot());

            // Optional immediate rotation adjustment so the view vector aligns this tick
            float targetYaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F;
            float horizontal = (float) Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            float targetPitch = (float) (-(Math.atan2(dir.y, horizontal) * (180.0 / Math.PI)));
            this.dragon.setYRot(targetYaw);
            this.dragon.setXRot(targetPitch);

            // Instruct dragon to breathe fire in that direction. This will spawn fire in front of the head.
            this.dragon.breatheFireInDirection(dir);

            // Start cooldown so dragon doesn't spam breath
            this.dragon.startFireBreathCooldown();
         }
    }
}

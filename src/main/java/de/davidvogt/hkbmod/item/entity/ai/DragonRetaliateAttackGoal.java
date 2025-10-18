package de.davidvogt.hkbmod.item.entity.ai;

import de.davidvogt.hkbmod.item.entity.custom.DragonConstants;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Goal to retaliate when the dragon is attacked. If dragon has a target, it will
 * either breathe fire at close range or shoot an explosive fireball at longer range.
 */
public class DragonRetaliateAttackGoal extends Goal {
    private final DragonEntity dragon;
    private LivingEntity target;
    private int attackCooldown = 0;

    public DragonRetaliateAttackGoal(DragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.dragon.level().isClientSide) return false;
        if (!this.dragon.isAlive()) return false;
        if (this.dragon.isSitting()) return false; // don't retaliate while sitting

        LivingEntity t = this.dragon.getTarget();
        if (t == null || !t.isAlive()) return false;

        // Safety: don't retaliate against the owner
        if (t instanceof net.minecraft.world.entity.player.Player) {
            java.util.Optional<java.util.UUID> ownerUuid = this.dragon.getOwnerUUID();
            if (ownerUuid.isPresent() && ownerUuid.get().equals(((net.minecraft.world.entity.player.Player) t).getUUID())) {
                return false;
            }
        }

        this.target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null || !this.target.isAlive()) return false;
        if (this.dragon.isSitting()) return false;
        LivingEntity cur = this.dragon.getTarget();
        return cur != null && cur.equals(this.target);
    }

    @Override
    public void start() {
        this.attackCooldown = 0;
    }

    @Override
    public void stop() {
        this.target = null;
        this.dragon.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) return;

        // Look at target
        this.dragon.getLookControl().setLookAt(this.target, 30.0F, (float)this.dragon.getMaxHeadXRot());

        double dx = this.target.getX() - this.dragon.getX();
        double dy = this.target.getY(0.5) - this.dragon.getY(0.5);
        double dz = this.target.getZ() - this.dragon.getZ();
        double distSq = dx*dx + dy*dy + dz*dz;

        // Cooldown between attacks (safety)
        if (--this.attackCooldown > 0) return;

        double breathRange = DragonConstants.FIRE_BREATH_RANGE; // close range
        double breathRangeSq = breathRange * breathRange;

        if (distSq <= breathRangeSq) {
            // Breathe fire at close range
            if (this.dragon.canBreatheFireOnGround()) {
                // Compute direction and use breatheFireInDirection for proper spawn position
                net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(dx, dy, dz).normalize();
                // Align rotation quickly
                float targetYaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F;
                float horizontal = (float) Math.sqrt(dir.x * dir.x + dir.z * dir.z);
                float targetPitch = (float) (-(Math.atan2(dir.y, horizontal) * (180.0 / Math.PI)));
                this.dragon.setYRot(targetYaw);
                this.dragon.setXRot(targetPitch);

                this.dragon.breatheFireInDirection(dir);
                this.dragon.startFireBreathCooldown();
                this.attackCooldown = DragonConstants.CHECK_INTERVAL_TICKS; // wait one check interval
            }
        } else {
            // Use fireball at longer range if possible
            if (this.dragon.canShootFireball()) {
                this.dragon.shootExplosiveFireball(this.target);
                this.dragon.setFireballCooldown(DragonConstants.FIREBALL_COOLDOWN_TICKS);
                this.attackCooldown = DragonConstants.CHECK_INTERVAL_TICKS; // small delay
            }
        }
    }
}

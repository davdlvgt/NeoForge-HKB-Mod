package de.davidvogt.hkbmod.item.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Custom explosive fireball that creates a TNT-like explosion on impact.
 * Similar to Ghast fireballs but specifically designed for dragon attacks.
 */
public class ExplosiveFireballEntity extends AbstractHurtingProjectile {

    public ExplosiveFireballEntity(EntityType<? extends AbstractHurtingProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public ExplosiveFireballEntity(Level level, LivingEntity shooter, double xPower, double yPower, double zPower) {
        super(EntityType.FIREBALL, level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getY(0.5), shooter.getZ());

        // Normalize and scale the direction vector
        double length = Math.sqrt(xPower * xPower + yPower * yPower + zPower * zPower);
        if (length > 0) {
            xPower = xPower / length;
            yPower = yPower / length;
            zPower = zPower / length;
        }

        // Set the velocity for the fireball
        double speed = 0.5; // Speed multiplier
        this.setDeltaMovement(xPower * speed, yPower * speed, zPower * speed);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (!this.level().isClientSide) {
            // Create explosion at impact point
            this.level().explode(
                this,
                this.getX(),
                this.getY(),
                this.getZ(),
                DragonConstants.EXPLOSION_POWER,
                Level.ExplosionInteraction.MOB
            );

            // Remove the fireball after explosion
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (!this.level().isClientSide) {
            // Deal direct damage to the entity hit
            result.getEntity().hurt(this.damageSources().mobProjectile(this, this.getOwner() instanceof LivingEntity le ? le : null), DragonConstants.FIREBALL_DIRECT_DAMAGE);
        }
    }

    @Override
    public boolean isOnFire() {
        return false; // Don't render as on fire
    }

    @Override
    protected boolean shouldBurn() {
        return false; // Fireballs shouldn't burn
    }
}

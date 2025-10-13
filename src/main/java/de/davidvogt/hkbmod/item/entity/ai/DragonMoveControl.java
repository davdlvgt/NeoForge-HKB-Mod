package de.davidvogt.hkbmod.item.entity.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.Mob;

/**
 * Custom move control for the dragon that doesn't interfere with rotation.
 * This allows the dragon to face its movement direction naturally.
 */
public class DragonMoveControl extends MoveControl {

    public DragonMoveControl(Mob mob) {
        super(mob);
    }

    @Override
    public void tick() {
        if (this.operation == MoveControl.Operation.MOVE_TO) {
            // Calculate direction to target
            double dx = this.wantedX - this.mob.getX();
            double dy = this.wantedY - this.mob.getY();
            double dz = this.wantedZ - this.mob.getZ();

            double distanceSq = dx * dx + dy * dy + dz * dz;

            if (distanceSq < 2.5E-7) {
                // Very close to target, stop
                this.mob.setZza(0.0F);
                return;
            }

            double distance = Math.sqrt(distanceSq);

            // Normalize direction
            dx = dx / distance;
            dy = dy / distance;
            dz = dz / distance;

            // Apply movement speed
            double speed = this.speedModifier * this.mob.getAttributeValue(Attributes.FLYING_SPEED);

            // Set velocity directly (we handle rotation separately in tick())
            this.mob.setDeltaMovement(
                this.mob.getDeltaMovement().add(
                    dx * speed * 0.1,
                    dy * speed * 0.1,
                    dz * speed * 0.1
                )
            );

            this.operation = MoveControl.Operation.WAIT;
        } else {
            // Not moving to target, just maintain current velocity with slight decay
            this.mob.setZza(0.0F);
        }
    }
}


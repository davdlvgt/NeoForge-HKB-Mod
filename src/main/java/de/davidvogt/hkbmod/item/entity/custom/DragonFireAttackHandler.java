package de.davidvogt.hkbmod.item.entity.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles all fire-based attacks for the dragon, including fire breath and fireball shooting.
 * This consolidates fire logic that was duplicated across DragonEntity and AI goals.
 */
public class DragonFireAttackHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonFireAttackHandler.class);

    private final DragonEntity dragon;
    private final Level level;

    public DragonFireAttackHandler(DragonEntity dragon) {
        this.dragon = dragon;
        this.level = dragon.level();
    }

    /**
     * Shoots an explosive fireball at a target entity
     */
    public void shootFireballAtTarget(LivingEntity target) {
        if (level.isClientSide) {
            return;
        }

        Vec3 direction = calculateDirectionToTarget(target);
        shootFireballInDirection(direction);
    }

    /**
     * Shoots a fireball in a specific direction
     */
    public void shootFireballInDirection(Vec3 direction) {
        if (level.isClientSide) {
            LOGGER.warn("shootFireballInDirection called on client side!");
            return;
        }

        Vec3 normalizedDir = direction.normalize();

        // Create fireball
        ExplosiveFireballEntity fireball = new ExplosiveFireballEntity(
            level,
            dragon,
            normalizedDir.x,
            normalizedDir.y,
            normalizedDir.z
        );

        // Position in front of dragon's mouth
        Vec3 spawnPos = calculateMouthPosition();
        fireball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        // Add to world and play sound
        level.addFreshEntity(fireball);
        dragon.playSound(SoundEvents.GHAST_SHOOT, 1.0F, 1.0F);

        LOGGER.debug("Dragon shot fireball in direction: {}", normalizedDir);
    }

    /**
     * Breathes fire at a target entity (close range attack)
     */
    public void breatheFireAtTarget(LivingEntity target) {
        if (level.isClientSide) {
            return;
        }

        Vec3 direction = calculateDirectionToTarget(target);
        breatheFireInDirection(direction);
    }

    /**
     * Breathes fire in a specific direction (cone-shaped attack)
     */
    public void breatheFireInDirection(Vec3 direction) {
        if (level.isClientSide) {
            LOGGER.warn("breatheFireInDirection called on client side!");
            return;
        }

        Vec3 normalizedDir = direction.normalize();
        Vec3 startPos = calculateMouthPosition();

        int particlesSpawned = 0;
        int entitiesHit = 0;

        // Create cone of fire
        for (double distance = 1.0; distance <= DragonConstants.FIRE_BREATH_RANGE; distance += 0.5) {
            // Create cone by checking offset positions
            for (double offset = -0.5; offset <= 0.5; offset += 0.25) {
                Vec3 checkPos = calculateConePosition(normalizedDir, startPos, distance, offset);

                // Spawn particles
                spawnFireParticles(checkPos, distance);
                particlesSpawned++;

                // Damage entities
                entitiesHit += damageEntitiesInArea(checkPos);
            }
        }

        // Play sound
        dragon.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 1.5F, 0.8F);

        LOGGER.debug("Dragon breathed fire: {} particle groups, {} entities hit", particlesSpawned, entitiesHit);
    }

    /**
     * Calculates direction vector from dragon to target
     */
    private Vec3 calculateDirectionToTarget(LivingEntity target) {
        double dx = target.getX() - dragon.getX();
        double dy = target.getY(0.5) - dragon.getY(0.5);
        double dz = target.getZ() - dragon.getZ();
        return new Vec3(dx, dy, dz);
    }

    /**
     * Calculates the position of the dragon's mouth for attack spawning
     */
    private Vec3 calculateMouthPosition() {
        Vec3 lookVec = dragon.getViewVector(1.0F);
        double spawnDistance = 2.0;
        return new Vec3(
            dragon.getX() + lookVec.x * spawnDistance,
            dragon.getY(0.5) + 0.5,
            dragon.getZ() + lookVec.z * spawnDistance
        );
    }

    /**
     * Calculates a position in the fire breath cone
     */
    private Vec3 calculateConePosition(Vec3 direction, Vec3 startPos, double distance, double offset) {
        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x).normalize();
        return startPos.add(
            direction.x * distance + perpendicular.x * offset * distance * 0.3,
            direction.y * distance,
            direction.z * distance + perpendicular.z * offset * distance * 0.3
        );
    }

    /**
     * Spawns fire particles at a position
     */
    private void spawnFireParticles(Vec3 position, double distance) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Flame particles
        serverLevel.sendParticles(
            net.minecraft.core.particles.ParticleTypes.FLAME,
            position.x, position.y, position.z,
            5,
            0.2, 0.2, 0.2,
            0.03
        );

        // Smoke particles (further out only)
        if (distance > 2.0 && dragon.getRandom().nextFloat() < 0.5F) {
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                position.x, position.y, position.z,
                3,
                0.25, 0.25, 0.25,
                0.02
            );
        }
    }

    /**
     * Damages entities in a small area around a position
     * @return number of entities hit
     */
    private int damageEntitiesInArea(Vec3 position) {
        AABB damageBox = new AABB(
            position.x - 0.6, position.y - 0.6, position.z - 0.6,
            position.x + 0.6, position.y + 0.6, position.z + 0.6
        );

        List<LivingEntity> entities = level.getEntitiesOfClass(
            LivingEntity.class,
            damageBox,
            entity -> entity != dragon
                && entity.isAlive()
                && !entity.equals(dragon.getControllingPassenger())
        );

        for (LivingEntity entity : entities) {
            entity.hurt(dragon.damageSources().mobAttack(dragon), DragonConstants.FIRE_BREATH_DAMAGE);
            entity.setRemainingFireTicks(DragonConstants.FIRE_DURATION_TICKS);
        }

        return entities.size();
    }
}

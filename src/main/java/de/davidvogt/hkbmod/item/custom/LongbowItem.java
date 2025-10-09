package de.davidvogt.hkbmod.item.custom;

import de.davidvogt.hkbmod.HKBMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class LongbowItem extends BowItem {
    private final Predicate<ItemStack> supportedProjectiles;

    // Longbow configuration - stronger than vanilla bow
    private static final float VELOCITY_MULTIPLIER = 1.35F; // 35% more velocity than vanilla bow (3.0 base -> 4.05)
    private static final float DAMAGE_BONUS = 2.5F; // +2.5 extra damage
    private static final int DRAW_TIME = 15; // Slightly slower than vanilla (20 ticks) - balanced for power
    private static final float BASE_DAMAGE = 2.0F;

    public LongbowItem(Properties properties, Predicate<ItemStack> supportedProjectiles) {
        super(properties);
        this.supportedProjectiles = supportedProjectiles;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return supportedProjectiles;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000; // Max draw time
    }

    /**
     * Returns the draw time in ticks (how long to fully charge)
     * Vanilla bow uses 20 ticks, longbow uses 15 for slightly slower but more powerful shots
     */
    public static int getDrawTime() {
        return DRAW_TIME;
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {
        // Apply velocity multiplier for stronger shots
        float enhancedVelocity = velocity * VELOCITY_MULTIPLIER;
        super.shootProjectile(shooter, projectile, index, enhancedVelocity, inaccuracy, angle, target);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
        Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, isCrit);

        // Add extra damage to arrows shot from longbow
        if (projectile instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(BASE_DAMAGE + DAMAGE_BONUS);
            HKBMod.LOGGER.info("Arrow shot from longbow with enhanced damage: " + BASE_DAMAGE + DAMAGE_BONUS);
        }

        return projectile;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player)) {
            HKBMod.LOGGER.info("Entity using longbow is not a player");
            return false;
        }

        ItemStack projectileStack = player.getProjectile(stack);
        if (projectileStack.isEmpty()) {
            HKBMod.LOGGER.info("Projectile stack is empty");
            return false;
        }

        int charge = this.getUseDuration(stack, entityLiving) - timeLeft;
        float power = getPowerForTime(charge);

        // Only shoot if bow is at least minimally drawn
        if (power < 0.1F) {
            HKBMod.LOGGER.info("Bow not drawn enough to shoot (power: " + power + ")");
            return false;
        }

        List<ItemStack> projectiles = draw(stack, projectileStack, player);
        if (level instanceof ServerLevel serverLevel && !projectiles.isEmpty()) {
            HKBMod.LOGGER.info("Projectile stack has been drawn");
            this.shoot(serverLevel, player, player.getUsedItemHand(), stack, projectiles, power * 3.0F, 1.0F, power == 1.0F, null);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);

        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        return false;
    }

    /**
     * Custom power calculation for longbow
     * Uses custom draw time for balanced charging
     */
    public static float getPowerForTime(int charge) {
        float f = (float) charge / (float) DRAW_TIME;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        return f;
    }
}

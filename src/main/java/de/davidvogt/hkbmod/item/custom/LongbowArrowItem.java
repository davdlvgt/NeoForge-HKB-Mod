package de.davidvogt.hkbmod.item.custom;

import de.davidvogt.hkbmod.item.entity.custom.LongbowArrowEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class LongbowArrowItem extends ArrowItem {

    public LongbowArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, ItemStack weapon) {
        return new LongbowArrowEntity(level, shooter, ammo.copyWithCount(1), weapon);
    }
}

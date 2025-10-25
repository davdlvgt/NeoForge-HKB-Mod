package de.davidvogt.hkbmod.item.entity.custom;

import de.davidvogt.hkbmod.item.ModItems;
import de.davidvogt.hkbmod.item.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class LongbowArrowEntity extends AbstractArrow implements ItemLike {

    public LongbowArrowEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public LongbowArrowEntity(Level level, LivingEntity shooter, ItemStack pickupItemStack, ItemStack weapon) {
        super(ModEntities.LONGBOW_ARROW.get(), shooter, level, pickupItemStack, weapon);
    }

    @Override
    protected @NotNull ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.LONGBOW_ARROW.get());
    }

    @Override
    public Item asItem() {
        return ModItems.LONGBOW_ARROW.get();
    }
}

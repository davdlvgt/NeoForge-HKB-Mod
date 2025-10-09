package de.davidvogt.hkbmod.item.custom;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class LongbowItem extends BowItem {
    private final Predicate<ItemStack> supportedProjectiles;

    public LongbowItem(Properties properties, Predicate<ItemStack> supportedProjectiles) {
        super(properties);
        this.supportedProjectiles = supportedProjectiles;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return supportedProjectiles;
    }
}

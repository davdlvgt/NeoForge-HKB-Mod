package de.davidvogt.hkbmod.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TimeSetterItem extends Item {
    public TimeSetterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // Set time to 0 (dawn)
            ServerLevel serverLevel = (ServerLevel) level;
            serverLevel.setDayTime(0);

            // Send feedback to player
            player.displayClientMessage(Component.literal("Time set to dawn!"), true);

            // Damage the item by 1 durability point
            itemStack.setDamageValue(itemStack.getDamageValue() + 1);

            // Check if item should break
            if (itemStack.getDamageValue() >= itemStack.getMaxDamage()) {
                itemStack.shrink(1); // Remove the item from the stack
            }
        }

        return InteractionResult.SUCCESS;
    }
}

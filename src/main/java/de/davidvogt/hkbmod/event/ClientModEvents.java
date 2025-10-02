package de.davidvogt.hkbmod.event;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.item.custom.MagicPickaxeItem;
import de.davidvogt.hkbmod.network.SetDigSizePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = HKBMod.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        if (player.isShiftKeyDown() && player.getMainHandItem().getItem() instanceof MagicPickaxeItem) {
            int current = ModEvents.PLAYER_DIG_SIZE.getOrDefault(player.getUUID(), 1);
            double delta = event.getScrollDeltaY();
            if (delta > 0 && current < 5) current++;
            if (delta < 0 && current > 1) current--;
            ModEvents.PLAYER_DIG_SIZE.put(player.getUUID(), current);

            // Send to server
            if (Minecraft.getInstance().getConnection() != null) {
                Minecraft.getInstance().getConnection().send(new SetDigSizePacket(current));
            }

            event.setCanceled(true);
            player.displayClientMessage(Component.literal("Grabungsgröße: " + (current * 2 + 1) + "x" + (current * 2 + 1)), true);
        }
    }
}

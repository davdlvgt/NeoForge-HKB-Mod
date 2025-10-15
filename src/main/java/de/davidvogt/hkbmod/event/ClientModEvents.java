package de.davidvogt.hkbmod.event;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.item.ModItems;
import de.davidvogt.hkbmod.item.custom.MagicPickaxeItem;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import de.davidvogt.hkbmod.network.SetDigSizePacket;
import de.davidvogt.hkbmod.network.DragonBreathFirePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Check if dragon fire key is being held down (not just clicked)
        if (KeyBindings.DRAGON_FIRE_KEY.isDown()) {
            // Check if player is riding a dragon
            if (player.getVehicle() instanceof DragonEntity dragon) {
                // Send packet to server every tick while key is held
                if (Minecraft.getInstance().getConnection() != null) {
                    Minecraft.getInstance().getConnection().send(new DragonBreathFirePacket());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFovModifierEvent(ComputeFovModifierEvent event) {
        if(event.getPlayer().isUsingItem() && event.getPlayer().getUseItem().getItem() == ModItems.LONGBOW.get()) {
            float fovModifier = 1f;
            int ticksUsingItem = event.getPlayer().getTicksUsingItem();
            float deltaTicks = (float)ticksUsingItem / 20f;
            if(deltaTicks > 1f) {
                deltaTicks = 1f;
            } else {
                deltaTicks *= deltaTicks;
            }
            fovModifier *= 1f - deltaTicks * 0.15f;
            event.setNewFovModifier(fovModifier);
        }
    }
}

@EventBusSubscriber(modid = HKBMod.MODID, value = Dist.CLIENT)
class KeyMappingRegistry {
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.DRAGON_FIRE_KEY);
    }
}

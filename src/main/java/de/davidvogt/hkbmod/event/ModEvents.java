package de.davidvogt.hkbmod.event;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.item.custom.MagicPickaxeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.*;

@EventBusSubscriber(modid = HKBMod.MODID)
public class ModEvents {
    private static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();
    public static final Map<UUID, Integer> PLAYER_DIG_SIZE = new HashMap<>();

    @SubscribeEvent
    public static void onMagicPickaxeUsage(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getMainHandItem();

        if(mainHandItem.getItem() instanceof MagicPickaxeItem magicPickaxe && player instanceof ServerPlayer serverPlayer) {
            BlockPos initialBlockPos = event.getPos();
            if(HARVESTED_BLOCKS.contains(initialBlockPos)) {
                return;
            }
            int range = PLAYER_DIG_SIZE.getOrDefault(player.getUUID(), 1);
            for(BlockPos pos : MagicPickaxeItem.getBlocksToBeDestroyed(range, initialBlockPos, serverPlayer)) {
                if(pos == initialBlockPos || !magicPickaxe.isCorrectToolForDrops(mainHandItem, event.getLevel().getBlockState(pos))) {
                    continue;
                }
                HARVESTED_BLOCKS.add(pos);
                serverPlayer.gameMode.destroyBlock(pos);
                HARVESTED_BLOCKS.remove(pos);
            }
        }
    }
}

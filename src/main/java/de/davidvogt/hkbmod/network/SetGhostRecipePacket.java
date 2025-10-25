package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.screen.cutsom.ResearchCraftingTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from server to client to display ghost items in the crafting grid.
 * Ghost items show what ingredients are needed for a recipe, even if the player doesn't have them.
 * Each entry is a pair of (slot index, ItemStack).
 */
public record SetGhostRecipePacket(List<GhostSlot> ghostSlots) implements CustomPacketPayload {

    public static final Type<SetGhostRecipePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("hkbmod", "set_ghost_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetGhostRecipePacket> STREAM_CODEC = StreamCodec.composite(
            GhostSlot.STREAM_CODEC.apply(ByteBufCodecs.list()), SetGhostRecipePacket::ghostSlots,
            SetGhostRecipePacket::new
    );

    /**
     * Handle the packet on the client side
     */
    public static void handle(SetGhostRecipePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof ResearchCraftingTableScreen screen) {
                // Convert ghost slots to a list indexed by slot position
                List<ItemStack> ghostItems = new ArrayList<>();
                for (int i = 0; i < 9; i++) {
                    ghostItems.add(ItemStack.EMPTY);
                }

                for (GhostSlot ghostSlot : packet.ghostSlots()) {
                    if (ghostSlot.slotIndex() >= 0 && ghostSlot.slotIndex() < 9) {
                        ghostItems.set(ghostSlot.slotIndex(), ghostSlot.stack());
                    }
                }

                screen.setGhostRecipe(ghostItems);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Represents a ghost item in a specific slot
     */
    public record GhostSlot(int slotIndex, ItemStack stack) {
        public static final StreamCodec<RegistryFriendlyByteBuf, GhostSlot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, GhostSlot::slotIndex,
                ItemStack.STREAM_CODEC, GhostSlot::stack,
                GhostSlot::new
        );
    }
}

package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.attachment.ModAttachments;
import de.davidvogt.hkbmod.research.PlayerResearchData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Packet to sync unlocked recipes from server to client.
 * Sent when player logs in or when a new recipe is unlocked.
 */
public record SyncUnlockedRecipesPacket(List<ResourceLocation> unlockedRecipes) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncUnlockedRecipesPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "sync_unlocked_recipes"));

    public static final StreamCodec<ByteBuf, SyncUnlockedRecipesPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SyncUnlockedRecipesPacket::unlockedRecipes,
            SyncUnlockedRecipesPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Creates a packet from a set of unlocked recipes
     */
    public static SyncUnlockedRecipesPacket fromSet(Set<ResourceLocation> unlockedRecipes) {
        return new SyncUnlockedRecipesPacket(List.copyOf(unlockedRecipes));
    }

    /**
     * Handles the packet on the client side
     */
    public static void handle(SyncUnlockedRecipesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null) {
                PlayerResearchData data = context.player().getData(ModAttachments.PLAYER_RESEARCH);

                // Clear existing unlocked recipes and replace with synced data
                data.clearUnlockedRecipes();
                data.unlockRecipes(packet.unlockedRecipes());

                HKBMod.LOGGER.info("CLIENT: Synced unlocked recipes - {} recipes unlocked",
                        packet.unlockedRecipes().size());
            }
        });
    }
}

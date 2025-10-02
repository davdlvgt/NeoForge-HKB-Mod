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

import java.util.HashMap;
import java.util.Map;

public record SyncPlayerResearchPacket(Map<String, Integer> completedLevels) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPlayerResearchPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "sync_player_research"));

    public static final StreamCodec<ByteBuf, SyncPlayerResearchPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
            SyncPlayerResearchPacket::completedLevels,
            SyncPlayerResearchPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPlayerResearchPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null) {
                PlayerResearchData data = context.player().getData(ModAttachments.PLAYER_RESEARCH);
                // Clear and update with synced data
                for (Map.Entry<String, Integer> entry : packet.completedLevels().entrySet()) {
                    data.completeLevel(entry.getKey(), entry.getValue());
                }
                HKBMod.LOGGER.info("CLIENT: Synced player research data - {} classes", packet.completedLevels().size());
            }
        });
    }
}

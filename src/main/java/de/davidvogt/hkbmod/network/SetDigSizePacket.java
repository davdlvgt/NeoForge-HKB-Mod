package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.event.ModEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetDigSizePacket(int digSize) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetDigSizePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "set_dig_size"));

    public static final StreamCodec<ByteBuf, SetDigSizePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SetDigSizePacket::digSize,
            SetDigSizePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetDigSizePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // Update server-side dig size for this player
                ModEvents.PLAYER_DIG_SIZE.put(serverPlayer.getUUID(), packet.digSize());
                HKBMod.LOGGER.debug("SERVER: Set dig size for player {} to {}",
                    serverPlayer.getName().getString(), packet.digSize());
            }
        });
    }
}

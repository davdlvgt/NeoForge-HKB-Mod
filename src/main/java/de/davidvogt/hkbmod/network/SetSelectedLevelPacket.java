package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.block.entity.ResearchTableBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSelectedLevelPacket(BlockPos pos, int levelIndex, String classType) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetSelectedLevelPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "set_selected_level"));

    public static final StreamCodec<ByteBuf, SetSelectedLevelPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetSelectedLevelPacket::pos,
            ByteBufCodecs.INT,
            SetSelectedLevelPacket::levelIndex,
            ByteBufCodecs.STRING_UTF8,
            SetSelectedLevelPacket::classType,
            SetSelectedLevelPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetSelectedLevelPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            HKBMod.LOGGER.info("SERVER: Received set selected level packet: level {} class {} at {}",
                packet.levelIndex(), packet.classType(), packet.pos());
            if (context.player() instanceof ServerPlayer serverPlayer) {
                var level = serverPlayer.level();
                if (level.getBlockEntity(packet.pos()) instanceof ResearchTableBlockEntity blockEntity) {
                    blockEntity.setSelectedLevel(packet.levelIndex(), packet.classType());
                    HKBMod.LOGGER.info("SERVER: Set selected level to {} class {}", packet.levelIndex(), packet.classType());
                }
            }
        });
    }
}

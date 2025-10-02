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

public record ResearchActionPacket(BlockPos pos, Action action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResearchActionPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "research_action"));

    public static final StreamCodec<ByteBuf, ResearchActionPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ResearchActionPacket::pos,
            Action.STREAM_CODEC,
            ResearchActionPacket::action,
            ResearchActionPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            HKBMod.LOGGER.info("SERVER: Received research action packet: {} at {}", packet.action(), packet.pos());
            if (context.player() instanceof ServerPlayer serverPlayer) {
                var level = serverPlayer.level();
                if (level.getBlockEntity(packet.pos()) instanceof ResearchTableBlockEntity blockEntity) {
                    HKBMod.LOGGER.info("SERVER: Found block entity, executing action: {}", packet.action());
                    switch (packet.action()) {
                        case START -> {
                            HKBMod.LOGGER.info("SERVER: Starting research for level {} class {}",
                                blockEntity.getSelectedLevelIndex(), blockEntity.getSelectedClass());
                            blockEntity.startResearch(blockEntity.getSelectedLevelIndex(), serverPlayer);
                        }
                        case CANCEL -> {
                            HKBMod.LOGGER.info("SERVER: Canceling research");
                            blockEntity.cancelResearch();
                        }
                    }
                } else {
                    HKBMod.LOGGER.warn("SERVER: Block entity not found at {}", packet.pos());
                }
            } else {
                HKBMod.LOGGER.warn("SERVER: Player is not a ServerPlayer");
            }
        });
    }

    public enum Action {
        START(0), CANCEL(1);

        private final int id;

        Action(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Action fromId(int id) {
            return id == 0 ? START : CANCEL;
        }

        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = StreamCodec.of(
            (buf, action) -> buf.writeByte(action.getId()),
            buf -> Action.fromId(buf.readByte())
        );
    }
}

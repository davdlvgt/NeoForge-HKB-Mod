package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.item.entity.custom.DragonConstants;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client to server when the player holds the fire key.
 * Server decides the attack type based on dragon state:
 * - On ground: Continuous fire breath (max 3 seconds, then 2 seconds cooldown)
 * - Flying: Fireball projectiles (max once per 1.5 seconds)
 */
public record DragonBreathFirePacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DragonBreathFirePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "dragon_breath_fire"));

    public static final StreamCodec<ByteBuf, DragonBreathFirePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {}, // No data to write
            buf -> new DragonBreathFirePacket() // No data to read
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DragonBreathFirePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            // Check if player is riding a dragon
            Entity vehicle = serverPlayer.getVehicle();
            if (!(vehicle instanceof DragonEntity dragon)) {
                return;
            }

            // Determine attack type based on dragon state
            boolean isOnGround = dragon.isLanded() && !dragon.isFlyingMode();

            if (isOnGround) {
                // === GROUND ATTACK: Fire Breath ===
                if (dragon.canBreatheFireOnGround()) {
                    dragon.breatheFireInDirection(serverPlayer.getLookAngle());
                    dragon.incrementFireBreathDuration();

                    if (dragon.getFireBreathDuration() >= DragonConstants.FIRE_BREATH_MAX_DURATION_TICKS) {
                        dragon.startFireBreathCooldown();
                    }
                }
            } else {
                // === AIR ATTACK: Fireball ===
                if (dragon.canShootFireball()) {
                    dragon.shootFireballInDirection(serverPlayer.getLookAngle());
                    dragon.setFireballCooldown(DragonConstants.FIREBALL_COOLDOWN_TICKS);
                }
            }
        });
    }
}

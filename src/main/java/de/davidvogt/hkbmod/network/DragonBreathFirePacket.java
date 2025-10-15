package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.HKBMod;
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
            // Simple check: isLanded() && !isFlyingMode() = on ground
            boolean isOnGround = dragon.isLanded() && !dragon.isFlyingMode();

            if (isOnGround) {
                // === GROUND ATTACK: Fire Breath ===
                // Check if we can breathe fire (not on cooldown)
                if (dragon.canBreatheFireOnGround()) {
                    // Breathe fire in the direction player is looking
                    dragon.breatheFireInDirection(serverPlayer.getLookAngle());

                    // Update fire breath duration
                    dragon.incrementFireBreathDuration();

                    // Check if we've reached max duration (3 seconds = 60 ticks)
                    if (dragon.getFireBreathDuration() >= 60) {
                        // Start cooldown (2 seconds = 40 ticks)
                        dragon.startFireBreathCooldown();
                    }
                }
                // If on cooldown, do nothing (cooldown is handled in tick())
            } else {
                // === AIR ATTACK: Fireball ===
                // Check if we can shoot fireball (not on cooldown)
                if (dragon.canShootFireball()) {
                    // Shoot fireball in the direction player is looking
                    dragon.shootFireballInDirection(serverPlayer.getLookAngle());

                    // Set cooldown (1.5 seconds = 30 ticks)
                    dragon.setFireballCooldown(30);
                }
                // If on cooldown, do nothing (cooldown is handled in tick())
            }
        });
    }
}

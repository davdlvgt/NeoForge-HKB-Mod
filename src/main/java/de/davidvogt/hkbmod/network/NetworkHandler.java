package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.HKBMod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network handler for multiplayer communication.
 * Uses NeoForge 21.8+ networking system with CustomPacketPayload.
 */
public class NetworkHandler {

    public static void registerMessages() {
        HKBMod.LOGGER.info("Network handler initialized for multiplayer support in {}", HKBMod.MODID);
        HKBMod.LOGGER.info("Network system ready for client-server communication");
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(HKBMod.MODID).versioned("1.0");

        registrar.playToServer(
            ResearchActionPacket.TYPE,
            ResearchActionPacket.STREAM_CODEC,
            ResearchActionPacket::handle
        );

        registrar.playToServer(
            SetSelectedLevelPacket.TYPE,
            SetSelectedLevelPacket.STREAM_CODEC,
            SetSelectedLevelPacket::handle
        );

        registrar.playToServer(
            SetDigSizePacket.TYPE,
            SetDigSizePacket.STREAM_CODEC,
            SetDigSizePacket::handle
        );

        registrar.playToClient(
            SyncPlayerResearchPacket.TYPE,
            SyncPlayerResearchPacket.STREAM_CODEC,
            SyncPlayerResearchPacket::handle
        );

        registrar.playToClient(
            SyncResearchDataPacket.TYPE,
            SyncResearchDataPacket.STREAM_CODEC,
            SyncResearchDataPacket::handle
        );

        HKBMod.LOGGER.info("Registered network packets");
    }
}
package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.HKBMod;

/**
 * Network handler for multiplayer communication.
 * Uses NeoForge 21.8+ networking system with CustomPacketPayload.
 */
public class NetworkHandler {

    public static void registerMessages() {
        HKBMod.LOGGER.info("Network handler initialized for multiplayer support in {}", HKBMod.MODID);

        // TODO: When you need to add network packets, implement them using CustomPacketPayload
        // See NeoForge documentation: https://docs.neoforged.net/docs/networking/

        HKBMod.LOGGER.info("Network system ready for client-server communication");
    }
}
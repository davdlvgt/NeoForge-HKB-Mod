package de.davidvogt.hkbmod.attachment;

import com.mojang.serialization.Codec;
import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.research.PlayerResearchData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HKBMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerResearchData>> PLAYER_RESEARCH =
            ATTACHMENT_TYPES.register("player_research", () ->
                    AttachmentType.builder(() -> new PlayerResearchData())
                            .serialize(PlayerResearchData.MAP_CODEC)
                            .copyOnDeath()
                            .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}

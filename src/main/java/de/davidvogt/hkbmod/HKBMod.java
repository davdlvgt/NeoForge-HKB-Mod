package de.davidvogt.hkbmod;

import com.mojang.logging.LogUtils;
import de.davidvogt.hkbmod.attachment.ModAttachments;
import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.block.entity.ModBlockEntities;
import de.davidvogt.hkbmod.item.ModItems;
import de.davidvogt.hkbmod.network.NetworkHandler;
import de.davidvogt.hkbmod.network.SyncPlayerResearchPacket;
import de.davidvogt.hkbmod.network.SyncResearchDataPacket;
import de.davidvogt.hkbmod.registry.ModCreativeTabs;
import de.davidvogt.hkbmod.research.PlayerResearchData;
import de.davidvogt.hkbmod.research.ResearchManager;
import de.davidvogt.hkbmod.screen.ModMenuTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(HKBMod.MODID)
public class HKBMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "hkbmod";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public HKBMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the creative tab content event on the mod event bus
        modEventBus.addListener(this::addCreative);

        // Register network packets
        modEventBus.addListener(NetworkHandler::register);

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);

        // Register all our mod's deferred registers
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModAttachments.register(modEventBus);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        // Initialize network handler for client-server communication
        event.enqueueWork(NetworkHandler::registerMessages);

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    public void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.EMERALD_AXE.get());
            event.accept(ModItems.EMERALD_PICKAXE.get());
            event.accept(ModItems.EMERALD_SHOVEL.get());
            event.accept(ModItems.EMERALD_HOE.get());

            event.accept(ModItems.MAGIC_PICKAXE.get());
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.EMERALD_SWORD.get());
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.TEST_BLOCK.get());
            event.accept(ModBlocks.CUSTOM_TEST_BLOCK.get());
            event.accept(ModBlocks.TEST_LAMP.get());
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.RESEARCH_TABLE.get());
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");

        // Load research data from JSON files
        ResearchManager.loadResearches(event.getServer().getResourceManager());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Sync research definitions to client
            serverPlayer.connection.send(SyncResearchDataPacket.create());

            // Sync player research data when they log in
            PlayerResearchData researchData = serverPlayer.getData(ModAttachments.PLAYER_RESEARCH);
            serverPlayer.connection.send(new SyncPlayerResearchPacket(researchData.getCompletedLevels()));
            LOGGER.info("Synced research data for player {} - {} classes completed",
                serverPlayer.getName().getString(), researchData.getCompletedLevels().size());
        }
    }


}

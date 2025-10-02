package de.davidvogt.hkbmod.block.entity;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, HKBMod.MODID);

    public static final Supplier<BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE_BE =
            BLOCK_ENTITIES.register("research_table_be",
                    () -> new BlockEntityType<>(
                            ResearchTableBlockEntity::new,
                            ModBlocks.RESEARCH_TABLE.get()
                    )
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

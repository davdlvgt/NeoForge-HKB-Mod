package de.davidvogt.hkbmod.item.entity;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.item.entity.custom.LongbowArrowEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HKBMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<LongbowArrowEntity>> LONGBOW_ARROW =
            ENTITY_TYPES.register("longbow_arrow", () -> EntityType.Builder.<LongbowArrowEntity>of(
                            LongbowArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(ResourceKey.create(
                            Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "longbow_arrow"))
                    ));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}

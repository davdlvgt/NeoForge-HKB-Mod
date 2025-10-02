package de.davidvogt.hkbmod.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record Research(
        String classType,
        int level,
        String displayName,
        List<ItemRequirement> requirements,
        List<String> unlocks,
        String description,
        AdvancementInfo advancement
) {
    public static final Codec<Research> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("class").forGetter(Research::classType),
                    Codec.INT.fieldOf("level").forGetter(Research::level),
                    Codec.STRING.fieldOf("displayName").forGetter(Research::displayName),
                    ItemRequirement.CODEC.listOf().fieldOf("requirements").forGetter(Research::requirements),
                    Codec.STRING.listOf().fieldOf("unlocks").forGetter(Research::unlocks),
                    Codec.STRING.optionalFieldOf("description", "").forGetter(Research::description),
                    AdvancementInfo.CODEC.optionalFieldOf("advancement", new AdvancementInfo("", "")).forGetter(Research::advancement)
            ).apply(instance, Research::new)
    );

    public record ItemRequirement(String item, int count) {
        public static final Codec<ItemRequirement> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("item").forGetter(ItemRequirement::item),
                        Codec.INT.fieldOf("count").forGetter(ItemRequirement::count)
                ).apply(instance, ItemRequirement::new)
        );
    }

    public record AdvancementInfo(String title, String description) {
        public static final Codec<AdvancementInfo> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("title").forGetter(AdvancementInfo::title),
                        Codec.STRING.fieldOf("description").forGetter(AdvancementInfo::description)
                ).apply(instance, AdvancementInfo::new)
        );
    }
}

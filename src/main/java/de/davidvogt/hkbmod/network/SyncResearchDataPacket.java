package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.research.Research;
import de.davidvogt.hkbmod.research.ResearchManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncResearchDataPacket(Map<String, List<ResearchData>> researches) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncResearchDataPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "sync_research_data"));

    // Simplified research data structure for network transmission
    public record ResearchData(String classType, int level, String displayName,
                               List<ItemReq> requirements, List<String> unlocks, String description,
                               List<PrereqData> prerequisites) {
        public static final StreamCodec<ByteBuf, ResearchData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ResearchData::classType,
                ByteBufCodecs.VAR_INT, ResearchData::level,
                ByteBufCodecs.STRING_UTF8, ResearchData::displayName,
                ItemReq.STREAM_CODEC.apply(ByteBufCodecs.list()), ResearchData::requirements,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ResearchData::unlocks,
                ByteBufCodecs.STRING_UTF8, ResearchData::description,
                PrereqData.STREAM_CODEC.apply(ByteBufCodecs.list()), ResearchData::prerequisites,
                ResearchData::new
        );
    }

    public record ItemReq(String item, int count) {
        public static final StreamCodec<ByteBuf, ItemReq> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ItemReq::item,
                ByteBufCodecs.VAR_INT, ItemReq::count,
                ItemReq::new
        );
    }

    public record PrereqData(String classType, int level) {
        public static final StreamCodec<ByteBuf, PrereqData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, PrereqData::classType,
                ByteBufCodecs.VAR_INT, PrereqData::level,
                PrereqData::new
        );
    }

    public static final StreamCodec<ByteBuf, SyncResearchDataPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new,
                ByteBufCodecs.STRING_UTF8,
                ResearchData.STREAM_CODEC.apply(ByteBufCodecs.list())),
            SyncResearchDataPacket::researches,
            SyncResearchDataPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncResearchDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Clear existing research data on client
            ResearchManager.clearResearches();

            // Convert ResearchData back to Research and load into ResearchManager
            int totalResearches = 0;
            for (Map.Entry<String, List<ResearchData>> entry : packet.researches().entrySet()) {
                for (ResearchData data : entry.getValue()) {
                    List<Research.ItemRequirement> requirements = new ArrayList<>();
                    for (ItemReq req : data.requirements()) {
                        requirements.add(new Research.ItemRequirement(req.item(), req.count()));
                    }
                    List<Research.ResearchPrerequisite> prerequisites = new ArrayList<>();
                    for (PrereqData prereq : data.prerequisites()) {
                        prerequisites.add(new Research.ResearchPrerequisite(prereq.classType(), prereq.level()));
                    }
                  
                    Research research = new Research(
                            data.classType(),
                            data.level(),
                            data.displayName(),
                            requirements,
                            data.unlocks(),
                            data.description(),
                            null,
                            prerequisites
                    );
                    ResearchManager.addResearch(research);
                    totalResearches++;
                }
            }
            HKBMod.LOGGER.info("CLIENT: Synced {} research classes with {} total researches from server",
                    packet.researches().size(), totalResearches);
        });
    }

    public static SyncResearchDataPacket create() {
        Map<String, List<ResearchData>> data = new HashMap<>();
        for (String classType : ResearchManager.getAllClasses()) {
            List<ResearchData> researchList = new ArrayList<>();
            for (Research research : ResearchManager.getResearchForClass(classType)) {
                List<ItemReq> requirements = new ArrayList<>();
                for (Research.ItemRequirement req : research.requirements()) {
                    requirements.add(new ItemReq(req.item(), req.count()));
                }
              
                List<PrereqData> prerequisites = new ArrayList<>();
              
                for (Research.ResearchPrerequisite prereq : research.prerequisites()) {
                    prerequisites.add(new PrereqData(prereq.classType(), prereq.level()));
                }
              
                researchList.add(new ResearchData(
                        research.classType(),
                        research.level(),
                        research.displayName(),
                        requirements,
                        research.unlocks(),
                        research.description(),
                        prerequisites

                ));
            }
            data.put(classType, researchList);
        }
        return new SyncResearchDataPacket(data);
    }
}

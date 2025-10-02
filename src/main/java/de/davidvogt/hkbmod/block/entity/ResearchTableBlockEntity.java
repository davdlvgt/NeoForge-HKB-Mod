package de.davidvogt.hkbmod.block.entity;

import com.mojang.serialization.Codec;
import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.attachment.ModAttachments;
import de.davidvogt.hkbmod.network.SyncPlayerResearchPacket;
import de.davidvogt.hkbmod.research.PlayerResearchData;
import de.davidvogt.hkbmod.research.Research;
import de.davidvogt.hkbmod.research.ResearchManager;
import de.davidvogt.hkbmod.screen.cutsom.ResearchTableMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResearchTableBlockEntity extends BlockEntity implements MenuProvider {
    public final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                ItemStack stack = getStackInSlot(slot);
                HKBMod.LOGGER.info("SERVER: Research Table inventory changed - slot {}, item: {}, count: {}, researching: {}",
                    slot, stack.isEmpty() ? "EMPTY" : stack.getItem(), stack.getCount(), isResearching);
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                // Check if research should be cancelled due to missing materials
                checkResearchRequirements();
            } else if (level != null) {
                ItemStack stack = getStackInSlot(slot);
                HKBMod.LOGGER.info("CLIENT: Research Table inventory changed - slot {}, item: {}, count: {}",
                    slot, stack.isEmpty() ? "EMPTY" : stack.getItem(), stack.getCount());
            }
        }
    };

    // Research state
    private boolean isResearching = false;
    private int selectedLevelIndex = -1;
    private String selectedClass = "";
    private long researchStartTime = 0;
    private UUID researchingPlayerUUID = null;
    private static final long RESEARCH_DURATION_MS = 10000; // 10 seconds

    public ResearchTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RESEARCH_TABLE_BE.get(), pos, blockState);
    }

    public void clearContents() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        drops();
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Research Table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ResearchTableMenu(i, inventory, this);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
        output.putBoolean("IsResearching", isResearching);
        output.putInt("SelectedLevel", selectedLevelIndex);
        output.putString("SelectedClass", selectedClass);
        output.putLong("ResearchStartTime", researchStartTime);
        if (researchingPlayerUUID != null) {
            output.putString("ResearchingPlayer", researchingPlayerUUID.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
        isResearching = input.getBooleanOr("IsResearching", false);
        selectedLevelIndex = input.getIntOr("SelectedLevel", -1);
        selectedClass = input.getStringOr("SelectedClass", "");
        researchStartTime = input.getLongOr("ResearchStartTime", 0L);
        String uuidString = input.getStringOr("ResearchingPlayer", "");
        researchingPlayerUUID = !uuidString.isEmpty() ? UUID.fromString(uuidString) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        // Manually add research state to the tag for client sync
        tag.putBoolean("IsResearching", isResearching);
        tag.putInt("SelectedLevel", selectedLevelIndex);
        tag.putString("SelectedClass", selectedClass);
        tag.putLong("ResearchStartTime", researchStartTime);
        HKBMod.LOGGER.info("SERVER: Creating update tag - isResearching: {}, level: {}, class: {}",
            isResearching, selectedLevelIndex, selectedClass);
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ValueInput input) {
        loadAdditional(input);
        HKBMod.LOGGER.info("CLIENT: Received sync packet - isResearching: {}, level: {}, class: {}",
            isResearching, selectedLevelIndex, selectedClass);
    }

    // Research getters and setters
    public boolean isResearching() {
        return isResearching;
    }

    public int getSelectedLevelIndex() {
        return selectedLevelIndex;
    }

    public String getSelectedClass() {
        return selectedClass;
    }

    public long getResearchStartTime() {
        return researchStartTime;
    }

    public void setSelectedLevel(int levelIndex, String classType) {
        this.selectedLevelIndex = levelIndex;
        this.selectedClass = classType;
        setChanged();
        if (!level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void startResearch(int levelIndex, Player player) {
        HKBMod.LOGGER.info("startResearch called with level {}, current class: {}", levelIndex, selectedClass);

        // Check if player can research this level
        PlayerResearchData researchData = player.getData(ModAttachments.PLAYER_RESEARCH);
        if (!researchData.canResearch(selectedClass, levelIndex)) {
            HKBMod.LOGGER.warn("Player cannot research level {} for class {} - prerequisites not met or already completed",
                levelIndex, selectedClass);
            return;
        }

        boolean hasMaterials = hasRequiredMaterials(levelIndex);
        HKBMod.LOGGER.info("hasRequiredMaterials returned: {}", hasMaterials);

        if (hasMaterials) {
            this.isResearching = true;
            this.selectedLevelIndex = levelIndex;
            this.researchStartTime = System.currentTimeMillis();
            this.researchingPlayerUUID = player.getUUID();
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            HKBMod.LOGGER.info("Research started! isResearching: {}", isResearching);
        } else {
            HKBMod.LOGGER.warn("Cannot start research - requirements not met!");
        }
    }

    public void cancelResearch() {
        this.isResearching = false;
        this.researchStartTime = 0;
        setChanged();
        if (!level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void finishResearch() {
        this.isResearching = false;

        // Remove required items from inventory and mark research as complete
        if (!level.isClientSide() && selectedClass != null && !selectedClass.isEmpty() && researchingPlayerUUID != null) {
            Research research = ResearchManager.getResearch(selectedClass, selectedLevelIndex);
            if (research != null) {
                // Mark research as complete for player
                Player player = level.getPlayerByUUID(researchingPlayerUUID);
                if (player instanceof ServerPlayer serverPlayer) {
                    PlayerResearchData researchData = serverPlayer.getData(ModAttachments.PLAYER_RESEARCH);
                    researchData.completeLevel(selectedClass, selectedLevelIndex);
                    HKBMod.LOGGER.info("Player {} completed research level {} for class {}",
                        serverPlayer.getName().getString(), selectedLevelIndex, selectedClass);

                    // Sync to client
                    serverPlayer.connection.send(new SyncPlayerResearchPacket(researchData.getCompletedLevels()));
                }

                // Count how many of each item to remove
                Map<String, Integer> toRemove = new HashMap<>();
                for (Research.ItemRequirement req : research.requirements()) {
                    toRemove.put(req.item(), req.count());
                }

                // Remove items from inventory
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                        String itemName = itemId.toString();

                        if (toRemove.containsKey(itemName)) {
                            int amountToRemove = Math.min(toRemove.get(itemName), stack.getCount());
                            stack.shrink(amountToRemove);
                            inventory.setStackInSlot(i, stack);

                            int remaining = toRemove.get(itemName) - amountToRemove;
                            if (remaining <= 0) {
                                toRemove.remove(itemName);
                            } else {
                                toRemove.put(itemName, remaining);
                            }
                        }
                    }
                }
            }
        }

        this.researchingPlayerUUID = null;
        setChanged();
        if (!level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public float getResearchProgress() {
        if (!isResearching) return 0;
        long elapsed = System.currentTimeMillis() - researchStartTime;
        return Math.min(1.0f, elapsed / (float) RESEARCH_DURATION_MS);
    }

    private void checkResearchRequirements() {
        if (isResearching) {
            boolean hasMaterials = hasRequiredMaterials(selectedLevelIndex);
            HKBMod.LOGGER.info("Checking research requirements - has materials: {}, class: {}, level: {}",
                hasMaterials, selectedClass, selectedLevelIndex);
            if (!hasMaterials) {
                HKBMod.LOGGER.info("Canceling research - requirements not met");
                cancelResearch();
            }
        }
    }

    public boolean hasRequiredMaterials(int levelIndex) {
        // Use the selected class to find the correct research
        if (selectedClass == null || selectedClass.isEmpty()) {
            return false;
        }

        Research research = ResearchManager.getResearch(selectedClass, levelIndex);
        if (research == null) {
            return false;
        }

        // Count available items in inventory
        Map<String, Integer> availableItems = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                String itemName = itemId.toString();
                availableItems.put(itemName, availableItems.getOrDefault(itemName, 0) + stack.getCount());
            }
        }

        // Check if all requirements are met
        for (Research.ItemRequirement req : research.requirements()) {
            int available = availableItems.getOrDefault(req.item(), 0);
            if (available < req.count()) {
                return false;
            }
        }

        return true;
    }

    public static void tick(ResearchTableBlockEntity blockEntity) {
        if (blockEntity.isResearching) {
            float progress = blockEntity.getResearchProgress();

            // Sync to client every 10 ticks
            if (!blockEntity.level.isClientSide() && blockEntity.level.getGameTime() % 10 == 0) {
                blockEntity.level.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            }

            // Log every 1 second (20 ticks)
            if (blockEntity.level.getGameTime() % 20 == 0) {
                String side = blockEntity.level.isClientSide() ? "CLIENT" : "SERVER";
                HKBMod.LOGGER.info("{}: Research progress: {}% (level: {}, class: {})",
                    side,
                    String.format("%.1f", progress * 100),
                    blockEntity.selectedLevelIndex,
                    blockEntity.selectedClass);
            }

            if (progress >= 1.0f && !blockEntity.level.isClientSide()) {
                HKBMod.LOGGER.info("SERVER: Research completed!");
                blockEntity.finishResearch();
            }
        }
    }
}

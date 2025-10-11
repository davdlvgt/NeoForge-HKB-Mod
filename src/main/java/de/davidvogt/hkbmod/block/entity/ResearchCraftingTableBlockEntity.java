package de.davidvogt.hkbmod.block.entity;

import de.davidvogt.hkbmod.screen.cutsom.ResearchCraftingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Research Crafting Table.
 * This crafting table only allows crafting of custom recipes (non-vanilla recipes).
 * It has a 3x3 crafting grid and a result slot.
 */
public class ResearchCraftingTableBlockEntity extends BlockEntity implements MenuProvider {

    // 3x3 crafting grid (9 slots)
    public final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    public ResearchCraftingTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RESEARCH_CRAFTING_TABLE_BE.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hkbmod.research_crafting_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ResearchCraftingTableMenu(containerId, playerInventory, this);
    }

    // ========== Data Persistence ==========

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    // Serialisiert das Inventar direkt ins ValueOutput
        inventory.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
    }


    // ========== Network Synchronization ==========

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    // ========== Inventory Management ==========

    /**
     * Clears all items from the crafting grid
     */
    public void clearContents() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    /**
     * Drops all items from the inventory when the block is broken
     */
    public void drops() {
        SimpleContainer simpleContainer = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            simpleContainer.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, simpleContainer);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        drops();
    }

    // ========== Helper Methods ==========

    /**
     * Checks if the inventory is empty
     */
    public boolean isEmpty() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}

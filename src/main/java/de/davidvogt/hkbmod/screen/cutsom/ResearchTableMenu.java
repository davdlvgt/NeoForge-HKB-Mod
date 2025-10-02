package de.davidvogt.hkbmod.screen.cutsom;

import de.davidvogt.hkbmod.attachment.ModAttachments;
import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.block.entity.ResearchTableBlockEntity;
import de.davidvogt.hkbmod.research.PlayerResearchData;
import de.davidvogt.hkbmod.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ResearchTableMenu extends AbstractContainerMenu {
    public final ResearchTableBlockEntity blockEntity;
    private final Level level;
    private final Player player;

    public ResearchTableMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ResearchTableMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.RESEARCH_TABLE_MENU.get(), containerId);
        this.blockEntity = ((ResearchTableBlockEntity) blockEntity);
        this.level = inv.player.level();
        this.player = inv.player;

        // Add BlockEntity inventory FIRST (slots 0-8)
        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 0, 9, 17));
        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 1, 27, 17));
        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 2, 45, 17));
        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 3, 9, 35));
        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 4, 27, 35));
        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 5, 45, 35));
        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 6, 9,  53));
        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 7, 27, 53));
        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 8, 45, 53));

        // Add player inventory AFTER (slots 9-44)
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    //  9 - 17 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  18 - 44 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    private static final int TE_INVENTORY_SLOT_COUNT = 9;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = 0;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT;

    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the TE slots
        if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.RESEARCH_TABLE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 9 + l * 18, 85 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 9 + i * 18, 143));
        }
    }

    public PlayerResearchData getPlayerResearchData() {
        return player.getData(ModAttachments.PLAYER_RESEARCH);
    }
}
package de.davidvogt.hkbmod.block.entity;

import de.davidvogt.hkbmod.block.custom.GateControlBlock;
import de.davidvogt.hkbmod.gate.GateAnimator;
import de.davidvogt.hkbmod.gate.GateStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Block entity for the Gate Control Block.
 * Manages gate state (open/closed) and coordinates gate movement animation.
 */
public class GateControlBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(GateControlBlockEntity.class);

    private boolean isOpen = false;
    private boolean isMoving = false;
    private boolean wasPowered = false;
    private GateStructure structure = null;
    private int movementProgress = 0;
    private int totalMovementSteps = 0;

    public GateControlBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.GATE_CONTROL_BE.get(), pos, blockState);
    }

    /**
     * Server-side tick method
     */
    public static void tick(Level level, BlockPos pos, BlockState state, GateControlBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (!blockEntity.isMoving || blockEntity.structure == null) {
            return;
        }

        blockEntity.movementProgress++;

        // Move one block every 3 ticks (0.15 seconds per block)
        if (blockEntity.movementProgress % 3 == 0) {
            int blocksMoved = blockEntity.movementProgress / 3;

            if (blocksMoved <= blockEntity.totalMovementSteps) {
                // Perform the movement
                boolean success;
                if (blockEntity.isOpen) {
                    // Moving up (opening)
                    success = GateAnimator.moveGateBlockUp((ServerLevel) level, blockEntity.structure, blocksMoved);
                } else {
                    // Moving down (closing)
                    success = GateAnimator.moveGateBlockDown((ServerLevel) level, blockEntity.structure, blocksMoved);
                }

                if (!success) {
                    LOGGER.warn("Gate movement blocked at step {}", blocksMoved);
                    blockEntity.stopMoving();
                    return;
                }

                // Play sound every few blocks
                if (blocksMoved % 2 == 0) {
                    level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.8F);
                }
            }

            // Check if movement is complete
            if (blocksMoved >= blockEntity.totalMovementSteps) {
                blockEntity.completeMovement();
            }
        }
    }

    /**
     * Starts the gate movement animation
     */
    public void startMoving() {
        if (this.structure == null) {
            LOGGER.warn("Cannot start movement - no structure detected");
            return;
        }

        this.isMoving = true;
        this.movementProgress = 0;
        this.totalMovementSteps = this.structure.getMaxTravel();
        this.setChanged();

        LOGGER.info("Starting gate movement - {} steps, direction: {}",
                totalMovementSteps, isOpen ? "UP" : "DOWN");
    }

    /**
     * Completes the movement and resets state
     */
    private void completeMovement() {
        this.isMoving = false;
        this.movementProgress = 0;
        this.setChanged();

        // Update visual state
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.setBlock(worldPosition, state.setValue(GateControlBlock.POWERED, false), 3);

            // Play completion sound
            level.playSound(null, worldPosition, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        LOGGER.info("Gate movement complete - gate is now {}", isOpen ? "OPEN" : "CLOSED");
    }

    /**
     * Stops movement (e.g., if blocked)
     */
    public void stopMoving() {
        this.isMoving = false;
        this.movementProgress = 0;
        this.setChanged();

        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.setBlock(worldPosition, state.setValue(GateControlBlock.POWERED, false), 3);
        }

        LOGGER.warn("Gate movement stopped unexpectedly");
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
        this.setChanged();
    }

    public boolean isMoving() {
        return isMoving;
    }

    public GateStructure getStructure() {
        return structure;
    }

    public void setStructure(GateStructure structure) {
        this.structure = structure;
        this.setChanged();
    }

    public boolean wasPowered() {
        return wasPowered;
    }

    public void setPowered(boolean powered) {
        // Nur aktualisieren wenn sich der Wert ändert, um Endlosschleifen zu vermeiden
        if (this.wasPowered != powered) {
            this.wasPowered = powered;
            // NICHT setChanged() aufrufen, um Block-Updates zu vermeiden!
            // Die Daten werden beim nächsten regulären Save gespeichert
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("IsOpen", isOpen);
        output.putBoolean("IsMoving", isMoving);
        output.putBoolean("WasPowered", wasPowered);
        output.putInt("MovementProgress", movementProgress);
        output.putInt("TotalMovementSteps", totalMovementSteps);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isOpen = input.getBooleanOr("IsOpen", false);
        isMoving = input.getBooleanOr("IsMoving", false);
        wasPowered = input.getBooleanOr("WasPowered", false);
        movementProgress = input.getIntOr("MovementProgress", 0);
        totalMovementSteps = input.getIntOr("TotalMovementSteps", 0);
    }
}

# Gate System Documentation

## Overview

The gate system is a multi-block structure that simulates a vertically sliding gate mechanism in Minecraft. The gate can be opened (raised) or closed (lowered) through player interaction or redstone signals, with smooth animated movement.

**Key Features:**
- Rectangular gate shapes of any size
- Smooth animated movement (one block per 0.15 seconds)
- Redstone-compatible control system
- Automatic structure detection
- Collision detection to prevent damage

## Components

### 1. Gate Block ([`GateBlock.java`](src/main/java/de/davidvogt/hkbmod/block/custom/GateBlock.java))

The main gate material that moves up and down.

**Properties:**
- Thin block (2 pixels thick, like iron bars) - see [`GateBlock.java:36-37`](src/main/java/de/davidvogt/hkbmod/block/custom/GateBlock.java#L36-L37)
- Can be arranged in any rectangular shape
- Has `OPEN` state (raised/lowered) - [`GateBlock.java:30`](src/main/java/de/davidvogt/hkbmod/block/custom/GateBlock.java#L30)
- Has `FACING` property for orientation - [`GateBlock.java:33`](src/main/java/de/davidvogt/hkbmod/block/custom/GateBlock.java#L33)

**Behavior:**
- Automatically aligns with adjacent gate slide blocks - [`GateBlock.java:78-93`](src/main/java/de/davidvogt/hkbmod/block/custom/GateBlock.java#L78-L93)
- Different collision shapes based on orientation - [`GateBlock.java:98-104`](src/main/java/de/davidvogt/hkbmod/block/custom/GateBlock.java#L98-L104)

### 2. Gate Slide Block ([`GateSlideBlock.java`](src/main/java/de/davidvogt/hkbmod/block/custom/GateSlideBlock.java))

Rail/guide blocks that the gate slides along.

**Properties:**
- Must be placed on both left and right sides of the gate
- Defines the maximum height the gate can travel
- Rotatable based on player facing direction - [`GateSlideBlock.java:39-41`](src/main/java/de/davidvogt/hkbmod/block/custom/GateSlideBlock.java#L39-L41)

**Purpose:**
- Acts as visual guide rails
- Used for structure detection to calculate gate boundaries
- Determines maximum travel distance

### 3. Gate Control Block ([`GateControlBlock.java`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java))

Special control block that activates the gate mechanism.

**Properties:**
- Can be placed anywhere in the slide column
- Has `POWERED` state for visual feedback - [`GateControlBlock.java:39`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L39)
- Supports both player interaction and redstone control

**Interactions:**
- **Right-click**: Toggles gate open/closed - [`GateControlBlock.java:76-101`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L76-L101)
- **Redstone**: Activates on rising edge (power on) - [`GateControlBlock.java:137-160`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L137-L160)
- Prevents activation while gate is already moving - [`GateControlBlock.java:92-95`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L92-L95)

## Structure Detection

### GateDetector ([`GateDetector.java`](src/main/java/de/davidvogt/hkbmod/gate/GateDetector.java))

The structure detection system automatically identifies and validates gate structures.

**Detection Process:**

1. **Find First Gate Block** - [`GateDetector.java:92-101`](src/main/java/de/davidvogt/hkbmod/gate/GateDetector.java#L92-L101)
   - Scans horizontally from control block
   - Looks for adjacent gate blocks

2. **Determine Control Position** - [`GateDetector.java:106-117`](src/main/java/de/davidvogt/hkbmod/gate/GateDetector.java#L106-L117)
   - Identifies if control is on left or right side
   - Uses position comparison to determine orientation

3. **Flood Fill Algorithm** - [`GateDetector.java:122-150`](src/main/java/de/davidvogt/hkbmod/gate/GateDetector.java#L122-L150)
   - Finds all connected gate blocks in all 6 directions
   - Handles multi-layer gates
   - Uses queue-based breadth-first search

4. **Calculate Gate Bounds** - [`GateDetector.java:155-173`](src/main/java/de/davidvogt/hkbmod/gate/GateDetector.java#L155-L173)
   - Determines width and height of gate structure
   - Calculates minimum and maximum coordinates

5. **Find Slide Columns** - [`GateDetector.java:178-211`](src/main/java/de/davidvogt/hkbmod/gate/GateDetector.java#L178-L211)
   - Locates slide blocks on both left and right sides
   - Scans vertically from gate bottom to top
   - Accepts both `GateSlideBlock` and `GateControlBlock`

6. **Calculate Max Travel** - [`GateDetector.java:216-221`](src/main/java/de/davidvogt/hkbmod/gate/GateDetector.java#L216-L221)
   - Formula: `min(leftSlideHeight, rightSlideHeight) - gateHeight`
   - Ensures gate can't travel higher than available slide space

### GateStructure ([`GateStructure.java`](src/main/java/de/davidvogt/hkbmod/gate/GateStructure.java))

Data class that stores all detected structure information.

**Stored Data:**
- List of all gate block positions - [`GateStructure.java:13`](src/main/java/de/davidvogt/hkbmod/gate/GateStructure.java#L13)
- Left and right slide block positions - [`GateStructure.java:14-15`](src/main/java/de/davidvogt/hkbmod/gate/GateStructure.java#L14-L15)
- Control block position - [`GateStructure.java:16`](src/main/java/de/davidvogt/hkbmod/gate/GateStructure.java#L16)
- Maximum travel distance - [`GateStructure.java:17`](src/main/java/de/davidvogt/hkbmod/gate/GateStructure.java#L17)
- Gate dimensions (width × height) - [`GateStructure.java:18-19`](src/main/java/de/davidvogt/hkbmod/gate/GateStructure.java#L18-L19)

**Validation** - [`GateStructure.java:68-73`](src/main/java/de/davidvogt/hkbmod/gate/GateStructure.java#L68-L73):
- Must have gate blocks, slide blocks on both sides
- Must have positive maximum travel distance

## Animation System

### GateAnimator ([`GateAnimator.java`](src/main/java/de/davidvogt/hkbmod/gate/GateAnimator.java))

Handles the smooth animated movement of gate blocks.

**Movement Strategy:**

Each animation step moves ALL gate blocks by exactly one block position.

#### Opening (Moving Up) - [`GateAnimator.java:29-70`](src/main/java/de/davidvogt/hkbmod/gate/GateAnimator.java#L29-L70)

1. Sort blocks by Y coordinate (bottom to top)
2. Process from TOP to BOTTOM to avoid overwriting
3. For each block:
   - Calculate current position: `originalPos + (currentStep - 1)`
   - Calculate new position: `originalPos + currentStep`
   - Check if target position is clear
   - Move block and set `OPEN` state to `true`
   - Replace old position with air

#### Closing (Moving Down) - [`GateAnimator.java:81-127`](src/main/java/de/davidvogt/hkbmod/gate/GateAnimator.java#L81-L127)

1. Sort blocks by Y coordinate (bottom to top)
2. Process from BOTTOM to TOP to avoid overwriting
3. Calculate offset countdown:
   - `currentOffset = maxTravel - currentStep + 1`
   - `newOffset = maxTravel - currentStep`
4. Move blocks down one position per step
5. Set `OPEN` state to `false` when reaching original position

**Collision Detection:**
- Checks target position before each movement - [`GateAnimator.java:58-62`](src/main/java/de/davidvogt/hkbmod/gate/GateAnimator.java#L58-L62)
- Returns `false` if blocked, stopping animation
- Allows gate blocks to pass through each other during movement

### GateControlBlockEntity ([`GateControlBlockEntity.java`](src/main/java/de/davidvogt/hkbmod/block/entity/GateControlBlockEntity.java))

Block entity that manages gate state and coordinates animation.

**State Management:**
- `isOpen` - Current gate state (open/closed) - [`GateControlBlockEntity.java:25`](src/main/java/de/davidvogt/hkbmod/block/entity/GateControlBlockEntity.java#L25)
- `isMoving` - Whether gate is currently animating - [`GateControlBlockEntity.java:26`](src/main/java/de/davidvogt/hkbmod/block/entity/GateControlBlockEntity.java#L26)
- `structure` - Cached structure data - [`GateControlBlockEntity.java:27`](src/main/java/de/davidvogt/hkbmod/block/entity/GateControlBlockEntity.java#L27)
- `movementProgress` - Current animation tick - [`GateControlBlockEntity.java:28`](src/main/java/de/davidvogt/hkbmod/block/entity/GateControlBlockEntity.java#L28)
- `totalMovementSteps` - Total blocks to move - [`GateControlBlockEntity.java:29`](src/main/java/de/davidvogt/hkbmod/block/entity/GateControlBlockEntity.java#L29)

**Tick System** - [`GateControlBlockEntity.java:38-81`](src/main/java/de/davidvogt/hkbmod/block/entity/GateControlBlockEntity.java#L38-L81):
- Runs only on server side
- Updates every game tick (20 ticks/second)
- Moves one block every 3 ticks (0.15 seconds per block)
- Plays piston sound every 2 blocks - [`GateControlBlockEntity.java:71-73`](src/main/java/de/davidvogt/hkbmod/block/entity/GateControlBlockEntity.java#L71-L73)

**Data Persistence** - [`GateControlBlockEntity.java:160-176`](src/main/java/de/davidvogt/hkbmod/block/entity/GateControlBlockEntity.java#L160-L176):
- Saves gate state (`isOpen`, `isMoving`, progress)
- Structure is NOT saved (re-detected on first use after reload)

## User Interaction Flow

### Manual Activation (Right-Click)

1. Player right-clicks Gate Control Block
2. Check if gate is already moving - [`GateControlBlock.java:92-95`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L92-L95)
3. Call `toggleGate()` - [`GateControlBlock.java:98`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L98)
4. Detect or retrieve structure - [`GateControlBlock.java:167-187`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L167-L187)
5. Toggle `isOpen` state - [`GateControlBlock.java:190`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L190)
6. Start movement animation - [`GateControlBlock.java:191`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L191)
7. Set visual `POWERED` state - [`GateControlBlock.java:194`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L194)
8. Play opening sound - [`GateControlBlock.java:197`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L197)

### Redstone Activation

1. Redstone signal changes at neighbor block - [`GateControlBlock.java:137-160`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L137-L160)
2. Check if receiving power (rising edge only) - [`GateControlBlock.java:153`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L153)
3. If powered, call `toggleGate()` - [`GateControlBlock.java:156-159`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L156-L159)
4. Same process as manual activation

**Note:** Only rising edge (power on) triggers the gate, preventing rapid toggling.

## Building a Gate

### Structure Requirements

1. **Gate Blocks**: Arrange in any rectangular shape
2. **Slide Blocks**: Place vertically on both left and right sides of the gate
   - Must extend above the gate to allow upward travel
   - Height determines maximum gate travel distance
3. **Control Block**: Place anywhere in either slide column

### Example Configuration

```
    [S] [S]      S = Gate Slide Block
    [S] [S]      C = Gate Control Block
    [C] [S]      G = Gate Block
    [G] [G]
    [G] [G]
```

**Gate Dimensions**: 2 blocks wide × 2 blocks tall
**Slide Height**: 5 blocks
**Max Travel**: 5 - 2 = 3 blocks

## Technical Details

### File Structure

```
src/main/java/de/davidvogt/hkbmod/
├── block/
│   ├── custom/
│   │   ├── GateBlock.java           # Main gate material
│   │   ├── GateSlideBlock.java      # Guide rails
│   │   └── GateControlBlock.java    # Control mechanism
│   └── entity/
│       └── GateControlBlockEntity.java  # State management
└── gate/
    ├── GateStructure.java           # Data container
    ├── GateDetector.java            # Structure detection
    └── GateAnimator.java            # Movement animation
```

### Performance Considerations

- Structure detection runs only once (cached in block entity)
- Animation runs at 3.33 blocks/second (3 ticks per block)
- Flood fill algorithm is O(n) where n = number of connected gate blocks
- Movement sorting is O(n log n) per animation step

### Limitations

- Gate must be rectangular (flood fill finds all connected blocks)
- Maximum travel limited by slide block height
- Cannot move if path is obstructed
- Structure not saved between world reloads (re-detected on first use)

## Sound Effects

- **Gate Start**: `IRON_DOOR_OPEN` - [`GateControlBlock.java:197`](src/main/java/de/davidvogt/hkbmod/block/custom/GateControlBlock.java#L197)
- **Movement**: `PISTON_EXTEND` (every 2 blocks) - [`GateControlBlockEntity.java:72`](src/main/java/de/davidvogt/hkbmod/block/entity/GateControlBlockEntity.java#L72)
- **Gate Complete**: `IRON_DOOR_CLOSE` - [`GateControlBlockEntity.java:115`](src/main/java/de/davidvogt/hkbmod/block/entity/GateControlBlockEntity.java#L115)

## Future Enhancements

Potential improvements:
- Configurable animation speed
- Horizontal sliding gates
- Multiple control blocks per gate
- Partial open/close positions
- Instant movement config option (already implemented - [`GateAnimator.java:132-162`](src/main/java/de/davidvogt/hkbmod/gate/GateAnimator.java#L132-L162))

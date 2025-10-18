# Dragon Code Refactoring Plan

## Completed
✅ Created `DragonConstants.java` - Central configuration for all magic numbers
✅ Created `DragonFireAttackHandler.java` - Consolidated fire attack logic
✅ Created `DragonStateManager.java` - State management consolidation

## Next Steps (Manual)

### 1. Update DragonEntity.java
Replace scattered state management with DragonStateManager:
```java
// Add at top
private final DragonStateManager stateManager = new DragonStateManager(this);
private final DragonFireAttackHandler fireHandler = new DragonFireAttackHandler(this);
private static final Logger LOGGER = LoggerFactory.getLogger(DragonEntity.class);
```

**State methods to delegate:**
- All `isLanded()`, `setLanded()` → `stateManager.isLanded()`, `stateManager.setLanded()`
- All timer methods → `stateManager.get/setXTimer()`
- All fire methods → `fireHandler.shootFireballX()`, `fireHandler.breatheFireX()`

**Replace System.out.println with LOGGER:**
- `System.out.println("[DRAGON]...")` → `LOGGER.debug(...)`
- Remove verbose position logging
- Keep only important state changes

**Use DragonConstants:**
Replace all magic numbers with constants:
- `100.0D` → `DragonConstants.MAX_HEALTH`
- `60` (taming timer) → `DragonConstants.TAMING_WINDOW_TICKS`
- etc.

### 2. Update DragonFlyingGoal.java
**Refactoring needed:**
- Extract `checkForStuck()` logic into separate `DragonCollisionHandler` class
- Use DragonConstants for all magic numbers
- Replace System.out.println with LOGGER
- Simplify `tick()` method - currently 850 lines!
- Extract landing logic into separate methods

**Suggested structure:**
```java
private void tick() {
    if (dragon.isLanded()) {
        handleLandedBehavior();
    } else if (isLandingMode) {
        handleLandingApproach();
    } else if (isPerformingEmergencyManeuver) {
        performEmergencyManeuver();
    } else {
        handleNormalFlight();
    }
}
```

### 3. Update DragonRestGoal.java
- Use DragonConstants for all magic numbers
- Replace System.out.println with LOGGER
- Simplify nest finding logic

### 4. Update DefendNestGoal.java
- Use DragonConstants
- Replace System.out.println with LOGGER
- Consider delegating fire attacks to DragonFireAttackHandler

### 5. Update FollowOwnerGoal.java
- Use DragonConstants
- Replace System.out.println with LOGGER

### 6. Update ReturnToNestGoal.java
- Use DragonConstants
- Replace System.out.println with LOGGER

### 7. Update ExplosiveFireballEntity.java
- Use DragonConstants.EXPLOSION_POWER
- Use DragonConstants.FIREBALL_DIRECT_DAMAGE

### 8. Update DragonBreathFirePacket.java
- Delegate to DragonFireAttackHandler instead of calling dragon methods directly

## Code Quality Improvements

### Remove German Comments
Replace all German comments with English:
- "Timer für die 3-Sekunden-Zeitspanne" → "Timer for 3-second window"
- "Stabile Boden-Erkennung" → "Stable ground detection"
- etc.

### Consistent Formatting
- Remove excessive blank lines
- Consistent indentation
- Group related methods together

### Documentation
Add proper Javadoc to:
- All public methods in DragonEntity
- All AI goal classes
- Complex algorithms (especially in DragonFlyingGoal)

## Testing Checklist

After refactoring, test:
- [ ] Wild dragon spawning and flying behavior
- [ ] Dragon landing and resting on nest
- [ ] Dragon defending nest (both ground and air attacks)
- [ ] Dragon returning to nest when far away
- [ ] Taming with enchanted golden apple + dragon saddle
- [ ] Sitting/standing commands
- [ ] Following owner when not sitting
- [ ] Riding dragon (ground walking)
- [ ] Riding dragon (flying with space bar)
- [ ] Riding dragon (fire attacks)
- [ ] Dragon death and loot drops

## Performance Considerations

Current performance issues:
1. Too many System.out.println calls (use LOGGER.debug instead)
2. Distance calculations every tick (cache or check less frequently)
3. BlockPos searches could be optimized with cached regions

## Architecture Benefits

After refactoring:
- **Single Responsibility**: Each class has one clear purpose
- **DRY**: No duplicated fire logic
- **Maintainability**: Magic numbers in one place
- **Testability**: State manager can be mocked
- **Performance**: Less debug output, better logging control
- **Readability**: Shorter methods, clear structure

/**
 * EXAMPLE: Refactored DragonEntity.java (Partial - showing key patterns)
 *
 * This demonstrates how to integrate the new utility classes.
 * Apply these same patterns throughout the full DragonEntity.java
 */

package de.davidvogt.hkbmod.item.entity.custom;

// ... imports ...
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DragonEntity extends Monster {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonEntity.class);

    // === MANAGERS ===
    private final DragonStateManager stateManager;
    private final DragonFireAttackHandler fireHandler;
    private final DragonFlightHistory flightHistory = new DragonFlightHistory();

    public DragonEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.stateManager = new DragonStateManager(this);
        this.fireHandler = new DragonFireAttackHandler(this);
        this.setNoGravity(true);
        this.moveControl = new DragonMoveControl(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        DragonStateManager.defineSynchedData(builder);
    }

    // === STATE DELEGATION (EXAMPLE) ===
    public boolean isLanded() {
        return stateManager.isLanded();
    }

    public void setLanded(boolean landed) {
        stateManager.setLanded(landed);
    }

    public boolean isTamed() {
        return stateManager.isTamed();
    }

    public void setTamed(boolean tamed) {
        stateManager.setTamed(tamed);
    }

    // ... all other state methods delegate to stateManager ...

    // === TIMER DELEGATION (EXAMPLE) ===
    public int getLandedTimer() {
        return stateManager.getLandedTimer();
    }

    public void setLandedTimer(int timer) {
        stateManager.setLandedTimer(timer);
    }

    // ... all other timer methods delegate to stateManager ...

    // === NEST DELEGATION (EXAMPLE) ===
    public BlockPos getNestPosition() {
        return stateManager.getNestPosition();
    }

    public void setNestPosition(BlockPos pos) {
        stateManager.setNestPosition(pos);
    }

    public boolean hasNest() {
        return stateManager.hasNest();
    }

    // === FIRE ATTACK DELEGATION (BEFORE/AFTER) ===

    // BEFORE: Duplicated fire logic
    /*
    public void shootExplosiveFireball(LivingEntity target) {
        if (this.level().isClientSide) return;
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5) - this.getY(0.5);
        double dz = target.getZ() - this.getZ();
        ExplosiveFireballEntity fireball = new ExplosiveFireballEntity(this.level(), this, dx, dy, dz);
        Vec3 lookVec = this.getViewVector(1.0F);
        double spawnDistance = 2.0;
        fireball.setPos(...);
        this.level().addFreshEntity(fireball);
        this.playSound(SoundEvents.GHAST_SHOOT, 1.0F, 1.0F);
    }
    */

    // AFTER: Clean delegation
    public void shootExplosiveFireball(LivingEntity target) {
        fireHandler.shootFireballAtTarget(target);
    }

    public void shootFireballInDirection(Vec3 direction) {
        fireHandler.shootFireballInDirection(direction);
    }

    public void breatheFire(LivingEntity target) {
        fireHandler.breatheFireAtTarget(target);
    }

    public void breatheFireInDirection(Vec3 direction) {
        fireHandler.breatheFireInDirection(direction);
    }

    // === GOALS REGISTRATION WITH CONSTANTS ===
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FollowOwnerGoal(
            this,
            DragonConstants.FOLLOW_SPEED_MODIFIER,
            DragonConstants.FOLLOW_MIN_DISTANCE,
            DragonConstants.FOLLOW_MAX_DISTANCE
        ));
        this.goalSelector.addGoal(1, new ReturnToNestGoal(this));
        this.goalSelector.addGoal(2, new DefendNestGoal(this));
        this.goalSelector.addGoal(3, new DragonRestGoal(this));
        this.goalSelector.addGoal(4, new DragonFlyingGoal(this));
    }

    // === ATTRIBUTES WITH CONSTANTS ===
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, DragonConstants.MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, DragonConstants.MOVEMENT_SPEED)
            .add(Attributes.FLYING_SPEED, DragonConstants.FLYING_SPEED)
            .add(Attributes.FOLLOW_RANGE, DragonConstants.FOLLOW_RANGE)
            .add(Attributes.ATTACK_DAMAGE, DragonConstants.ATTACK_DAMAGE)
            .add(Attributes.ARMOR, DragonConstants.ARMOR)
            .add(Attributes.KNOCKBACK_RESISTANCE, DragonConstants.KNOCKBACK_RESISTANCE);
    }

    public float getDragonScale() {
        return DragonConstants.DRAGON_SCALE;
    }

    // === TICK METHOD - SIMPLIFIED ===
    @Override
    public void tick() {
        super.tick();

        // Tick all cooldowns and timers
        stateManager.tickTimers();

        // Ground detection with hysteresis
        if (!level().isClientSide) {
            updateGroundDetection();
        }

        // Update rotation based on movement
        updateRotationFromMovement();
    }

    /**
     * Handles stable ground detection with hysteresis to prevent flickering
     */
    private void updateGroundDetection() {
        if (!isVehicle() || getControllingPassenger() == null) {
            stateManager.setOnGroundTimer(0);
            stateManager.setInAirTimer(0);
            return;
        }

        // Flying mode overrides ground detection
        if (stateManager.isFlyingMode()) {
            stateManager.setOnGroundTimer(0);
            stateManager.setInAirTimer(0);
            if (stateManager.isLanded()) {
                stateManager.setLanded(false);
            }
            return;
        }

        // Check ground with tolerance
        boolean effectivelyOnGround = isEffectivelyOnGround();

        if (effectivelyOnGround) {
            stateManager.setOnGroundTimer(stateManager.getOnGroundTimer() + 1);
            stateManager.setInAirTimer(0);

            if (stateManager.getOnGroundTimer() >= DragonConstants.GROUND_STABILITY_THRESHOLD_TICKS
                && !stateManager.isLanded()) {
                stateManager.setLanded(true);
                LOGGER.debug("Dragon {} stable on ground", getId());
            }
        } else {
            stateManager.setInAirTimer(stateManager.getInAirTimer() + 1);
            stateManager.setOnGroundTimer(0);

            if (stateManager.getInAirTimer() >= DragonConstants.AIR_STABILITY_THRESHOLD_TICKS
                && stateManager.isLanded()) {
                stateManager.setLanded(false);
                LOGGER.debug("Dragon {} stable in air", getId());
            }
        }
    }

    /**
     * Checks if dragon is on ground or very close to it (within tolerance)
     */
    private boolean isEffectivelyOnGround() {
        if (onGround()) {
            return true;
        }

        // Check if hovering just above ground
        BlockPos posBelow = blockPosition().below();
        double distanceToGround = getY() - posBelow.getY() - 1.0;
        return distanceToGround <= DragonConstants.GROUND_TOLERANCE_DISTANCE;
    }

    /**
     * Updates dragon's rotation to match movement direction
     */
    private void updateRotationFromMovement() {
        Vec3 deltaMovement = getDeltaMovement();
        double horizontalSpeed = Math.sqrt(deltaMovement.x * deltaMovement.x + deltaMovement.z * deltaMovement.z);

        if (horizontalSpeed <= 0.001D) {
            return;
        }

        // Calculate target yaw from movement
        float targetYaw = (float)(Mth.atan2(deltaMovement.z, deltaMovement.x) * (180.0 / Math.PI)) - 90.0F;
        float newYaw = Mth.rotLerp(0.98F, getYRot(), targetYaw);

        setYRot(newYaw);
        yBodyRot = newYaw;
        yHeadRot = newYaw;

        // Calculate target pitch from movement
        float targetPitch = (float)(-Mth.atan2(deltaMovement.y, horizontalSpeed) * (180.0 / Math.PI));
        targetPitch = Mth.clamp(targetPitch, -60.0F, 60.0F);
        float newPitch = Mth.rotLerp(0.9F, getXRot(), targetPitch);

        setXRot(newPitch);
    }

    // === TAMING WITH CONSTANTS AND LOGGING ===
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.CONSUME;
        }

        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        // Handle tamed dragon interactions
        if (stateManager.isTamed() && isOwner(player)) {
            return handleTamedInteraction(player, mainHandItem, offHandItem);
        }

        // Handle taming process
        if (!stateManager.isTamed()) {
            return handleTamingProcess(player, mainHandItem, offHandItem);
        }

        return super.mobInteract(player, hand);
    }

    private boolean isOwner(Player player) {
        return stateManager.getOwnerUUID().isPresent()
            && stateManager.getOwnerUUID().get().equals(player.getUUID());
    }

    private InteractionResult handleTamedInteraction(Player player, ItemStack mainHand, ItemStack offHand) {
        // Ride with lead in offhand
        if (offHand.is(Items.LEAD) && !stateManager.isSitting()) {
            if (player.startRiding(this)) {
                playSound(SoundEvents.HORSE_SADDLE.value(), 1.0F, 1.0F);
                LOGGER.debug("Player {} mounted dragon {}", player.getName().getString(), getId());
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }

        // Toggle sitting with empty hands
        if (mainHand.isEmpty() && offHand.isEmpty()) {
            boolean wasSitting = stateManager.isSitting();
            stateManager.setSitting(!wasSitting);

            if (stateManager.isSitting()) {
                getNavigation().stop();
                stateManager.setLanded(true);
                setNoGravity(true);
            } else {
                stateManager.setLanded(true);
                stateManager.setLandingMode(false);
                setNoGravity(false);
                stateManager.setResting(false);
            }

            playSound(SoundEvents.WOLF_STEP, 1.0F, 1.0F);
            LOGGER.debug("Dragon {} toggled sitting: {}", getId(), stateManager.isSitting());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleTamingProcess(Player player, ItemStack mainHand, ItemStack offHand) {
        // Step 1: Feed enchanted golden apple
        if (offHand.is(Items.ENCHANTED_GOLDEN_APPLE) && !stateManager.hasEatenGoldenApple()) {
            if (!player.isCreative()) {
                offHand.shrink(1);
            }

            stateManager.setAteGoldenApple(true);
            stateManager.setTamingTimer(DragonConstants.TAMING_WINDOW_TICKS);

            playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
            level().broadcastEntityEvent(this, (byte) 7);

            LOGGER.info("Dragon {} ate golden apple, taming window started", getId());
            return InteractionResult.SUCCESS;
        }

        // Step 2: Apply saddle within time window
        if (stateManager.hasEatenGoldenApple()
            && mainHand.is(ModItems.DRAGON_SADDLE.get())
            && stateManager.getTamingTimer() > 0) {

            if (!player.isCreative()) {
                mainHand.shrink(1);
            }

            stateManager.setTamed(true);
            stateManager.setOwnerUUID(player.getUUID());
            stateManager.setAteGoldenApple(false);
            stateManager.setTamingTimer(0);

            playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
            level().broadcastEntityEvent(this, (byte) 7);
            setHealth(getMaxHealth());

            LOGGER.info("Dragon {} tamed by {}", getId(), player.getName().getString());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // === SAVE/LOAD WITH CONSTANTS ===
    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        // Save nest
        BlockPos nest = stateManager.getNestPosition();
        if (nest != null) {
            output.putInt("NestX", nest.getX());
            output.putInt("NestY", nest.getY());
            output.putInt("NestZ", nest.getZ());
            output.putBoolean("HasNest", true);
        } else {
            output.putBoolean("HasNest", false);
        }

        // Save timers
        output.putInt("LandedTimer", stateManager.getLandedTimer());
        output.putInt("FlyingTimer", stateManager.getFlyingTimer());
        output.putInt("TamingTimer", stateManager.getTamingTimer());

        // Save taming data
        output.putBoolean("IsTamed", stateManager.isTamed());
        output.putBoolean("IsSitting", stateManager.isSitting());
        output.putBoolean("AteGoldenApple", stateManager.hasEatenGoldenApple());

        stateManager.getOwnerUUID().ifPresent(uuid ->
            output.putString("OwnerUUID", uuid.toString())
        );
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        // Load nest
        if (input.getBooleanOr("HasNest", false)) {
            int x = input.getIntOr("NestX", 0);
            int y = input.getIntOr("NestY", 0);
            int z = input.getIntOr("NestZ", 0);
            stateManager.setNestPosition(new BlockPos(x, y, z));
        }

        // Load timers
        stateManager.setLandedTimer(input.getIntOr("LandedTimer", 0));
        stateManager.setFlyingTimer(input.getIntOr("FlyingTimer", 0));
        stateManager.setTamingTimer(input.getIntOr("TamingTimer", 0));

        // Load taming data
        stateManager.setTamed(input.getBooleanOr("IsTamed", false));
        stateManager.setSitting(input.getBooleanOr("IsSitting", false));
        stateManager.setAteGoldenApple(input.getBooleanOr("AteGoldenApple", false));

        String ownerUuidString = input.getStringOr("OwnerUUID", "");
        if (!ownerUuidString.isEmpty()) {
            try {
                stateManager.setOwnerUUID(UUID.fromString(ownerUuidString));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Failed to parse owner UUID: {}", ownerUuidString);
            }
        }
    }

    // === DEATH LOOT WITH CONSTANTS ===
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);

        int skinCount = DragonConstants.MIN_DRAGON_SKIN_DROP +
            random.nextInt(DragonConstants.MAX_DRAGON_SKIN_DROP - DragonConstants.MIN_DRAGON_SKIN_DROP + 1);

        for (int i = 0; i < skinCount; i++) {
            spawnAtLocation(level, new ItemStack(ModItems.DRAGON_SKIN.get()));
        }
    }
}

/**
 * KEY PATTERNS TO APPLY:
 *
 * 1. Replace all System.out.println with LOGGER.debug/info/warn
 * 2. Replace all magic numbers with DragonConstants.XXX
 * 3. Delegate all state methods to stateManager
 * 4. Delegate all fire methods to fireHandler
 * 5. Extract complex methods into smaller, focused methods
 * 6. Remove German comments, use English
 * 7. Use proper logging levels (debug for frequent, info for important)
 */

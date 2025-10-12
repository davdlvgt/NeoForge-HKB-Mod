package de.davidvogt.hkbmod.item.entity.custom;

import de.davidvogt.hkbmod.item.ModItems;
import de.davidvogt.hkbmod.item.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class DeerEntity extends Animal {

    private static final Ingredient FOOD_ITEMS = Ingredient.of(Items.WHEAT, Items.APPLE, Items.CARROT);

    public DeerEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        // Priority 0: Float in water to avoid drowning
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Priority 1: Panic when hurt (run away fast)
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4D));

        // Priority 2: Avoid players - run away when they get too close
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class,
                20.0F,      // Distance to start fleeing (10 blocks)
                1.5D,       // Walk speed when fleeing
                1.75D));     // Sprint speed when fleeing

        // Priority 3: Breeding behavior
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));

        // Priority 4: Follow parent when baby
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));

        // Priority 5: Be tempted by food items (overrides fleeing behavior)
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.2D, FOOD_ITEMS, false));

        // Priority 6: Wander around randomly
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        // Priority 7: Look at nearby players (when not fleeing)
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));

        // Priority 8: Look around randomly when idle
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /**
     * Define the attributes for the deer entity (health, movement speed, etc.)
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)      // 6 hearts (slightly more than cow's 10)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)   // Faster than cow (0.2D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)    // How far they notice players
                .add(Attributes.TEMPT_RANGE, 10.0D);    // Range for tempting with food
    }

    /**
     * Check if an item is food for the deer (for breeding and tempting)
     */
    @Override
    public boolean isFood(ItemStack stack) {
        return FOOD_ITEMS.test(stack);
    }

    /**
     * Create a baby deer when bred
     */
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return new DeerEntity(ModEntities.DEER.get(), level);
    }

    /**
     * Ambient sound (idle sound the deer makes)
     * Using cow sounds as placeholder - you can create custom sounds later
     */
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COW_AMBIENT;
    }

    /**
     * Sound when the deer takes damage
     */
    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.COW_HURT;
    }

    /**
     * Sound when the deer dies
     */
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COW_DEATH;
    }

    /**
     * Volume of the deer's sounds
     */
    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    /**
     * Custom death loot drops
     * Deer drop venison and antlers when killed
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);

        // Drop deer beef (venison) - 1-3 pieces
        int beefCount = 1 + this.random.nextInt(3);
        for (int i = 0; i < beefCount; i++) {
            // Drop cooked beef if killed by fire, otherwise raw
            if (this.isOnFire()) {
                this.spawnAtLocation(level, new ItemStack(ModItems.COOKED_DEER_BEEF.get()));
            } else {
                this.spawnAtLocation(level, new ItemStack(ModItems.DEER_BEEF.get()));
            }
        }

        // Drop antlers - 50% chance, only from adults
        if (!this.isBaby() && this.random.nextBoolean()) {
            this.spawnAtLocation(level, new ItemStack(ModItems.DEER_ANTLERS.get()));
        }
    }

}

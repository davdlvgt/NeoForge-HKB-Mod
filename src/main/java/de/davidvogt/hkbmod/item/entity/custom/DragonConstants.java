package de.davidvogt.hkbmod.item.entity.custom;

/**
 * Central configuration for all dragon-related constants and magic numbers.
 * This makes it easy to tune dragon behavior without hunting through multiple files.
 */
public final class DragonConstants {

    // === ATTRIBUTES ===
    public static final double MAX_HEALTH = 100.0;
    public static final double MOVEMENT_SPEED = 0.5;
    public static final double FLYING_SPEED = 4.5;
    public static final double FOLLOW_RANGE = 64.0;
    public static final double ATTACK_DAMAGE = 8.0;
    public static final double ARMOR = 4.0;
    public static final double KNOCKBACK_RESISTANCE = 0.5;
    public static final float DRAGON_SCALE = 0.5F;

    // === FLIGHT BEHAVIOR ===
    public static final int MIN_FLYING_DURATION_TICKS = 600; // 30 seconds
    public static final int MAX_FLYING_DURATION_TICKS = 2400; // 2 minutes
    public static final int MIN_LANDING_DURATION_TICKS = 400; // 20 seconds
    public static final int MAX_LANDING_DURATION_TICKS = 1600; // 80 seconds
    public static final double MIN_FLIGHT_ALTITUDE = 175.0;
    public static final double MAX_FLIGHT_ALTITUDE = 215.0;
    public static final double TARGET_FLIGHT_ALTITUDE_MIN = 180.0;
    public static final double TARGET_FLIGHT_ALTITUDE_MAX = 210.0;
    public static final double LANDING_SPOT_MIN_DISTANCE = 80.0;
    public static final double LANDING_SPOT_MAX_DISTANCE = 150.0;
    public static final double LANDING_ARRIVAL_DISTANCE = 2.0;
    public static final double RELAXED_FLIGHT_SPEED = 0.6;
    public static final double NORMAL_FLIGHT_SPEED = 1.0;

    // === GROUND BEHAVIOR ===
    public static final int GROUND_STABILITY_THRESHOLD_TICKS = 10;
    public static final int AIR_STABILITY_THRESHOLD_TICKS = 10;
    public static final double GROUND_TOLERANCE_DISTANCE = 0.1;

    // === COMBAT ===
    public static final int FIREBALL_COOLDOWN_TICKS = 30; // 1.5 seconds
    public static final int FIRE_BREATH_MAX_DURATION_TICKS = 60; // 3 seconds
    public static final int FIRE_BREATH_COOLDOWN_TICKS = 40; // 2 seconds
    public static final double FIRE_BREATH_RANGE = 8.0;
    public static final float FIRE_BREATH_DAMAGE = 3.0F;
    public static final float FIREBALL_DIRECT_DAMAGE = 6.0F;
    public static final float EXPLOSION_POWER = 2.0F;
    public static final int FIRE_DURATION_TICKS = 100; // 5 seconds

    // === NEST BEHAVIOR ===
    public static final double NEST_DEFENSE_RADIUS = 50.0;
    public static final double NEST_RETURN_DISTANCE = 128.0;
    public static final double NEST_ARRIVAL_DISTANCE = 10.0;
    public static final double LOW_HEALTH_THRESHOLD = 0.3; // 30%
    public static final int NEST_SEARCH_RADIUS = 15; // blocks in each direction

    // === REST BEHAVIOR ===
    public static final int REST_DURATION_TICKS = 2400; // 2 minutes
    public static final int MIN_TIME_BETWEEN_RESTS_TICKS = 0;
    public static final double RESTING_DETECTION_RADIUS = 10.0;
    public static final double NEST_POSITION_THRESHOLD = 3.0;

    // === TAMING ===
    public static final int TAMING_WINDOW_TICKS = 60; // 3 seconds

    // === FOLLOWING OWNER ===
    public static final double FOLLOW_SPEED_MODIFIER = 0.6;
    public static final float FOLLOW_MIN_DISTANCE = 3.0F;
    public static final float FOLLOW_MAX_DISTANCE = 15.0F;
    public static final float TELEPORT_DISTANCE = 15.0F;

    // === RIDING ===
    public static final float RIDING_FLYING_SPEED_MULTIPLIER = 0.15F;
    public static final float RIDING_GROUND_SPEED_MULTIPLIER = 1.0F;
    public static final float BOOST_SPEED_MULTIPLIER = 1.0F; // Added to base when boosting
    public static final float STRAFE_SPEED_MULTIPLIER = 0.8F;
    public static final double DESCENT_SPEED = 0.6;
    public static final double GLIDE_DESCENT_MULTIPLIER = 0.95;
    public static final double GLIDE_DESCENT_BASE = 0.05;

    // === COLLISION DETECTION ===
    public static final int STUCK_THRESHOLD_TICKS = 40; // 2 seconds
    public static final double MIN_MOVEMENT_PER_TICK = 0.05;
    public static final int EMERGENCY_MANEUVER_DURATION_TICKS = 60; // 3 seconds

    // === TIMERS & INTERVALS ===
    public static final int CHECK_INTERVAL_TICKS = 20; // 1 second
    public static final int PATH_RECALC_INTERVAL_TICKS = 10; // 0.5 seconds

    // === DEATH LOOT ===
    public static final int MIN_DRAGON_SKIN_DROP = 1;
    public static final int MAX_DRAGON_SKIN_DROP = 3;

    private DragonConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}

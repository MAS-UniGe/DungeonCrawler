package classes;

/**
 * Represents a PowerUp in the game. PowerUps increase specific attributes of
 * a GameEntity (health, attack, or defense) by a percentage when collected.
 */
public class PowerUp {
    // ----------------------------------------------
    // Constants
    // ----------------------------------------------
    private static final float DEFAULT_BOOST_PERCENTAGE = 0.2f; // Default boost percentage for power-ups

    // Attributes
    private final PowerUpType type;          // Type of the power-up (HEALTH, ATTACK, DEFENSE)
    private final float boostPercentage;     // The percentage boost this power-up provides
    private final Position position;         // The position of the power-up in the game

    // ----------------------------------------------
    // Constructors
    // ----------------------------------------------

    /**
     * Creates a PowerUp with specified type, boost percentage, and position.
     *
     * @param type The type of the power-up (HEALTH, ATTACK, DEFENSE)
     * @param boostPercentage The percentage by which the attribute will be boosted
     * @param position The position of the power-up in the game
     */
    public PowerUp(PowerUpType type, float boostPercentage, Position position) {
        this.type = type;
        this.boostPercentage = boostPercentage;
        this.position = position;
    }

    /**
     * Creates a PowerUp with a default boost percentage.
     *
     * @param type The type of the power-up (HEALTH, ATTACK, DEFENSE)
     * @param position The position of the power-up in the game
     */
    public PowerUp(PowerUpType type, Position position) {
        this(type, DEFAULT_BOOST_PERCENTAGE, position);
    }

    // ----------------------------------------------
    // Getters
    // ----------------------------------------------

    public PowerUpType getType() { return type; }
    public Position getPosition() {return position; }

    // ----------------------------------------------
    // Power-Up Logic
    // ----------------------------------------------

    /**
     * Applies the effect of this power-up to the specified collector (a GameEntity).
     *
     * @param collector The GameEntity that collects this power-up
     */
    public void applyTo(GameEntity collector) {
        applyBoostByType(collector);
    }

    /**
     * Determines which attribute of the collector to boost based on the power-up type
     * and applies the corresponding boost percentage.
     *
     * @param collector The GameEntity that collects the power-up
     */
    private void applyBoostByType(GameEntity collector) {
        switch (type) {
            case HEALTH:
                collector.increaseHealth(boostPercentage);
                break;
            case ATTACK:
                collector.increaseAttackPwr(boostPercentage);
                break;
            case DEFENSE:
                collector.increaseDefensePwr(boostPercentage);
                break;
            default:
                throw new IllegalStateException("Unknown PowerUpType: " + type);
        }
    }
}
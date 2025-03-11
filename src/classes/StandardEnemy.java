package classes;

/**
 * Represents a StandardEnemy in the game, inheriting shared functionality
 * from GameEntity. Provides default attributes and support for enhancements.
 */
public class StandardEnemy extends GameEntity {
    // ----------------------------------------------
    // Constants
    // ----------------------------------------------
    private static final int DEFAULT_HEALTH = 80;    // Default health points for a standard enemy
    private static final int DEFAULT_ATTACK = 12;   // Default attack power for a standard enemy
    private static final int DEFAULT_DEFENSE = 12;  // Default defense power for a standard enemy

    // ----------------------------------------------
    // Constructors
    // ----------------------------------------------

    /**
     * Creates a StandardEnemy with specified attributes.
     *
     * @param position The initial position of the enemy
     * @param room The room where the enemy is initialized
     * @param healthPts The enemy's starting health points
     * @param attackPwr The enemy's attack power
     * @param defensePwr The enemy's defense power
     */
    public StandardEnemy(Position position, Room room, int healthPts, int attackPwr, int defensePwr) {
        super(position, room, healthPts, attackPwr, defensePwr);
    }

    /**
     * Creates a StandardEnemy with default attributes.
     *
     * @param position The initial position of the enemy
     * @param room The room where the enemy is initialized
     */
    public StandardEnemy(Position position, Room room) {
        this(position, room, DEFAULT_HEALTH, DEFAULT_ATTACK, DEFAULT_DEFENSE);
    }

    /**
     * Creates a StandardEnemy with default attributes in an undefined room.
     *
     * @param position The initial position of the enemy
     */
    public StandardEnemy(Position position) {
        this(position, null);
    }

    // ----------------------------------------------
    // Getters and setters
    // ----------------------------------------------

    /**
     * Identifies this entity as an enemy.
     *
     * @return The EntityType representing an enemy
     */
    @Override
    public EntityType getEntityType() { return EntityType.ENEMY; }

    // ----------------------------------------------
    // Placeholder methods (for subclasses to implement)
    // ----------------------------------------------

    public void enhanceAttributes() {}
}
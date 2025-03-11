package classes;

/**
 * Represents a BossEnemy, which is a stronger version of a StandardEnemy.
 * BossEnemies have additional features such as a special attack with charges
 * and enhanced attribute-boosting capabilities.
 */
public class BossEnemy extends StandardEnemy {
    private static final int DEFAULT_HEALTH = 150;  // Default health points for a boss
    private static final int DEFAULT_ATTACK = 15;  // Default attack power for a boss
    private static final int DEFAULT_DEFENSE = 15; // Default defense power for a boss
    private static final int DEFAULT_SPECIAL_ATTACK_CHARGES = 3; // Default special attack charges
    private static final int LIFE_BOOST = 100;  // Health points boost when enhancing
    private static final int DEFENSE_BOOST = 2; // Defense power boost when enhancing
    private static final int ATTACK_BOOST = 2; // Attack power boost when enhancing
    private int specialAttackCharges; // Number of special attacks available

    // ----------------------------------------------
    // Constructors
    // ----------------------------------------------

    /**
     * Creates a BossEnemy with specified attributes.
     *
     * @param position The initial position of the boss
     * @param room The room where the boss is initialized
     * @param healthPts The boss's starting health points
     * @param attackPwr The boss's attack power
     * @param defensePwr The boss's defense power
     * @param specialAttackCharges The number of special attack charges available
     */
    public BossEnemy(Position position, Room room, int healthPts, int attackPwr, int defensePwr, int specialAttackCharges) {
        super(position, room, healthPts, attackPwr, defensePwr);
        this.specialAttackCharges = specialAttackCharges;
    }

    /**
     * Creates a BossEnemy with default attributes.
     *
     * @param position The initial position of the boss
     * @param room The room where the boss is initialized
     */
    public BossEnemy(Position position, Room room) {
        this(position, room, DEFAULT_HEALTH, DEFAULT_ATTACK, DEFAULT_DEFENSE, DEFAULT_SPECIAL_ATTACK_CHARGES);
    }

    /**
     * Creates a BossEnemy with default attributes in an undefined room.
     *
     * @param position The initial position of the boss
     */
    public BossEnemy(Position position) {
        this(position, null);
    }

    // ----------------------------------------------
    // Unique Boss Methods
    // ----------------------------------------------

    /**
     * Enhances the boss's attributes.
     * Adds a boost to health, attack power, and defense power.
     */
    @Override
    public void enhanceAttributes() {
        increaseHealth(LIFE_BOOST);
        increaseAttackPwr(ATTACK_BOOST);
        increaseDefensePwr(DEFENSE_BOOST);
    }

    /**
     * Executes a special attack on a target, dealing double attack power as damage.
     * Reduces the special attack charges by 1 after successful execution.
     *
     * @param target The target entity to attack
     * @return True if the special attack succeeds, false otherwise
     */
    public boolean specialAttack(GameEntity target) {
        if (specialAttackCharges <= 0)
            return false; // No charges left, special attack fails

        specialAttackCharges--; // Decrement the charges
        target.takeDamage(getAttackPwr() * 2); // Deal double damage
        return true; // Attack successfully performed
    }


}

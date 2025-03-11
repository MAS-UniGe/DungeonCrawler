package classes;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents a Player in the game, inheriting common functionality
 * from GameEntity while adding player-specific logic for ranged attacks
 * and managing ranged ammunition.
 */
public class Player extends GameEntity {
    // ----------------------------------------------
    // Attributes
    // ----------------------------------------------
    private static final int DEFAULT_HEALTH = 150;  // Default health points for a player
    private static final int DEFAULT_ATTACK = 15;   // Default attack power for a player
    private static final int DEFAULT_DEFENSE = 12;  // Default defense power for a player
    private static final int DEFAULT_RANGED_AMMO = 3;   // Default ranged ammo amount
    private int rangedAmmo;  // Track the player's ranged ammunition count

    // ----------------------------------------------
    // Constructors
    // ----------------------------------------------

    /**
     * Creates a Player with a specified position, room, and attributes.
     *
     * @param position The initial position of the player
     * @param currentRoom The room the player is currently in
     * @param healthPts The player's starting health points
     * @param attackPwr The player's attack power
     * @param defensePwr The player's defense power
     */
    public Player(Position position, Room currentRoom, int healthPts, int attackPwr, int defensePwr) {
        super(position, currentRoom, healthPts, attackPwr, defensePwr);
        this.rangedAmmo = DEFAULT_RANGED_AMMO; // Initialize ranged ammo by default
    }

    /**
     * Creates a Player with default attributes.
     *
     * @param position The initial position of the player
     * @param currentRoom The room the player is currently in
     */
    public Player(Position position, Room currentRoom) {
        this(position, currentRoom, DEFAULT_HEALTH, DEFAULT_ATTACK, DEFAULT_DEFENSE);
    }

    // ----------------------------------------------
    // Getters and Setters
    // ----------------------------------------------

    public void setRangedAmmo(int amount) { this.rangedAmmo = amount; }

    @Override
    public int getRangedAmmo() { return this.rangedAmmo; }

    /**
     * Gets the type of this entity as a player.
     *
     * @return The EntityType representing the player
     */
    @Override
    public EntityType getEntityType() { return EntityType.PLAYER; }

    // ----------------------------------------------
    // Combat Methods
    // ----------------------------------------------

    /**
     * Performs a ranged attack on a target, reducing the player's ranged ammunition.
     *
     * @param target The target GameEntity to attack
     * @return True if the attack succeeds, false otherwise
     */
    @Override
    public boolean rangedAttack(GameEntity target) {
        rangedAmmo--; // Decrease ranged ammo
        return this.attack(target); // Perform the attack
    }
}
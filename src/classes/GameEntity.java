package classes;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents a game entity with position, room, health, attack, and defense attributes.
 * Provides shared functionalities for subclasses such as movement, attack, and damage handling.
 */
public class GameEntity {
    private Position position;               // Position of the entity in the game
    protected Room currentRoom;             // The room the entity is currently in
    private int healthPts;                  // Health points of the entity
    private final int maxHealhPts;         // Maximum health points of the entity (to have a track of it when the current healthPts decrease
    private int attackPwr;                  // Attack power value
    private int defensePwr;                 // Defense power value
    private static final int rangedAttackRange = 6; // Range for ranged attacks

    /**
     * Constructor to initialize the entity with its attributes.
     *
     * @param position Initial position of the entity
     * @param currentRoom Room the entity is placed in
     * @param healthPts Initial health points
     * @param attackPwr Initial attack power
     * @param defensePwr Initial defense power
     */
    public GameEntity(Position position, Room currentRoom, int healthPts, int attackPwr, int defensePwr) {
        this.position = position;
        this.currentRoom = currentRoom;
        this.healthPts = healthPts;
        this.maxHealhPts = healthPts;
        this.attackPwr = attackPwr;
        this.defensePwr = defensePwr;
    }

    // ----------------------------------------------
    // Getters and Setters
    // ----------------------------------------------
    public int getHealthPts() { return healthPts; }
    public int getMaxHealthPts() { return maxHealhPts; }
    public void setHealthPts(int healthPts) { this.healthPts = healthPts; }
    public int getAttackPwr() { return attackPwr; }
    public void setAttackPwr(int attackPwr) { this.attackPwr = attackPwr; }
    public int getDefensePwr() { return defensePwr; }
    public void setDefensePwr(int defensePwr) { this.defensePwr = defensePwr; }
    public Position getPosition() { return position; }
    public Room getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(Room room) { this.currentRoom = room; }
    public int getRangedAmmo() { return 0; }

    // ----------------------------------------------
    // Movement Methods
    // ----------------------------------------------
    /**
     * Attempts to move the entity to a new position. Validates if the position
     * is within the room and updates the position if the move is valid.
     *
     * @param newPosition The new position to move to
     * @return True if the move is successful, false otherwise
     */
    public boolean move(Position newPosition) {
        if (isMoveValid(newPosition)) {
            processRoomMove(newPosition);
            return true;
        }
        return false;
    }

    private boolean isMoveValid(Position newPosition) {
        return currentRoom.canMoveTo(newPosition);
    }

    private void processRoomMove(Position newPosition) {
        currentRoom.clearGridCell(this.position);
        this.position = newPosition;
        currentRoom.updateGridWithEntity(this); // Update room state
    }

    // ----------------------------------------------
    // Combat Methods
    // ----------------------------------------------
    /**
     * Determines if the current entity is still alive.
     *
     * @return True if health points are greater than zero, false otherwise
     */
    public boolean isAlive() {
        return healthPts > 0;
    }

    /**
     * Executes an attack roll and checks if it hits the target based on defense.
     *
     * @param target The target of the attack
     * @return True if the attack succeeds, false otherwise
     */
    private boolean performAttackRoll(GameEntity target) {
        int attackRoll = getRandomInclusive(1, 20); // D20 roll
        return attackRoll > target.getDefensePwr();
    }

    public boolean meleeAttack(GameEntity target) {
        return this.attack(target);
    }

    public boolean attack(GameEntity target) {
        if (performAttackRoll(target)) {
            target.takeDamage(getAttackPwr());
            return true;
        }
        return false;
    }

    /**
     * Reduces the health of the entity by the specified damage amount and handles
     * the aftermath if health drops to zero or below.
     *
     * @param damage The amount of damage to be taken
     */
    public void takeDamage(int damage) {
        this.healthPts -= damage;

        if (!isAlive()) {
            kill();
        }
    }

    /**
     * Handles what occurs when the entity is killed.
     */
    public void kill() {
        currentRoom.clearGridCell(this.position);
    }

    /**
     * Checks if the target is within melee attack range.
     *
     * @param target The target entity
     * @return True if the target is adjacent, false otherwise
     */
    public boolean isInMeleeRange(GameEntity target) {
        return this.position.isAdjacentTo(target.position);
    }

    /**
     * Checks if the target is within ranged attack range.
     *
     * @param target The target entity
     * @return True if the target is within range, false otherwise
     */
    public boolean isInRangedRange(GameEntity target) {
        return this.position.isInRange(target.position, rangedAttackRange);
    }

    // ----------------------------------------------
    // Attribute Management Methods
    // ----------------------------------------------
    private void updateAttribute(Supplier<Integer> getter, Consumer<Integer> setter, float boostPercentage) {
        int currentValue = getter.get();
        float boost = currentValue * boostPercentage;
        setter.accept(Math.round(currentValue + boost));
    }

    public void increaseHealth(float boostPercentage) {
        updateAttribute(this::getHealthPts, this::setHealthPts, boostPercentage);
    }

    public void increaseAttackPwr(float boostPercentage) {
        updateAttribute(this::getAttackPwr, this::setAttackPwr, boostPercentage);
    }

    public void increaseDefensePwr(float boostPercentage) {
        updateAttribute(this::getDefensePwr, this::setDefensePwr, boostPercentage);
    }

    // ----------------------------------------------
    // Utility Methods
    // ----------------------------------------------
    public int getRandomInclusive(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    // Placeholder methods (for subclasses to implement)
    public EntityType getEntityType() { return null; }

    public boolean rangedAttack(GameEntity target) { return false; }
}


package classes;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a game room with a grid of tiles, enemies, and power-ups. 
 * Manages the placement, movement, and removal of entities within the room.
 */
public class Room {
    // ----------------------------------------------
    // Constants
    // ----------------------------------------------
    private static final RoomTileType DEFAULT_TILE = RoomTileType.EMPTY; // Default tile type for empty spaces
    private static final RoomTileType WALL_TILE = RoomTileType.WALL;     // Wall tile type for boundaries

    // ----------------------------------------------
    // Room properties
    // ----------------------------------------------
    private final int width, height; // Room dimensions
    private final RoomTileType[][] grid; // Room grid in terms of tiles
    private final List<StandardEnemy> enemies; // Enemies in the room
    private final List<PowerUp> powerUps; // PowerUps in the room
    private final List<Position> powerUpPositions; // PowerUps positions (useful for the enemy agents)

    // ----------------------------------------------
    // Constructor
    // ----------------------------------------------

    /**
     * Creates a Room with specified dimensions and initializes its grid and entities.
     *
     * @param width  The width of the room (number of columns)
     * @param height The height of the room (number of rows)
     */
    public Room(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new RoomTileType[width][height];
        this.enemies = new ArrayList<>();
        this.powerUps = new ArrayList<>();
        this.powerUpPositions = new ArrayList<>();
        initializeGrid();
    }

    // ----------------------------------------------
    // Grid Initialization
    // ----------------------------------------------

    /**
     * Initializes the room's grid with default or wall tiles based on boundary positions.
     */
    private void initializeGrid() {
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                grid[x][y] = isBoundaryTile(x, y) ? WALL_TILE : DEFAULT_TILE;
    }

    /**
     * Checks if the specified position is a boundary tile (along the edges of the room).
     *
     * @param x The x-coordinate of the position
     * @param y The y-coordinate of the position
     * @return True if the position is part of the boundary, false otherwise
     */
    private boolean isBoundaryTile(int x, int y) {
        return x == 0 || y == 0 || x == width - 1 || y == height - 1;
    }

    // ----------------------------------------------
    // Entity Management
    // ----------------------------------------------

    /**
     * Adds an enemy to the room and updates the grid with the corresponding tile at its position.
     *
     * @param enemy The enemy to add
     */
    public void addEnemy(StandardEnemy enemy) {
        enemies.add(enemy);
        updateGridWithEntity(enemy);
    }

    /**
     * Removes an enemy from the room and clears its tile from the grid.
     *
     * @param enemy The enemy to remove
     */
    public void removeEnemy(StandardEnemy enemy) {
        enemies.remove(enemy);
        clearGridCell(enemy.getPosition());
    }

    /**
     * Adds a power-up to the room and updates the grid with the corresponding tile at its position.
     *
     * @param powerUp The power-up to add
     */
    public void addPowerUp(PowerUp powerUp) {
        powerUps.add(powerUp);
        powerUpPositions.add(powerUp.getPosition());
        updateGridWithEntity(powerUp);
    }

    /**
     * Removes a power-up from the room and clears its position from the grid.
     *
     * @param powerUp The power-up to remove
     */
    public void removePowerUp(PowerUp powerUp) {
        powerUps.remove(powerUp);
        clearGridCell(powerUp.getPosition());
    }

    /**
     * Collects and removes the power-up at the specified position.
     *
     * @param position The position of the power-up to collect
     * @return The collected power-up, or null if none is found
     */
    public PowerUp collectPowerUpAt(Position position) {
        for (PowerUp powerUp : powerUps) {
            if (powerUp.getPosition().equals(position)) {
                removePowerUp(powerUp);
                return powerUp;
            }
        }
        return null;
    }

    /**
     * Adds the player to the grid the corresponding tile at their position.
     *
     * @param player The player to add to the grid
     */
    public void addPlayerToGrid(Player player) {
        updateGridWithEntity(player);
    }

    /**
     * Removes the player from the grid.
     *
     * @param player The player to remove from the grid
     */
    public void removePlayerFromGrid(Player player) {
        clearGridCell(player.getPosition());
    }

    // ----------------------------------------------
    // Grid Operations
    // ----------------------------------------------

    /**
     * Updates the grid with an entity's position and tile type.
     *
     * @param entity The entity to update on the grid
     */
    public void updateGridWithEntity(Object entity) {
        Position position = getPositionFromEntity(entity);
        if (position != null) {
            RoomTileType tileType = getTileTypeForEntity(entity);
            updateGridCell(position, tileType);
        }
    }

    /**
     * Clears a grid cell by setting its tile to the default tile type.
     *
     * @param position The position of the cell to clear
     */
    public void clearGridCell(Position position) {
        updateGridCell(position, DEFAULT_TILE);
    }

    /**
     * Updates a specific grid cell with a new tile type.
     *
     * @param position The position to update
     * @param tileType The new tile type for the cell
     */
    private void updateGridCell(Position position, RoomTileType tileType) {
        grid[position.getX()][position.getY()] = tileType;
    }

    // ----------------------------------------------
    // Utility Methods
    // ----------------------------------------------

    /**
     * Checks if a position is valid and movable within the grid.
     *
     * @param position The position to check
     * @return True if the position is valid and empty, false otherwise
     */
    public boolean canMoveTo(Position position) {
        int x = position.getX();
        int y = position.getY();
        return x >= 0 && y >= 0 && x < width && y < height && grid[x][y] == DEFAULT_TILE;
    }

    /**
     * Checks if all enemies in the room have been defeated.
     *
     * @return True if no enemies are alive, false otherwise
     */
    public boolean allEnemiesDefeated() {
        return enemies.stream().noneMatch(StandardEnemy::isAlive);
    }

    /**
     * Clears the entire room by resetting the grid and removing all entities.
     */
    public void clearRoom() {
        initializeGrid();
        enemies.clear();
        powerUps.clear();
    }

    // ----------------------------------------------
    // Entity Helper Methods
    // ----------------------------------------------

    /**
     * Retrieves the position of an entity.
     *
     * @param entity The entity to get the position of
     * @return The entity's position, or null if the entity is unrecognized
     */
    private Position getPositionFromEntity(Object entity) {
        if (entity instanceof GameEntity gameEntity)
            return gameEntity.getPosition();
        else if (entity instanceof PowerUp powerUp)
            return powerUp.getPosition();
        
        return null;
    }

    /**
     * Retrieves the tile type corresponding to an entity.
     *
     * @param entity The entity to get the tile type of
     * @return The appropriate tile type for the entity
     */
    private RoomTileType getTileTypeForEntity(Object entity) {
        if (entity instanceof Player) {
            return RoomTileType.PLAYER;
        } else if (entity instanceof StandardEnemy) {
            return RoomTileType.ENEMY;
        } else if (entity instanceof PowerUp) {
            return RoomTileType.POWERUP;
        }
        return DEFAULT_TILE;
    }

    // ----------------------------------------------
    // Getters
    // ----------------------------------------------

    public RoomTileType[][] getGrid() {
        return grid;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<StandardEnemy> getEnemies() {
        return enemies;
    }

    public List<PowerUp> getPowerUps() {
        return powerUps;
    }

    public List<Position> getPowerUpsPositions() {
        return powerUpPositions;
    }
}
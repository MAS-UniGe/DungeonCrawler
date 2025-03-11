package utils;

import agents.StandardEnemyAgent;
import classes.EntityType;
import classes.GameManager;
import classes.Position;
import classes.RoomTileType;

import java.util.*;

/**
 * Utility class for handling movement logic of in-game entities.
 * Provides methods for pathfinding, position validation, and pursuing targets.
 */
public class MovementUtils {
    private static final double RANDOM_THRESHOLD = 0.5; // Threshold for random movement direction choice

    // ----------------------------------------------
    // Pathfinding Methods
    // ----------------------------------------------

    /**
     * Computes the next step towards the target position based on the shortest path.
     * Supports random movement when both horizontal and vertical movements are valid.
     *
     * @param current The current position of the entity
     * @param target  The target position to move towards
     * @return The computed next position
     */
    /*public static Position computeNextStepTowardsTarget(Position current, Position target) {
        // Determine directional differences
        int horizontalDirection = Integer.compare(target.getX(), current.getX());
        int verticalDirection = Integer.compare(target.getY(), current.getY());

        // Compute the directional step
        return computeDirectionalStep(current, horizontalDirection, verticalDirection);
    }*/

    /**
     * Computes the next step based on directional priorities (horizontal/vertical).
     * Includes randomization when both directions are available.
     *
     * @param current              The current position
     * @param horizontalDirection  Horizontal movement direction (-1, 0, 1)
     * @param verticalDirection    Vertical movement direction (-1, 0, 1)
     * @return The next computed position
     */
    /*public static Position computeDirectionalStep(Position current, int horizontalDirection, int verticalDirection) {
        if (horizontalDirection != 0 && verticalDirection != 0)
            // Randomly prioritize horizontal or vertical movement
            return Math.random() < RANDOM_THRESHOLD
                    ? current.translate(horizontalDirection, 0)
                    : current.translate(0, verticalDirection);


        if (horizontalDirection != 0) // Only horizontal movement
            return current.translate(horizontalDirection, 0);


        if (verticalDirection != 0) // Only vertical movement
            return current.translate(0, verticalDirection);


        // No movement (already at target position)
        return current;
    }*/

    // ----------------------------------------------
    // Position Validation Methods
    // ----------------------------------------------

    /**
     * Validates if a given position is within bounds and walkable in the game.
     *
     * @param pos          The position to validate
     * @param gameManager  The game manager responsible for the game state
     * @return True if the position is valid, false otherwise
     */
    public static boolean isPositionValid(Position pos, GameManager gameManager) {
        // Check if the position is within game boundaries
        if (!isWithinGameBounds(pos, gameManager))
            return false;

        // Check if the tile at the position is walkable
        RoomTileType tileType = gameManager.getCurrentRoom().getGrid()[pos.getX()][pos.getY()];
        return tileType == RoomTileType.EMPTY || tileType == RoomTileType.PLAYER; // Valid if the tile is empty
    }

    /**
     * Checks if a position is within the boundary of the game's current room.
     *
     * @param pos         The position to check
     * @param gameManager The game manager responsible for managing the game
     * @return True if the position is within game bounds, false otherwise
     */
    private static boolean isWithinGameBounds(Position pos, GameManager gameManager) {
        int width = gameManager.getCurrentRoom().getWidth();
        int height = gameManager.getCurrentRoom().getHeight();
        return pos.getX() >= 0 && pos.getX() < width && pos.getY() >= 0 && pos.getY() < height;
    }

    // ----------------------------------------------
    // Additional Movement Utilities
    // ----------------------------------------------

    /**
     * Finds the nearest valid position to the specified target that the entity can move to.
     * Uses a spiral search pattern, starting from the target position and expanding.
     *
     * @param startingPos The starting position of the entity
     * @param targetPos   The desired target position
     * @param gameManager The game manager responsible for the game world
     * @return The nearest valid position if found, otherwise the starting position
     */
    public static Position findNearestValidPosition(Position startingPos, Position targetPos, GameManager gameManager) {
        // Spiral search within the game room's boundaries
        int searchRadius = 1;

        while (searchRadius < gameManager.getCurrentRoom().getWidth()) { // Limit search to the map's dimensions
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                    if (dx == 0 && dy == 0) continue; // Skip the target position itself

                    // Compute candidate position
                    Position candidatePos = new Position(targetPos.getX() + dx, targetPos.getY() + dy);

                    // Validate the candidate position
                    if (isPositionValid(candidatePos, gameManager)) {
                        return candidatePos; // Return if a valid position is found
                    }
                }
            }
            searchRadius++; // Increment the search radius
        }

        // Return starting position as a fallback
        return startingPos;
    }

    // ----------------------------------------------
    // Entity Movement Handling
    // ----------------------------------------------

    /**
     * Moves the given entity towards the target position of one step.
     *
     * @param standardEnemyAgent The agent controlling the entity
     * @param enemyPos           The current position of the entity
     * @param targetPos          The target position to pursue
     */
    public static void pursueTarget(StandardEnemyAgent standardEnemyAgent, Position enemyPos, Position targetPos) {
        // Compute the next step towards the target
        Position nextStep = computeNextStepAvoidingObstacles(enemyPos, targetPos, standardEnemyAgent.getGameManager());
        Position oldPos = standardEnemyAgent.getEnemy().getPosition();

        // Execute the movement logic in a separate thread for asynchronous operation
        new Thread(() -> {
            try {
                // Attempt to move the entity
                if (standardEnemyAgent.getEnemy().move(nextStep)) {
                    // Notify about the movement event to the game manager's event listener
                    if (standardEnemyAgent.getGameManager() != null && standardEnemyAgent.getGameManager().getGameEventListener() != null) {
                        standardEnemyAgent
                                .getGameManager()
                                .getGameEventListener()
                                .onEntityMoved(oldPos, nextStep, EntityType.ENEMY);
                    }
                }
            } catch (Exception e) {
                // Log and handle movement errors
                e.printStackTrace();
                System.err.println("Error during enemy movement: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Computes the next step towards the target position based on A*-like logic.
     * Evaluates the immediate neighbors of the current position and prioritizes
     * valid moves that minimize the cost to the target.
     *
     * @param current     The current position of the entity.
     * @param target      The target position to move towards.
     * @param gameManager The game manager responsible for the game state.
     * @return The next position to move to, or the current position if no valid move exists.
     */
    public static Position computeNextStepAvoidingObstacles(Position current, Position target, GameManager gameManager) {
        // Priority queue for neighbors with fCost evaluation
        PriorityQueue<Position> openQueue = new PriorityQueue<>((posA, posB) -> {
            int fCostA = computeGCost(current, posA) + heuristic(posA, target);
            int fCostB = computeGCost(current, posB) + heuristic(posB, target);

            // Compare fCost for tie-breaking
            if (fCostA == fCostB)
                return Math.random() < 0.5 ? -1 : 1; // Random tie-breaker

            return Integer.compare(fCostA, fCostB); // Sort by fCost
        });

        // Explore all possible neighbors (up, down, left, right)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // Skip the current position itself
                if (Math.abs(dx + dy) == 2) continue; // Skip diagonal neighbors

                Position neighbor = current.translate(dx, dy);

                // Only add valid neighbors to the priority queue
                if (isPositionValid(neighbor, gameManager)) {
                    openQueue.add(neighbor);
                }
            }
        }

        // Select the best next step (lowest fCost)
        Position nextStep = openQueue.poll(); // Retrieves and removes the best step
        return nextStep != null ? nextStep : current; // If no valid move, stay in current position
    }

    /**
     * Computes the G cost (movement cost) between the start and the current position.
     *
     * @param start   The starting position.
     * @param current The current position.
     * @return The G cost, which is the Manhattan Distance.
     */
    private static int computeGCost(Position start, Position current) {
        return Math.abs(current.getX() - start.getX()) + Math.abs(current.getY() - start.getY());
    }

    /**
     * Heuristic function that uses Manhattan Distance as the estimated cost to the target.
     *
     * @param a The current position.
     * @param b The target position.
     * @return The Manhattan Distance between the two positions.
     */
    private static int heuristic(Position a, Position b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }
}
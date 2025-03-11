package utils;

import agents.EnemyState;
import agents.StandardEnemyAgent;
import agents.behaviours.AttackingBehaviour;
import agents.behaviours.IdleBehaviour;
import agents.behaviours.RequestReinforcementsBehaviour;
import classes.GameManager;
import classes.Position;
import classes.StandardEnemy;

/**
 * Utility class providing common methods for managing enemy behaviors in the game.
 * Facilitates actions such as attacking the player, chasing the player, and transitioning to idle states.
 */
public class BehaviourUtils {

    /**
     * Updates the dungeon state to reflect the movement of an enemy to a target position.
     * The method internally utilizes a separate thread to handle event listener calls asynchronously.
     *
     * @param gameManager    The game manager handling game events
     * @param standardEnemy The enemy performing the action
     * @param targetPosition The position the enemy is moving to
     */
    public static void updateDungeon(GameManager gameManager, StandardEnemy standardEnemy, Position targetPosition) {
        new Thread(() -> {
            try {
                if (gameManager != null && gameManager.getGameEventListener() != null) {
                    gameManager.getGameEventListener().collectPowerUp(standardEnemy, targetPosition, standardEnemy.getCurrentRoom());
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error during enemy movement: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Checks if the player is still alive.
     *
     * @param gameManager The game manager handling the game state
     * @return True if the player is alive, false otherwise
     */
    public static boolean playerIsAlive(GameManager gameManager) {
        return gameManager.getPlayer().isAlive();
    }

    /**
     * Transitions to AttackingBehaviour if the player is adjacent to the enemy agent.
     * If the enemy is not a boss, it requests reinforcements.
     *
     * @param standardEnemyAgent The agent controlling the enemy
     * @return True if the player is in melee range and an attack was initiated, false otherwise
     */
    public static boolean attackIfPlayerIsNear(StandardEnemyAgent standardEnemyAgent) {
        Position enemyPos = standardEnemyAgent.getEnemy().getPosition();
        Position playerPos = standardEnemyAgent.getGameManager().getPlayer().getPosition();

        if (enemyPos.isAdjacentTo(playerPos)) {
            // Enemy transitions to attack mode
            standardEnemyAgent.speak("Player in melee range. Attacking.");
            standardEnemyAgent.addBehaviour(
                    new AttackingBehaviour(standardEnemyAgent, StandardEnemyAgent.DEFAULT_ATTACKING_COOLDOWN, standardEnemyAgent.getGameManager().getPlayer())
            ); // 2-second cooldown for attack behavior

            // Request reinforcements if not a boss enemy
            if (!standardEnemyAgent.isBossAgent()) {
                standardEnemyAgent.addBehaviour(
                        new RequestReinforcementsBehaviour(standardEnemyAgent, StandardEnemyAgent.DEFAULT_REQUEST_REINFORCEMENT_DELAY, standardEnemyAgent.getGameManager().getPlayer())
                );
            }

            return true;
        }

        return false;
    }

    /**
     * Moves the enemy to chase the player if the player is within a specified range.
     *
     * @param standardEnemyAgent The agent controlling the enemy
     * @param range              The maximum range to check for the player's position
     * @return True if the player is within range and the chase action was initiated, false otherwise
     */
    public static boolean chaseIfPlayerIsInRange(StandardEnemyAgent standardEnemyAgent, int range) {
        Position enemyPos = standardEnemyAgent.getEnemy().getPosition();
        Position playerPos = standardEnemyAgent.getGameManager().getPlayer().getPosition();

        if (enemyPos.isInRange(playerPos, range)) {
            // Enemy transitions to a chasing mode
            MovementUtils.pursueTarget(standardEnemyAgent, enemyPos, playerPos);
            return true;
        }

        return false;
    }

    /**
     * Stops the enemy's movement and transitions to an idle state if the enemy reaches the target position.
     *
     * @param standardEnemyAgent The agent controlling the enemy
     * @param targetPos          The target position for the enemy
     * @return True if the enemy reached the target position, false otherwise
     */
    public static boolean stopIfTargetPositionReached(StandardEnemyAgent standardEnemyAgent, Position targetPos) {
        Position enemyPos = standardEnemyAgent.getEnemy().getPosition();

        if (enemyPos.equals(targetPos)) {
            // Enemy transitions to idle behavior
            standardEnemyAgent.speak("Target position reached. Player not in sight. Returning to idle.");
            standardEnemyAgent.addBehaviour(new IdleBehaviour(standardEnemyAgent, 1000)); // 1-second cooldown for idle behavior
            return true;
        }
        return false;
    }
}

package agents.behaviours;

import agents.EnemyState;
import agents.StandardEnemyAgent;
import classes.Position;
import classes.PowerUp;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import utils.MessageHandlerUtils;

import java.util.Arrays;
import java.util.Comparator;

/**
 * LowHealthEscapeBehaviour handles the behavior of an enemy agent
 * when its health drops below a critical threshold. The agent
 * transitions into a retreating state and searches for a nearby
 * power-up to recover strength.
 */
public class LowHealthEscapeBehaviour extends TickerBehaviour {
    private final StandardEnemyAgent standardEnemyAgent; // Reference to the enemy agent

    /**
     * Constructor that initializes the behaviour with the given enemy agent.
     * Sets a 1000ms delay between ticks for periodic updates.
     *
     * @param agent The agent this behaviour is attached to
     */
    public LowHealthEscapeBehaviour(StandardEnemyAgent agent) {
        super(agent, 1000);
        this.standardEnemyAgent = agent;
    }

    /**
     * Periodically called based on the delay interval.
     * Checks the agent's health and transitions to a retreating behavior
     * if the health falls below the critical threshold.
     */
    @Override
    public void onTick() {
        // If the player is no longer alive, remove this behaviour
        if (!standardEnemyAgent.getGameManager().getPlayer().isAlive()) {
            standardEnemyAgent.removeBehaviour(this);
            return;
        }

        // Check if the agent's health is below 30%, and it's not already retreating
        if (standardEnemyAgent.getEnemy().getHealthPts() <= (standardEnemyAgent.getEnemy().getMaxHealthPts() * 0.3)
                && !(standardEnemyAgent.isRetreating())) {

            // Update the agent's state to "RETREATING" and notify its intent via a broadcast
            standardEnemyAgent.updateDFState(EnemyState.RETREATING);
            standardEnemyAgent.speak("Retreating due to low health");
            Position currentEnemyPosition = standardEnemyAgent.getEnemy().getPosition();

            // Requesting cover
            MessageHandlerUtils.broadcastAlert(
                    standardEnemyAgent,
                    "RETREATING",
                    Arrays.asList(
                            EnemyState.IDLE,
                            EnemyState.CHASING_TARGET,
                            EnemyState.CHASING_POWERUP,
                            EnemyState.CHASING_PLAYER,
                            EnemyState.REINFORCING,
                            EnemyState.ATTACKING
                    ),
                    currentEnemyPosition,
                    ACLMessage.REQUEST
            );


            // Locate the nearest power-up for strength recovery
            Position nearestPowerUpPosition = findNearestPowerUp();
            if (nearestPowerUpPosition != null) {
                standardEnemyAgent.speak("Moving to the nearest power up");
                // Transition to RetreatingBehaviour to move toward the identified power-up
                standardEnemyAgent.addBehaviour(
                        new RetreatingBehaviour(
                                standardEnemyAgent,
                                500,
                                EnemyState.RETREATING,
                                nearestPowerUpPosition
                        )
                );
                standardEnemyAgent.removeBehaviour(this); // Remove current behavior since transitioned to retreating
            }
        }
    }

    /**
     * Searches for the nearest power-up position in the current room.
     * Prioritizes active power-ups but falls back to any power-up position if none are active.
     *
     * @return The position of the nearest power-up, or null if no power-ups exist.
     */
    private Position findNearestPowerUp() {
        var currentRoom = standardEnemyAgent.getGameManager().getCurrentRoom();

        // Find the closest power-up position. Prioritize active power-ups.
        return currentRoom.getPowerUps().stream()
                .map(PowerUp::getPosition) // Map each power-up to its physical position
                .min(Comparator.comparingDouble(pos ->
                        pos.distanceTo(standardEnemyAgent.getEnemy().getPosition()))) // Find the nearest active power-up
                .or(() -> currentRoom.getPowerUpsPositions().stream()
                        .min(Comparator.comparingDouble(pos ->
                                pos.distanceTo(standardEnemyAgent.getEnemy().getPosition())))) // Fallback to any power-up position
                .orElse(null); // Return null if no power-ups exist
    }
}
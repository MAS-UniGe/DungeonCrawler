package agents.behaviours;

import agents.EnemyState;
import agents.StandardEnemyAgent;
import classes.Position;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import utils.MessageHandlerUtils;
import utils.MovementUtils;
import utils.BehaviourUtils;

import java.util.List;

/**
 * RetreatingBehaviour manages the behavior of an enemy agent when it is attempting to retreat to a
 * specified target position. This behavior is triggered when the enemy's health is low.
 * The agent moves toward the target position (e.g., a power-up)and transitions out of this behavior
 * upon reaching its destination.
 */
public class RetreatingBehaviour extends TickerBehaviour {
    private final StandardEnemyAgent standardEnemyAgent; // Reference to the enemy agent
    private final EnemyState currentState;               // Current state of the enemy (e.g., RETREATING)
    private final Position targetPosition;               // Target position to which the agent is retreating

    /**
     * Constructor that initializes the behavior for retreating.
     * Sets a periodic delay for updates, ensuring the behavior is responsive to changes.
     *
     * @param agent          The agent this behavior is attached to
     * @param delay          The interval in milliseconds for updates via onTick
     * @param currentState   The state of the agent (e.g., RETREATING)
     * @param targetPosition The target position the agent is heading toward (e.g., a power-up)
     */
    public RetreatingBehaviour(StandardEnemyAgent agent, long delay, EnemyState currentState, Position targetPosition) {
        super(agent, delay);
        this.standardEnemyAgent = agent;
        this.currentState = currentState;
        this.targetPosition = targetPosition;
    }

    /**
     * Periodically invoked based on the defined tick interval.
     * Handles the movement of the agent toward its target and monitors its progress.
     */
    @Override
    protected void onTick() {
        // Remove this behavior if the player is no longer alive
        if (!BehaviourUtils.playerIsAlive(standardEnemyAgent.getGameManager())) {
            standardEnemyAgent.removeBehaviour(this);
            return;
        }

        // Update the agent's state in the Directory Facilitator (DF)
        standardEnemyAgent.updateDFState(currentState);

        // Obtain the current position of the enemy
        Position enemyPos = standardEnemyAgent.getEnemy().getPosition();

        // Check if the agent is adjacent to the target position
        if (enemyPos.isAdjacentTo(targetPosition)) {
            // Trigger updates of the dungeon to trigger the collection of the power-up (if present)
            BehaviourUtils.updateDungeon(standardEnemyAgent.getGameManager(), standardEnemyAgent.getEnemy(), targetPosition);

            // Broadcast to other agents that the retreating enemy has reached the retreating position
            MessageHandlerUtils.broadcastAlert(
                    standardEnemyAgent,
                    "RETREATING_POWER_UP_COLLECTED",
                    List.of(EnemyState.COVERING),
                    targetPosition,
                    ACLMessage.INFORM
            );

            // Transition to the IdleBehaviour once reached the retreating position
            standardEnemyAgent.addBehaviour(new IdleBehaviour(standardEnemyAgent, 1000));
            standardEnemyAgent.removeBehaviour(this);
            return;
        }

        // Continue moving toward the target position if not already adjacent
        MovementUtils.pursueTarget(standardEnemyAgent, enemyPos, targetPosition);
    }
}
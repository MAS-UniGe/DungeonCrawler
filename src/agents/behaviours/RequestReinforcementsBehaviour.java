package agents.behaviours;

import agents.EnemyState;
import classes.Player;
import jade.core.behaviours.TickerBehaviour;
import agents.StandardEnemyAgent;
import jade.lang.acl.ACLMessage;
import utils.BehaviourUtils;
import utils.MessageHandlerUtils;

import java.util.Arrays;

/**
 * RequestReinforcementsBehaviour manages the behavior of an enemy agent
 * when reinforcements are required during an active engagement with the player.
 * The agent periodically broadcasts a reinforcement request to other non engaged agents
 * while it remains in the attacking state.
 */
public class RequestReinforcementsBehaviour extends TickerBehaviour {
    private final StandardEnemyAgent standardEnemyAgent; // Reference to the enemy agent
    private final Player player; // Reference to the player

    /**
     * Constructor that initializes the behaviour with the specified enemy agent,
     * interval for update ticks, and the target player involved in the engagement.
     *
     * @param standardEnemyAgent The agent this behaviour is attached to
     * @param interval The interval in milliseconds for the behavior's periodic execution
     * @param player Player object providing the target's position
     */
    public RequestReinforcementsBehaviour(StandardEnemyAgent standardEnemyAgent, long interval, Player player) {
        super(standardEnemyAgent, interval);
        this.standardEnemyAgent = standardEnemyAgent;
        this.player = player;
    }

    /**
     * Periodically invoked based on the defined tick interval.
     * Ensures that reinforcements are requested only when the agent is actively attacking the player.
     */
    @Override
    protected void onTick() {
        // Remove the behavior if the player is no longer alive
        if (!BehaviourUtils.playerIsAlive(standardEnemyAgent.getGameManager())) {
            standardEnemyAgent.removeBehaviour(this);
            return;
        }

        // If the agent is in the "ATTACKING" state, request reinforcements
        if (standardEnemyAgent.getCurrentState().equals(EnemyState.ATTACKING)) {
            standardEnemyAgent.speak("Requesting reinforcements");

            // Requesting reinforcements
            MessageHandlerUtils.broadcastAlert(
                    standardEnemyAgent,
                    "REINFORCEMENT_REQUEST",
                    Arrays.asList(
                            EnemyState.IDLE,
                            EnemyState.CHASING_TARGET,
                            EnemyState.CHASING_POWERUP,
                            EnemyState.REINFORCING
                    ),
                    player.getPosition(), // Include the player's current position for reinforcements to target
                    ACLMessage.REQUEST
            );
        } else {
            // If not attacking, remove this behavior as it's no longer relevant
            standardEnemyAgent.removeBehaviour(this);
        }
    }
}
package agents.behaviours;

import agents.EnemyState;
import classes.StandardEnemy;
import jade.core.behaviours.TickerBehaviour;
import agents.StandardEnemyAgent;
import classes.Position;
import jade.lang.acl.ACLMessage;
import utils.MessageHandlerUtils;
import utils.MovementUtils;
import utils.BehaviourUtils;

import java.util.Arrays;

/**
 * GoingToTargetBehaviour handles the movement of an enemy agent toward a specified target position.
 * While pursuing its target, the agent responds to environmental triggers, such as detecting
 * the player or receiving specific broadcasts from other agents.
 */
public class GoingToTargetBehaviour extends TickerBehaviour {
    private final StandardEnemyAgent standardEnemyAgent; // Reference to the enemy agent
    private EnemyState currentState;                     // Current state of the agent
    private Position targetPosition;                     // Target position to move toward
    private final MessageHandlerUtils messageHandler;    // Utility for handling message broadcasting and processing

    /**
     * Constructor that initializes the behaviour with the enemy agent, delay (tick frequency),
     * current state, and initial target position.
     *
     * @param agent         The agent this behaviour is attached to
     * @param delay         The interval in milliseconds for the behaviour's execution
     * @param currentState  The initial state of the agent (e.g., CHASING_TARGET)
     * @param targetPosition The initial target position for the agent
     */
    public GoingToTargetBehaviour(StandardEnemyAgent agent, long delay, EnemyState currentState, Position targetPosition) {
        super(agent, delay);
        this.standardEnemyAgent = agent;
        this.currentState = currentState;
        this.targetPosition = targetPosition;
        this.messageHandler = new MessageHandlerUtils();
        standardEnemyAgent.speak("Moving to target position");

        registerMessageHandlers(); // Register handlers for specific incoming messages
    }

    /**
     * Periodically called based on the delay interval to update the agent’s behavior.
     * Handles movement toward the target, environmental checks, and reacts to in-game events.
     */
    @Override
    protected void onTick() {
        // If the player is no longer alive, remove this behaviour from the agent
        if (!BehaviourUtils.playerIsAlive(standardEnemyAgent.getGameManager())) {
            standardEnemyAgent.removeBehaviour(this);
            return; // Exit the current tick
        }

        // Update the current state of the agent in the Directory Facilitator (DF)
        standardEnemyAgent.updateDFState(currentState);

        Position enemyPos = standardEnemyAgent.getEnemy().getPosition();

        // Attack the player if they are nearby by transitioning to Attacking behaviour
        // via the attachIfPlayerIsNear utility method
        if (BehaviourUtils.attackIfPlayerIsNear(standardEnemyAgent)) {
            standardEnemyAgent.removeBehaviour(this); // Stop pursuing the target since transitioned to attacking
            return;
        }

        // Process incoming messages if available and the agent is not a boss
        // (this cannot receive messages except in its specific behaviour)
        ACLMessage receivedMessage = standardEnemyAgent.receive();
        if (receivedMessage != null && !standardEnemyAgent.isBossAgent())
            messageHandler.handleMessage(receivedMessage);

        // Check if the enemy has reached the target position and transition to idle behaviour
        // if so via stopIfTargetPositionReached utility method
        if (BehaviourUtils.stopIfTargetPositionReached(standardEnemyAgent, targetPosition)) {
            standardEnemyAgent.removeBehaviour(this); // Stop the behaviour upon reaching the target since transitioned to idle
            return;
        }

        // If the player exists, check for proximity to transition to chasing player
        if (standardEnemyAgent.getGameManager().getPlayer() != null) {
            Position playerPos = standardEnemyAgent.getGameManager().getPlayer().getPosition();

            // Pursue if the player is in range via the chaseIfPlayerIsInRange utility method
            if (BehaviourUtils.chaseIfPlayerIsInRange(standardEnemyAgent, 5)) {
                // Log the detection and alert the system,
                standardEnemyAgent.speak("Player spotted, chasing and alerting other enemies");
                standardEnemyAgent.getGameManager().notifyEnemiesAlerted(standardEnemyAgent);
                MessageHandlerUtils.broadcastAlert(
                        standardEnemyAgent,
                        "PLAYER_SPOTTED",
                        Arrays.asList(EnemyState.IDLE, EnemyState.CHASING_TARGET),
                        playerPos, // Include the player's current position for enemies to target
                        ACLMessage.INFORM
                );

                // Add the chasing behaviour
                standardEnemyAgent.addBehaviour(
                        new ChasingPlayerBehaviour(
                                standardEnemyAgent,
                                1000,
                                EnemyState.CHASING_PLAYER
                        )
                );

                standardEnemyAgent.removeBehaviour(this); // remove this behaviour since transitioned to chasing player behaviour
                return;
            }
        }

        // Continue moving toward the current target position if no other triggers
        MovementUtils.pursueTarget(standardEnemyAgent, enemyPos, targetPosition);
    }

    /**
     * Registers handlers for specific types of messages this agent can process.
     * Each message handler maps to a specific in-game event.
     */
    private void registerMessageHandlers() {
        messageHandler.registerHandler("POWER_UP_COLLECTED", this::handlePowerUpCollected);
        messageHandler.registerHandler("RETREATING", this::handleRetreating);
        messageHandler.registerHandler("POWER_UP_SPOTTED", this::handlePowerUpSpotted);
        messageHandler.registerHandler("REINFORCEMENT_REQUEST", this::handleReinforcementRequest);
    }

    /**
     * Updates the target state and position for the current behaviour.
     *
     * @param newState  The new state to transition to
     * @param message   The incoming message
     * @param posString The string representation of the target position
     */
    private void processTarget(EnemyState newState, ACLMessage message, String posString) {
        currentState = newState;
        standardEnemyAgent.updateDFState(currentState);
        targetPosition = Position.fromString(posString);
    }

    /**
     * Handle the "POWER_UP_COLLECTED" message by transitioning to the chasing power-up state
     * and updating the target position via the processTarget utility method.
     * Broadcasts an alert to other agents as well.
     *
     * @param message The received message to process
     */
    private void handlePowerUpCollected(ACLMessage message) {
        standardEnemyAgent.speak("Player collected a power up nearby, notifying other enemies");
        MessageHandlerUtils.extractMessageData(message).ifPresent(posString -> {
            processTarget(EnemyState.CHASING_POWERUP, message, posString);
            MessageHandlerUtils.broadcastAlert(
                    standardEnemyAgent,
                    "POWER_UP_SPOTTED",
                    Arrays.asList(EnemyState.IDLE, EnemyState.CHASING_TARGET),
                    Position.fromString(posString), // Include the collected power-up's position for other agents to target
                    ACLMessage.INFORM
            );
        });
    }

    /**
     * Handle the "POWER_UP_SPOTTED" message by transitioning to the chasing power-up state
     * and updating the target position via the processTarget utility method.
     *
     * @param message The received message to process
     */
    private void handlePowerUpSpotted(ACLMessage message) {
        String senderName = message.getSender().getLocalName();
        standardEnemyAgent.speak(senderName + " spotted the player near a power up position, going there");
        MessageHandlerUtils.extractMessageData(message).ifPresent(posString -> {
            processTarget(EnemyState.CHASING_POWERUP, message, posString);
        });
    }

    /**
     * Handle the "REINFORCEMENT_REQUEST" message by transitioning to the reinforcement state
     * and updating the target position via the processTarget utility method.
     *
     * @param message The received message to process
     */
    private void handleReinforcementRequest(ACLMessage message) {
        String senderName = message.getSender().getLocalName();
        standardEnemyAgent.speak(senderName + " asked for reinforcements, going there");
        MessageHandlerUtils.extractMessageData(message).ifPresent(posString -> {
            processTarget(EnemyState.REINFORCING, message, posString);
        });
    }

    /**
     * Handle the "RETREATING" message by transitioning to CoverRetreatBehaviour.
     *
     * @param message The received message to process
     */
    private void handleRetreating(ACLMessage message) {
        String senderName = message.getSender().getLocalName();
        standardEnemyAgent.speak(senderName + " is retreating, covering retreat for ally.");

        // Acknowledge the request
        ACLMessage reply = message.createReply(); // Create a reply to the sender
        try {
            // Process the data in the received message
            MessageHandlerUtils.extractMessageData(message).ifPresent(data -> {
                // Send an AGREE message to acknowledge the request
                reply.setPerformative(ACLMessage.AGREE);
                reply.setContent("Covering retreat for ally");
                standardEnemyAgent.send(reply);

                // Transition to behaviour for covering the retreating ally
                Position playerPosition = standardEnemyAgent.getGameManager().getPlayer().getPosition();
                StandardEnemy retreatingEnemy = standardEnemyAgent.getGameManager().getEnemyByName(data);

                standardEnemyAgent.addBehaviour(
                        new CoverRetreatBehaviour(standardEnemyAgent, retreatingEnemy, playerPosition)
                );

                standardEnemyAgent.removeBehaviour(this); // Remove idle state since CoverRetreat is the new behavior
            });
        } catch (Exception e) {
            e.printStackTrace(); // Log exceptions if any issues occur during processing
        }
    }
}
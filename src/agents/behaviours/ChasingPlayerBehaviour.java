package agents.behaviours;

import agents.EnemyState;
import classes.StandardEnemy;
import jade.core.behaviours.TickerBehaviour;
import agents.StandardEnemyAgent;
import classes.Position;
import jade.lang.acl.ACLMessage;
import utils.BehaviourUtils;
import utils.MessageHandlerUtils;

/**
 * ChasingPlayerBehaviour manages the behavior of an enemy when it is actively chasing the player.
 * This behavior is triggered when the player is detected within a specified range. It continuously monitors
 * the player's position to pursue, transitions to other behaviors if the player is no longer in sight
 * or the enemy is close enough to attack or as a reaction to other agents' messages.
 */
public class ChasingPlayerBehaviour extends TickerBehaviour {
    private final StandardEnemyAgent standardEnemyAgent; // Reference to the enemy agent
    private EnemyState currentState;                    // Current state of the agent (e.g., CHASING_PLAYER)
    private final MessageHandlerUtils messageHandler;   // Utility for handling message broadcasts and processing

    /**
     * Constructor initializes the chasing behavior.
     * Sets the update delay and initializes helper utilities.
     *
     * @param agent          The agent this behavior is attached to
     * @param delay          The interval in milliseconds for behavior updates
     * @param currentState   The initial state of the agent (e.g., CHASING_PLAYER)
     */
    public ChasingPlayerBehaviour(StandardEnemyAgent agent, long delay, EnemyState currentState) {
        super(agent, delay);
        this.standardEnemyAgent = agent;
        this.currentState = currentState;
        this.messageHandler = new MessageHandlerUtils();

        registerMessageHandlers(); // Register necessary message handlers
    }

    /**
     * Executes periodically to update the enemy's chasing behavior.
     * Continuously monitors the player's position and adapts to the player state.
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

        // Check if the player is within attack range, and engage the player if possible
        if (BehaviourUtils.attackIfPlayerIsNear(standardEnemyAgent)) {
            standardEnemyAgent.removeBehaviour(this);  // Remove chasing behavior since transitioned to attacking
            return;
        }

        // Process incoming messages if available and the agent is not a boss
        // (this cannot receive messages except in its specific behaviour)
        ACLMessage receivedMessage = standardEnemyAgent.receive();
        if (receivedMessage != null && !standardEnemyAgent.isBossAgent())
            messageHandler.handleMessage(receivedMessage);

        // Handle chasing the player
        if (currentState == EnemyState.CHASING_PLAYER && standardEnemyAgent.getGameManager().getPlayer() != null) {
            // If the player is out of range, transition back to idle behavior,
            // otherwise pursue via the chaseIfPlayerIsInRange utility method
            if (!BehaviourUtils.chaseIfPlayerIsInRange(standardEnemyAgent, 5)) {
                standardEnemyAgent.speak("Player out of sight. Returning to idle.");
                standardEnemyAgent.addBehaviour(new IdleBehaviour(standardEnemyAgent, 1000));
                standardEnemyAgent.removeBehaviour(this);
            }
            // Log the continuing of the chase
            else {
                standardEnemyAgent.speak("Player still in sight. Keep chasing.");
            }
        }
    }

    /**
     * Registers handlers for processing specific types of messages.
     */
    private void registerMessageHandlers() {
        messageHandler.registerHandler("RETREATING", this::handleRetreating);
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

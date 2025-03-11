package agents.behaviours;

import agents.EnemyState;
import classes.Player;
import classes.StandardEnemy;
import jade.core.behaviours.TickerBehaviour;
import agents.StandardEnemyAgent;
import utils.MessageHandlerUtils;
import classes.Position;
import jade.lang.acl.ACLMessage;
import java.util.Arrays;

/**
 * IdleBehaviour manages the idle state of an enemy agent.
 * While in idle state, the agent monitors environmental triggers like
 * the presence of a player or broadcasts from other agents.
 */
public class IdleBehaviour extends TickerBehaviour {
    private final StandardEnemyAgent standardEnemyAgent; // Reference to the enemy agent
    private final MessageHandlerUtils messageHandler;    // Utility for handling message broadcasting and processing

    /**
     * Constructor that initializes the behaviour with the enemy agent and
     * sets the delay (the tick frequency).
     *
     * @param agent The agent this behaviour is attached to
     * @param delay The interval in milliseconds for the behaviour's execution
     */
    public IdleBehaviour(StandardEnemyAgent agent, long delay) {
        super(agent, delay);
        this.standardEnemyAgent = agent;
        this.messageHandler = new MessageHandlerUtils();

        registerMessageHandlers(); // Register handlers for various incoming messages
    }

    /**
     * Called periodically based on the delay interval.
     * Handles state updates, monitoring the player’s behavior, and reacting to events.
     */
    @Override
    protected void onTick() {
        // If the player is no longer alive, remove this behaviour
        if (!standardEnemyAgent.getGameManager().getPlayer().isAlive()) {
            standardEnemyAgent.removeBehaviour(this);
            return; // Exit the current tick
        }

        // Update the agent's state in the Directory Facilitator (idle state).
        // This allows other agents to recognize its state.
        standardEnemyAgent.updateDFState(EnemyState.IDLE);

        Player player = standardEnemyAgent.getGameManager().getPlayer();
        StandardEnemy enemy = standardEnemyAgent.getEnemy(); // Reference to the associated game enemy

        // Return if the player is not in the same room as the enemy,
        // a failsafe to avoid the behaviour to be executed
        if (!player.getCurrentRoom().equals(enemy.getCurrentRoom()))
            return;

        Position playerPos = player.getPosition();
        Position enemyPos = enemy.getPosition();

        if (enemyPos.isInRange(playerPos, 5)) {
            // Log the detection and alert the system,
            standardEnemyAgent.speak("Player spotted, chasing and alerting other enemies");
            standardEnemyAgent.getGameManager().notifyEnemiesAlerted(standardEnemyAgent);

            // Broadcast an alert to other enemy agents
            MessageHandlerUtils.broadcastAlert(
                    standardEnemyAgent,
                    "PLAYER_SPOTTED",
                    Arrays.asList(EnemyState.IDLE, EnemyState.CHASING_TARGET),
                    playerPos, // Include the player's current position for agents to target
                    ACLMessage.INFORM
            );

            // Transition to chasing player behavior
            standardEnemyAgent.addBehaviour(
                    new ChasingPlayerBehaviour(
                            standardEnemyAgent,
                            1000,
                            EnemyState.CHASING_PLAYER
                    )
            );

            // Remove the current idle behaviour since the agent is actively chasing the player
            standardEnemyAgent.removeBehaviour(this);
            return;
        }

        // Process incoming messages if available and the agent is not a boss
        // (this cannot receive messages except in its specific behaviour)
        ACLMessage receivedMessage = standardEnemyAgent.receive();
        if (receivedMessage != null && !standardEnemyAgent.isBossAgent())
            messageHandler.handleMessage(receivedMessage);
    }

    /**
     * Registers handlers for different types of messages that this agent can process.
     * Each message handler maps to a specific event type.
     */
    private void registerMessageHandlers() {
        messageHandler.registerHandler("PLAYER_SPOTTED", this::handlePlayerSpotted);
        messageHandler.registerHandler("POWER_UP_COLLECTED", this::handlePowerUpCollected);
        messageHandler.registerHandler("POWER_UP_SPOTTED", this::handlePowerUpSpotted);
        messageHandler.registerHandler("REINFORCEMENT_REQUEST", this::handleReinforcementRequest);
        messageHandler.registerHandler("RETREATING", this::handleRetreating);
    }

    /**
     * Processes a response message and updates the agent's state accordingly.
     *
     * @param newState  The new state to transition to
     * @param message   The incoming message
     * @param posString The string representation of the position to move to
     */
    private void processResponse(EnemyState newState, ACLMessage message, String posString) {
        Position spottedPosition = Position.fromString(posString);

        // Transition to going to target behaviour
        standardEnemyAgent.addBehaviour(
                new GoingToTargetBehaviour(
                        standardEnemyAgent,
                        1000,
                        newState,
                        spottedPosition
                )
        );

        standardEnemyAgent.removeBehaviour(this); // Remove idle behaviour since agent is now transitioning
        standardEnemyAgent.updateDFState(newState); // Update state in the Directory Facilitator
    }

    /**
     * Handle the "PLAYER_SPOTTED" message by transitioning to the chasing target state
     * and the going to target behaviour via the processResponse utility method.
     *
     * @param message The received message to process
     */
    private void handlePlayerSpotted(ACLMessage message) {
        String senderName = message.getSender().getLocalName();
        standardEnemyAgent.speak(senderName + " spotted the player, going there");
        MessageHandlerUtils.extractMessageData(message).ifPresent(posString -> {
            processResponse(EnemyState.CHASING_TARGET, message, posString);
        });
    }

    /**
     * Handle the "POWER_UP_COLLECTED" message by transitioning to the chasing target state
     * and the going to target behaviour via the processResponse utility method. Broadcast
     * an alert to other agents as well.
     *
     * @param message The received message to process
     */
    private void handlePowerUpCollected(ACLMessage message) {
        standardEnemyAgent.speak("Player collected a power up nearby, notifying other enemies");
        MessageHandlerUtils.extractMessageData(message).ifPresent(posString -> {
            processResponse(EnemyState.CHASING_POWERUP, message, posString);
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
     * Handle "POWER_UP_SPOTTED" by transitioning to the chasing power-up state
     * and the going to target behaviour via the processResponse utility method.
     *
     * @param message The received message to process
     */
    private void handlePowerUpSpotted(ACLMessage message) {
        String senderName = message.getSender().getLocalName();
        standardEnemyAgent.speak(senderName + " spotted the player near a power up position, going there");
        MessageHandlerUtils.extractMessageData(message).ifPresent(posString -> {
            processResponse(EnemyState.CHASING_POWERUP, message, posString);
        });
    }

    /**
     * Handle "REINFORCEMENT_REQUEST" by transitioning to the reinforcing state
     * and the going to target behaviour via the processResponse utility method.
     *
     * @param message The received message to process
     */
    private void handleReinforcementRequest(ACLMessage message) {
        String senderName = message.getSender().getLocalName();
        standardEnemyAgent.speak(senderName + " asked for reinforcements, going there");

        ACLMessage reply = message.createReply(); // Create a reply to the sender

        MessageHandlerUtils.extractMessageData(message).ifPresent(posString -> {
            // Send an AGREE message to acknowledge the request
            reply.setPerformative(ACLMessage.AGREE);
            reply.setContent("Reinforcing ally");
            standardEnemyAgent.send(reply);

            processResponse(EnemyState.REINFORCING, message, posString);
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
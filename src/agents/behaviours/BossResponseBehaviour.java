package agents.behaviours;

import agents.BossEnemyAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import utils.MessageHandlerUtils;

/**
 * BossResponseBehaviour manages the behavior of a boss enemy agent
 * when it needs to respond to alerts from its minions. The boss continuously
 * listens for messages and dynamically adjusts its attributes or actions based
 * on those alerts.
 */
public class BossResponseBehaviour extends CyclicBehaviour {
    private final BossEnemyAgent bossEnemyAgent;        // Reference to the boss enemy agent
    private final MessageHandlerUtils messageHandler;   // Utility for handling message broadcasting and processing

    /**
     * Constructor that initializes the response behavior for the boss enemy.
     * The boss begins listening for alerts from StandardEnemyAgents.
     *
     * @param agent The boss agent this behavior is attached to
     */
    public BossResponseBehaviour(BossEnemyAgent agent) {
        super(agent);
        bossEnemyAgent = agent;
        this.messageHandler = new MessageHandlerUtils();

        registerMessageHandlers(); // Register handlers for processing specific messages
    }

    /**
     * Continuously monitors for messages. Processes any incoming alerts and reacts accordingly.
     * If the player is no longer active, this behavior is removed.
     */
    @Override
    public void action() {
        // If the player is no longer alive, remove this behavior
        if (!bossEnemyAgent.getGameManager().getPlayer().isAlive()) {
            bossEnemyAgent.removeBehaviour(this);
            return;
        }

        // Receive and process incoming messages
        ACLMessage message = myAgent.receive();
        if (message != null)
            messageHandler.handleMessage(message);
    }

    /**
     * Registers handlers for specific types of messages.
     * This ensures dynamic responses to alerts from StandardEnemyAgents.
     */
    private void registerMessageHandlers() {
        messageHandler.registerHandler("PLAYER_SURVIVAL_ALERT", this::handlePlayerSurvival);
    }

    /**
     * Handles the "PLAYER_SURVIVAL_ALERT" message.
     * Upon receiving the alert, the boss enhances its attributes to prepare for confrontation
     * and removes this behavior from the boss agent.
     *
     * @param message The received alert message from minions
     */
    private void handlePlayerSurvival(ACLMessage message) {
        bossEnemyAgent.speak("My minions alerted me of your presence. Enhancing attributes");
        bossEnemyAgent.getEnemy().enhanceAttributes(); // Enhance the boss's abilities dynamically
        bossEnemyAgent.removeBehaviour(this);          // Remove this behavior after reacting to the alert
    }
}
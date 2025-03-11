package agents.behaviours;

import agents.StandardEnemyAgent;
import agents.EnemyState;
import classes.BossEnemy;
import classes.StandardEnemy;
import jade.core.behaviours.TickerBehaviour;
import classes.Player;
import classes.Position;
import jade.lang.acl.ACLMessage;
import utils.MessageHandlerUtils;

/**
 * AttackingBehaviour manages the attack phase of an enemy agent when engaged with the player.
 * The behavior ensures the enemy continues attacking the player as long as the player is adjacent
 * and the enemy is not in a retreating state.
 */
public class AttackingBehaviour extends TickerBehaviour {
    private final StandardEnemyAgent standardEnemyAgent; // Reference to the enemy agent
    private final Player player;                         // Reference to the player being attacked
    private final MessageHandlerUtils messageHandler;   // Utility for handling message broadcasts and processing

    /**
     * Constructor to initialize the attacking behavior.
     *
     * @param agent  The enemy agent initiating the attack
     * @param delay  Execution interval (tick delay) for attack attempts
     * @param player The player being targeted
     */
    public AttackingBehaviour(StandardEnemyAgent agent, long delay, Player player) {
        super(agent, delay);
        this.standardEnemyAgent = agent;
        this.player = player;
        this.messageHandler = new MessageHandlerUtils();

        registerMessageHandlers(); // Register necessary message handlers
    }

    /**
     * Periodically executes the attack logic. Checks if the player is adjacent to be targeted
     * for melee attacks and continues attacking or transitions back to chasing if required.
     */
    @Override
    protected void onTick() {
        // Remove attacking behavior if the player is no longer alive
        if (!player.isAlive()) {
            standardEnemyAgent.removeBehaviour(this);
            return;
        }

        // Interrupt attacking behavior if the enemy is retreating
        if (standardEnemyAgent.isRetreating()) {
            standardEnemyAgent.removeBehaviour(this);
            return;
        }

        // Update the Directory Facilitator (DF) state to "ATTACKING" unless in a covering state
        if (standardEnemyAgent.getCurrentState() != EnemyState.COVERING)
            standardEnemyAgent.updateDFState(EnemyState.ATTACKING);

        Position enemyPos = standardEnemyAgent.getEnemy().getPosition();
        Position playerPos = player.getPosition();

        // Perform attack if the player is still adjacent, else transition back to chasing
        if (enemyPos.isAdjacentTo(playerPos)) {
            attackPlayer(); // Execute the attack action
        } else {
            // If the player moves out of melee range, switch back to chasing behavior if not in covering state
            if (standardEnemyAgent.getCurrentState() != EnemyState.COVERING) {
                standardEnemyAgent.speak("Player moved out of melee range. Returning to chasing.");
                standardEnemyAgent.addBehaviour(new ChasingPlayerBehaviour(standardEnemyAgent, 1000, EnemyState.CHASING_PLAYER));
            }
            standardEnemyAgent.removeBehaviour(this); // Remove the attacking behavior since player out of range for melee attacks
        }

        // Process incoming messages if available and the agent is not a boss
        // (this cannot receive messages except in its specific behaviour)
        ACLMessage receivedMessage = standardEnemyAgent.receive();
        if (receivedMessage != null && !standardEnemyAgent.isBossAgent())
            messageHandler.handleMessage(receivedMessage);
    }

    /**
     * Executes the attack on the player.
     * If the agent is a BossEnemyAgent, the additional logic for special attacks is dealt with.
     */
    private void attackPlayer() {
        new Thread(() -> {
            try {
                if (standardEnemyAgent.getGameManager() != null && standardEnemyAgent.getEnemy() != null) {
                    boolean useSpecialAttack = false;

                    // Boss agents have a 20% chance to use a special attack
                    if (standardEnemyAgent.isBossAgent()) {
                        useSpecialAttack = Math.random() < 0.2;
                        if (useSpecialAttack) {
                            // Perform the special attack and notify the game manager
                            standardEnemyAgent.getGameManager().specialAttack((BossEnemy) standardEnemyAgent.getEnemy());
                            standardEnemyAgent.speak("Using special attack");
                        }
                    }

                    // Perform the standard attack (for BossEnemyAgent only if needed), log the attack outcome and notify the game event listener
                    if (useSpecialAttack || standardEnemyAgent.getGameManager().meleeAttack(standardEnemyAgent.getEnemy(), player)) {
                        standardEnemyAgent.speak("Attacked you successfully!");
                        standardEnemyAgent.getGameManager().getGameEventListener().onPlayerAttacked(player);
                    } else
                        standardEnemyAgent.speak("Missed you!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error during enemy-player interaction: " + e.getMessage());
            }
        }).start();
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
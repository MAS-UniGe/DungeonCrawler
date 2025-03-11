package agents.behaviours;

import agents.EnemyState;
import classes.StandardEnemy;
import jade.core.behaviours.TickerBehaviour;
import agents.StandardEnemyAgent;
import classes.Position;
import jade.lang.acl.ACLMessage;
import utils.BehaviourUtils;
import utils.MovementUtils;
import utils.MessageHandlerUtils;

import static utils.BehaviourUtils.attackIfPlayerIsNear;

/**
 * CoverRetreatBehaviour manages the behavior of an enemy agent
 * providing cover during the retreat of another allied enemy.
 * The agent moves dynamically to optimal positions and attacks
 * the player to protect the retreating enemy.
 */
public class CoverRetreatBehaviour extends TickerBehaviour {
    private final StandardEnemyAgent standardEnemyAgent; // Reference to the enemy agent
    private final StandardEnemy retreatingEnemy;         // Reference to the retreating ally
    private Position playerPosition;                     // Current position of the player
    private final MessageHandlerUtils messageHandler;    // Utility for handling message broadcasting and processing

    /**
     * Constructor to initialize the behaviour for covering another retreating agent.
     * Sets a 500ms delay for frequent updates, ensuring responsiveness to dynamic events.
     *
     * @param agent          The agent this behaviour is attached to
     * @param retreatingEnemy The allied enemy this agent is covering
     * @param playerPosition The player's current position
     */
    public CoverRetreatBehaviour(StandardEnemyAgent agent, StandardEnemy retreatingEnemy, Position playerPosition) {
        super(agent, 500);
        this.standardEnemyAgent = agent;
        this.retreatingEnemy = retreatingEnemy;
        this.playerPosition = playerPosition;
        this.messageHandler = new MessageHandlerUtils();

        registerMessageHandlers(); // Register handlers for specific incoming messages
    }

    /**
     * Periodically called based on the delay interval.
     * The agent dynamically moves to a cover position, monitors
     * the retreating enemy and player, and reacts accordingly.
     */
    @Override
    protected void onTick() {
        // Check if the player is alive; if not, remove this behaviour
        if (!BehaviourUtils.playerIsAlive(standardEnemyAgent.getGameManager())) {
            standardEnemyAgent.removeBehaviour(this);
            return;
        }

        // If the agent itself transitions into a retreating state, stop covering
        if (standardEnemyAgent.isRetreating()) {
            standardEnemyAgent.removeBehaviour(this);
            return;
        }

        // Update the agent's state to "COVERING" in the Directory Facilitator (DF)
        standardEnemyAgent.updateDFState(EnemyState.COVERING);

        // Update the player position if available; else, transition to IdleBehaviour
        if (standardEnemyAgent.getGameManager().getPlayer() != null)
            playerPosition = standardEnemyAgent.getGameManager().getPlayer().getPosition();
        else {
            standardEnemyAgent.addBehaviour(new IdleBehaviour(standardEnemyAgent, 1000));
            standardEnemyAgent.removeBehaviour(this);
            return;
        }

        // If the retreating enemy is dead, transition back to idle behaviour
        if (retreatingEnemy == null || !retreatingEnemy.isAlive()) {
            standardEnemyAgent.speak("Returning to idle since retreating enemy defeated");
            standardEnemyAgent.addBehaviour(new IdleBehaviour(standardEnemyAgent, 1000));
            standardEnemyAgent.removeBehaviour(this);
            return;
        }

        // Process incoming messages if available and the agent is not a boss
        // (this cannot receive messages except in its specific behaviour)
        ACLMessage receivedMessage = standardEnemyAgent.receive();
        if (receivedMessage != null && !standardEnemyAgent.isBossAgent())
            messageHandler.handleMessage(receivedMessage);

        // If player comes into attack range, initiate an attack
        attackIfPlayerIsNear(standardEnemyAgent);

        // Calculate and move the agent to the optimal cover position
        Position coverPosition = calculateCoverPosition(playerPosition, retreatingEnemy.getPosition());
        moveToCoverPosition(coverPosition);
    }

    /**
     * Registers message handlers for specific in-game events.
     * Covering agents respond only to the successful retreat of the retreating agent
     */
    private void registerMessageHandlers() {
        messageHandler.registerHandler("RETREATING_POWER_UP_COLLECTED", this::handlePowerUpCollected);
    }

    /**
     * Handles the "RETREATING_POWER_UP_COLLECTED" message.
     * Transitions the agent to IdleBehaviour as the cover duty is no longer necessary.
     *
     * @param message The received message to process
     */
    private void handlePowerUpCollected(ACLMessage message) {
        standardEnemyAgent.speak("Player collected a power up nearby, notifying other enemies");
        standardEnemyAgent.addBehaviour(new IdleBehaviour(standardEnemyAgent, 1000));
        standardEnemyAgent.removeBehaviour(this);
    }

    /**
     * Calculate the optimal cover position for the agent to provide
     * protection between the retreating enemy and the player.
     *
     * @param playerPos      The player's current position
     * @param retreatingPos  The retreating enemy's current position
     * @return The calculated cover position
     */
    private Position calculateCoverPosition(Position playerPos, Position retreatingPos) {
        // Compute a vector from the player to the retreating enemy
        int dx = retreatingPos.getX() - playerPos.getX();
        int dy = retreatingPos.getY() - playerPos.getY();

        // Normalize the vector to determine movement direction
        int normX = Integer.signum(dx); // X-directional movement: -1, 0, or 1
        int normY = Integer.signum(dy); // Y-directional movement: -1, 0, or 1

        // Calculate the candidate cover position based on the retreating enemy's position
        Position candidatePos = new Position(
                retreatingPos.getX() - normX,
                retreatingPos.getY() - normY
        );

        // If the candidate position is not valid, try to find an alternate nearest valid position
        if (!MovementUtils.isPositionValid(candidatePos, standardEnemyAgent.getGameManager())) {
            return MovementUtils.findNearestValidPosition(
                    retreatingPos,
                    playerPos,
                    standardEnemyAgent.getGameManager()
            );
        }

        return candidatePos; // Return the valid cover position
    }

    /**
     * Moves the agent to the provided cover position. If movement is successful,
     * notifies the GameManager about the position change.
     *
     * @param coverPosition The target cover position for the agent
     */
    private void moveToCoverPosition(Position coverPosition) {
        Position currentPos = standardEnemyAgent.getEnemy().getPosition();

        // If already at the target position, no movement is needed
        if (!currentPos.equals(coverPosition))
            MovementUtils.pursueTarget(standardEnemyAgent, currentPos, coverPosition);
    }
}
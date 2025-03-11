package agents;

import agents.behaviours.BossAlertBehaviour;
import agents.behaviours.IdleBehaviour;
import agents.behaviours.LowHealthEscapeBehaviour;
import jade.core.AID;
import jade.core.Agent;
import classes.GameManager;
import classes.StandardEnemy;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.Property;

/**
 * The StandardEnemyAgent class represents an enemy agent in the game.
 * It is capable of interacting with the game environment, updating its state,
 * and performing specific behaviors like idling, escaping, attacking, and alerting the boss.
 */
public class StandardEnemyAgent extends Agent {
    // Constants
    protected static final long DEFAULT_BEHAVIOUR_DELAY = 1000; // 1-second delay for ticked behaviors
    private static final int BOSS_ALERT_DELAY = 120000; // 60-second delay for sending alerts to the boss agent
    public static final int DEFAULT_ATTACKING_COOLDOWN = 2000; //2-second cooldown for attacking behaviours
    public static final int DEFAULT_REQUEST_REINFORCEMENT_DELAY = 5000; //5-second delay to ask for reinforcements
    private static final String ENEMY_AGENT_TYPE = "enemy"; // Service type for enemy agents (used in Directory Facilitator)

    // Attributes
    protected StandardEnemy enemy; // Reference to the enemy's game representation
    protected GameManager gameManager; // Reference to the game manager controlling game state
    protected EnemyState currentState = EnemyState.IDLE; // Initial state of the enemy (Idle)
    protected AID bossAgentAID; // AID (Agent Identifier) of the boss associated with this enemy

    /**
     * Setup method that initializes the agent upon its creation.
     * It prepares the agent by initializing its attributes,
     * registering it in the DF, waiting for the GUI readiness,
     * and initiating its behaviours.
     */
    @Override
    protected void setup() {
        // Initialize internal agent attributes from passed arguments
        initializeAgent();

        // Wait for the GUI to be initialized before execution
        waitForGuiReady();

        // Register this agent with the Directory Facilitator using the default state (Idle)
        registerWithDF(EnemyState.IDLE);

        // Initialize and add relevant behaviors for this enemy agent
        initializeBehaviours();
    }

    /**
     * Extract and initialize agent parameters from arguments passed during initialization.
     */
    protected void initializeAgent() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            this.enemy = (StandardEnemy) args[0];
            this.gameManager = (GameManager) args[1];
            this.bossAgentAID = (AID) args[2];
        }
    }

    /**
     * Wait for the game interface (GUI) latch to ensure proper setup before this agent runs.
     */
    private void waitForGuiReady() {
        try {
            gameManager.getGuiReadyLatch().await(); // Blocks execution until the GUI is ready
        } catch (InterruptedException e) {
            System.err.println("Error waiting for GUI readiness");
            e.printStackTrace();
        }
    }

    /**
     * Initializes and add the starting behaviors for this enemy agent.
     */
    protected void initializeBehaviours() {
        // Add Idle behavior that executes with a periodic delay
        addBehaviour(new IdleBehaviour(this, DEFAULT_BEHAVIOUR_DELAY));

        // Add escape behavior for the agent when health is low
        addBehaviour(new LowHealthEscapeBehaviour(this));

        // Add a behavior to alert the boss agent
        setupBossAlert(this, BOSS_ALERT_DELAY); // Boss alert scheduled after BOSS_ALERT_DELAY seconds

        // Monitor and terminate agent if the player or enemy is no longer alive
        addBehaviour(new TickerBehaviour(this, DEFAULT_BEHAVIOUR_DELAY) {
            @Override
            protected void onTick() {
                if (!enemy.isAlive() || !gameManager.getPlayer().isAlive()) {
                    doDelete();
                }
            }
        });
    }

     protected void registerWithDF(EnemyState state) {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID()); // Register with this agent's AID
            ServiceDescription sd = new ServiceDescription();
            sd.setType("enemy");  // Defines this agent type
            sd.setName("EnemyAgent_" + getAID().getLocalName()); // Descriptive name
            sd.addProperties(new Property("state", state));
            dfd.addServices(sd);

            DFService.register(this, dfd); // Register with JADE DF
            currentState = state; // Update the local state tracker
        } catch (Exception e) {
            System.err.println("Failed to register EnemyAgent with DF");
            e.printStackTrace();
        }
    }

    public void updateDFState(EnemyState newState) {
        if (newState.equals(currentState)) {
            return;
        }

        try {
            // Deregister and re-register with updated state
            DFService.deregister(this); // Remove current registration
            registerWithDF(newState);  // Re-register with the new state
        } catch (Exception e) {
            System.err.println("Failed to update EnemyAgent state in DF");
            e.printStackTrace();
        }
    }

    /**
     * Helper method to schedule a Boss Alert behaviour for an enemy agent after a delay.
     *
     * @param agent The enemy agent that needs to send the alert
     * @param delay The time in milliseconds to delay before triggering the alert
     */
    public static void setupBossAlert(StandardEnemyAgent agent, long delay) {
        agent.addBehaviour(new WakerBehaviour(agent, delay) {
            @Override
            protected void onWake() {
                // Trigger the one-time Boss Alert Behavior
                agent.addBehaviour(new BossAlertBehaviour(agent));
            }
        });
    }

    /**
     * Log messages through the game event listener associated with this agent.
     *
     * @param message The message to log
     */
    public void speak(String message) {
        getGameManager().getGameEventListener()
                .printLogMessage(getAID().getLocalName() + ": " + message);
    }

    /**
     * Cleans up resources and remove the enemy from the game's list of active enemies
     * when this agent is being destroyed.
     */
    @Override
    protected void takeDown() {
        if (enemy != null) {
            String enemyName = getAID().getLocalName();
            gameManager.getEnemies().remove(enemyName); // Remove the reference of this enemy from the game
            enemy = null; // Nullify the reference to indicate clean up
        }
    }

    // Accessor Methods
    public StandardEnemy getEnemy() {
        return this.enemy;
    }

    public GameManager getGameManager() {
        return this.gameManager;
    }

    public AID getBossAgentAID() {
        return this.bossAgentAID;
    }

    public EnemyState getCurrentState() {
        return this.currentState;
    }

    public boolean isBossAgent() {
        return false;
    }

    public boolean isRetreating() {
        return currentState == EnemyState.RETREATING;
    }
}
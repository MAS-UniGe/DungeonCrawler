package classes;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import agents.StandardEnemyAgent;
import com.google.gson.JsonParseException;
import jade.core.AID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.core.Runtime;
import jade.wrapper.StaleProxyException;

/**
 * The GameManager class orchestrates the game, controlling its main flow, player interactions,
 * agents spawning, rooms, and integration with external systems like JADE and Rasa.
 */
public class GameManager {
    // ----------------------------------------------
    // Constants
    // ----------------------------------------------
    private static final String RASA_BASE_URL = "http://localhost:5005"; // RASA URL
    private static final int ATTACK_COOLDOWN_MS = 500; // Melee attack cooldown (0.5 seconds)
    private static final int RANGED_ATTACK_COOLDOWN_MS = 1000; // Ranged attack cooldown (1 second)

    // ----------------------------------------------
    // Core Game Components
    // ----------------------------------------------
    private final Dungeon dungeon; // The dungeon containing all rooms
    private Player player; // The player's character
    private Room currentRoom; // The room the player is currently in
    private int currentRoomIndex = 0; // Index of the current room

    // ----------------------------------------------
    // JADE Agent Management
    // ----------------------------------------------
    private Runtime runtime; // The JADE runtime
    private AgentContainer container; // Container for managing agents
    private AgentController proxyAgentController; // Proxy agent for communication
    private AID bossAgentAID; // Boss enemy agent identifier
    private final Map<String, StandardEnemy> enemies = new HashMap<>(); // Map for tracking enemies instances

    // ----------------------------------------------
    // Game Listeners and Synchronization
    // ----------------------------------------------
    private GameEventListener gameEventListener; // Game event listener interface
    private final CountDownLatch guiReadyLatch = new CountDownLatch(1); // Synchronization latch for GUI readiness

    // ----------------------------------------------
    // Cooldowns and Task Scheduling
    // ----------------------------------------------
    private boolean isAttackOnCooldown = false; // Flag to check whether the attack is on cooldown
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // Scheduler to deal with the cooldown management

    // ----------------------------------------------
    // Rasa Integration
    // ----------------------------------------------
    private final RasaBridge rasaBridge; // Rasa communication bridge

    /**
     * Constructor initializes game components and setups JADE and the game state.
     */
    public GameManager() {
        dungeon = new Dungeon();
        rasaBridge = new RasaBridge(RASA_BASE_URL);
        setupJADE();
        initializeGame();
    }

    // ------------------------------------------------------------------------------------------------
    // Core Game Setup Methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Sets up the JADE runtime and creates a proxy agent for message delivery.
     */
    private void setupJADE() {
        try {
            runtime = Runtime.instance();
            ProfileImpl profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN, Boolean.TRUE.toString()); // Main container
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            profile.setParameter(Profile.MAIN_PORT, "1099"); // Default port

            // Initialize JADE main container
            container = runtime.createMainContainer(profile);
            System.out.println("JADE platform initialized successfully");

            // Create and start a ProxyAgent
            proxyAgentController = container.createNewAgent(
                    "ProxyAgent",
                    "agents.ProxyAgent",
                    null
            );
            proxyAgentController.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error initializing the JADE platform: " + e.getMessage());
        }
    }

    /**
     * Initializes the game by creating rooms, starting the current room, and setting up the player and enemies.
     */
    private void initializeGame() {
        // Setup rooms with their own configurations
        setupRooms();

        // Prepare player attributes and placement
        Room startingRoom = dungeon.getRoom(0);
        Position startingPosition = new Position(startingRoom.getWidth() / 2, startingRoom.getHeight() - 2);
        player = new Player(startingPosition, startingRoom);
        setCurrentRoom(startingRoom);

        playRoom(startingRoom); // Starts the game in the first room
        chatWithBot("Hi"); // Initial interaction with Rasa
    }

    /**
     * Configures and populates rooms in the dungeon.
     */
    private void setupRooms() {
        dungeon.addRoom(createRoom(
                40, 40,
                List.of(new StandardEnemy(new Position(10, 10))),
                List.of(new PowerUp(PowerUpType.ATTACK, new Position(15, 15)))
        ));

        dungeon.addRoom(createRoom(
                40, 40,
                List.of(new StandardEnemy(new Position(5, 5)), new StandardEnemy(new Position(10, 10)),
                        new StandardEnemy(new Position(20, 20))),
                List.of(new PowerUp(PowerUpType.HEALTH, new Position(8, 8)), new PowerUp(PowerUpType.DEFENSE, new Position(25, 30)))
        ));

        // Room with the boss enemy
        BossEnemy bossEnemy = new BossEnemy(new Position(20, 20));
        dungeon.addRoom(createRoom(40, 40, List.of(bossEnemy), List.of()));
        bossAgentAID = spawnBossAgent(bossEnemy);
    }

    /**
     * Creates a room including enemies and power-ups.
     *
     * @param width    The width of the room.
     * @param height   The height of the room.
     * @param enemies  List of enemies to place in the room.
     * @param powerUps List of power-ups to place in the room.
     * @return The configured Room instance.
     */
    private Room createRoom(int width, int height, List<StandardEnemy> enemies, List<PowerUp> powerUps) {
        Room room = new Room(width, height);

        enemies.forEach(enemy -> {
            enemy.setCurrentRoom(room);
            room.addEnemy(enemy);
        });
        powerUps.forEach(room::addPowerUp);

        return room;
    }

    // ------------------------------------------------------------------------------------------------
    // Player and Room Management
    // ------------------------------------------------------------------------------------------------

    /**
     * Sets the current room and add the player to it.
     *
     * @param room The new room to set.
     */
    private void setCurrentRoom(Room room) {
        this.currentRoom = room;
        this.currentRoomIndex = dungeon.getRooms().indexOf(room);
        room.addPlayerToGrid(player);
    }

    /**
     * Moves the player to the next room after clearing the current one.
     *
     * @param player The player instance to be moved.
     */
    public void movePlayerToNextRoom(Player player) {
        currentRoom.clearRoom(); // Cleanup current room
        currentRoomIndex++;
        Room newRoom = dungeon.getRoom(currentRoomIndex);
        player.setCurrentRoom(newRoom);

        Position startPos = new Position(newRoom.getWidth() / 2, newRoom.getHeight() - 2);
        movePlayer(startPos); // Move the player to the new room starting position
        setCurrentRoom(newRoom);

        if (currentRoomIndex != dungeon.size() - 1) playRoom(newRoom); // Start the new room
        gameEventListener.onPlayerMovedToNextRoom(newRoom);
    }

    /**
     * Plays a room by spawning its enemies and syncing with the Rasa tracker.
     *
     * @param room The room to play.
     */
    private void playRoom(Room room) {
        room.getEnemies().forEach(enemy -> spawnStandardEnemyAgent(enemy, this.bossAgentAID));

        // Notify Rasa about active enemies in the room
        try {
            rasaBridge.sendMessageToRasaTrackerEvents(Map.of(
                    "event", "slot",
                    "name", "alive_enemies",
                    "value", String.valueOf(currentRoom.getEnemies().size())
            ));
        } catch (IOException e) {
            System.err.println("Failed to notify Rasa tracker: " + e.getMessage());
        }
    }

    /**
     * Moves the player to a new position within the current room.
     *
     * @param newPosition The position to move the player to.
     * @return True if the move is successful, false otherwise.
     */
    public boolean movePlayer(Position newPosition) {
        return player.move(newPosition);
    }

    // ------------------------------------------------------------------------------------------------
    // Enemy Management
    // ------------------------------------------------------------------------------------------------

    private AID spawnBossAgent(BossEnemy bossEnemy) {
        return spawnAgent(bossEnemy, "BossEnemyAgent", null);
    }

    private AID spawnStandardEnemyAgent(StandardEnemy enemy, AID bossAgentAID) {
        return spawnAgent(enemy, "StandardEnemyAgent", bossAgentAID);
    }

    /**
     * Spawns a game entity as an agent in the JADE environment.
     *
     * @param enemy          The enemy instance.
     * @param agentClassName The class name of the agent.
     * @param bossAgentAID   Optional boss agent AID.
     * @return The AID of the created agent.
     */
    private AID spawnAgent(StandardEnemy enemy, String agentClassName, AID bossAgentAID) {
        try {
            String enemyName = agentClassName + "-" + enemy.hashCode();
            AID agentAID = new AID(enemyName, AID.ISLOCALNAME);
            AgentController agent = container.createNewAgent(
                    enemyName,
                    "agents." + agentClassName,
                    new Object[]{enemy, this, bossAgentAID}
            );
            agent.start();
            enemies.put(enemyName, enemy);
            return agentAID;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Finds enemies near a given position within a specified "hearing range."
     *
     * @param powerUpPos The position to check for nearby enemies.
     * @return A list of nearby enemies within the range of 10 units.
     */
    private List<StandardEnemy> findNearbyEnemies(Position powerUpPos) {
        List<StandardEnemy> nearbyEnemies = new ArrayList<>();
        for (StandardEnemy enemy : currentRoom.getEnemies()) { // Assuming a method to fetch all enemies
            Position enemyPos = enemy.getPosition();
            if (enemyPos.isInRange(powerUpPos, 10)) { // Assumes a "hearing range" of 10
                nearbyEnemies.add(enemy);
            }
        }
        return nearbyEnemies;
    }

    /**
     * Notifies all nearby enemies about a power-up collected at a specific position.
     *
     * @param powerUpPos The position where the power-up was collected.
     */
    private void notifyNearbyEnemies(Position powerUpPos) {
        List<StandardEnemy> nearbyEnemies = findNearbyEnemies(powerUpPos);

        for (StandardEnemy enemy : nearbyEnemies) {
            ACLMessage notification = new ACLMessage(ACLMessage.INFORM);
            notification.setContent("POWER_UP_COLLECTED:" + powerUpPos.toString());

            // Create a unique identifier for each enemy's agent
            String uniqueAgentId = "StandardEnemyAgent-" + enemy.hashCode();
            AID receiverAID = new AID(uniqueAgentId, AID.ISLOCALNAME);
            notification.addReceiver(receiverAID);

            // Send the notification to the enemy agent
            sendUsingProxyAgent(notification);
        }
    }

    /**
     * Sends an ACL message through the proxy agent.
     *
     * @param message The ACL message to send.
     */
    private void sendUsingProxyAgent(ACLMessage message) {
        try {
            if (proxyAgentController != null) {
                // Use the proxy agent to send the message
                proxyAgentController.putO2AObject(message, AgentController.SYNC);
            } else {
                System.err.println("ProxyAgent is not initialized. Cannot send the message.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to send message using ProxyAgent.");
        }
    }

    /**
     * Removes an enemy from the current room and updates the game state,
     * including notifying Rasa and determining if the player can proceed to the next room.
     *
     * @param enemy The enemy to remove.
     */
    public void removeEnemy(StandardEnemy enemy) {
        currentRoom.removeEnemy(enemy);

        try {
            // Notify Rasa that an enemy has been removed
            rasaBridge.sendMessageToRasaTrackerEvents(Map.of(
                    "event", "slot",
                    "name", "alive_enemies",
                    "value", String.valueOf(currentRoom.getEnemies().size())
            ));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to notify Rasa tracker: " + e.getMessage());
        }

        // If no enemies remain, check if the room flow should proceed
        if (currentRoom.getEnemies().isEmpty() && currentRoomIndex != dungeon.size() - 1) {
            movePlayerToNextRoom(player);
            return;
        }

        // Notify event listener about the enemy's removal
        gameEventListener.onEnemyKilled(enemy.getPosition(), enemy);
    }

    // ----------------------------------------------
    // Combat Methods
    // ----------------------------------------------

    /**
     * Executes an attack for a given attacker, ensuring it is within range,
     * not on cooldown, and successfully performed.
     *
     * @param isOnCooldown        Flag to check if the action is on cooldown.
     * @param rangeCheck          Lambda to check if the target is within range.
     * @param attackAction        Lambda representing the actual attack logic.
     * @param cooldownDuration    Cooldown duration in milliseconds.
     * @param cooldownMessage     Message when the attack is on cooldown.
     * @param rangeErrorMessage   Message when the target is out of range.
     * @param missedErrorMessage  Message when the attack misses or fails.
     * @return True if the attack succeeds, false otherwise.
     */
    private boolean performAttack(boolean isOnCooldown, Supplier<Boolean> rangeCheck,
                                  Supplier<Boolean> attackAction, int cooldownDuration,
                                  String cooldownMessage, String rangeErrorMessage,
                                  String missedErrorMessage) {
        // Check if the attack is currently on cooldown
        if (isOnCooldown) {
            logMessage(cooldownMessage);
            return false;
        }

        // Check if the target is in range for the given attack
        if (!rangeCheck.get()) {
            logMessage(rangeErrorMessage);
            return false;
        }

        // Set the cooldown timer for the attack
        setCooldown(cooldownDuration);

        // Perform the attack action and check if it succeeded
        if (!attackAction.get()) {
            logMessage(missedErrorMessage);
            return false;
        }

        return true;
    }

    /**
     * Sets the cooldown for an attack action by scheduling a timer.
     *
     * @param cooldownDuration The duration of the cooldown in milliseconds.
     */
    private void setCooldown(int cooldownDuration) {
        isAttackOnCooldown = true;
        scheduler.schedule(() -> isAttackOnCooldown = false, cooldownDuration, TimeUnit.MILLISECONDS);
    }

    /**
     * Performs a melee attack from an attacker on a target.
     *
     * @param attacker The entity performing the attack.
     * @param target   The target of the attack.
     * @return True if the attack is successful, false otherwise.
     */
    public boolean meleeAttack(GameEntity attacker, GameEntity target) {
        return performAttack(isAttackOnCooldown,
                () -> attacker.isInMeleeRange(target),
                () -> attacker.meleeAttack(target),
                ATTACK_COOLDOWN_MS,
                "Melee attack is on cooldown!",
                "Target is not in melee range!",
                (attacker instanceof Player ? "Attack missed!" : null));
    }

    /**
     * Executes a special attack by a boss enemy on the player.
     *
     * @param attacker The boss enemy performing the special attack.
     */
    public void specialAttack(BossEnemy attacker) {
        attacker.specialAttack(player);
    }

    /**
     * Performs a melee attack by the player on a target.
     *
     * @param target The target of the player's melee attack.
     * @return True if the attack is successful, false otherwise.
     */
    public boolean meleeAttack(GameEntity target) {
        return meleeAttack(player, target);
    }

    /**
     * Performs a ranged attack by the player on a target.
     *
     * @param target The target of the player's ranged attack.
     * @return True if the attack is successful, false otherwise.
     */
    public boolean rangedAttack(GameEntity target) {
        return rangedAttack(player, target);
    }

    /**
     * Performs a ranged attack from an attacker on a target.
     *
     * @param attacker The entity performing the ranged attack.
     * @param target   The target of the ranged attack.
     * @return True if the attack is successful, false otherwise.
     */
    private boolean rangedAttack(GameEntity attacker, GameEntity target) {
        if (attacker.getRangedAmmo() <= 0) {
            logMessage("Not enough ammo!");
            return false;
        }

        return performAttack(isAttackOnCooldown,
                () -> attacker.isInRangedRange(target),
                () -> attacker.rangedAttack(target),
                RANGED_ATTACK_COOLDOWN_MS,
                "Ranged attack is on cooldown!",
                "Target is not in ranged range!",
                "Attack missed!");
    }

    // ------------------------------------------------------------------------------------------------
    // Bot Management and Logs
    // ------------------------------------------------------------------------------------------------

    /**
     * Notifies the chatbot that an enemy agent has spotted the player.
     *
     * @param notifier The enemy agent that spotted the player.
     */
    public void notifyEnemiesAlerted(StandardEnemyAgent notifier) {
        String alertMessage = notifier.getAID().getLocalName() + " has spotted the player!";
        String botResponse = chatWithBot(alertMessage); // Notify Rasa chatbot
        gameEventListener.printLogMessage("Rasa Bot: " + botResponse);
    }

    /**
     * Logs debug messages related to player actions.
     *
     * @param message The debug message to log.
     */
    private void logMessage(String message) {
        if (message != null)
            gameEventListener.printPlayerMessage(message);
    }

    /**
     * Interacts with the Rasa bot using the given input and returns its response.
     *
     * @param input The input to send to the bot.
     * @return The response from the bot.
     */
    public String chatWithBot(String input) {
        try {
            return rasaBridge.sendMessageToRasaBot(input);
        } catch (IOException | IllegalArgumentException | JsonParseException e) {
            System.err.println("Error communicating with Rasa bot: " + e.getMessage());
            return "Error communicating with the bot.";
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Other methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Handles the collection of a power-up by a game entity. The effect of the power-up is
     * applied to the collector, and nearby enemies are notified if the collector is a player.
     *
     * @param collector The entity that collected the power-up.
     * @param powerUp   The power-up that was collected.
     * @param position  The position where the power-up was collected.
     */
    public void onPowerUpCollected(GameEntity collector, PowerUp powerUp, Position position) {
        powerUp.applyTo(collector);

        if (collector instanceof Player)
            notifyNearbyEnemies(position);
    }

    // ------------------------------------------------------------------------------------------------
    // Game Over
    // ------------------------------------------------------------------------------------------------

    /**
     * Ends the game, cleaning up game resources, shutting down JADE components, and notifying the listener.
     */
    public void gameOver() {
        System.err.println("Game Over! Shutting down all activities...");
        try {
            clearGameResources();
            stopProxyAgent();
            shutDownJadePlatform();
            shutDownScheduler();
            releaseGuiLatch();
            finalizeGame();

            System.err.println("Game shutdown completed.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error during game shutdown: " + e.getMessage());
        }
    }

    /**
     * Clears in-game resources, including the current room and any active enemies.
     * Ensures all game entities are properly cleaned up before exiting.
     */
    private void clearGameResources() {
        if (currentRoom != null) currentRoom.clearRoom();
        enemies.clear();
    }

    /**
     * Stops the Proxy Agent controller if it's active, ensuring no lingering processes are left running.
     * Sets the controller instance to null after stopping to avoid memory leaks.
     */
    private void stopProxyAgent() throws StaleProxyException {
        if (proxyAgentController != null) {
            proxyAgentController.kill();
            proxyAgentController = null;
            System.err.println("Proxy agent stopped.");
        }
    }

    /**
     * Shuts down the JADE container and runtime to release resources allocated by the multi-agent system.
     * Both the JADE platform container and runtime are safely terminated.
     */
    private void shutDownJadePlatform() throws StaleProxyException {
        if (container != null) {
            container.kill();
            container = null;
            System.err.println("JADE container stopped.");
        }

        if (runtime != null) {
            runtime.shutDown();
            runtime = null;
            System.err.println("JADE runtime stopped.");
        }
    }

    /**
     * Shuts down the scheduler to terminate any ongoing or scheduled tasks.
     * Ensures no tasks are left active when the game exits.
     */
    private void shutDownScheduler() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdownNow();
            System.err.println("Scheduler stopped.");
        }
    }

    /**
     * Releases the GUI latch that prevents the interface from hanging.
     * Ensures the game is ready to completely shut down after releasing the latch.
     */
    private void releaseGuiLatch() {
        if (guiReadyLatch.getCount() > 0) {
            guiReadyLatch.countDown();
        }
    }

    /**
     * Performs final clean-up tasks, resetting the player instance and notifying any game event listeners.
     * Ensures that listeners are informed about the game-over event and resets critical game components.
     */
    private void finalizeGame() {
        player = null;
        if (gameEventListener != null)
            gameEventListener.onGameOver(false);
    }

    // ------------------------------------------------------------------------------------------------
    // Getters & Setters
    // ------------------------------------------------------------------------------------------------

    public Dungeon getDungeon() { return dungeon; }
    public Player getPlayer() { return player; }
    public Room getCurrentRoom() { return currentRoom; }
    public CountDownLatch getGuiReadyLatch() { return guiReadyLatch; }
    public GameEventListener getGameEventListener() { return gameEventListener; }
    public void setGameEventListener(GameEventListener listener) { this.gameEventListener = listener; }
    public StandardEnemy getEnemyByName(String name) { return enemies.get(name); }
    public Map<String, StandardEnemy> getEnemies() { return enemies; }
}

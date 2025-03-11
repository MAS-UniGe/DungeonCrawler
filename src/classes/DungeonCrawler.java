package classes;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main entry class for the Dungeon Crawler game.
 * Handles the integration of JavaFX for the game's graphical user interface,
 * manages user input, and bridges the connection between the GUI and game logic (GameManager).
 */
public class DungeonCrawler extends Application implements GameEventListener {

    // ----------------------------------------------
    // Attributes
    // ----------------------------------------------

    private GameManager gameManager; // Manages core game logic and events
    private int currentRoomIndex = 0; // Index of the currently displayed room
    private Room currentRoom; // Reference to the currently active room
    private BorderPane root; // Main container for the GUI layout
    private VBox statsPane; // Displays player statistics
    private VBox roomPane; // Container for visualizing the current room
    private ScrollPane logScrollPane; // Log panel for in-game messages
    private TextArea userInputField; // Field for the player to input messages
    private ExecutorService threadPool; // Manages background tasks

    // ----------------------------------------------
    // Main Application Lifecycle
    // ----------------------------------------------

    /**
     * The main entry point for JavaFX. Sets up the game environment,
     * initializes UI components, and starts the application.
     *
     * @param primaryStage The main application window
     */
    @Override
    public void start(Stage primaryStage) {
        setupGameEnvironment(); // Initialize core game components
        setupUIComponents(primaryStage); // Build and configure the JavaFX UI
        primaryStage.show();

        // Notify the game manager when the GUI is ready
        Platform.runLater(() -> gameManager.getGuiReadyLatch().countDown());
    }

    /**
     * Initializes the game environment.
     */
    private void setupGameEnvironment() {
        gameManager = new GameManager();
        gameManager.setGameEventListener(this); // Register to listen for game events
        threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Configures the JavaFX UI components and initializes the main game window.
     *
     * @param primaryStage The main application window
     */
    private void setupUIComponents(Stage primaryStage) {
        root = new BorderPane();
        roomPane = new VBox();

        // Prepare the side panels (stats and log container)
        VBox statsAndLogContainer = setupStatsAndLogContainer();

        root.setRight(statsAndLogContainer);

        // Display the first room of the dungeon
        showRoom(currentRoomIndex);

        // Configure the stage and scene dimensions
        Scene mainScene = new Scene(root, 1800, 1000);
        setupPlayerMovement(mainScene, gameManager.getPlayer());
        primaryStage.setTitle("Dungeon Crawler");
        primaryStage.setScene(mainScene);

        // Handle shutdown when the application is closed
        primaryStage.setOnCloseRequest(event -> shutDownGame());
    }

    // ----------------------------------------------
    // UI Setup & Layout
    // ----------------------------------------------

    /**
     * Initializes the stats and log container that appears on the right side of the UI.
     *
     * @return VBox containing the stats panel, log panel, and input field
     */
    private VBox setupStatsAndLogContainer() {
        setupStatsPane();
        setupLogPane();
        setupUserInputField();

        VBox container = new VBox(statsPane, logScrollPane, userInputField); // Arrange all components vertically in a container
        container.prefWidthProperty().bind(root.widthProperty().multiply(0.3)); // Allocate 30% width to the container

        // Dynamically adjust height proportions for the stats, logs, and input
        statsPane.prefHeightProperty().bind(container.heightProperty().multiply(0.3));
        logScrollPane.prefHeightProperty().bind(container.heightProperty().multiply(0.65));
        userInputField.prefHeightProperty().bind(container.heightProperty().multiply(0.05));

        return container;
    }

    /**
     * Configures the stats panel to display player health, attack, and defense.
     */
    private void setupStatsPane() {
        statsPane = new VBox();
        statsPane.setPadding(new Insets(10));
        statsPane.setStyle("-fx-background-color: lightgray;");
        updatePlayerStats(); // Populate the panel with initial data
    }

    /**
     * Configures the log pane to display in-game messages.
     */
    private void setupLogPane() {
        VBox logPane = new VBox();
        logPane.setSpacing(5);
        logPane.setPadding(new Insets(10));

        logScrollPane = new ScrollPane(logPane); // Make logs scrollable
        logScrollPane.setFitToWidth(true);
        logScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Disable horizontal scrolling
        logScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    /**
     * Prepares the user input text area and configures behavior on key presses.
     */
    public void setupUserInputField() {
        userInputField = new TextArea();
        userInputField.setPromptText("Type your message here...");
        userInputField.setWrapText(true);

        Platform.runLater(() -> root.requestFocus()); // Prevent focus from automatically going to the input field

        // Handle user pressing the "Enter" key
        userInputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume(); // Prevent creating new lines on Enter
                handleUserInput(); // Process the input
                Platform.runLater(() -> root.requestFocus());
            }
        });
    }

    // ----------------------------------------------
    // User Interaction Handling
    // ----------------------------------------------

    /**
     * Handles player input, sends it to the chatbot for processing, and updates the game log.
     */
    private void handleUserInput() {
        String userInput = userInputField.getText().trim();
        if (userInput.isEmpty()) return;

        userInputField.clear();
        addPlayerMessage(userInput); // Log the player's message

        // Process the user input in the background
        threadPool.submit(() -> {
            String response = gameManager.chatWithBot(userInput);
            Platform.runLater(() -> addLogMessage("Bot: " + response));
        });
    }

    /**
     * Adds a player-generated message to the game log.
     *
     * @param message The message to display
     */
    private void addPlayerMessage(String message) {
        addLogMessage("You: " + message);
    }

    /**
     * Adds a log message to the game log and updates the scroll position.
     *
     * @param message The log message
     */
    private void addLogMessage(String message) {
        Platform.runLater(() -> {
            Label logMessage = new Label(message);
            logMessage.setStyle("-fx-text-fill: black;");
            logMessage.setWrapText(true);
            addToScrollPane(logMessage);
        });
    }

    /**
     * Adds a log message to the log scroll pane and ensures the scroll pane
     * automatically scrolls to the bottom to display the latest message.
     *
     * @param logMessage The log message to add to the scroll pane.
     */
    private void addToScrollPane(Label logMessage) {
        VBox logPane = (VBox) logScrollPane.getContent();

        logPane.getChildren().add(logMessage);

        // Ensure the scroll pane automatically scrolls to show the latest message
        if (logScrollPane != null) {
            logScrollPane.layout(); // Recalculate layout to update the scrollbar
            logScrollPane.setVvalue(1.0); // Set scrollbar to maximum (bottom)
        }
    }

    // ==================================
    // Player Movement and Interaction
    // ==================================

    /**
     * Sets up player movement controls and interaction handling in the game.
     * Listens for key press events to trigger movement or interactions.
     *
     * @param scene  The current game scene.
     * @param player The player instance.
     */
    private void setupPlayerMovement(Scene scene, Player player) {
        scene.setOnKeyPressed(event -> {
            KeyCode keyCode = event.getCode();

            // Handle directional movement keys (W, A, S, D)
            if (handleMovementInput(keyCode, player)) return;

            // Handle interaction key (E for collecting power-ups)
            if (keyCode == KeyCode.E) handlePowerUpInteraction();
        });
    }

    /**
     * Handles movement input based on the key pressed and attempts to move the player.
     *
     * @param keyCode The key pressed by the player.
     * @param player  The player instance.
     * @return True if movement was successfully handled, otherwise false.
     */
    private boolean handleMovementInput(KeyCode keyCode, Player player) {
        // Determine the new position based on key pressed, or return null if key isn't movement-related
        Position newPosition = switch (keyCode) {
            case W -> player.getPosition().translate(0, -1); // Move up
            case S -> player.getPosition().translate(0, 1);  // Move down
            case A -> player.getPosition().translate(-1, 0); // Move left
            case D -> player.getPosition().translate(1, 0);  // Move right
            default -> null;
        };

        if (newPosition == null) return false; // No movement if key doesn't correspond to any direction

        // Execute the movement logic in the background to ensure responsiveness
        threadPool.submit(() -> {
            Position currentPosition = player.getPosition();
            if (gameManager.movePlayer(newPosition)) {
                // Update the game state and UI on the JavaFX thread
                Platform.runLater(() -> onEntityMoved(currentPosition, newPosition, EntityType.PLAYER));
            }
        });

        return true; // Movement successfully handled
    }

    /**
     * Handles the interaction logic when the player attempts to collect a power-up.
     * Scans adjacent positions for collectible power-ups.
     */
    private void handlePowerUpInteraction() {
        // Perform the power-up collection logic in a background thread
        threadPool.submit(() -> {
            Position playerPosition = gameManager.getPlayer().getPosition();
            Room room = gameManager.getDungeon().getRoom(currentRoomIndex);

            // Find and collect any adjacent power-ups
            boolean collected = room.getPowerUps().stream()
                    .filter(pu -> playerPosition.isAdjacentTo(pu.getPosition())) // Check for proximity
                    .findFirst()
                    .map(pu -> {
                        collectPowerUp(gameManager.getPlayer(), pu.getPosition(), room);
                        return true; // Indicate power-up was collected
                    }).orElse(false);

            // If no power-up is found, notify the player
            if (!collected) addLogMessage("There's no power-up adjacent to you.");
        });
    }

    /**
     * Collects a power-up at the specified position and updates the game state/UI.
     *
     * @param collector       The entity collecting the power-up (player).
     * @param powerupPosition The position of the power-up being collected.
     * @param currentRoom     The room containing the power-up.
     */
    public void collectPowerUp(GameEntity collector, Position powerupPosition, Room currentRoom) {
        // Remove the power-up from the room and get its data
        PowerUp collectedPowerUp = currentRoom.collectPowerUpAt(powerupPosition);

        if (collectedPowerUp != null) {
            // Notify the GameManager about the collected power-up
            gameManager.onPowerUpCollected(collector, collectedPowerUp, powerupPosition);

            // Update the room UI and player's stats on the JavaFX thread
            Platform.runLater(() -> {
                updateRoomTile(powerupPosition, currentRoom); // Update room tile UI
                updatePlayerStats(); // Update the stats panel
            });

            // Add a log message confirming the power-up collection
            addLogMessage("Collected " + collectedPowerUp.getType() + " power-up!");
        }
    }

    /**
     * Handles attacking logic for a given position, supporting both melee and ranged attacks.
     *
     * @param position The position to attack.
     * @param isRanged Flag indicating whether the attack is ranged (true) or melee (false).
     */
    private void handleAttack(Position position, boolean isRanged) {
        // Check if there is an enemy at the specified position
        StandardEnemy targetEnemy = currentRoom.getEnemies().stream()
                .filter(enemy -> enemy.getPosition().equals(position)) // Match enemy at position
                .findFirst()
                .orElse(null);

        if (targetEnemy != null) {
            // Determine the type of attack (ranged or melee) and execute it
            boolean attackSuccessful = isRanged
                    ? gameManager.rangedAttack(targetEnemy) // Ranged attack
                    : gameManager.meleeAttack(targetEnemy); // Melee attack

            if (attackSuccessful) {
                highlightEntityTile(position); // Highlight the hit tile

                // Handle the enemy's death if applicable
                if (!targetEnemy.isAlive()) {
                    addPlayerMessage("Enemy defeated");
                    currentRoom.removeEnemy(targetEnemy); // Remove from room
                    gameManager.removeEnemy(targetEnemy); // Remove from GameManager
                }
            }
        } else
            // Notify the player if no enemy is present at the clicked position
            addPlayerMessage("No available target at the clicked position.");
    }

    /**
     * Handles ranged attack logic at the specified position.
     *
     * @param position The position to target with a ranged attack.
     */
    private void handleRangedAttack(Position position) {
        handleAttack(position, true); // Delegate to the generic attack handler with ranged flag
    }

    /**
     * Handles melee attack logic at the specified position.
     *
     * @param position The position to target with a melee attack.
     */
    private void handleMeleeAttack(Position position) {
        handleAttack(position, false); // Delegate to the generic attack handler with melee flag
    }


    // ==================================
    // Room & Dungeon State Management
    // ==================================

    /**
     * Initializes the room UI by generating a grid of labels representing the room tiles.
     * Each label corresponds to a tile in the room and is clickable for interaction.
     *
     * @param grid The 2D array representing the room grid.
     */
    private void initializeRoomUI(RoomTileType[][] grid) {
        for (int y = 0; y < grid[0].length; y++) {
            HBox row = new HBox(); // Create a horizontal box for each row
            for (int x = 0; x < grid.length; x++) {
                Label cell = new Label(); // Create a label (tile) for each cell
                cell.setPrefSize(40, 40);

                Position position = new Position(x, y);

                // Handle mouse click events for attacking enemies
                cell.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY)
                        handleMeleeAttack(position); // Handle melee attack with left-click
                    else if (event.getButton() == MouseButton.SECONDARY)
                        handleRangedAttack(position); // Handle ranged attack with right-click
                });

                row.getChildren().add(cell);
            }
            roomPane.getChildren().add(row);
        }
    }

    /**
     * Updates the room UI by creating a new grid if needed or refreshing the visuals
     * of the existing grid based on the current state of the room.
     *
     * @param room The current room being updated.
     */
    private void updateRoomUI(Room room) {
        RoomTileType[][] grid = room.getGrid();

        // Create the UI grid only once if it's not initialized yet
        if (roomPane.getChildren().isEmpty())
            initializeRoomUI(grid);

        // Iterate through the entire grid and update individual tiles
        for (int y = 0; y < grid[0].length; y++) {
            for (int x = 0; x < grid.length; x++) {
                updateRoomTile(new Position(x, y), room);
            }
        }
    }

    /**
     * Highlights a specific tile in the room (e.g., when an entity is attacked).
     * Temporarily changes the tile's style and then resets it after a delay.
     *
     * @param position The position of the tile to highlight.
     */
    private void highlightEntityTile(Position position) {
        HBox row = (HBox) roomPane.getChildren().get(position.getY());
        Label cell = (Label) row.getChildren().get(position.getX());

        cell.setStyle("-fx-background-color: yellow;"); // Set highlight color

        // Reset the style after a delay of 100ms using a background thread
        threadPool.submit(() -> {
            try {
                Thread.sleep(100); // Delay to highlight the tile temporarily
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> updateRoomTile(position, currentRoom)); // Reset the tile's style
        });
    }

    /**
     * Updates a specific tile in the room based on its current state.
     *
     * @param position The position of the tile to update.
     * @param room     The room containing the tile.
     */
    private void updateRoomTile(Position position, Room room) {
        RoomTileType[][] grid = room.getGrid();
        HBox row = (HBox) roomPane.getChildren().get(position.getY());
        Label cell = (Label) row.getChildren().get(position.getX());

        RoomTileType tileType = grid[position.getX()][position.getY()];

        // Update the tile's style based on the tile type
        updateRoomTileStyles(cell, tileType);
    }

    /**
     * Updates the visual style of a tile (label) based on its type.
     *
     * @param cell     The tile (label) to update.
     * @param tileType The type of the tile
     */
    private void updateRoomTileStyles(Label cell, RoomTileType tileType) {
        switch (tileType) {
            case WALL:
                cell.setStyle("-fx-background-color: gray;");
                break;
            case PLAYER:
                cell.setStyle("-fx-background-color: blue;");
                break;
            case ENEMY:
                cell.setStyle("-fx-background-color: red;");
                break;
            case POWERUP:
                cell.setStyle("-fx-background-color: green;");
                break;
            default:
                cell.setStyle("-fx-background-color: white;");
        }
    }

    /**
     * Displays the specified room by room index, updating the room UI and refreshing
     * its content. Handles the transition between completed rooms and the end of the dungeon.
     *
     * @param roomIndex The index of the room to show.
     */
    private void showRoom(int roomIndex) {
        // Check if the room index exceeds the dungeon size (end of dungeon)
        if (roomIndex >= gameManager.getDungeon().size()) {
            endGame(); // Trigger the end game sequence
            return;
        }

        // Retrieve and set the current room based on the index
        currentRoomIndex = roomIndex;
        currentRoom = gameManager.getDungeon().getRoom(roomIndex);

        roomPane.getChildren().clear();
        updateRoomUI(currentRoom);

        Label roomLabel = new Label("You are in Room " + (roomIndex + 1));

        roomPane.getChildren().add(roomLabel);
        root.setCenter(roomPane);
    }

    // ==================================
    // Player Statistics
    // ==================================

    /**
     * Dynamically updates the player's stats in the side panel.
     * Clears the current stats and repopulates the panel with updated information.
     */
    private void updatePlayerStats() {
        statsPane.setPadding(new Insets(5));
        statsPane.setSpacing(5);

        Player player = gameManager.getPlayer();

        // Update player stats by clearing existing content and repopulating
        statsPane.getChildren().clear();
        statsPane.getChildren().add(new Label("Player Stats:"));
        statsPane.getChildren().add(new Label("Health: " + player.getHealthPts()));
        statsPane.getChildren().add(new Label("Attack: " + player.getAttackPwr()));
        statsPane.getChildren().add(new Label("Defense: " + player.getDefensePwr()));
    }

    // ==================================
    // Game Lifecycle
    // ==================================

    /**
     * Shuts down the game cleanly. Ends the game session and closes the application.
     */
    private void shutDownGame() {
        gameManager.gameOver(); // Notify the GameManager that the game is over
        threadPool.shutdownNow();
        System.exit(0); // Terminate the application
    }

    /**
     * Displays a congratulatory message to the player when the game is ended
     */
    private void endGame() {
        VBox endGamePane = new VBox();
        endGamePane.setPadding(new Insets(20));
        endGamePane.setSpacing(10);

        Label endLabel = new Label("Congratulations! You've cleared the dungeon.");
        endGamePane.getChildren().add(endLabel);
        root.setCenter(endGamePane);
    }

    /**
     * Handles the victory event when the player wins the game.
     * Delegates to the generic game over handler with a win condition.
     */
    @Override
    public void onWin() {
        onGameOver(true); // Handle the win condition (game successfully completed)
    }

    /**
     * Handles the game-over event, displaying an appropriate message for winning or losing.
     *
     * @param win True if the player won, false if the game was lost.
     */
    @Override
    public void onGameOver(boolean win) {
        Platform.runLater(() -> { // Update UI safely on the JavaFX thread
            // Clear the existing UI components
            root.setRight(null);
            root.setLeft(null);
            root.setBottom(null);
            root.setTop(null);
            root.setCenter(null);

            // Display the Game Over or Victory message
            VBox gameOverPane = new VBox();
            gameOverPane.setPadding(new Insets(20));
            gameOverPane.setSpacing(10);
            gameOverPane.setStyle("-fx-alignment: center;");

            // Display "Game Over" or "You've won!" based on win parameter
            Label gameOverLabel = new Label(win ? "You've won!" : "Game Over");
            // Apply styles for larger text size and color
            gameOverLabel.setStyle("-fx-font-size: 36px; -fx-text-fill: red;");
            gameOverPane.getChildren().add(gameOverLabel);

            root.setCenter(gameOverPane);
        });
    }

    // ==================================
    // GameEventListener Implementations
    // ==================================

    /**
     * Handles the event when an entity (e.g., player, enemy) moves.
     * Updates only the tiles that are affected: the old position (cleared) and the new position.
     *
     * @param oldPosition The previous position of the entity.
     * @param newPosition The new position of the entity.
     * @param entityType  The type of the entity that moved (PLAYER, ENEMY, etc.).
     */
    @Override
    public void onEntityMoved(Position oldPosition, Position newPosition, EntityType entityType) {
        javafx.application.Platform.runLater(() -> {
            // Redraw only the tiles affected by the movement
            Room currentRoom = gameManager.getDungeon().getRoom(currentRoomIndex);
            updateRoomTile(oldPosition, currentRoom);
            updateRoomTile(newPosition, currentRoom);
        });
    }

    /**
     * Handles the event when an enemy is killed.
     * Updates the tile where the enemy was located to reflect the absence of the enemy.
     *
     * @param position The position where the enemy was killed.
     * @param enemy    The enemy that was killed.
     */
    @Override
    public void onEnemyKilled(Position position, StandardEnemy enemy) {
        Room currentRoom = gameManager.getDungeon().getRoom(currentRoomIndex);

        // Update the UI to remove the enemy from the tile
        Platform.runLater(() -> updateRoomTile(position, currentRoom));
    }

    /**
     * Handles the event where the player is attacked by an enemy.
     * Highlights the player's tile briefly, updates the player's stats, and handles the player's death if health reaches 0.
     *
     * @param player The player who was attacked.
     */
    @Override
    public void onPlayerAttacked(Player player) {
        Position position = player.getPosition();

        highlightEntityTile(position); // Highlight the player's tile to indicate the attack

        // Handle player death by ending the game if health is 0
        if (!player.isAlive()) {
            gameManager.gameOver();
            return;
        }

        // Update the UI to reflect any changes in the player's stats
        Platform.runLater(this::updatePlayerStats);
    }

    /**
     * Handles the event where the player moves to the next room.
     * Updates the current room index and reloads the UI to display the new room.
     *
     * @param targetRoom The next room the player is moving into.
     */
    @Override
    public void onPlayerMovedToNextRoom(Room targetRoom) {
        currentRoomIndex++; // Increment the room index to represent the transition
        currentRoom = targetRoom;

        // Update the UI on the JavaFX thread to load the new room
        Platform.runLater(() -> {
            showRoom(currentRoomIndex);
        });
    }

    /**
     * Logs a generic message to the game's log panel.
     *
     * @param message The message to be logged.
     */
    @Override
    public void printLogMessage(String message) {
        addLogMessage(message);
    }

    /**
     * Logs a message specific to the player, prefixing it with "You: " for clarity.
     *
     * @param message The message directed to the player.
     */
    @Override
    public void printPlayerMessage(String message) {
        printLogMessage("You: " + message);
    }

    // ==================================
    // Main
    // ==================================

    public static void main(String[] args) {
        launch(args); // Start the JavaFX application
    }
}
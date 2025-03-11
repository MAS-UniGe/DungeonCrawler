import classes.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RoomTest {

    @Test
    public void should_AddEnemyToRoom_When_AddEnemyCalled() {
        Room room = new Room(40, 40);
        StandardEnemy enemy = new StandardEnemy(new Position(0, 0), room);

        // Adding an enemy to the room
        room.addEnemy(enemy);

        // Validate the enemy was added successfully
        assertAll(
                () -> assertTrue(room.getEnemies().contains(enemy), "Enemy should be added to the list"),
                () -> assertEquals(1, room.getEnemies().size(), "Enemy list size should be 1")
        );
    }

    @Test
    public void should_AddPowerUpToRoom_When_AddPowerUpCalled() {
        Room room = new Room(40, 40);
        PowerUp powerUp = new PowerUp(PowerUpType.HEALTH, new Position (10,10));

        // Adding a power-up to the room
        room.addPowerUp(powerUp);

        // Validate the power-up was added successfully
        assertAll(
                () -> assertTrue(room.getPowerUps().contains(powerUp), "Power-up should be added to the list"),
                () -> assertEquals(1, room.getPowerUps().size(), "Power-up list size should be 1")
        );
    }

    @Test
    public void should_RemoveEnemyFromRoom_When_RemoveEnemyCalled() {
        Room room = new Room(40, 40);
        StandardEnemy enemy = new StandardEnemy(new Position(0, 0), room);

        // Adding and removing an enemy from the room
        room.addEnemy(enemy);
        room.removeEnemy(enemy);

        // Validate the enemy was removed successfully
        assertAll(
                () -> assertFalse(room.getEnemies().contains(enemy), "Enemy should be removed from the list"),
                () -> assertEquals(0, room.getEnemies().size(), "Enemy list size should be 0")
        );
    }

    @Test
    public void should_RemovePowerUpFromRoom_When_RemovePowerUpCalled() {
        Room room = new Room(40, 40);
        PowerUp powerUp = new PowerUp(PowerUpType.HEALTH, new Position (10,10));

        // Adding and removing a power-up from the room
        room.addPowerUp(powerUp);
        room.removePowerUp(powerUp);

        // Validate the power-up was removed successfully
        assertAll(
                () -> assertFalse(room.getPowerUps().contains(powerUp), "Power-up should be removed from the list"),
                () -> assertEquals(0, room.getPowerUps().size(), "Power-up list size should be 0")
        );
    }

    @Test
    public void should_ReturnTrue_When_NoEnemiesAreAliveAndCanProgressCalled() {
        Room room = new Room(30, 30);
        StandardEnemy deadEnemy = new StandardEnemy(new Position(0, 0), room);
        room.addEnemy(deadEnemy);
        room.removeEnemy(deadEnemy);

        // Validate that the room can progress when no enemies are alive
        assertTrue(room.allEnemiesDefeated(), "Room should be able to progress if no enemies are alive");
    }

    @Test
    public void should_ReturnFalse_When_EnemiesAreAliveAndCanProgressCalled() {
        Room room = new Room(49, 40);
        StandardEnemy aliveEnemy = new StandardEnemy(new Position(0, 0), room);
        room.addEnemy(aliveEnemy);

        // Validate that the room cannot progress when there are alive enemies
        assertFalse(room.allEnemiesDefeated(), "Room should not be able to progress if any enemy is alive");
    }

    @Test
    public void should_InitializeEmptyLists_When_RoomConstructorCalled() {
        Room room = new Room(40, 40);

        // Validate empty initialization of lists
        assertAll(
                () -> assertNotNull(room.getEnemies(), "Enemies list should be initialized"),
                () -> assertNotNull(room.getPowerUps(), "Power-ups list should be initialized"),
                () -> assertTrue(room.getEnemies().isEmpty(), "Enemies list should be empty"),
                () -> assertTrue(room.getPowerUps().isEmpty(), "Power-ups list should be empty")
        );
    }
}
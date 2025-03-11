import classes.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PowerUpTest {

    /**
     * Tests for the PowerUp class.
     * <p>
     * The PowerUp class applies specific boosts to a Player's attributes such as health, attack, or defense
     * based on the type of the PowerUp and its boost percentage. The applyTo method internally calls the
     * corresponding increase method on the Player object depending on the type of the PowerUp.
     */

    @Test
    public void should_IncreaseHealthPoints_When_HealthPowerUpApplied() {
        // Arrange
        float boostPercentage = 0.25f;
        Room room = new Room(40, 40);
        PowerUp powerUp = new PowerUp(PowerUpType.HEALTH, boostPercentage, new Position (10,10));
        Player player = new Player(new Position(0, 0), room, 100, 50, 30);
        int initialHealth = player.getHealthPts();

        // Act
        powerUp.applyTo(player);

        // Assert
        int expectedHealth = (int) (initialHealth + Math.round(initialHealth * boostPercentage));
        assertEquals(expectedHealth, player.getHealthPts());
    }

    @Test
    public void should_IncreaseAttackPoints_When_AttackPowerUpApplied() {
        // Arrange
        float boostPercentage = 0.15f;
        Room room = new Room(40, 40);
        PowerUp powerUp = new PowerUp(PowerUpType.ATTACK, boostPercentage, new Position (10,10));
        Player player = new Player(new Position(0, 0), room, 100, 50, 30);
        int initialAttack = player.getAttackPwr();

        // Act
        powerUp.applyTo(player);

        // Assert
        int expectedAttackPower = (int) (initialAttack + Math.round(initialAttack * boostPercentage));
        assertEquals(expectedAttackPower, player.getAttackPwr());
    }

    @Test
    public void should_IncreaseDefensePoints_When_DefensePowerUpApplied() {
        // Arrange
        float boostPercentage = 0.3f;
        Room room = new Room(40, 40);
        PowerUp powerUp = new PowerUp(PowerUpType.DEFENSE, boostPercentage, new Position (10,10));
        Player player = new Player(new Position(0, 0), room, 100, 50, 30);
        int initialDefense = player.getDefensePwr();

        // Act
        powerUp.applyTo(player);

        // Assert
        int expectedDefensePower = (int) (initialDefense + Math.round(initialDefense * boostPercentage));
        assertEquals(expectedDefensePower, player.getDefensePwr());
    }

    @Test
    public void should_IncreaseHealthPoints_When_HealthPowerUpWithDefaultBoostApplied() {
        // Arrange
        PowerUp powerUp = new PowerUp(PowerUpType.HEALTH, new Position (10,10)); // Default boost of 0.2f
        Room room = new Room(40, 40);
        Player player = new Player(new Position(0, 0), room, 100, 50, 30);
        int initialHealth = player.getHealthPts();

        // Act
        powerUp.applyTo(player);

        // Assert
        int expectedHealth = (int) (initialHealth + (initialHealth * 0.2f));
        assertEquals(expectedHealth, player.getHealthPts());
    }

    @Test
    public void should_IncreaseAttackPoints_When_AttackPowerUpWithDefaultBoostApplied() {
        // Arrange
        PowerUp powerUp = new PowerUp(PowerUpType.ATTACK, new Position (10,10)); // Default boost of 0.2f
        Room room = new Room(40, 40);
        Player player = new Player(new Position(0, 0), room, 100, 50, 30);
        int initialAttack = player.getAttackPwr();

        // Act
        powerUp.applyTo(player);

        // Assert
        int expectedAttackPower = (int) (initialAttack + Math.round(initialAttack * 0.2f));
        assertEquals(expectedAttackPower, player.getAttackPwr());
    }

    @Test
    public void should_IncreaseDefensePoints_When_DefensePowerUpWithDefaultBoostApplied() {
        // Arrange
        PowerUp powerUp = new PowerUp(PowerUpType.DEFENSE, new Position (10,10)); // Default boost of 0.2f
        Room room = new Room(40, 40);
        Player player = new Player(new Position(0, 0), room, 100, 50, 30);
        int initialDefense = player.getDefensePwr();

        // Act
        powerUp.applyTo(player);

        // Assert
        int expectedDefensePower = (int) Math.round(initialDefense + (initialDefense * 0.2f));
        assertEquals(expectedDefensePower, player.getDefensePwr());
    }
}
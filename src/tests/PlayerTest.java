import classes.Player;
import classes.Position;
import classes.StandardEnemy;
import classes.Room;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class PlayerTest {

    @Test
    void should_DecreaseEnemyHealthAndReduceAmmo_When_RangedAttackIsSuccessful() {
        Position playerPosition = new Position(0, 0);
        Room room = new Room(40, 40);
        Player player = new MockedPlayer(playerPosition, room, 50, 20, 15, 15);
        StandardEnemy enemy = new StandardEnemy(new Position(5, 5), room, 50, 10, 5);
        player.rangedAttack(enemy);
        Assertions.assertAll(new Executable[]{
                () -> Assertions.assertEquals(30, enemy.getHealthPts(), "Enemy health should be decreased after a successful attack."),
                () -> Assertions.assertEquals(2, player.getRangedAmmo(), "Ranged ammo should decrease by 1 after a successful attack.")
        });
    }

    @Test
    void should_NotChangeEnemyHealthAndMaintainAmmo_When_RangedAttackHasNoAmmo() {
        Position playerPosition = new Position(0, 0);
        Room room = new Room(40, 40);
        Player player = new Player(playerPosition, room, 50, 20, 15);
        StandardEnemy enemy = new StandardEnemy(new Position(5, 5), room, 50, 10, 5);
        player.setRangedAmmo(0);
        player.rangedAttack(enemy);
        Assertions.assertAll(new Executable[]{
                () -> Assertions.assertEquals(50, enemy.getHealthPts(), "Enemy health should not change when there is no ammo."),
                () -> Assertions.assertEquals(0, player.getRangedAmmo(), "Ranged ammo should remain at 0.")
        });
    }

    @Test
    void should_IncreaseHealthBy25Percent_When_IncreaseHealthCalledWith25PercentBoost() {
        Position playerPosition = new Position(0, 0);
        Room room = new Room(40, 40);
        Player player = new Player(playerPosition, room, 100, 20, 15);
        player.increaseHealth(0.25F);
        Assertions.assertEquals(125, player.getHealthPts(), "Health should increase by 25%.");
    }

    @Test
    void should_RoundUpHealthProperly_When_IncreaseHealthCalledWithSmallPercentBoost() {
        Position playerPosition = new Position(0, 0);
        Room room = new Room(40, 40);
        Player player = new Player(playerPosition, room, 100, 20, 15);
        player.increaseHealth(0.1F);
        Assertions.assertEquals(100 + Math.round(10.0F), player.getHealthPts(), "Health should round up correctly when increasing by 10%.");
    }

    @Test
    void should_IncreaseAttackPowerBy50Percent_When_IncreaseAttackPowerCalledWith50PercentBoost() {
        Position playerPosition = new Position(0, 0);
        Room room = new Room(40, 40);
        Player player = new Player(playerPosition, room, 100, 20, 15);
        player.increaseAttackPwr(0.5F);
        Assertions.assertEquals(30, player.getAttackPwr(), "Attack power should increase by 50%.");
    }

    @Test
    void should_IncreaseDefenseBy33Percent_When_IncreaseDefenseCalledWith33PercentBoost() {
        Position playerPosition = new Position(0, 0);
        Room room = new Room(40, 40);
        Player player = new Player(playerPosition, room, 100, 20, 15);
        player.increaseDefensePwr(0.33F);
        Assertions.assertEquals(20, player.getDefensePwr(), "Defense power should increase by approximately 33%, with rounding.");
    }

    private static class MockedPlayer extends Player {
        private final int fixedAttackRoll;

        public MockedPlayer(Position position, Room room, int healthPts, int attackPwr, int defensePwr, int fixedAttackRoll) {
            super(position, room, healthPts, attackPwr, defensePwr);
            this.fixedAttackRoll = fixedAttackRoll;
        }

        public int getRandomInclusive(int min, int max) {
            return this.fixedAttackRoll;
        }
    }
}
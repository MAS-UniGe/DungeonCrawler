package tests;

import classes.GameEntity;
import classes.Position;
import classes.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameEntityTest {

    private static final int INITIAL_HEALTH = 100;
    private static final int INITIAL_ATTACK = 20;
    private static final int INITIAL_DEFENSE = 15;

    private static final Position INITIAL_POSITION = new Position(0, 0);
    private static final Position ADJACENT_POSITION = new Position(1, 0);
    private static final Position NON_ADJACENT_POSITION = new Position(2, 0);

    private static final Room INITIAL_ROOM = new Room(40, 40);

    private GameEntity entity;
    private GameEntity adjacentEntity;
    private GameEntity nonAdjacentEntity;

    @BeforeEach
    void setup() {
        entity = new GameEntity(INITIAL_POSITION, INITIAL_ROOM, INITIAL_HEALTH, INITIAL_ATTACK, INITIAL_DEFENSE);
        adjacentEntity = new GameEntity(ADJACENT_POSITION, INITIAL_ROOM, 80, 15, 10);
        nonAdjacentEntity = new GameEntity(NON_ADJACENT_POSITION, INITIAL_ROOM, 50, 10, 25);
    }

    @Test
    void should_ReturnHealthPoints_When_GetHealthPtsCalled() {
        assertEquals(INITIAL_HEALTH, entity.getHealthPts());
    }

    @Test
    void should_UpdateHealthPoints_When_SetHealthPtsCalled() {
        entity.setHealthPts(80);
        assertEquals(80, entity.getHealthPts());
    }

    @Test
    void should_ReturnAttackPower_When_GetAttackPwrCalled() {
        assertEquals(INITIAL_ATTACK, entity.getAttackPwr());
    }

    @Test
    void should_UpdateAttackPower_When_SetAttackPwrCalled() {
        entity.setAttackPwr(25);
        assertEquals(25, entity.getAttackPwr());
    }

    @Test
    void should_ReturnDefensePower_When_GetDefensePwrCalled() {
        assertEquals(INITIAL_DEFENSE, entity.getDefensePwr());
    }

    @Test
    void should_UpdateDefensePower_When_SetDefensePwrCalled() {
        entity.setDefensePwr(18);
        assertEquals(18, entity.getDefensePwr());
    }

    @Test
    void should_BeAlive_When_HealthIsPositive() {
        assertTrue(entity.isAlive());
    }

    @Test
    void should_NotBeAlive_When_HealthIsZeroOrNegative() {
        entity.setHealthPts(0);
        assertFalse(entity.isAlive());

        entity.setHealthPts(-10);
        assertFalse(entity.isAlive());
    }

    @Test
    void should_ReturnRandomValueWithinRange_When_GetRandomInclusiveCalled() {
        for (int i = 0; i < 100; i++) {
            int randomValue = entity.getRandomInclusive(10, 20);
            assertTrue(randomValue >= 10 && randomValue <= 20);
        }
    }

    @Test
    void should_BeInMeleeRange_When_TargetsAreAdjacent() {
        assertTrue(entity.isInMeleeRange(adjacentEntity));
    }

    @Test
    void should_NotBeInMeleeRange_When_TargetsAreNotAdjacent() {
        assertFalse(entity.isInMeleeRange(nonAdjacentEntity));
    }

    @Test
    void should_ReduceHealthCorrectly_When_TakeDamageCalledWithLessThanHealth() {
        entity.takeDamage(50);
        assertEquals(50, entity.getHealthPts());
    }

    @Test
    void should_SetHealthToNegative_When_TakeDamageCalledWithMoreThanHealth() {
        entity.takeDamage(110);
        assertEquals(-10, entity.getHealthPts());
    }

    // Refactored Test Cases using MockedGameEntity
    @Test
    void should_ReduceTargetHealth_When_AttackRollSucceeds() {
        GameEntity attacker = new MockedGameEntity(INITIAL_POSITION, INITIAL_ROOM, INITIAL_HEALTH, INITIAL_ATTACK, INITIAL_DEFENSE, 15);
        attacker.attack(adjacentEntity);
        assertTrue(adjacentEntity.getHealthPts() < 80);
    }

    @Test
    void should_NotChangeTargetHealth_When_AttackRollFails() {
        GameEntity attacker = new MockedGameEntity(INITIAL_POSITION, INITIAL_ROOM, INITIAL_HEALTH, INITIAL_ATTACK, INITIAL_DEFENSE, 5);
        attacker.attack(nonAdjacentEntity);
        assertEquals(50, nonAdjacentEntity.getHealthPts());
    }

    @Test
    void should_ReduceTargetHealth_When_MeleeAttackSucceedsAndInRange() {
        GameEntity attacker = new MockedGameEntity(INITIAL_POSITION, INITIAL_ROOM, INITIAL_HEALTH, INITIAL_ATTACK, INITIAL_DEFENSE, 15);
        attacker.meleeAttack(adjacentEntity);
        assertTrue(adjacentEntity.getHealthPts() < 80);
    }

    @Test
    void should_NotAttack_When_TargetNotInRange() {
        GameEntity attacker = new MockedGameEntity(INITIAL_POSITION, INITIAL_ROOM, INITIAL_HEALTH, INITIAL_ATTACK, INITIAL_DEFENSE, 15);
        attacker.meleeAttack(nonAdjacentEntity);
        assertEquals(50, nonAdjacentEntity.getHealthPts());
    }

    // Extracted Helper Class
    private static class MockedGameEntity extends GameEntity {
        private final int fixedAttackRoll;

        public MockedGameEntity(Position position, Room currentRoom, int healthPts, int attackPwr, int defensePwr, int fixedAttackRoll) {
            super(position, currentRoom, healthPts, attackPwr, defensePwr);
            this.fixedAttackRoll = fixedAttackRoll;
        }

        @Override
        public int getRandomInclusive(int min, int max) {
            return fixedAttackRoll; // Returns the fixed roll value for tests
        }
    }
}
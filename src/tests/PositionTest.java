package tests;

import classes.Position;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PositionTest {
    private static final Position POSITION_BASE = new Position(3, 5);

    public PositionTest() {
    }

    private void assertAdjacent(Position position1, Position position2) {
        Assertions.assertTrue(position1.isAdjacentTo(position2));
    }

    private void assertNotAdjacent(Position position1, Position position2) {
        Assertions.assertFalse(position1.isAdjacentTo(position2));
    }

    @Test
    public void should_ReturnTrue_When_PositionIsAdjacentHorizontally() {
        Position adjacentHorizontally = new Position(4, POSITION_BASE.getY());
        this.assertAdjacent(POSITION_BASE, adjacentHorizontally);
    }

    @Test
    public void should_ReturnTrue_When_PositionIsAdjacentVertically() {
        Position adjacentVertically = new Position(POSITION_BASE.getX(), 6);
        this.assertAdjacent(POSITION_BASE, adjacentVertically);
    }

    @Test
    public void should_ReturnTrue_When_PositionIsAdjacentDiagonally() {
        Position adjacentDiagonally = new Position(4, 6);
        this.assertAdjacent(POSITION_BASE, adjacentDiagonally);
    }

    @Test
    public void should_ReturnFalse_When_PositionIsTooFarHorizontally() {
        Position distantHorizontally = new Position(5, POSITION_BASE.getY());
        this.assertNotAdjacent(POSITION_BASE, distantHorizontally);
    }

    @Test
    public void should_ReturnFalse_When_PositionIsTooFarVertically() {
        Position distantVertically = new Position(POSITION_BASE.getX(), 7);
        this.assertNotAdjacent(POSITION_BASE, distantVertically);
    }

    @Test
    public void should_ReturnFalse_When_PositionIsSame() {
        Position samePosition = new Position(POSITION_BASE.getX(), POSITION_BASE.getY());
        this.assertNotAdjacent(POSITION_BASE, samePosition);
    }

    @Test
    public void should_ReturnTrue_When_PositionIsAdjacentWithNegativeCoordinates() {
        Position position1 = new Position(-2, -3);
        Position position2 = new Position(-1, -3);
        this.assertAdjacent(position1, position2);
    }

    @Test
    public void should_ReturnFalse_When_PositionsAreAcrossQuadrants() {
        Position positionAcrossQuadrants = new Position(1, 2);
        Position negativePosition = new Position(-2, -3);
        this.assertNotAdjacent(negativePosition, positionAcrossQuadrants);
    }

    @Test
    public void should_ReturnXCoordinate_When_GetXCalled() {
        Position position = new Position(4, 5);
        Assertions.assertEquals(4, position.getX());
    }

    @Test
    public void should_ReturnYCoordinate_When_GetYCalled() {
        Position position = new Position(4, 5);
        Assertions.assertEquals(5, position.getY());
    }

    @Test
    public void should_UpdateXCoordinate_When_SetXCalled() {
        Position position = new Position(4, 5);
        position.setX(7);
        Assertions.assertEquals(7, position.getX());
    }

    @Test
    public void should_UpdateYCoordinate_When_SetYCalled() {
        Position position = new Position(4, 5);
        position.setY(8);
        Assertions.assertEquals(8, position.getY());
    }

    @Test
    public void should_ReturnFalse_When_NegativeDiagonalPositionsAreNotAdjacent() {
        Position position1 = new Position(-5, -5);
        Position position2 = new Position(-7, -7);
        this.assertNotAdjacent(position1, position2);
    }
}
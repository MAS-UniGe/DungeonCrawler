package tests;

import classes.Dungeon;
import classes.Room;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DungeonTest {

    @Test
    public void should_InitializeEmptyDungeon_When_ConstructorIsCalled() {
        // Arrange & Act
        Dungeon dungeon = new Dungeon();

        // Assert
        assertAll(
                () -> Assertions.assertNotNull(dungeon.getRooms(), "getRooms should not return null"),
                () -> Assertions.assertEquals(0, dungeon.size(), "Dungeon size should be 0")
        );
    }

    @Test
    public void should_AddRoomSuccessfully_When_ValidRoomIsAdded() {
        // Arrange
        Dungeon dungeon = new Dungeon();
        Room room = new Room(40, 40);

        // Act
        dungeon.addRoom(room);

        // Assert
        assertAll(
                () -> Assertions.assertEquals(1, dungeon.size(), "Dungeon size should increase to 1"),
                () -> Assertions.assertSame(room, dungeon.getRoom(0), "The added room should be retrievable from dungeon")
        );
    }

    @Test
    public void should_ThrowException_When_NullRoomIsAdded() {
        // Arrange
        Dungeon dungeon = new Dungeon();

        // Act & Assert
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> dungeon.addRoom(null),
                "Adding null room should throw IllegalArgumentException"
        );

        assertEquals("Cannot add a null room to the dungeon.", exception.getMessage());
    }

    @Test
    public void should_IncreaseDungeonSize_When_MultipleRoomsAreAdded() {
        // Arrange
        Dungeon dungeon = new Dungeon();
        Room room1 = new Room(40, 40);
        Room room2 = new Room(40, 40);

        // Act
        dungeon.addRoom(room1);
        dungeon.addRoom(room2);

        // Assert
        List<Room> rooms = dungeon.getRooms();
        assertAll(
                () -> Assertions.assertEquals(2, dungeon.size(), "Dungeon size should increase to 2"),
                () -> assertTrue(rooms.contains(room1), "Rooms list should contain room1"),
                () -> assertTrue(rooms.contains(room2), "Rooms list should contain room2")
        );
    }

    @Test
    public void should_ReturnCorrectRoom_When_ValidIndexIsProvided() {
        // Arrange
        Dungeon dungeon = new Dungeon();
        Room room = new Room(40, 40);
        dungeon.addRoom(room);

        // Act
        Room retrievedRoom = dungeon.getRoom(0);

        // Assert
        assertSame(room, retrievedRoom, "getRoom should return the correct room");
    }

    @Test
    public void should_ThrowException_When_IndexIsOutOfBounds() {
        // Arrange
        Dungeon dungeon = new Dungeon();

        // Act & Assert
        Exception exception = assertThrows(
                IndexOutOfBoundsException.class,
                () -> dungeon.getRoom(0),
                "Accessing an out-of-bounds index should throw IndexOutOfBoundsException"
        );

        assertEquals("Index 0 is out of bounds for dungeon size 0", exception.getMessage());
    }

    @Test
    public void should_ReturnAllRooms_When_RoomsAreAdded() {
        // Arrange
        Dungeon dungeon = new Dungeon();
        Room room1 = new Room(40, 40);
        Room room2 = new Room(40, 40);
        dungeon.addRoom(room1);
        dungeon.addRoom(room2);

        // Act
        List<Room> rooms = dungeon.getRooms();

        // Assert
        assertAll(
                () -> assertEquals(2, rooms.size(), "getRooms should return list with size 2"),
                () -> assertTrue(rooms.contains(room1), "getRooms should contain room1"),
                () -> assertTrue(rooms.contains(room2), "getRooms should contain room2")
        );
    }
}
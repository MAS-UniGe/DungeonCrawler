package classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a dungeon consisting of multiple rooms.
 * Provides methods for managing rooms, retrieving specific rooms,
 * and querying the size and position of rooms within the dungeon.
 */
public class Dungeon {
    private final List<Room> rooms; // List to store rooms in the dungeon

    /**
     * Constructs an empty Dungeon instance.
     */
    public Dungeon() {
        this.rooms = new ArrayList<>();
    }

    // ----------------------------------------------
    // Room Management
    // ----------------------------------------------

    /**
     * Gets the total number of rooms in the dungeon.
     *
     * @return The number of rooms in the dungeon
     */
    public int size() {
        return rooms.size();
    }

    /**
     * Adds a new room to the dungeon.
     *
     * @param room The room to add
     * @throws IllegalArgumentException If the room is null
     */
    public void addRoom(Room room) {
        if (room == null)
            throw new IllegalArgumentException("Cannot add a null room to the dungeon.");

        rooms.add(room);
    }

    /**
     * Retrieves a room at the specified index in the dungeon.
     *
     * @param index The index of the room to retrieve
     * @return The room at the specified index
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public Room getRoom(int index) {
        if (index < 0 || index >= rooms.size())
            throw new IndexOutOfBoundsException(
                    "Index " + index + " is out of bounds for dungeon size " + rooms.size()
            );

        return rooms.get(index);
    }

    /**
     * Retrieves an unmodifiable list of all rooms in the dungeon.
     *
     * @return An unmodifiable view of the list of rooms
     */
    public List<Room> getRooms() { return rooms; }

    /**
     * Gets the index of a specific room in the dungeon.
     *
     * @param room The room to find
     * @return The index of the room, or -1 if the room is not in the dungeon
     * @throws IllegalArgumentException If the room is null
     */
    public int getRoomIndex(Room room) {
        if (room == null)
            throw new IllegalArgumentException("Room cannot be null.");

        return rooms.indexOf(room);
    }
}
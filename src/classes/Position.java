package classes;

/**
 * Represents a 2D position in a coordinate system with x and y coordinates.
 * Provides utility methods for operations, such as translation, range checks,
 * distance calculation, and parsing from string.
 */
public class Position {
    private int x; // The x-coordinate of the position
    private int y; // The y-coordinate of the position

    /**
     * Constructs a Position with the specified x and y coordinates.
     *
     * @param x The x-coordinate of the position
     * @param y The y-coordinate of the position
     */
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // ----------------------------------------------
    // Getters and Setters
    // ----------------------------------------------

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    // ----------------------------------------------
    // Transformation and Utility Methods
    // ----------------------------------------------

    /**
     * Translates this position by the specified dx and dy offsets.
     *
     * @param dx The offset to apply to the x-coordinate
     * @param dy The offset to apply to the y-coordinate
     * @return A new Position representing the translated position
     */
    public Position translate(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    /**
     * Checks if this position is within a grid-based range of another position.
     *
     * @param other The position to check range against
     * @param range The Manhattan range
     * @return True if the distance is within range, false otherwise
     */
    public boolean isInRange(Position other, int range) {
        int xDiff = Math.abs(other.x - this.x);
        int yDiff = Math.abs(other.y - this.y);
        return xDiff + yDiff <= range; // Manhattan distance comparison
    }

    /**
     * Checks if this position is adjacent to another.
     *
     * @param other The position to check adjacency with
     * @return True if adjacent, false otherwise
     */
    public boolean isAdjacentTo(Position other) {
        return this.isInRange(other, 1);
    }

    /**
     * Calculates the Euclidean distance to another position.
     *
     * @param other The position to calculate the distance to
     * @return The Euclidean distance as a double
     */
    public double distanceTo(Position other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy); // Pythagorean theorem
    }

    // ----------------------------------------------
    // Parsing and String Representation
    // ----------------------------------------------

    /**
     * Generates a string representation of the position in the format "(x, y)".
     *
     * @return A string representation of this position
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    /**
     * Parses a Position object from a string representation of the format "(x, y)".
     *
     * @param position The string representation of the position
     * @return A new Position constructed from the parsed string
     * @throws IllegalArgumentException If the input string is not correctly formatted
     */
    public static Position fromString(String position) {
        try {
            String[] splitPosition = position.replace("(", "").replace(")", "").split(", ");
            return new Position(Integer.parseInt(splitPosition[0]), Integer.parseInt(splitPosition[1]));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid position format: " + position, e);
        }
    }

    // ----------------------------------------------
    // Equality Check
    // ----------------------------------------------

    /**
     * Checks if this position is equivalent to another.
     *
     * @param other The other position to compare with
     * @return True if both positions have the same x and y coordinates, false otherwise
     */
    public boolean equals(Position other) {
        if (other == null) return false;
        return this.x == other.x && this.y == other.y;
    }
}

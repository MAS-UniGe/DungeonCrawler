package classes;

public interface GameEventListener {
    void onEntityMoved(Position oldPosition, Position newPosition, EntityType entityType);
    void onEnemyKilled(Position position, StandardEnemy enemy);
    void onPlayerAttacked(Player player);
    void onGameOver(boolean win);
    void printLogMessage(String message);
    void printPlayerMessage(String message);

    void  collectPowerUp(GameEntity collector, Position powerupPosition, Room currentRoom);
    void onWin();
    void onPlayerMovedToNextRoom(Room targetRoom);
}
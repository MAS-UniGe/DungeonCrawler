package agents;

import agents.behaviours.BossResponseBehaviour;
import agents.behaviours.IdleBehaviour;
import classes.BossEnemy;
import classes.GameManager;
import jade.core.behaviours.TickerBehaviour;

public class BossEnemyAgent extends StandardEnemyAgent {

    @Override
    protected void setup() {
        super.setup();
    }

    /**
     * Extract and initialize agent parameters from arguments passed during initialization.
     */
    @Override
    protected void initializeAgent() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            super.enemy = (BossEnemy) args[0];
            this.gameManager = (GameManager) args[1];
        }
    }

    /**
     * Initializes and add the desired behaviors for this boss agent.
     */
    @Override
    protected void initializeBehaviours() {
        // Add initial behavior for movement and action
        addBehaviour(new IdleBehaviour(this, DEFAULT_BEHAVIOUR_DELAY)); // Delay of 1 second between actions
        addBehaviour(new BossResponseBehaviour(this));

        // Add behaviour to monitor the life of the enemy
        addBehaviour(new TickerBehaviour(this, DEFAULT_BEHAVIOUR_DELAY) { // Check every 1 second
            @Override
            protected void onTick() {
                if (!enemy.isAlive() || !gameManager.getPlayer().isAlive()) {
                   gameManager.getGameEventListener().printLogMessage("Boss killed");
                    gameManager.getGameEventListener().onWin();
                    doDelete(); // Delete agent if enemy is dead
                }
            }
        });
    }

    @Override
    public boolean isBossAgent() { return true; }
}
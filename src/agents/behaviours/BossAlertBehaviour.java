package agents.behaviours;

import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import agents.StandardEnemyAgent;

/**
 * BossAlertBehaviour handles the alerting of the BossEnemyAgent
 * regarding the player's presence. This behavior is executed as a
 * one-shot action where an enemy informs the boss about the player's
 * survival to enhance its strength.
 */
public class BossAlertBehaviour extends OneShotBehaviour {
    private final StandardEnemyAgent enemyAgent; // Reference to the enemy agent initiating the alert

    /**
     * Constructor that initializes the alert behavior.
     *
     * @param agent The standard enemy agent responsible for the alert
     */
    public BossAlertBehaviour(StandardEnemyAgent agent) {
        super(agent);
        this.enemyAgent = agent;
    }

    /**
     * Notify the boss agent about the player's presence by sending a message.
     * This method terminates the behavior after the notification is sent.
     */
    @Override
    public void action() {
        // If the player is no longer alive, remove this behavior from the agent
        if (!enemyAgent.getGameManager().getPlayer().isAlive()) {
            enemyAgent.removeBehaviour(this);
            return;
        }

        // Log the alert notification
        enemyAgent.speak("Notifying " + enemyAgent.getBossAgentAID().getLocalName() + " about player presence.");

        // Create and send an alert message to the BossEnemyAgent
        ACLMessage alertMessage = new ACLMessage(ACLMessage.INFORM);
        alertMessage.setContent("PLAYER_SURVIVAL_ALERT");        // Specify the purpose of the alert
        alertMessage.addReceiver(enemyAgent.getBossAgentAID()); // Address the boss agent

        enemyAgent.send(alertMessage); // Dispatch the message to the boss
    }
}
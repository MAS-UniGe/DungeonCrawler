package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * The ProxyAgent class is a JADE agent responsible for communicating on behalf of the GameManager
 * by enabling Object-to-Agent (O2A) messaging and forwarding messages to other agents.
 */
public class ProxyAgent extends Agent {

    /**
     * Initializes the ProxyAgent.
     * It enables O2A communication and sets up a behavior to handle and forward messages.
     */
    @Override
    protected void setup() {
        // Enable Object-to-Agent communication
        setEnabledO2ACommunication(true, 0);

        // Add a cyclic behavior to process incoming O2A messages
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                // Retrieve and process messages from the O2A queue
                Object object = getO2AObject(); // Retrieve an object from the O2A queue
                if (object instanceof ACLMessage message) {
                    send(message); // Sends an ACLMessage using the agent's messaging system.
                }
            }
        });
    }

    /**
     * Cleans up resources when the agent is terminated.
     */
    @Override
    protected void takeDown() {
        // Disable Object-to-Agent communication
        setEnabledO2ACommunication(false, 0);
    }
}
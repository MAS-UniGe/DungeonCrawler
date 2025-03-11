package utils;

import agents.EnemyState;
import classes.Position;
import agents.StandardEnemyAgent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.util.*;
import java.util.function.Consumer;

/**
 * Utility class for handling message triggers, processing inter-agent communication,
 * and broadcasting alerts within the game environment.
 */
public class MessageHandlerUtils {
    // Map of message triggers to their respective handlers
    private final Map<String, Consumer<ACLMessage>> messageHandlers = new HashMap<>();

    // ----------------------------------------------
    // Message Handler Registration and Execution
    // ----------------------------------------------

    /**
     * Register a handler for a specific message trigger.
     *
     * @param trigger The message trigger type (e.g., "RETREATING", "POWER_UP_COLLECTED").
     * @param handler The logic to execute for the specific trigger.
     */
    public void registerHandler(String trigger, Consumer<ACLMessage> handler) {
        messageHandlers.put(trigger, handler);
    }

    /**
     * Handles an incoming message by invoking the appropriate registered handler
     * based on the message's trigger type.
     *
     * @param message The message to process.
     */
    public void handleMessage(ACLMessage message) {
        if (message == null || message.getContent() == null) {
            return; // No action if the message or its content is null
        }

        // Extract the trigger from the message content (format: "TRIGGER:data")
        String trigger = message.getContent().split(":")[0];

        if (messageHandlers.containsKey(trigger)) {
            // Call the appropriate handler for the extracted trigger
            messageHandlers.get(trigger).accept(message);
        }
    }

    /**
     * Extracts the data portion of the message. Assumes the format "TRIGGER:data".
     *
     * @param message The ACLMessage from which to extract data.
     * @return An Optional containing the message data if present.
     */
    public static Optional<String> extractMessageData(ACLMessage message) {
        if (message == null || message.getContent() == null) {
            return Optional.empty(); // Return empty if message or content doesn't exist
        }

        // Assuming format "TRIGGER:data"
        String[] parts = message.getContent().split(":");
        if (parts.length > 1) {
            return Optional.of(parts[1]); // Return the data part
        }

        return Optional.empty();
    }

    // ----------------------------------------------
    // Inter-Agent Communication: Agent Search
    // ----------------------------------------------

    /**
     * Finds agents based on their current state.
     *
     * @param standardEnemyAgent The agent performing the search.
     * @param state              The desired state of the agents to search for.
     * @return A list of agent descriptions matching the criteria.
     */
    private static List<DFAgentDescription> findAgentsByState(StandardEnemyAgent standardEnemyAgent, EnemyState state) {
        try {
            // Descriptor template for querying the Directory Facilitator (DF)
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("enemy"); // Specify the type of service
            sd.addProperties(new Property("state", state)); // Add state property for filtering
            template.addServices(sd); // Add services to the template

            // Perform the search and return results
            return Arrays.asList(DFService.search(standardEnemyAgent, template));
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception for debugging
            return List.of(); // Return an empty list in case of an error
        }
    }

    // ----------------------------------------------
    // Inter-Agent Communication: Broadcast Alerts
    // ----------------------------------------------

    /**
     * Broadcasts an alert message to other agents based on their states.
     *
     * @param sender         The agent sending the alert.
     * @param alertType      The type of alert (e.g., "RETREATING", "POWER_UP_COLLECTED").
     * @param receiverTypes  A list of the states of agents that should receive the alert.
     * @param targetPos      The target position related to the alert.
     * @param performative   The specified performative to use in the message.
     */
    public static void broadcastAlert(StandardEnemyAgent sender, String alertType, List<EnemyState> receiverTypes, Position targetPos, int performative) {
        try {
            List<DFAgentDescription> receivers = new ArrayList<>();

            // Find all agents matching the given receiver states
            for (EnemyState receiverType : receiverTypes)
                receivers.addAll(findAgentsByState(sender, receiverType));

            // Create a new alert message with the specified performative
            ACLMessage alertMessage = new ACLMessage(performative);

            String contentString = alertType + ":";
            if (alertType.equals("RETREATING"))
                contentString += "StandardEnemyAgent-" + sender.getEnemy().hashCode(); // Add sender info in retreat case
            else
                contentString += targetPos.toString(); // Add target position in other cases

            alertMessage.setContent(contentString); // Set the message content

            // Add all relevant agents as recipients
            for (DFAgentDescription agent : receivers)
                if (!agent.getName().equals(sender.getAID())) // Avoid sending to itself
                    alertMessage.addReceiver(agent.getName());

            // Send the alert message
            sender.send(alertMessage);
        } catch (Exception e) {
            e.printStackTrace(); // Log any errors during broadcast
        }
    }
}
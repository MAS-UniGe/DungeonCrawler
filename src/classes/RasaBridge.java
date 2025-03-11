package classes;

import com.google.gson.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RasaBridge {
    private final String rasaBotEndpointUrl;
    private static final String RASA_BOT_ENDPOINT = "/webhooks/rest/webhook";
    private static final String DEFAULT_CONVERSATION_ID = "user";
    private final String conversation_id;
    private static final String RASA_TRACKER_EVENTS_ENDPOINT = "/conversations/";
    private final String rasaTrackerEventsURL;

    public RasaBridge(String rasaEndpointUrl, String conversationId) {
        if (rasaEndpointUrl == null || rasaEndpointUrl.isEmpty() || conversationId == null || conversationId.isEmpty())
            throw new IllegalArgumentException("Rasa endpoint URL cannot be null or empty");

        this.conversation_id = conversationId;
        this.rasaBotEndpointUrl = rasaEndpointUrl + RASA_BOT_ENDPOINT;
        this.rasaTrackerEventsURL = rasaEndpointUrl + RASA_TRACKER_EVENTS_ENDPOINT + conversation_id + "/tracker/events";

    }

    public RasaBridge(String rasaEndpointUrl) {
        if (rasaEndpointUrl == null || rasaEndpointUrl.isEmpty())
            throw new IllegalArgumentException("Rasa endpoint URL cannot be null or empty");

        this.conversation_id = DEFAULT_CONVERSATION_ID;
        this.rasaBotEndpointUrl = rasaEndpointUrl + RASA_BOT_ENDPOINT;
        this.rasaTrackerEventsURL = rasaEndpointUrl + RASA_TRACKER_EVENTS_ENDPOINT + conversation_id + "/tracker/events";
    }

    /**
     * Sends a message to the Rasa bot and returns its response.
     *
     * @param message The user's message to send to Rasa.
     * @return The bot's response as a String.
     * @throws IOException If communication with Rasa fails.
     * @throws IllegalArgumentException If the input message is invalid.
     */
    public String sendMessageToRasaBot(String message) throws IOException {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        try {
            // Set up the connection
            HttpURLConnection connection = setUpConnection(rasaBotEndpointUrl);

            // Building the JSON request payload
            String inputJson = buildJsonPayload(Map.of(
                    "sender", "user",
                    "message", message
            ));

            // Send message to Rasa
            sendMessage(inputJson, connection);

            // Read the response from the server
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String responsePayload = getResponsePayload(connection);

                // Parse JSON response and extract the "text" field using Gson
                return extractResponse(responsePayload);
            } else {
                throw new IOException("Unexpected HTTP response code: " + responseCode);
            }

        } catch (MalformedURLException e) {
            throw new IOException("Invalid Rasa bot endpoint URL: " + rasaBotEndpointUrl, e);
        } catch (IOException e) {
            throw new IOException("Error communicating with Rasa bot", e);
        }

    }

    /**
     * Sends a custom JSON payload to update Rasa's tracker events.
     *
     * @param jsonPayloadPropertiesToSend A map of properties to include in the JSON payload.
     * @throws IOException If communication with Rasa fails.
     * @throws IllegalArgumentException If the payload is invalid.
     */
    public void sendMessageToRasaTrackerEvents(Map<String, String> jsonPayloadPropertiesToSend) throws IOException {
        if (jsonPayloadPropertiesToSend == null || jsonPayloadPropertiesToSend.isEmpty())
            throw new IllegalArgumentException("Payload properties cannot be null or empty");

        try {
            // Open HTTP connection
            HttpURLConnection connection = setUpConnection(rasaTrackerEventsURL);

            // Prepare JSON payload
            String inputJson = buildJsonPayload(jsonPayloadPropertiesToSend);

            // Send the request
            sendMessage(inputJson, connection);

            // Check the response code to ensure the update was successful
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK)
                System.out.println("Successfully updated Rasa tracker.");
            else
                throw new IOException("Failed to update Rasa tracker: HTTP " + responseCode);

        } catch (MalformedURLException e) {
            throw new IOException("Invalid Rasa tracker events endpoint URL: " + rasaTrackerEventsURL, e);
        } catch (IOException e) {
            throw new IOException("Error communicating with Rasa tracker events", e);
        }

    }

    /**
     * Sets up the HTTP connection to the specified endpoint.
     *
     * @param endpoint The endpoint URL to connect to.
     * @return An initialized HttpURLConnection object.
     * @throws IOException If an error occurs during connection setup.
     */
    private HttpURLConnection setUpConnection(String endpoint) throws IOException {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            return connection;
        } catch (MalformedURLException e) {
            throw new MalformedURLException("Invalid endpoint URL: " + endpoint);
        }
    }

    /**
     * Builds a JSON payload from the provided properties.
     *
     * @param properties A map of key-value pairs to include in the JSON payload.
     * @return A string representation of the JSON payload.
     */
    private String buildJsonPayload(Map<String, String> properties) {
        Gson gson = new Gson();
        JsonObject payload = new JsonObject();
        properties.forEach(payload::addProperty);
        return gson.toJson(payload);
    }

    /**
     * Sends the JSON payload to the server via the provided connection.
     *
     * @param inputJson  The JSON payload as a String.
     * @param connection The HttpURLConnection to use.
     * @throws IOException If an I/O error occurs during the send operation.
     */
    private void sendMessage(String inputJson, HttpURLConnection connection) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = inputJson.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }

    /**
     * Reads the response payload from the endpoint.
     *
     * @param connection The HttpURLConnection to read from.
     * @return The response payload as a String.
     * @throws IOException If an I/O error occurs during reading.
     */
    private String getResponsePayload(HttpURLConnection connection) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    /**
     * Extracts the "text" field from the Rasa bot's JSON response.
     *
     * @param responsePayload The response JSON payload as a String.
     * @return The value of the "text" field.
     * @throws JsonParseException If the response JSON is invalid or doesn't include the expected field.
     */
    private String extractResponse(String responsePayload) {
        JsonArray jsonArray = JsonParser.parseString(responsePayload).getAsJsonArray();
        if (!jsonArray.isEmpty()) {
            JsonElement textElement = jsonArray.get(0).getAsJsonObject().get("text");
            if (textElement != null) {
                return textElement.getAsString();
            }
        }
        throw new JsonParseException("Response JSON does not contain a valid 'text' field");
    }

}
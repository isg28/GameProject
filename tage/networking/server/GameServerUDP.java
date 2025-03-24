package tage.networking.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;
import java.util.HashMap;

/**
 * Represents the UDP-based server for handling multiplayer game communication.
 * <p>
 * GameServerUDP class extends {@link GameConnectionServer} and processes
 * various message types such as join, create, move, bye, and skybox updates from connected clients.
 * It manages client connections, maintains their avatar positions, and relays messages
 * between all active clients in the session.
 * </p>
 * 
 * <p>This class acts as the core of the server-side multiplayer architecture and enables
 * synchronized state across all connected clients using the UDP protocol.</p>
 * 
 * Message formats include:
 * <ul>
 *   <li><code>join,&lt;UUID&gt;</code></li>
 *   <li><code>create,&lt;UUID&gt;,&lt;x&gt;,&lt;y&gt;,&lt;z&gt;</code></li>
 *   <li><code>move,&lt;UUID&gt;,&lt;x&gt;,&lt;y&gt;,&lt;z&gt;</code></li>
 *   <li><code>bye,&lt;UUID&gt;</code></li>
 *   <li><code>skybox,&lt;index&gt;</code></li>
 * </ul>
 * @author 
 */
public class GameServerUDP extends GameConnectionServer<UUID> {
    private HashMap<UUID, String[]> clientPositions = new HashMap<>();
    private int currentSkyboxIndex = 0;

    /**
     * Constructs the UDP game server bound to the specified port.
     *
     * @param localPort The port number the server listens on.
     * @throws IOException If there is an error initializing the server socket.
    */
    public GameServerUDP(int localPort) throws IOException {
        super(localPort, ProtocolType.UDP);
        System.out.println("[Server] Started UDP server on port " + localPort);
    }

    /**
     * Processes incoming packets from clients based on their message type.
     *
     * @param o        The message object (expected to be a String).
     * @param senderIP The IP address of the sender.
     * @param sndPort  The sender's port number.
    */
    @Override
    public void processPacket(Object o, InetAddress senderIP, int sndPort) {
        String message = (String) o;
        System.out.println("[Server] Received packet: " + message); 

        String[] msgTokens = message.split(",");
        if (msgTokens.length > 0) {
            UUID clientID;

            switch (msgTokens[0]) {
                case "join":
                    handleJoin(msgTokens, senderIP, sndPort);
                    break;

                case "create":
                    clientID = UUID.fromString(msgTokens[1]);
                    String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
                    System.out.println("[Server] Processing create request for " + clientID);
                    sendCreateMessages(clientID, pos);
                    sendWantsDetailsMessages(clientID);
                    break;

                case "bye":
                    clientID = UUID.fromString(msgTokens[1]);
                    System.out.println("[Server] Client " + clientID + " disconnected.");
                    sendByeMessages(clientID);
                    removeClient(clientID);
                    break;

                case "move":
                    clientID = UUID.fromString(msgTokens[1]);
                    String[] newPos = { msgTokens[2], msgTokens[3], msgTokens[4] };
                
                    clientPositions.put(clientID, newPos);
                
                    System.out.println("[Server] Client " + clientID + " moved to " + newPos[0] + ", " + newPos[1] + ", " + newPos[2]);
                    sendMoveMessages(clientID, newPos);
                break;

                case "skybox":
                    currentSkyboxIndex = Integer.parseInt(msgTokens[1]);
                    broadcastSkyboxChange(currentSkyboxIndex);
                break;

                default:
                    System.out.println("[Server] Unknown message type: " + msgTokens[0]);
                    break;
            }
        }
    }
    /**
     * Handles new client join requests and registers them with the server.
     *
     * @param msgTokens The message tokens from the join request.
     * @param senderIP  The IP address of the joining client.
     * @param sndPort   The port number of the joining client.
    */
    private void handleJoin(String[] msgTokens, InetAddress senderIP, int sndPort) {
        try {
            UUID clientID = UUID.fromString(msgTokens[1]);
            IClientInfo ci = getServerSocket().createClientInfo(senderIP, sndPort);
            addClient(ci, clientID);
            sendJoinedMessage(clientID, true);
            System.out.println("[Server] Client " + clientID + " joined.");
    
            //  Broadcast the new client's presence to all existing players
            String[] defaultPosition = { "0.0", "0.0", "0.0" };  
            sendCreateMessages(clientID, defaultPosition);
    
            // Ask existing players to send their details to the new player
            sendWantsDetailsMessages(clientID);
            sendSkyboxToClient(clientID); 

        } catch (IOException e) {
            System.out.println("[Server] Error processing join request!");
            e.printStackTrace();
        }
    }
    /**
     * Sends a join acknowledgment back to the client.
     *
     * @param clientID The UUID of the client.
     * @param success  Whether the join was successful.
    */
    public void sendJoinedMessage(UUID clientID, boolean success) {
        try {
            String message = "join," + (success ? "success" : "failure");
            sendPacket(message, clientID);
            System.out.println("[Server] Sent join response to " + clientID + ": " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Broadcasts a new client's creation to all existing clients.
     *
     * @param clientID The UUID of the new client.
     * @param position The initial position of the client avatar.
    */
    public void sendCreateMessages(UUID clientID, String[] position) {
        try {
            String message = "create," + clientID.toString() + "," + position[0] + "," + position[1] + "," + position[2];
            System.out.println("[Server] Broadcasting creation of " + clientID + " to all clients.");
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Sends details of all currently connected clients to a new client.
     *
     * @param newClientID The UUID of the new client.
    */
    public void sendWantsDetailsMessages(UUID newClientID) {
        try {
            System.out.println("[Server] Sending details of existing clients to new client " + newClientID);
    
            for (UUID existingClientID : getClients().keySet()) {
                if (!existingClientID.equals(newClientID)) {
                    String[] position = clientPositions.getOrDefault(existingClientID, new String[]{"0.0", "0.0", "0.0"});
                    String message = "create," + existingClientID.toString() + "," + position[0] + "," + position[1] + "," + position[2];
    
                    sendPacket(message, newClientID);
                    System.out.println("[Server] Sent actual position of client " + existingClientID + " to new client " + newClientID);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Broadcasts a movement update to all clients except the sender.
     *
     * @param clientID The UUID of the client that moved.
     * @param position The new position of the client.
    */
    public void sendMoveMessages(UUID clientID, String[] position) {
        try {
            if (position.length < 3) {
                System.out.println("[Server] ERROR: Malformed position data for client " + clientID);
                return;
            }
    
            String message = "move," + clientID.toString() + "," + position[0] + "," + position[1] + "," + position[2];
            System.out.println("[Server] Broadcasting movement for " + clientID + " → " + message);
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Notifies all clients that a specific client has left.
     *
     * @param clientID The UUID of the departing client.
    */
    public void sendByeMessages(UUID clientID) {
        try {
            String message = "bye," + clientID.toString();
            System.out.println("[Server] Notifying clients that " + clientID + " has left.");
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Broadcasts a skybox change index to all clients.
     *
     * @param index The skybox index to apply.
    */
    public void broadcastSkyboxChange(int index) {
        try {
            String message = "skybox," + index;
            System.out.println("[Server] Broadcasting skybox index: " + index);
    
            // Send to all connected clients manually
            for (UUID clientID : getClients().keySet()) {
                sendPacket(message, clientID);
            }
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Sends the current skybox index to a newly joined client.
     *
     * @param clientID The UUID of the new client.
    */
    private void sendSkyboxToClient(UUID clientID) {
        try {
            String message = "skybox," + currentSkyboxIndex;
            sendPacket(message, clientID);
            System.out.println("[Server] Sent current skybox index " + currentSkyboxIndex + " to new client " + clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
}

package tage.networking.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import a3.GhostManager;
import a3.MyGame;
import a3.SkyboxManager;
import tage.networking.IGameConnection.ProtocolType;
import tage.networking.client.GameConnectionClient;

/**
 * Handles all network communication between the client and the server.
 * <p>
 * ProtocolClient is responsible for sending and receiving
 * protocol-specific messages in a multiplayer environment. It interprets incoming
 * messages such as avatar creation, movement updates, disconnections, and skybox changes,
 * and performs the appropriate client-side actions.
 * </p>
 *
 * @author YourName
 */

public class ProtocolClient extends GameConnectionClient {
    private MyGame game;
    private UUID id;
    private GhostManager ghostManager;

    /**
     * Constructs a ProtocolClient used for communicating with the server.
     *
     * @param remAddr  The server's IP address.
     * @param remPort  The port number the server is listening on.
     * @param pType    The protocol type (UDP).
     * @param game     The main game instance.
     * @throws IOException if socket connection fails.
    */
    public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game) throws IOException {
        super(remAddr, remPort, pType);
        this.game = game;
        this.id = UUID.randomUUID();
        this.ghostManager = game.getGhostManager();
    }

    /**
     * Handles incoming messages from the server.
     *
     * @param message The message received from the server.
    */
    @Override
    protected void processPacket(Object message) {
        if (!(message instanceof String)) {
            System.out.println("[Client] ERROR: Received non-string packet: " + message);
            return;
        }
        String msg = (String) message; 
        System.out.println("[Client] Received packet: " + msg);
        String[] msgTokens = msg.split(","); 
    
        if (msgTokens.length < 2) {  
            System.out.println("[Client] ERROR: Malformed message received: " + msg);
            return;
        }
    
        switch (msgTokens[0]) {
            case "create":
                if (msgTokens.length < 5) {
                    System.out.println("[Client] ERROR: Create message is incomplete: " + msg);
                    return;
                }
    
                UUID newClientID = UUID.fromString(msgTokens[1]);
                try {
                    float x = Float.parseFloat(msgTokens[2]);
                    float y = Float.parseFloat(msgTokens[3]);
                    float z = Float.parseFloat(msgTokens[4]);
                    Vector3f newGhostPosition = new Vector3f(x, y, z);
    
                    System.out.println("[Client] Creating ghost for new client: " + newClientID);
                    try {
                        game.getGhostManager().createGhost(newClientID, newGhostPosition);
                    } catch (IOException e) {
                        System.out.println("[Client] ERROR: Failed to create ghost avatar for " + newClientID);
                        e.printStackTrace();
                    }

                    game.getProtocolClient().sendDetailsForMessage(newClientID, game.getPlayerPosition());
                } catch (NumberFormatException e) {
                    System.out.println("[Client] ERROR: Failed to parse create position values: " + msg);
                }
                break;
    
            case "move":
                if (msgTokens.length < 5) {
                    System.out.println("[Client] ERROR: Move message is incomplete: " + msg);
                    return;
                }
    
                UUID remoteID = UUID.fromString(msgTokens[1]);
                try {
                    float x = Float.parseFloat(msgTokens[2]);
                    float y = Float.parseFloat(msgTokens[3]);
                    float z = Float.parseFloat(msgTokens[4]);
    
                    //System.out.println("[Client] Updating ghost " + remoteID + " position to (" + x + ", " + y + ", " + z + ")");
                    game.getGhostManager().updateGhostPosition(remoteID, new Vector3f(x, y, z));
                } catch (NumberFormatException e) {
                    System.out.println("[Client] ERROR: Failed to parse move position values: " + msg);
                }
            break;

            case "bye":
                if (msgTokens.length < 2) {
                    System.out.println("[Client] ERROR: Bye message is incomplete: " + msg);
                    return;
                }

                UUID departedID = UUID.fromString(msgTokens[1]);
                System.out.println("[Client] Removing ghost avatar for departed client: " + departedID);
                game.getGhostManager().removeGhostAvatar(departedID);
            break;

            case "skybox":
                int index = Integer.parseInt(msgTokens[1]);
                if (game.getSkyboxManager() != null) {
                    SkyboxManager sm = game.getSkyboxManager();
                    if (sm != null) {
                        sm.setSkyboxByIndex(index);
                    } else {
                        game.setPendingSkyboxIndex(index);
                    }
                } else {
                    game.setPendingSkyboxIndex(index);
                }
            break;
            case "rotate":
                if (msgTokens.length < 6) break;
                UUID rid = UUID.fromString(msgTokens[1]);
                float rx = Float.parseFloat(msgTokens[2]);
                float ry = Float.parseFloat(msgTokens[3]);
                float rz = Float.parseFloat(msgTokens[4]);
                float rw = Float.parseFloat(msgTokens[5]);
                ghostManager.updateGhostRotation(rid, new Quaternionf(rx, ry, rz, rw));
            break;


    
            default:
                System.out.println("[Client] Unrecognized message type: " + msgTokens[0]);
                break;
        }
        
    }
    
    /**
     * Sends a join message to the server.
    */
    public void sendJoinMessage() { 
        try {
            sendPacket(new String("join," + id.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to inform the server of this client's avatar creation.
     */
    public void sendCreateMessage(Vector3f pos) { 
        try {
            String message = "create," + id.toString();
            message += "," + pos.x + "," + pos.y + "," + pos.z;
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to notify the server that this client is disconnecting.
     */    
    public void sendByeMessage() { 
        try {
            sendPacket("bye," + id.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a "details-for" message to update a newly joined client
     * about this client's position.
     *
     * @param remId The UUID of the remote client.
     * @param pos   The position of this client's avatar.
     */    
    public void sendDetailsForMessage(UUID remId, Vector3f pos) { 
        try {
            String message = "dsfr," + remId.toString();
            message += "," + pos.x + "," + pos.y + "," + pos.z;
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a move update to the server indicating this client's new avatar position.
     *
     * @param position The new position of the client's avatar.
     */
    public void sendMoveMessage(Vector3f position) { 
        try {
            String message = "move," + id.toString();
            message += "," + position.x + "," + position.y + "," + position.z;
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Sends a skybox index update to the server to sync with other clients.
     *
     * @param index The index of the skybox to switch to.
     */
    public void sendSkyboxIndex(int index) {
        try {
            sendPacket("skybox," + index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**  
     * Sends a rotation update to the server indicating this client's new avatar orientation.  
     */
    public void sendRotateMessage(Quaternionf q) {
        try {
            String message = String.format(
                "rotate,%s,%.6f,%.6f,%.6f,%.6f",
                id.toString(),
                q.x, q.y, q.z, q.w
            );
            sendPacket(message);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}

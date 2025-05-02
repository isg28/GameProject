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
 * messages such as avatar creation, movement updates, disconnections, skybox changes,
 * and bee attacks, and performs the appropriate client-side actions.
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
     * @param remAddr The server's IP address.
     * @param remPort The port number the server is listening on.
     * @param pType   The protocol type (UDP).
     * @param game    The main game instance.
     * @throws IOException if socket connection fails.
     */
    public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game) throws IOException {
        super(remAddr, remPort, pType);
        this.game = game;
        this.id = UUID.randomUUID();
        this.ghostManager = game.getGhostManager();
        System.out.println("[ProtocolClient] Initialized with server: " + remAddr + ":" + remPort + ", local port: " + getLocalPort());
    }

    /**
     * Gets the local port the client is bound to.
     * @return The local port number or -1 if socket is unavailable.
     */
    public int getLocalPort() {
        // Fallback since getSocket() is undefined
        return -1; // Log unknown port; GameConnectionClient handles socket internally
    }

    /**
     * Handles incoming messages from the server.
     *
     * @param message The message received from the server.
     */
    @Override
    protected void processPacket(Object message) {
        if (!(message instanceof String)) {
            System.out.println("[ProtocolClient] ERROR: Received non-string packet: " + message);
            return;
        }
        String msg = (String) message;
        System.out.println("[ProtocolClient] Received packet: " + msg);
        String[] msgTokens = msg.split(",");

        if (msgTokens.length < 2) {
            System.out.println("[ProtocolClient] ERROR: Malformed message received: " + msg);
            return;
        }

        switch (msgTokens[0]) {
            case "create":
                if (msgTokens.length < 5) {
                    System.out.println("[ProtocolClient] ERROR: Create message is incomplete: " + msg);
                    return;
                }
                UUID newClientID = UUID.fromString(msgTokens[1]);
                try {
                    float x = Float.parseFloat(msgTokens[2]);
                    float y = Float.parseFloat(msgTokens[3]);
                    float z = Float.parseFloat(msgTokens[4]);
                    Vector3f newGhostPosition = new Vector3f(x, y, z);
                    System.out.println("[ProtocolClient] Creating ghost for new client: " + newClientID);
                    try {
                        game.getGhostManager().createGhost(newClientID, newGhostPosition);
                    } catch (IOException e) {
                        System.out.println("[ProtocolClient] ERROR: Failed to create ghost avatar for " + newClientID);
                        e.printStackTrace();
                    }
                    game.getProtocolClient().sendDetailsForMessage(newClientID, game.getPlayerPosition());
                } catch (NumberFormatException e) {
                    System.out.println("[ProtocolClient] ERROR: Failed to parse create position values: " + msg);
                }
                break;

            case "move":
                if (msgTokens.length < 5) {
                    System.out.println("[ProtocolClient] ERROR: Move message is incomplete: " + msg);
                    return;
                }
                UUID remoteID = UUID.fromString(msgTokens[1]);
                try {
                    float x = Float.parseFloat(msgTokens[2]);
                    float y = Float.parseFloat(msgTokens[3]);
                    float z = Float.parseFloat(msgTokens[4]);
                    game.getGhostManager().updateGhostPosition(remoteID, new Vector3f(x, y, z));
                } catch (NumberFormatException e) {
                    System.out.println("[ProtocolClient] ERROR: Failed to parse move position values: " + msg);
                }
                break;

            case "bye":
                if (msgTokens.length < 2) {
                    System.out.println("[ProtocolClient] ERROR: Bye message is incomplete: " + msg);
                    return;
                }
                UUID departedID = UUID.fromString(msgTokens[1]);
                System.out.println("[ProtocolClient] Removing ghost avatar for departed client: " + departedID);
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

            case "water":
                UUID remoteID2 = UUID.fromString(msgTokens[1]);
                boolean turningOn = msgTokens[2].equals("1");
                if (remoteID2.equals(id))
                    break;
                ghostManager.setGhostWatering(remoteID2, turningOn);
                break;

            case "plant":
                UUID pId = UUID.fromString(msgTokens[1]);
                float px = Float.parseFloat(msgTokens[2]);
                float py = Float.parseFloat(msgTokens[3]);
                float pz = Float.parseFloat(msgTokens[4]);
                UUID cropId = UUID.fromString(msgTokens[5]);
                String type = msgTokens[6];
                game.getGhostManager().ghostPlant(pId, cropId, new Vector3f(px, py, pz), type);
                break;

            case "harvest":
                UUID hSrc = UUID.fromString(msgTokens[1]);
                UUID cropId1 = UUID.fromString(msgTokens[2]);
                game.getGhostManager().ghostHarvest(hSrc, cropId1);
                game.onCropHarvested(cropId1);
                break;

            case "grow":
                UUID who = UUID.fromString(msgTokens[1]);
                UUID cropId2 = UUID.fromString(msgTokens[2]);
                float gx = Float.parseFloat(msgTokens[3]);
                float gy = Float.parseFloat(msgTokens[4]);
                float gz = Float.parseFloat(msgTokens[5]);
                String type2 = msgTokens[6];
                ghostManager.ghostGrow(who, cropId2, new Vector3f(gx, gy, gz), type2);
                break;

            case "beeAttack":
                System.out.println("[ProtocolClient] Processing beeAttack message: " + msg);
                if (msgTokens.length < 5) {
                    System.out.println("[ProtocolClient] ERROR: Incomplete beeAttack message: " + msg);
                    return;
                }
                try {
                    UUID target = UUID.fromString(msgTokens[1]);
                    System.out.println("[ProtocolClient] Target client ID: " + target + ", Local client ID: " + id);
                    if (target.equals(id)) {
                        float dx = Float.parseFloat(msgTokens[2]);
                        float dy = Float.parseFloat(msgTokens[3]);
                        float dz = Float.parseFloat(msgTokens[4]);
                        Vector3f impulse = new Vector3f(dx, dy, dz);
                        System.out.println("[ProtocolClient] Applying knockback with impulse: " + impulse);
                        game.applyBeeKnockback(impulse);
                        System.out.println("[ProtocolClient] beeAttack processed successfully");
                    } else {
                        System.out.println("[ProtocolClient] Ignoring beeAttack: Client ID mismatch. Expected: " + id + ", Received: " + target);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("[ProtocolClient] ERROR: Failed to parse beeAttack impulse values: " + msg + ", Error: " + e.getMessage());
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    System.out.println("[ProtocolClient] ERROR: Invalid UUID in beeAttack message: " + msg + ", Error: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    System.out.println("[ProtocolClient] ERROR: Unexpected error processing beeAttack: " + msg + ", Error: " + e.getMessage());
                    e.printStackTrace();
                }
                break;

            default:
                System.out.println("[ProtocolClient] Unrecognized message type: " + msgTokens[0]);
                break;
        }
    }

    /**
     * Sends a join message to the server.
     */
    public void sendJoinMessage() {
        try {
            sendPacket(new String("join," + id.toString()));
            System.out.println("[ProtocolClient] Sent join message: join," + id);
        } catch (IOException e) {
            System.out.println("[ProtocolClient] ERROR: Failed to send join message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to inform the server of this client's avatar creation.
     */
    public void sendCreateMessage(Vector3f pos) {
        try {
            String message = "create," + id.toString() + "," + pos.x + "," + pos.y + "," + pos.z;
            sendPacket(message);
            System.out.println("[ProtocolClient] Sent create message: " + message);
        } catch (IOException e) {
            System.out.println("[ProtocolClient] ERROR: Failed to send create message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to notify the server that this client is disconnecting.
     */
    public void sendByeMessage() {
        try {
            sendPacket("bye," + id.toString());
            System.out.println("[ProtocolClient] Sent bye message: bye," + id);
        } catch (IOException e) {
            System.out.println("[ProtocolClient] ERROR: Failed to send bye message: " + e.getMessage());
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
            String message = "dsfr," + remId.toString() + "," + pos.x + "," + pos.y + "," + pos.z;
            sendPacket(message);
            System.out.println("[ProtocolClient] Sent details-for message: " + message);
        } catch (IOException e) {
            System.out.println("[ProtocolClient] ERROR: Failed to send details-for message: " + e.getMessage());
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
            String message = "move," + id.toString() + "," + position.x + "," + position.y + "," + position.z;
            sendPacket(message);
        } catch (IOException e) {
            System.out.println("[ProtocolClient] ERROR: Failed to send move message: " + e.getMessage());
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
            System.out.println("[ProtocolClient] ERROR: Failed to send skybox message: " + e.getMessage());
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
        } catch (IOException e) {
            System.out.println("[ProtocolClient] ERROR: Failed to send rotate message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends a watering update to the server.
     */
    public void sendWateringMessage(boolean starting) {
        try {
            String message = "water," + id + "," + (starting ? "1" : "0");
            sendPacket(message);
        } catch (IOException e) {
            System.out.println("[ProtocolClient] ERROR: Failed to send watering message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Allows other classes to know this client’s UUID
     */
    public UUID getClientId() {
        return id;
    }

    public void sendPlantMessage(Vector3f pos, String cropId, String type) throws IOException {
        String message = "plant," + id + "," + pos.x + "," + pos.y + "," + pos.z + "," + cropId + "," + type;
        sendPacket(message);
    }

    public void sendHarvestMessage(String cropId) throws IOException {
        String message = "harvest," + id + "," + cropId;
        sendPacket(message);
    }

    /**
     * Tell all clients this crop has matured into carrot/wheat
     */
    public void sendGrowMessage(String cropId, Vector3f pos, String type) throws IOException {
        String msg = String.format("grow,%s,%s,%.3f,%.3f,%.3f,%s",
                id.toString(),
                cropId,
                pos.x, pos.y, pos.z,
                type);
        sendPacket(msg);
    }
    public void sendBeeAttack(UUID target, float dx, float dy, float dz) {
        try {
            String m = String.format("beeAttack,%s,%.3f,%.3f,%.3f",
                                     target.toString(), dx, dy, dz);
            sendPacket(m);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
}
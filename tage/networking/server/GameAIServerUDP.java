package tage.networking.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import a3.NPCcontroller;
import tage.networking.server.GameConnectionServer;
import tage.networking.IGameConnection.ProtocolType;

/**
 * A UDP server that drives NPC behavior and relays "beeAttack" messages
 * back to exactly one client when the BT fires.
 * @author Isabel Santoyo-Garcia
 */
public class GameAIServerUDP extends GameConnectionServer<UUID>
{
    private NPCcontroller npcCtrl;

    /**
     * Constructs and binds the AI server to a local UDP port.
     *
     * @param localPort the UDP port to listen on for AI-related messages
     * @param npc       the NPCcontroller managing AI state and callbacks
     * @throws IOException if the server socket cannot be opened on the port
    */
    public GameAIServerUDP(int localPort, NPCcontroller npc) throws IOException {
        super(localPort, ProtocolType.UDP);
        this.npcCtrl = npc;
        System.out.println("[AI Server] listening on UDP port " + localPort);
    }

    /**
     * Broadcasts a proximity-check request to all connected clients.
     * <p>
     * Formats and sends the message "isnear,&lt;npcId&gt;" so clients
     * can respond if they are near the NPC.
     * </p>
     */   
    public void sendCheckForAvatarNear() {
        try {
            String msg = "isnear," + npcCtrl.getNPC().getId();
            sendPacketToAll(msg);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a knock-back command to exactly one client:
     *   beeAttack,<clientId>,<dx>,<dy>,<dz>
     */
    public void sendBeeAttack(UUID clientID, float dx, float dy, float dz) {
        try {
            String msg = String.format("beeAttack,%s,%.3f,%.3f,%.3f",
                                       clientID.toString(), dx, dy, dz);
            sendPacket(msg, clientID);
            System.out.println("[AI Server] Sending beeAttack to client " + clientID + ": " + msg);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Processes incoming UDP packets from clients.
     *
     * @param o        the message object (expected to be a String)
     * @param senderIP the InetAddress of the sender
     * @param port     the source port of the sender
     */
    @Override
    public void processPacket(Object o, InetAddress senderIP, int port) {
        String message = (String)o;
        String[] tokens = message.split(",");
        switch(tokens[0]) {

            case "isnear":
                UUID srcId = UUID.fromString(tokens[1]);
                npcCtrl.handleNearTiming(srcId);
                break;

            default:
                System.out.println("[AI Server] unknown: " + message);
                break;
        }
    }
}
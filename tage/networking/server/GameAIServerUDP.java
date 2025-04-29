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
 */
public class GameAIServerUDP extends GameConnectionServer<UUID>
{
    private NPCcontroller npcCtrl;

    public GameAIServerUDP(int localPort, NPCcontroller npc) throws IOException {
        super(localPort, ProtocolType.UDP);
        this.npcCtrl = npc;
        System.out.println("[AI Server] listening on UDP port " + localPort);
    }

    /** Called by your NPCcontroller when it's time to check for proximity. */
    public void sendCheckForAvatarNear() {
        try {
            // format: isnear,<myNPCid>
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
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

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
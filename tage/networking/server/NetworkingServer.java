package tage.networking.server;

import java.io.IOException;
import tage.networking.IGameConnection.ProtocolType;

/**
 * Launches and maintains the server-side instance of the game.
 * <p>
 * NetworkingServer class sets up the multiplayer networking backend by
 * creating a {@link GameServerUDP} object that listens on a specified port for client
 * connections and message handling.
 * </p>
 * 
 * @author 
 */
public class NetworkingServer
{
    private GameServerUDP thisUDPServer;
    /**
     * Constructs and initializes the networking server.
     *
     * @param serverPort The port number on which to listen for client connections.
     * @param protocol   The networking protocol to use 
    */
    public NetworkingServer(int serverPort, String protocol)
    { 
        try
        { 
            thisUDPServer = new GameServerUDP(serverPort);
        }
        catch (IOException e)
        { 
            e.printStackTrace();
        } 
    }
    /**
     * Main entry point for starting the server.
     * 
    */
    public static void main(String[] args) { 
        if (args.length < 2) { 
            System.out.println("Usage: java tage.networking.server.NetworkingServer <port> <protocol>");
            return;
        }
        
        System.out.println("Starting Networking Server on port " + args[0] + " using protocol " + args[1]);
        
        NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
        
        System.out.println("Server is now running...");
        
        while (true) { 
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
}

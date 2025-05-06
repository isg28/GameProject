package a3;

import java.util.UUID;
import org.joml.Vector3f;

import tage.GameObject;
import tage.ai.behaviortrees.BTCompositeType;
import tage.ai.behaviortrees.BTSequence;
import tage.ai.behaviortrees.BehaviorTree;
import tage.networking.server.GameAIServerUDP;
import tage.nodeControllers.OrbitAroundController;

/**
 * NPCcontroller manages the behavior of a bee NPC in the multiplayer game.
 * <p>
 * It uses a behavior tree to switch between idle orbiting and
 * actively pursuing the player's avatar when in range.
 * It also integrates with the GameAIServerUDP to receive and send AI events.
 * </p>
 * 
 * @author Isabel Santoyo-Garcia
 */
public class NPCcontroller implements Runnable {
    private MyGame game;
    private NPC npc;
    private BehaviorTree bt;
    private GameAIServerUDP server;
    private volatile UUID lastNearClient;
    private volatile boolean isPursuingAvatar = false;
    private OrbitAroundController orbitController;
    private long lastAttackTime = 0;
    private static final long ATTACK_COOLDOWN_MS = 2000; 
    /**
     * Constructs an NPCcontroller for the given game, creating a new NPC
     * and behavior tree selector root.
     *
     * @param game the main game instance to associate with this controller
     */
    public NPCcontroller(MyGame game) {
        this.game = game;
        this.npc = new NPC();
        this.bt = new BehaviorTree(BTCompositeType.SELECTOR);
    }
    /**
     * Retrieves the underlying NPC model.
     *
     * @return the NPC instance managed by this controller
     */
    public NPC getNPC() {
        return npc;
    }
    /**
     * Gets the UUID of the last client detected near the avatar.
     *
     * @return UUID of the most recent near client, or null if none
     */
    public UUID getLastNearClient() {
        return lastNearClient;
    }
    /**
     * Retrieves the current world position of the player's avatar.
     * Logs a warning and returns (0,0,0) if the avatar GameObject is missing.
     *
     * @return the avatar's position as a Vector3f
     */
    public Vector3f getPlayerPosition() {
        GameObject avatar = game.getAvatar();
        if (avatar == null) {
            System.out.println("[NPCcontroller] Warning: Avatar is null, returning default position");
            return new Vector3f(0, 0, 0);
        }
        return avatar.getWorldLocation();
    }
    /**
     * Handles logic when a client comes within proximity: stores its ID,
     * logs to the game, and prints diagnostics.
     *
     * @param c the UUID of the client now near the avatar
     */
    public void handleNearTiming(UUID c) {
        lastNearClient = c;
        game.logNPCClientId(c);
        System.out.println("[NPCcontroller] Bee pursuing avatar, client ID: " + c);
    }
    /**
     * Assigns the orbit controller used when the NPC should circle the avatar.
     *
     * @param controller the OrbitAroundController to use
     */
    public void setOrbitController(OrbitAroundController controller) {
        this.orbitController = controller;
    }
    /**
     * Retrieves the GameAIServerUDP server instance for AI messaging.
     *
     * @return the AI server in use
     */
    public GameAIServerUDP getServer() {
        return server;
    }
    /**
     * Enables or disables avatar pursuit mode, starting cooldown when disabling.
     *
     * @param pursuing true to start pursuit; false to stop
     */
    public void setPursuingAvatar(boolean pursuing) {
        this.isPursuingAvatar = pursuing;
        if (!pursuing) {
            lastAttackTime = System.currentTimeMillis(); 
        }
    }
    /**
     * Starts the AI server thread and initializes the behavior tree.
     *
     * @param srv the GameAIServerUDP instance to attach to
     */
    public void start(GameAIServerUDP srv) {
        this.server = srv;
        setupBehaviorTree();
        new Thread(this).start();
    }

    private void setupBehaviorTree() {
        bt.insertAtRoot(new BTSequence(30));
        bt.insert(30, new BeeNear(this, npc, 2.0f, false));
        bt.insert(30, new BeeFlyToAvatar(this, npc, 2.0f));
    }

    /**
     * Main loop running AI updates: proximity checks, cooldown logic,
     * pursuit toggles, orbit control, and behavior-tree ticking.
    */
    @Override
    public void run() {
        long last = System.nanoTime();
        while (true) {
            long now = System.nanoTime();
            float dt = (now - last) / 1_000_000f; 
            last = now;

            Vector3f rabbitPos = getPlayerPosition();
            Vector3f beePos = new Vector3f((float)npc.getX(), (float)npc.getY(), (float)npc.getZ());
            float distance = rabbitPos.distance(beePos);
            boolean wasPursuing = isPursuingAvatar;
            long currentTime = System.currentTimeMillis();
            boolean cooldownExpired = (currentTime - lastAttackTime) >= ATTACK_COOLDOWN_MS;

            if (cooldownExpired && distance <= 2.0f) {
                isPursuingAvatar = true;
            } else if (distance > 2.0f) {
                isPursuingAvatar = false;
            }

            if (isPursuingAvatar && !wasPursuing && orbitController != null && orbitController.isEnabled()) {
                orbitController.disable();
            } else if (!isPursuingAvatar && wasPursuing && orbitController != null && !orbitController.isEnabled()) {
                orbitController.enable();
            }

            if (isPursuingAvatar) {
                if (!wasPursuing) {
                    UUID clientId = game.getProtocolClient().getClientId();
                    handleNearTiming(clientId);
                    System.out.println("[NPCcontroller] Initiating pursuit for client: " + clientId);
                }
            } else {
                GameObject bee = game.getBee();
                if (bee != null) {
                    Vector3f beeGameObjectPos = bee.getWorldLocation();
                    npc.setLocation(beeGameObjectPos.x(), beeGameObjectPos.y(), beeGameObjectPos.z());
                } else {
                }
            }

            bt.update(dt);

            try {
                Thread.sleep(25); 
            } catch (InterruptedException e) {
            }
        }
    }
    /**
     * Checks if the NPC is currently set to pursue the avatar.
     *
     * @return true if in pursuit mode
     */
    public boolean isPursuingAvatar() {
        return isPursuingAvatar;
    }
    /**
     * Retrieves the OrbitAroundController used for idle circling.
     *
     * @return the orbit controller
     */
    public OrbitAroundController getOrbitController() {
        return orbitController;
    }
    /**
     * Retrieves the associated game instance.
     *
     * @return the MyGame instance
    */
    public MyGame getGame() {
        return game;
    }


    /**
     * Logs a beeAttack event for diagnostics with client ID.
     *
     * @param clientId the UUID of the target client
     */
    public void logBeeAttack(UUID clientId) {
        System.out.println("[NPCcontroller] Sending beeAttack to client: " + clientId);
    }
}
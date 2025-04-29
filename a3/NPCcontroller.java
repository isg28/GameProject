package a3;

import java.util.UUID;
import org.joml.Vector3f;
import tage.ai.behaviortrees.BTCompositeType;
import tage.ai.behaviortrees.BTSequence;
import tage.ai.behaviortrees.BehaviorTree;
import tage.networking.server.GameAIServerUDP;
import tage.nodeControllers.OrbitAroundController;

public class NPCcontroller implements Runnable {
    private MyGame game;
    private NPC npc;
    private BehaviorTree bt;
    private GameAIServerUDP server;
    private volatile UUID lastNearClient;
    private volatile boolean isPursuingAvatar = false;
    private OrbitAroundController orbitController;

    public NPCcontroller(MyGame game) {
        this.game = game;
        this.npc = new NPC();
        this.bt = new BehaviorTree(BTCompositeType.SELECTOR);
    }

    public NPC getNPC() {
        return npc;
    }

    public UUID getLastNearClient() {
        return lastNearClient;
    }

    public Vector3f getPlayerPosition() {
        System.out.println("[NPCcontroller] Avatar position: " + game.getPlayerPosition());
        return game.getPlayerPosition();
    }

    public void handleNearTiming(UUID c) {
        lastNearClient = c;
    }

    public void setOrbitController(OrbitAroundController controller) {
        this.orbitController = controller;
    }

    /** Start the AI thread */
    public void start(GameAIServerUDP srv) {
        this.server = srv;
        setupBehaviorTree();
        new Thread(this).start();    
    }

    private void setupBehaviorTree() {
        // Sequence id=30 for bee logic
        bt.insertAtRoot(new BTSequence(30));
        bt.insert(30, new BeeNear(this, npc, /*radius=*/2.0f, false));
        bt.insert(30, new BeeFlyToAvatar(this, npc, /*speed=*/2.0f)); // Fly at 2 units/second
        bt.insert(30, new BeeKnockBack(server, this, npc, /*strength=*/5.0f));
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (true) {
            System.out.println("[NPCcontroller] Thread running");
            long now = System.nanoTime();
            float dt = (now - last) / 1_000_000f;
            last = now;
    
            // 1) Check proximity to determine pursuit state
            Vector3f rabbitPos = game.getPlayerPosition();
            Vector3f beePos = new Vector3f((float) npc.getX(), (float) npc.getY(), (float) npc.getZ());
            boolean wasPursuing = isPursuingAvatar;
            isPursuingAvatar = rabbitPos.distance(beePos) <= 2.0f;
    
            // 2) Toggle orbit controller
            if (isPursuingAvatar && orbitController != null && orbitController.isEnabled()) {
                orbitController.disable();
            } else if (!isPursuingAvatar && orbitController != null && !orbitController.isEnabled()) {
                orbitController.enable();
            }
    
            // 3) Update NPC position based on state
            if (isPursuingAvatar) {
                // When pursuing, the behavior tree (BeeFlyToAvatar) updates NPC position
                if (isPursuingAvatar) {
                    this.handleNearTiming(game.getProtocolClient().getClientId());
                }
            } else {
                // When not pursuing, sync NPC position with bee GameObject (set by orbit controller)
                Vector3f beeGameObjectPos = game.getBee().getWorldLocation(); // Assuming you add getBee() to MyGame
                npc.setLocation(beeGameObjectPos.x(), beeGameObjectPos.y(), beeGameObjectPos.z());
            }
    
            // 4) Tick the behavior tree
            bt.update(dt);
    
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
            }
        }
    }
     /** Allow others to ask are we currently pursuing the avatar */
    public boolean isPursuingAvatar() {
            return isPursuingAvatar;
    }
    
}
package a3;

import java.util.UUID;
import org.joml.Vector3f;

import tage.GameObject;
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
    private long lastAttackTime = 0;
    private static final long ATTACK_COOLDOWN_MS = 2000; // 2 seconds cooldown

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
        GameObject avatar = game.getAvatar();
        if (avatar == null) {
            System.out.println("[NPCcontroller] Warning: Avatar is null, returning default position");
            return new Vector3f(0, 0, 0);
        }
        return avatar.getWorldLocation();
    }

    public void handleNearTiming(UUID c) {
        lastNearClient = c;
        game.logNPCClientId(c);
        System.out.println("[NPCcontroller] Bee pursuing avatar, client ID: " + c);
    }

    public void setOrbitController(OrbitAroundController controller) {
        this.orbitController = controller;
    }

    public GameAIServerUDP getServer() {
        return server;
    }

    public void setPursuingAvatar(boolean pursuing) {
        this.isPursuingAvatar = pursuing;
        if (!pursuing) {
            lastAttackTime = System.currentTimeMillis(); // Start cooldown
        }
    }

    public void start(GameAIServerUDP srv) {
        this.server = srv;
        setupBehaviorTree();
        new Thread(this).start();
    }

    private void setupBehaviorTree() {
        bt.insertAtRoot(new BTSequence(30));
        bt.insert(30, new BeeNear(this, npc, /*radius=*/2.0f, false));
        bt.insert(30, new BeeFlyToAvatar(this, npc, /*speed=*/2.0f));
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (true) {
            long now = System.nanoTime();
            float dt = (now - last) / 1_000_000f; // Convert to milliseconds
            last = now;

            // 1) Check proximity to determine pursuit state
            Vector3f rabbitPos = getPlayerPosition();
            Vector3f beePos = new Vector3f((float)npc.getX(), (float)npc.getY(), (float)npc.getZ());
            float distance = rabbitPos.distance(beePos);
            boolean wasPursuing = isPursuingAvatar;
            long currentTime = System.currentTimeMillis();
            boolean cooldownExpired = (currentTime - lastAttackTime) >= ATTACK_COOLDOWN_MS;

            // Only pursue if cooldown has expired and avatar is within range
            if (cooldownExpired && distance <= 2.0f) {
                isPursuingAvatar = true;
            } else if (distance > 2.0f) {
                isPursuingAvatar = false;
            }

            // Log pursuit state
            if (isPursuingAvatar) {
                System.out.println("[NPCcontroller] Bee pursuing avatar, distance: " + distance + ", client ID: " + lastNearClient);
            } else {
                System.out.println("[NPCcontroller] Bee not pursuing, distance: " + distance + ", cooldown remaining: " + 
                    Math.max(0, ATTACK_COOLDOWN_MS - (currentTime - lastAttackTime)) + "ms");
            }

            // 2) Toggle orbit controller
            if (isPursuingAvatar && !wasPursuing && orbitController != null && orbitController.isEnabled()) {
                System.out.println("[NPCcontroller] Disabling orbit controller");
                orbitController.disable();
            } else if (!isPursuingAvatar && wasPursuing && orbitController != null && !orbitController.isEnabled()) {
                System.out.println("[NPCcontroller] Enabling orbit controller");
                orbitController.enable();
            }

            // 3) Update NPC position based state
            if (isPursuingAvatar) {
                // Behavior tree (BeeFlyToAvatar) updates NPC position
                if (!wasPursuing) {
                    UUID clientId = game.getProtocolClient().getClientId();
                    handleNearTiming(clientId);
                    System.out.println("[NPCcontroller] Initiating pursuit for client: " + clientId);
                }
            } else {
                // Sync NPC position with bee GameObject (set by orbit controller)
                GameObject bee = game.getBee();
                if (bee != null) {
                    Vector3f beeGameObjectPos = bee.getWorldLocation();
                    npc.setLocation(beeGameObjectPos.x(), beeGameObjectPos.y(), beeGameObjectPos.z());
                } else {
                    System.out.println("[NPCcontroller] Warning: Bee GameObject is null");
                }
            }

            // 4) Tick the behavior tree
            bt.update(dt);

            try {
                Thread.sleep(25); // ~40 FPS
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    public boolean isPursuingAvatar() {
        return isPursuingAvatar;
    }

    public OrbitAroundController getOrbitController() {
        return orbitController;
    }
    public MyGame getGame() {
        return game;
    }


    // Log beeAttack for diagnostics (called by BeeFlyToAvatar)
    public void logBeeAttack(UUID clientId) {
        System.out.println("[NPCcontroller] Sending beeAttack to client: " + clientId);
    }
}
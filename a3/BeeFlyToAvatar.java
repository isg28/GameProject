package a3;

import java.io.IOException;
import java.util.UUID;

import org.joml.Vector3f;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;
import tage.nodeControllers.OrbitAroundController;

public class BeeFlyToAvatar extends BTAction {
    private NPCcontroller npcCtrl;
    private NPC npc;
    private float speed;
    private static final float ATTACK_DISTANCE = 0.08f; // Based on log distance 0.07356197

    public BeeFlyToAvatar(NPCcontroller controller, NPC npc, float speed) {
        super();
        this.npcCtrl = controller;
        this.npc = npc;
        this.speed = speed;
    }

    @Override
    protected void onInitialize() {
        // No initialization needed
    }

    @Override
    protected BTStatus update(float elapsedTime) {
        if (!npcCtrl.isPursuingAvatar()) {
            System.out.println("[BeeFlyToAvatar] Not pursuing, skipping update");
            return BTStatus.BH_SUCCESS;
        }

        Vector3f avatarPos = npcCtrl.getPlayerPosition();
        Vector3f beePos    = new Vector3f((float)npc.getX(),
                                        (float)npc.getY(),
                                        (float)npc.getZ());
        float distance     = avatarPos.distance(beePos);

        if (distance <= ATTACK_DISTANCE) {
            UUID myClientId = npcCtrl
                .getGame()                    
                .getProtocolClient()          
                .getClientId();

            String msg = String.format(
                "beeAttack,%s,%.3f,%.3f,%.3f",
                myClientId.toString(),
                0.0f, 0.0f, 0.0f
            );
            try {
                npcCtrl.getGame()
                    .getProtocolClient()
                    .sendPacket(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("[BeeFlyToAvatar] Sent beeAttack through GameServerUDP: " + msg);

            npcCtrl.setPursuingAvatar(false);
            OrbitAroundController oc = npcCtrl.getOrbitController();
            if (oc != null && !oc.isEnabled()) {
                System.out.println("[BeeFlyToAvatar] Re-enabling orbit controller after attack");
                oc.enable();
            }
            return BTStatus.BH_SUCCESS;
            // --- END attack branch replacement ---
        }

        Vector3f direction = avatarPos.sub(beePos).normalize();
        Vector3f movement  = direction.mul(speed * elapsedTime / 1000f);
        npc.setLocation(
            npc.getX() + movement.x(),
            npc.getY() + movement.y(),
            npc.getZ() + movement.z()
        );

        System.out.println("[BeeFlyToAvatar] Moving bee towards avatar, distance: " + distance);
        return BTStatus.BH_RUNNING;
    }



}
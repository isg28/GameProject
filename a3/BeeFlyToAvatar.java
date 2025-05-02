package a3;

import java.util.UUID;

import org.joml.Vector3f;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;

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
            return BTStatus.BH_SUCCESS; // Exit early if not pursuing
        }

        Vector3f avatarPos = npcCtrl.getPlayerPosition();
        Vector3f beePos = new Vector3f((float)npc.getX(), (float)npc.getY(), (float)npc.getZ());
        float distance = avatarPos.distance(beePos);

        if (distance <= ATTACK_DISTANCE) {
            UUID myClientId = npcCtrl.getGame()
                                    .getProtocolClient()
                                    .getClientId();
            System.out.println("[BeeFlyToAvatar] Sending beeAttack to client " + myClientId);
            npcCtrl.getServer().sendBeeAttack(myClientId, 0f, 0f, 0f);
            npcCtrl.setPursuingAvatar(false);
            if (npcCtrl.getOrbitController() != null 
            && !npcCtrl.getOrbitController().isEnabled()) {
                System.out.println("[BeeFlyToAvatar] Re-enabling orbit controller after attack");
                npcCtrl.getOrbitController().enable();
            }
            return BTStatus.BH_SUCCESS;

        }

        // Move towards avatar
        Vector3f direction = avatarPos.sub(beePos).normalize();
        Vector3f movement = direction.mul(speed * elapsedTime / 1000f); // Convert ms to seconds
        npc.setLocation(
            npc.getX() + movement.x(),
            npc.getY() + movement.y(),
            npc.getZ() + movement.z()
        );

        System.out.println("[BeeFlyToAvatar] Moving bee towards avatar, distance: " + distance);
        return BTStatus.BH_RUNNING;
    }
}
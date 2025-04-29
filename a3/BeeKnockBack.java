// a3/BeeKnockBack.java
package a3;

import java.util.UUID;
import org.joml.Vector3f;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;
import tage.networking.server.GameAIServerUDP;

public class BeeKnockBack extends BTAction {
    private GameAIServerUDP server;
    private NPCcontroller   controller;
    private NPC             bee;
    private float           strength;

    public BeeKnockBack(GameAIServerUDP s, NPCcontroller c, NPC b, float strength) {
        super();
        this.server     = s;
        this.controller = c;
        this.bee        = b;
        this.strength   = strength;
    }

    @Override
    protected BTStatus update(float elapsedTime) {
        // get the last client who reported near
        UUID target = controller.getLastNearClient();
        if (target != null) {
            Vector3f rabbitPos = controller.getPlayerPosition();
            Vector3f beePos     = new Vector3f(
                                     (float)bee.getX(),
                                     (float)bee.getY(),
                                     (float)bee.getZ());
            Vector3f dir       = rabbitPos.sub(beePos, new Vector3f())
                                          .normalize()
                                          .mul(strength);
            server.sendBeeAttack(target, dir.x, dir.y, dir.z);

             controller.handleNearTiming(null);        
        }
        return BTStatus.BH_SUCCESS;
    }
}
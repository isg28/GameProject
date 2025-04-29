package a3;

import org.joml.Vector3f;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;

public class BeeFlyToAvatar extends BTAction {
    private NPCcontroller controller;
    private NPC bee;
    private float speed;

    public BeeFlyToAvatar(NPCcontroller c, NPC b, float speed) {
        super();
        this.controller = c;
        this.bee = b;
        this.speed = speed;
    }

    @Override
    protected BTStatus update(float elapsedTime) {
        Vector3f rabbitPos = controller.getPlayerPosition();
        Vector3f beePos = new Vector3f((float) bee.getX(), (float) bee.getY(), (float) bee.getZ());
        Vector3f direction = rabbitPos.sub(beePos, new Vector3f()).normalize();
        Vector3f movement = direction.mul(speed * elapsedTime / 1000.0f);
        beePos.add(movement);
        bee.setLocation(beePos.x(), beePos.y(), beePos.z());
        float distance = rabbitPos.distance(beePos);
        System.out.println("[BeeFlyToAvatar] Moving to avatar, new pos: " + beePos + ", distance: " + distance);
        if (distance < 0.5f) {
            return BTStatus.BH_SUCCESS;
        }
        return BTStatus.BH_RUNNING;
    }
}
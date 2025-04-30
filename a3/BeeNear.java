package a3;

import org.joml.Vector3f;

import tage.ai.behaviortrees.BTCondition;

public class BeeNear extends BTCondition {
    private NPCcontroller controller;
    private NPC bee;
    private float radius;

    public BeeNear(NPCcontroller c, NPC b, float radius, boolean toNegate) {
        super(toNegate);
        this.controller = c;
        this.bee = b;
        this.radius = radius;
    }

    @Override
    protected boolean check() {
        Vector3f rabbitPos = controller.getPlayerPosition();
        Vector3f beePos = new Vector3f((float)bee.getX(), (float)bee.getY(), (float)bee.getZ());
        float distance = rabbitPos.distance(beePos);
        boolean isNear = distance <= radius;
        return isNear;
    }
}
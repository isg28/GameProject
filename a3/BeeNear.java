package a3;

import org.joml.Vector3f;

import tage.ai.behaviortrees.BTCondition;

/**
 * BeeNear is a behavior-tree condition that checks if the bee NPC is within a
 * specified radius of the player's avatar (rabbit).
 * <p>
 * It evaluates to true when the distance between the bee and the player
 * is less than or equal to the given threshold. 
 * @author Isabel Santoyo-Garcia
 */
public class BeeNear extends BTCondition {
    private NPCcontroller controller;
    private NPC bee;
    private float radius;
    /**
     * Constructs a BeeNear condition.
     *
     * @param c the NPCcontroller managing AI behaviors and state
     * @param b the NPC instance representing the bee
     * @param radius the maximum distance for the condition to evaluate true
     * @param toNegate if true, the boolean result will be inverted
     */
    public BeeNear(NPCcontroller c, NPC b, float radius, boolean toNegate) {
        super(toNegate);
        this.controller = c;
        this.bee = b;
        this.radius = radius;
    }
    /**
     * Checks if the bee is within the specified radius of the player's avatar.
     *
     * @return true if the bee is near (or far when negated);
     *         false otherwise
     */
    @Override
    protected boolean check() {
        Vector3f rabbitPos = controller.getPlayerPosition();
        Vector3f beePos = new Vector3f((float)bee.getX(), (float)bee.getY(), (float)bee.getZ());
        float distance = rabbitPos.distance(beePos);
        boolean isNear = distance <= radius;
        return isNear;
    }
}
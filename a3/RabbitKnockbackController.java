package a3;

import tage.NodeController;
import tage.GameObject;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import a3.MyGame;

/**
 * RabbitKnockbackController applies a temporary knockback force to the rabbit GameObject,
 * pushing it in a specified direction for a set duration and automatically adjusting
 * its height to conform to the terrain.
 * @author Isabel Santoyo-Garcia
 */
public class RabbitKnockbackController extends NodeController {
    private GameObject rabbit;
    private Vector3f direction = new Vector3f();
    private float     speed, duration, elapsed;
    private boolean   active;
    /**
     * Constructs a new RabbitKnockbackController for the given rabbit GameObject.
     * Automatically registers the rabbit as the control target.
     *
     * @param rabbit the GameObject to apply knockback to
     */
    public RabbitKnockbackController(GameObject rabbit) {
        super();
        this.rabbit = rabbit;
        addTarget(rabbit);
        this.active = false;
    }

    /**
     * Kick off a knockback.
     * @param dir       normalized direction to push
     * @param strength  world units per second
     * @param secs      how long the push lasts
     */
    public void knock(Vector3f dir, float strength, float secs) {
        this.direction.set(dir).normalize();
        this.speed     = strength;
        this.duration  = secs;
        this.elapsed   = 0f;
        this.active    = true;

        float yaw = (float)Math.atan2(-direction.x, direction.z);
        rabbit.setLocalRotation(new Matrix4f().rotationY(yaw));

        enable();
    }

    /**
     * Called automatically each frame on every target.
     */
    @Override
    public void apply(GameObject t) {
        if (!active) return;

        float dt = getElapsedTime() / 1000f;
        elapsed += dt;

        Vector3f pos   = rabbit.getWorldLocation();
        Vector3f delta = new Vector3f(direction).mul(speed * dt);
        Vector3f newPos= pos.add(delta);

        float h = MyGame.getInstance()
                         .getTerr()
                         .getHeight(newPos.x(), newPos.z());
        rabbit.setLocalLocation(new Vector3f(newPos.x(), h + 0.1f, newPos.z()));

        if (elapsed >= duration) {
            active = false;
            disable();
        }
    }
}

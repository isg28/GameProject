package tage.nodeControllers;

import tage.*;
import org.joml.*;
import java.lang.Math;
import java.util.HashMap;
import java.util.Map;

/**
 * BouncingController applies a bouncing animation to objects.
 * <p>
 * Objects controlled by this class oscillate vertically (Y-axis)
 *  floating/bouncing effect.
 * <p>

 * @author Isabel Santoyo-Garcia
 */
public class BouncingController extends NodeController {
    private float bounceHeight = 0.15f; 
    private float cycleTime = 2000.0f;  
    private boolean isActive = false;
    
    private Map<GameObject, Vector3f> initialPositions = new HashMap<>();
    private Map<GameObject, Float> objectTimes = new HashMap<>();  
    
    private long lastUpdateTime;

    /**
     * Constructs a BouncingController with a specified bounce cycle duration.
     *
     * @param ctime The duration of a full bounce cycle.
     */
    public BouncingController(float ctime) {
        super();
        cycleTime = ctime;
        lastUpdateTime = System.nanoTime();  
    }

    /**
     * Activates bouncing for a specific GameObject.
     * If the object hasn't been tracked before, its initial position is stored.
     *
     * @param go The GameObject to apply the bouncing effect to.
     */
    public void activate(GameObject go) {
        isActive = true;
        if (!initialPositions.containsKey(go)) {
            initialPositions.put(go, new Vector3f(go.getWorldLocation()));
            objectTimes.put(go, 0f);  
        }
    }

    /**
     * Deactivates bouncing for the object, clearing stored positions and timers.
    */
    public void deactivate() {
        isActive = false;
        initialPositions.clear();
        objectTimes.clear();
    }

    /**
     * Applies the bouncing effect to a given GameObject.
     * <p>
     *
     * @param go The GameObject to update with the bouncing effect.
     */
    @Override
    public void apply(GameObject go) {
        if (!isActive || !initialPositions.containsKey(go)) {
            return;
        }

        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastUpdateTime) / 1_000_000f;  
        lastUpdateTime = currentTime;

        float objectTime = objectTimes.get(go) + deltaTime;
        objectTimes.put(go, objectTime);

        float bounceOffset = (float) Math.sin(objectTime * Math.PI * 2 / cycleTime) * bounceHeight;

        Vector3f originalPosition = initialPositions.get(go);
        Vector3f newPosition = new Vector3f(originalPosition.x, originalPosition.y + bounceOffset, originalPosition.z);
        go.setLocalTranslation(new Matrix4f().translation(newPosition));

    }
}

package tage.nodeControllers;
import tage.*;
import org.joml.*;

/**
 * StretchController dynamically scales a game object (torus, sphere, and cube) along the X and Z axes
 * while keeping its height relatively small to create a stretching effect.
 * 
 * 
 * @author Isabel Santoyo-Garcia
 */

public class StretchController extends NodeController {
    private float scaleRate = .0003f;
    private float cycleTime = 2000.0f;
    private float totalTime = 0.0f;
    private float direction = 1.0f;
    private Matrix4f curScale, newScale;
    private boolean isActive = false;
    private GameObject sphere; 

    /**
     * Constructs a StretchController with a specified cycle time and target object.
     *
     * @param ctime The duration of a full stretch cycle.
     */

    public StretchController(float ctime) {
        super();
        cycleTime = ctime;
        newScale = new Matrix4f();
    }

    /**
     * Activates the stretching effect, allowing the object to dynamically scale.
     */
    public void activate() {
        isActive = true;
    }

    /**
     * Deactivates the stretching effect, stopping any scaling changes.
     */
    public void deactivate() {
        isActive = false;
    }

    /**
     * Applies the stretching effect to a given GameObject.
     * The object expands and contracts in the X and Z axes over time,
     * while maintaining a reduced height along the Y-axis.
     *
     * @param go The GameObject to apply the stretching effect to.
     */
    @Override
    public void apply(GameObject go) {
        if (!isActive) return;
    
        float elapsedTime = super.getElapsedTime();
        totalTime += elapsedTime / 1000.0f;
    
        if (totalTime > cycleTime) {
            direction = -direction;
            totalTime = 0.0f;
        }
    
        Matrix4f curScale = go.getLocalScale();
        float scaleAmt = 1.0f + direction * scaleRate * elapsedTime;
        float newYScale = curScale.m11() * 0.1f; 
    
        Matrix4f newScale = new Matrix4f().scaling(curScale.m00() * scaleAmt, newYScale, curScale.m22() * scaleAmt);
        go.setLocalScale(newScale);
    }
}
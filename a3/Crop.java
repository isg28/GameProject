package a3;

import java.util.UUID;

import org.joml.Matrix4f;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;

/**
* Crop represents a planted seed in the game, tracking its growth cycle,
* watering state, and final harvested state. It manages timing for maturation,
* swapping obj upon readiness, and notifying growth events.
* 
* @author Isabel Santoyo-Garcia
*/
public class Crop {
    private String type;
    private long growTimeMillis;
    private long startTime;
    private boolean ready;
    private boolean hasGrown;
    private GameObject plantedObject;
    private ObjShape targetShape; 
    private TextureImage targetTexture; 
    private boolean harvested = false;
    private boolean wateredOnce = false;
    private UUID id;  
    private boolean readyNotified = false;

    /**
     * Constructs a new Crop with specified parameters.
     *
     * @param type crop type string 
     * @param growTimeSeconds time in seconds until maturity
     * @param targetShape shape to display at maturity
     * @param targetTexture texture to display at maturity
     */
    public Crop(String type, double growTimeSeconds, ObjShape targetShape, TextureImage targetTexture) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.growTimeMillis = (long)(growTimeSeconds * 1000);
        this.startTime = System.currentTimeMillis();
        this.targetShape = targetShape;
        this.targetTexture= targetTexture;
    }

    /**
     * Updates the crop's internal timer and swaps in the matured crop obj & texture
     * when the grow time has elapsed.
     *
     */
    public void update() {
        if (!ready) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - startTime) >= growTimeMillis) {
                ready = true;
                hasGrown = true;
                System.out.println("Crop is now ready to harvest!");
                if (plantedObject != null && targetShape != null && targetTexture != null) {
                    plantedObject.setShape(targetShape);
                    plantedObject.setTextureImage(targetTexture);
                    plantedObject.setLocalScale(
                        new Matrix4f().scaling(type.equals("Carrot") ? 0.3f : 0.2f)
                    );
                }
            }
        }
    }

    /**
     * Waters the crop once, reducing its remaining grow time by the specified amount.
     * Only effective a single time per crop.
     *
     * @param seconds number of seconds to subtract from remaining grow time
     */
    public void water(double seconds) {
        if (wateredOnce || ready) return;
        wateredOnce = true;
        startTime -= (long)(seconds * 1000);
        System.out.println("Crop watered: knocking off " + seconds + "s");
    }
    /**
     * Checks if the crop has reached maturity and is ready for harvest.
     *
     * @return true if ready to harvest
     */
    public boolean isReadyToHarvest() {
        return ready;
    }
    /**
     * Checks if the crop has been updated to mature state.
     *
     * @return true if the mature appearance has been applied
     */
    public boolean hasGrown() {
        return hasGrown;
    }
    /**
     * Sets the internal grown state of the crop.
     *
     * @param value true to mark the crop as grown
     */
    public void setHasGrown(boolean value) {
        hasGrown = value;
    }
    /**
     * Associates the visual GameObject with this crop instance.
     *
     * @param obj the GameObject representing the planted crop
     */
    public void setPlantedObject(GameObject obj) {
        plantedObject = obj;
    }
    /**
     * Retrieves the GameObject tied to this crop.
     *
     * @return the planted GameObject, or null if not set
     */
    public GameObject getPlantedObject() {
        return plantedObject;
    }
    /**
     * Gets the crop's type string.
     *
     * @return the type of this crop
     */
    public String getType() {
        return type;
    }
    /**
     * Checks if this crop has already been harvested.
     *
     * @return true if harvested
     */
    
    public boolean isHarvested() {
        return harvested;
    }
    /**
     * Marks the crop as harvested, preventing further harvest actions.
     */
    public void markHarvested() {
        harvested = true;
    }
    /**
     * Returns this crop's unique identifier.
     *
     * @return the UUID of this crop
     */
    public UUID getId() { return id; }
    /**
     * Sets the UUID for this crop (used when syncing remote crops).
     *
     * @param id the UUID to assign
     */
    public void setId(UUID id) { this.id = id; }
    /**
     * Forces the crop to mature immediately, swapping its obj model & texture.
     * Does nothing if already mature.
     */
     public void forceGrowNow() {
        if (!ready) {
            ready = true;
            hasGrown = true;
            System.out.println("Crop forced to grown state!");
            if (plantedObject != null && targetShape != null && targetTexture != null) {
                plantedObject.setShape(targetShape);
                plantedObject.setTextureImage(targetTexture);
                plantedObject.setLocalScale(
                    new Matrix4f().scaling(type.equals("Carrot") ? 0.3f : 0.2f)
                );
            }
        }
    }
   /**
     * Advances growth and returns true if the crop just transitioned to ready state
     * and has not yet been notified.
     *
     * @return true if newly ready and not previously notified
     */
    public boolean updateAndCheckReady() {
        boolean wasReady = ready;
        update();                 
        if (!wasReady && ready && !readyNotified) {
            readyNotified = true;
            return true;
        }
        return false;
    }

}
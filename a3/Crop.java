package a3;

import org.joml.Matrix4f;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;

public class Crop {
    private String type;
    private long growTimeMillis;
    private long startTime;
    private boolean ready;
    private boolean hasGrown;
    private GameObject plantedObject;
    private ObjShape targetShape; // Shape when ready (e.g., wheatS, carrotS)
    private TextureImage targetTexture; // Texture when ready (e.g., wheattx, carrottx)
    private boolean harvested = false;


    public Crop(String type, double growTimeSeconds, ObjShape targetShape, TextureImage targetTexture) {
        this.type = type;
        this.growTimeMillis = (long)(growTimeSeconds * 1000);
        this.startTime = System.currentTimeMillis();
        this.ready = false;
        this.hasGrown = false;
        this.targetShape = targetShape;
        this.targetTexture = targetTexture;
    }

    public void update() {
        if (!ready) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - startTime) >= growTimeMillis) {
                ready = true;
                hasGrown = true;
                System.out.println("Crop is now ready to harvest!");
                // Update plantedObject to final shape and texture
                if (plantedObject != null && targetShape != null && targetTexture != null) {
                    plantedObject.setShape(targetShape);
                    plantedObject.setTextureImage(targetTexture);
                    // Adjust scale for final form (e.g., match wheat/carrot scaling)
                    plantedObject.setLocalScale(new Matrix4f().scaling(type.equals("Carrot") ? 0.3f : 0.2f));
                }
            }
        }
    }

    public void water(double seconds) {
        if (!ready && !hasGrown) {
            growTimeMillis -= (long)(seconds * 1000);
            if (growTimeMillis <= 0) {
                growTimeMillis = 0;
                ready = true;
                hasGrown = true;
                System.out.println("Crop watered and now ready to harvest!");
                // Update plantedObject to final shape and texture
                if (plantedObject != null && targetShape != null && targetTexture != null) {
                    plantedObject.setShape(targetShape);
                    plantedObject.setTextureImage(targetTexture);
                    plantedObject.setLocalScale(new Matrix4f().scaling(type.equals("Carrot") ? 0.3f : 0.2f));
                }
            }
        }
    }

    public boolean isReadyToHarvest() {
        return ready;
    }

    public boolean hasGrown() {
        return hasGrown;
    }

    public void setHasGrown(boolean value) {
        hasGrown = value;
    }

    public void setPlantedObject(GameObject obj) {
        plantedObject = obj;
    }

    public GameObject getPlantedObject() {
        return plantedObject;
    }

    public String getType() {
        return type;
    }
    
    public boolean isHarvested() {
        return harvested;
    }
    
    public void markHarvested() {
        harvested = true;
    }
    
}
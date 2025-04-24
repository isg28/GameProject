package a3;

import tage.GameObject;

public class Crop {
    private String type;
    private long growTimeMillis;      
    private long startTime;           
    private boolean ready;            
    private boolean hasGrown;         
    private GameObject plantedObject; 

    public Crop(String type, double growTimeSeconds) {
        this.type = type;
        this.growTimeMillis = (long)(growTimeSeconds * 1000); 
        this.startTime = System.currentTimeMillis();
        this.ready = false;
        this.hasGrown = false;
    }

    public void update() {
        if (!ready) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - startTime) >= growTimeMillis) {
                ready = true;
                System.out.println("Crop is now ready to harvest!");
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
}

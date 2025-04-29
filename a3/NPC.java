package a3;

import java.util.UUID;

public class NPC {
    private UUID id;
    private double locationX, locationY, locationZ;
    private double dir = 0.1;

    public NPC() {
        this.id = UUID.randomUUID();
        this.locationX = 0;
        this.locationY = 0;
        this.locationZ = 0;
    }

    public UUID getId() {
        return id;
    }

    public double getX() {
        return locationX;
    }

    public double getY() {
        return locationY;
    }

    public double getZ() {
        return locationZ;
    }

    public void setLocation(double x, double y, double z) {
        this.locationX = x;
        this.locationY = y;
        this.locationZ = z;
    }

    /** Call once at startup to scatter them. */
    public void randomizeLocation(int seedX, int seedZ) {
        this.locationX = seedX / 4.0 - 5.0;
        this.locationY = 0;
        this.locationZ = seedZ / 4.0 - 5.0;
    }

    public void updateLocation() {

    }
}
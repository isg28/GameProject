package a3;

import java.util.UUID;

/**
 * NPC represents a non-player character with a unique identifier and 3D position.
 * <p>
 * NPC instances track their world coordinates and can be randomized or explicitly set.
 * Intended for use with AI controllers that update movement and behavior per frame.
 * </p>
 * 
 * @author Isabel Santoyo-Garcia
 */
public class NPC {
    private UUID id;
    private double locationX, locationY, locationZ;
    private double dir = 0.1;

    /**
     * Constructs a new NPC with a random UUID and origin location (0,0,0).
    */
    public NPC() {
        this.id = UUID.randomUUID();
        this.locationX = 0;
        this.locationY = 0;
        this.locationZ = 0;
    }

    /**
     * Retrieves the unique identifier of this NPC.
     *
     * @return the UUID assigned to this NPC
     */
    public UUID getId() {
        return id;
    }
    /**
     * Gets the X-coordinate of the NPC.
     *
     * @return current X position
     */
    public double getX() {
        return locationX;
    }
    /**
     * Gets the Y-coordinate of the NPC.
     *
     * @return current Y position
     */
    public double getY() {
        return locationY;
    }
    /**
     * Gets the Z-coordinate of the NPC.
     *
     * @return current Z position
     */
    public double getZ() {
        return locationZ;
    }
    /**
     * Sets the NPC's location in world space.
     *
     * @param x new X-coordinate
     * @param y new Y-coordinate
     * @param z new Z-coordinate
     */
    public void setLocation(double x, double y, double z) {
        this.locationX = x;
        this.locationY = y;
        this.locationZ = z;
    }

    /**
     * Randomizes the NPC’s X and Z coordinates based on seed values,
     * scattering NPCs across the map. Y is reset to ground level (0).
     *
     * @param seedX integer seed for X positioning
     * @param seedZ integer seed for Z positioning
     */    
    public void randomizeLocation(int seedX, int seedZ) {
        this.locationX = seedX / 4.0 - 5.0;
        this.locationY = 0;
        this.locationZ = seedZ / 4.0 - 5.0;
    }
    /**
    * Placeholder for per-frame location updates
    */
    public void updateLocation() {

    }
}
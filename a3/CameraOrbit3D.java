package a3;
import java.lang.Math;

import org.joml.Vector3f;

import net.java.games.input.Event;
import tage.Camera;
import tage.Engine;
import tage.GameObject;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;

/**
 * CameraOrbit3D provides an orbiting camera system that allows the player 
 * to rotate, tilt, and zoom around the dolphin.
 * <p>
 * The camera uses azimuth, elevation, & radius to position itself relative to the dolphin and updates dynamically based on user input.
 *
 * @author Isabel Santoyo-Garcia
 * 
 **/

public class CameraOrbit3D {
    private Engine engine;
    private Camera camera;
    private GameObject avatar; 
    private float cameraAzimuth; 
    private float cameraElevation; 
    private float cameraRadius; 

    /**
     * Constructs a CameraOrbit3D instance, initializing the camera's position 
     * and setting default values for azimuth, elevation, and distance.
     *
     * @param cam The camera to be controlled.
     * @param av The target, dolphin, the camera orbits around.
     * @param e The game engine instance.
     */
    public CameraOrbit3D(Camera cam, GameObject av, Engine e) {
        engine = e;
        camera = cam;
        avatar = av;
        cameraAzimuth = 0.0f; 
        cameraElevation = 20.0f;
        cameraRadius = 2.0f; 
        updateCameraPosition();
    }

    /**
     * Updates the camera position based on the current azimuth, elevation, and distance. 
     * And uses it to properly position the camera relative to the dolphin.
     */
    public void updateCameraPosition() {
        double theta = Math.toRadians(cameraAzimuth); 
        double phi = Math.toRadians(cameraElevation); 

        float x = cameraRadius * (float) (Math.cos(phi) * Math.sin(theta));
        float y = cameraRadius * (float) (Math.sin(phi));
        float z = cameraRadius * (float) (Math.cos(phi) * Math.cos(theta));

        Vector3f avatarPos = avatar.getWorldLocation();
        camera.setLocation(new Vector3f(x, y, z).add(avatarPos));
        camera.lookAt(avatar);
    }

    /**
     * Rotates the camera left or right around the avatar (azimuth).
     *
     * @param delta The amount of rotation in degrees.
     *              - Positive values rotate right.
     *              - Negative values rotate left.
     */
    public void orbitAzimuth(float delta) {
        cameraAzimuth += delta;
        cameraAzimuth = cameraAzimuth % 360; 
        updateCameraPosition();
    }

    /**
     * Adjusts the camera's elevation (tilting up or down).
     *
     * @param delta The amount of tilt in degrees.
     *              - Positive values tilt up.
     *              - Negative values tilt down.
     * <p>
     * The elevation is restricted to prevent excessive tilting.
     */
    public void orbitElevation(float delta) {
        cameraElevation += delta;
        cameraElevation = Math.min(Math.max(cameraElevation, 10.0f), 80.0f); 
        updateCameraPosition();
    }

    /**
     * Adjusts the camera's distance from the avatar (zoom in/out).
     *
     * @param delta The amount to adjust the distance.
     *              - Positive values zoom out.
     *              - Negative values zoom in.
     */
    public void zoom(float delta) {
        cameraRadius += delta;
        cameraRadius = Math.min(Math.max(cameraRadius, 2.0f), 10.0f); 
        updateCameraPosition();
    }

}

package a3;

import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

/**
 * OrbitAzimuthAction controls the horizontal orbit rotation (azimuth)
 * of the camera around the avatar.
 * <p>
 * This action allows the player to rotate the camera left or right around
 * the avatar using the keyboard input. 
 *
 * Usage:
 * <ul>
 *  <li> Positive delta, Rotates the camera to the right.</li>
 *  <li> Negative delta, Rotates the camera to the left. </li>
 * </ul>
 *
 * @author Isabel Santoyo-Garcia
 */

public class OrbitAzimuthAction extends AbstractInputAction {
    private CameraOrbit3D orbitController;
    private float delta;

    /**
     * Constructs an OrbitAzimuthAction instance.
     *
     * @param orbitController The CameraOrbit3D instance that manages camera movement.
     * @param delta The amount of rotation to apply per input event
     */
    public OrbitAzimuthAction(CameraOrbit3D orbitController, float delta) {
        this.orbitController = orbitController;
        this.delta = delta;
    }

    /**
     * Executes the orbit azimuth rotation when an input event occurs.
     *
     * @param time  The time elapsed since the last action.
     * @param event The input event triggering the action.
     */
    @Override
    public void performAction(float time, Event event) {
        orbitController.orbitAzimuth(delta);
    }
}
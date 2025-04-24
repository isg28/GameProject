package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import tage.Camera;
import tage.GameObject;
import org.joml.*;

/**
 * TurnAction handles rotation controls for the player's dolphin. 
 * <p>
 * It enables:
 * <ul>
 *   <li> Yaw (left/right rotation), Controlled via A/D keys or gamepad left stick (X-axis).</li>
 *   <li> Pitch (up/down tilt), Controlled via UP/DOWN keys or gamepad right stick (Y-axis).</li>
 *   <li> Roll (side tilting), Controlled via LEFT/RIGHT keys or gamepad left stick (RX-axis).</li>
 * </ul>
 * The rotation affects the avatar (Dolphin) but does not directly affect the camera.
 *
 * @author Isabel Santoyo-Garcia
 */

public class TurnAction extends AbstractInputAction
{ 
    private MyGame game;
    private GameObject av;

    /**
     * Constructs a TurnAction instance associated with the given game instance.
     *
     * @param myGame The game instance.
     */
    public TurnAction(MyGame myGame) {
        game = myGame;
    }

    /**
     * Handles the rotation of dolphin based on input events.
     * Supports yaw, pitch, and roll using keyboard keys and gamepad input.
     *
     * @param time The time elapsed since the last update.
     * @param e The input event triggering the action.
     */
    @Override
    public void performAction(float time, Event e) {
        if (game.getMarketMode() != MyGame.MarketMode.NONE || game.isBuyingSeeds()){
            return;
        }

        float keyValue = e.getValue();
        av = game.getAvatar();
    
        float rotationAmount = 0.02f; 
        
        // Handle Horiztonal Turns, Yaw (A/D keys or Gamepad Right Stick X-Axis)
        if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.A) {
            //yawDolphin(rotationAmount);
            yawDolphinGlobal(rotationAmount);
        } else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.D) {
            //yawDolphin(-rotationAmount);
            yawDolphinGlobal(-rotationAmount);
        } else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Axis.X) {
            if (keyValue < -0.2f) {  
                //yawDolphin(rotationAmount);
                yawDolphinGlobal(rotationAmount);

            } else if (keyValue > 0.2f) {
                //yawDolphin(-rotationAmount);
                yawDolphinGlobal(-rotationAmount);

            }
        }
        // Handle Vertical Pitch (UP/DOWN keys or Gamepad Right Stick Y-Axis)
        else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.UP) {
            pitchDolphin(rotationAmount);
        } else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.DOWN) {
            pitchDolphin(-rotationAmount);
        } else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Axis.RY) {
            if (keyValue < -0.2f) {  
                pitchDolphin(rotationAmount);
            } else if (keyValue > 0.2f) {  
                pitchDolphin(-rotationAmount);
            }
        }
        // Handle Side Tilt (LEFT/RIGHT keys or Gamepad Left Stick Y-Axis)
        else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.LEFT) {
            pitchSideDolphin(rotationAmount);
        } else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.RIGHT) {
            pitchSideDolphin(-rotationAmount);
        } else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Axis.RX) { 
            if (keyValue < -0.2f) {  
                pitchSideDolphin(rotationAmount); 
            } else if (keyValue > 0.2f) {  
                pitchSideDolphin(-rotationAmount); 
            }
        }
        
    }

    /**
     * Rotates the dolphin locally (Yaw).
     * - Rotates around the Dolphin's local Y-axis
     *  
     * @param angle The rotation angle.
     */
    public void yawDolphin(float angle) {
        av = game.getAvatar(); 
        if (av == null) {
            System.out.println("Dolphin is null");
            return;
        }
    
        Matrix4f currentRotation = new Matrix4f(av.getLocalRotation());
        Matrix4f rotationMatrix = new Matrix4f().rotate(angle, 0, 1, 0);
        currentRotation.mul(rotationMatrix);
        av.setLocalRotation(currentRotation);
    } 
    
    /**
     * Rotates the dolphin globally (Yaw).
     * - Rotates around the global Y-axis (0,1,0).
     */
    public void yawDolphinGlobal(float angle) {
        av = game.getAvatar(); 
        if (av == null) {
            System.out.println("Dolphin is null");
            return;
        }
    
        Matrix4f currentRotation = new Matrix4f(av.getLocalRotation());
        Matrix4f rotationMatrix = new Matrix4f().rotate(angle, 0, 1, 0); 
        currentRotation.mul(rotationMatrix);
        av.setLocalRotation(currentRotation);
    }
    
    /**
     * Tilts the dolphin up or down (Pitch).
     *
     * @param angle The rotation angle.
     */
    public void pitchDolphin(float angle) {
        av = game.getAvatar(); 
        if (av == null) {
            System.out.println("Dolphin is null");
            return;
        }
    
        Matrix4f currentRotation = new Matrix4f(av.getLocalRotation());
        Vector3f right = av.getWorldRightVector();
        Matrix4f rotationMatrix = new Matrix4f().rotate(angle, right.x(), right.y(), right.z());
        currentRotation.mul(rotationMatrix);
        av.setLocalRotation(currentRotation);
    }
    
    /**
     * Rolls the dolphin sideways.
     *
     * @param angle The rotation angle.
     */
    public void pitchSideDolphin(float angle) {
        av = game.getAvatar(); 
        if (av == null) {
            System.out.println("Dolphin is null");
            return;
        }
    
        Matrix4f currentRotation = new Matrix4f(av.getLocalRotation());
        Vector3f forward = av.getWorldForwardVector();
        Matrix4f rotationMatrix = new Matrix4f().rotate(angle, forward.x(), forward.y(), forward.z());
        currentRotation.mul(rotationMatrix);
        av.setLocalRotation(currentRotation);
    
    }
}

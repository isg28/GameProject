package a3;

import tage.input.action.AbstractInputAction;
import tage.networking.server.ProtocolClient;
import net.java.games.input.Event;
import tage.Camera;
import tage.GameObject;
import org.joml.*;

/**
 * FwdAction handles the forward and backward movement of the dolphin.
 * It listens for user input (keyboard, gamepad buttons, or joystick movements)
 * and moves the avatar accordingly.
 * <p>
 * The movement speed is determined by a constant value, and movement is applied
 * based on the camera's or avatar's forward direction.
 *
 * @author Isabel Santoyo-Garcia
 */
public class FwdAction extends AbstractInputAction
{   
    private MyGame game;
    private Vector3f oldPosition;
    private Vector4f fwdDirection;
    private final float movementSpeed = 0.02f;
    private ProtocolClient protClient;

    /**
     * Constructs a FwdAction instance, associating it with the given game instance.
     *
     * @param g The game instance to associate this action with.
     * @param pc The protocol client used for networking.
     */
    public FwdAction(MyGame g, ProtocolClient pc) {
        game = g;
        protClient = pc;
    }
    /**
     * Handles the movement of the avatar based on input events.
     * <p>
     * Recognizes input from keyboard keys (W, S), gamepad buttons (A, B),
     * and joystick movement along the Y-axis. Moves forward or backward accordingly.
     *
     * @param time The time elapsed since the last update.
     * @param e The input event triggering the action.
     */
    @Override
    public void performAction(float time, Event e){
        float keyValue = e.getValue();
        boolean moveForward = false;
        boolean moveBackward = false;

        // Handle Foward & Backward Movement (W/S keys or Gamepad Left Stick Y-Axis/Buttons 0/1)
        if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.W ||
            e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._0) { // A button on controller 
            moveForward = true;
        } 
        else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.S ||
                 e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._1) { // B button on controller
            moveBackward = true;
        }
        else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Axis.Y) {
            if (keyValue < -0.2f) { 
                moveForward = true;
            } 
            else if (keyValue > 0.2f) { 
                moveBackward = true;
            }
        }
        int direction = (moveForward) ? 1 : (moveBackward) ? -1 : 0;
        if (direction != 0) {

            GameObject dol = game.getAvatar();
            oldPosition = dol.getWorldLocation();
            fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
            fwdDirection.mul(dol.getWorldRotation());
            fwdDirection.mul(movementSpeed * direction);
            Vector3f newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
            dol.setLocalLocation(newPosition);
            if (game.getProtocolClient() != null && game.isConnected()) {
                GameObject avatar = game.getAvatar();
                if (avatar != null) {
                    Vector3f position = avatar.getWorldLocation();
                    game.getProtocolClient().sendMoveMessage(position);
                } else {
                    System.out.println("[Client] Warning: Avatar is null, cannot send move message.");
                }
            } else {
                System.out.println("[Client] Warning: Client not connected, cannot send move message.");
            }
            
            
        } 
        
    }
}

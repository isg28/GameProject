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
    public void performAction(float time, Event e) {
        if (game.getMarketMode() != MyGame.MarketMode.NONE || game.isBuyingSeeds()){
            return;
        }

        float keyValue = e.getValue();
        boolean moveForward = false;
        boolean moveBackward = false;
    
        if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.W ||
            e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._0) {
            moveForward = true;
        } 
        else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.S ||
                 e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._1) {
            moveBackward = true;
        } 
        else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Axis.Y) {
            if (keyValue < -0.2f) moveForward = true;
            else if (keyValue > 0.2f) moveBackward = true;
        }
    
        int direction = moveForward ? 1 : moveBackward ? -1 : 0;
        if (direction != 0) {
            GameObject dol = game.getAvatar();
            oldPosition = dol.getWorldLocation();
            fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
            fwdDirection.mul(dol.getWorldRotation());
            fwdDirection.mul(movementSpeed * direction);
            Vector3f newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
    
            // Clamp movement within terrain bounds
            float minX = -15.0f;
            float maxX = 15.0f;
            float minZ = -15.0f;
            float maxZ = 15.0f;
    
            if (newPosition.x() < minX || newPosition.x() > maxX || 
                newPosition.z() < minZ || newPosition.z() > maxZ) {
                return;
            }
    
            dol.setLocalLocation(newPosition);
    
            if (game.getProtocolClient() != null && game.isConnected()) {
                game.getProtocolClient().sendMoveMessage(newPosition);
            }
        }
    }
    
}

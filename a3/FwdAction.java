package a3;

import tage.input.action.AbstractInputAction;
import tage.networking.server.ProtocolClient;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import net.java.games.input.Event;
import tage.Camera;
import tage.GameObject;

import java.util.Arrays;

import org.joml.*;

/**
 * FwdAction handles the forward and backward movement of the avatar.
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
     * Recognizes input from keyboard keys (W, S), gamepad buttons (A, B),
     * and joystick movement along the Y-axis. Moves forward or backward accordingly.
     * Detects collisions with home, market, pig, or chicken, triggering physics-based
     * behavior for collisions.
     *
     * @param time The time elapsed since the last update.
     * @param e The input event triggering the action.
     */
    @Override
    public void performAction(float time, Event e) {
        if (game.getMarketMode() != MyGame.MarketMode.NONE || game.isBuyingSeeds()) {
            return; // Block movement during market interactions
        }

        // Prevent movement if physics is active or rabbit is face down
        if (game.isAvatarPhysicsActive() || game.isFaceDown()) {
            return;
        }

        float keyValue = e.getValue();
        boolean moveForward = false;
        boolean moveBackward = false;
    
        // Determine movement direction based on input
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
            GameObject avatar = game.getAvatar();
            if (avatar == null) {
                System.out.println("Avatar is null in FwdAction!");
                return;
            }
            oldPosition = avatar.getWorldLocation();
            fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
            fwdDirection.mul(avatar.getWorldRotation());
            fwdDirection.mul(movementSpeed * direction);

            Vector3f newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());

            // Terrain bounds
            float minX = -15.0f, maxX = 15.0f, minZ = -15.0f, maxZ = 15.0f;
            if (newPosition.x() < minX || newPosition.x() > maxX ||
                newPosition.z() < minZ || newPosition.z() > maxZ) {
                return;
            }

            // Collision detection with home, market, pig, and chicken
            float blockRadius = 0.50f; // For home and market
            float animalCollisionRadius = 0.4f; // For pig and chicken (rabbit ~0.2f, animals ~0.2f)
            Vector3f homePos = game.getHome().getWorldLocation();
            Vector3f marketPos = game.getMarket().getWorldLocation();
            Vector3f pigPos = game.getPig() != null ? game.getPig().getWorldLocation() : null;
            Vector3f chickenPos = game.getChicken() != null ? game.getChicken().getWorldLocation() : null;

            boolean isAnimalCollision = false;

            Vector3f radioPos = game.getRadio().getWorldLocation();
            // Check home and market collisions
            if (newPosition.distance(homePos) < blockRadius || newPosition.distance(marketPos) < blockRadius || newPosition.distance(radioPos)  < blockRadius) 
            {
                PhysicsEngine physicsEngine = game.getPhysicsEngine();
                Vector3f avatarPos = avatar.getWorldLocation();

                // Create transform for physics object
                double[] xform = new double[] {
                    1, 0, 0, 0,
                    0, 1, 0, 0,
                    0, 0, 1, 0,
                    avatarPos.x(), avatarPos.y(), avatarPos.z(), 1
                };

                // Create physics object
                PhysicsObject avatarPhysicsObject = physicsEngine.addSphereObject(
                    physicsEngine.nextUID(),
                    1.0f, // Mass
                    xform,
                    0.3f  // Radius
                );

                if (avatarPhysicsObject == null) {
                    System.err.println("Failed to create avatar physics object!");
                    return;
                }

                // Configure physics properties
                avatarPhysicsObject.setBounciness(0.3f);
                avatarPhysicsObject.setFriction(0.2f);
                avatarPhysicsObject.setDamping(0.5f, 0.5f);

                // Calculate bounce velocity
                Vector3f backwardDir = avatar.getWorldForwardVector().normalize().mul(direction > 0 ? -1 : 1);
                float backwardBounceStrength = 0.5f;
                float bounceUpwardStrength = 0.0f;

                Vector3f bounceVelocity = new Vector3f(
                    backwardDir.x() * backwardBounceStrength,
                    bounceUpwardStrength,
                    backwardDir.z() * backwardBounceStrength
                );

                avatarPhysicsObject.setLinearVelocity(new float[] {
                    bounceVelocity.x(),
                    bounceVelocity.y(),
                    bounceVelocity.z()
                });

                System.out.println("Collision with home/market! Applied bounce velocity: " + bounceVelocity);
                System.out.println("Physics object created at: " + avatarPos);

                // Store physics object and activate physics in MyGame
                game.setAvatarPhysicsObject(avatarPhysicsObject);
                game.setAvatarPhysicsActive(true);
                game.setPhysicsActivateTime(System.currentTimeMillis());
                avatar.setPhysicsObject(avatarPhysicsObject);
                return;
            }

            // Check pig collision
            if (pigPos != null && newPosition.distance(pigPos) < animalCollisionRadius) {
                isAnimalCollision = true;
            }

            // Check chicken collision
            if (chickenPos != null && newPosition.distance(chickenPos) < animalCollisionRadius) {
                isAnimalCollision = true;
            }

            if (isAnimalCollision) {
                PhysicsEngine physicsEngine = game.getPhysicsEngine();
                Vector3f avatarPos = avatar.getWorldLocation();

                // Create transform for physics object
                double[] xform = new double[] {
                    1, 0, 0, 0,
                    0, 1, 0, 0,
                    0, 0, 1, 0,
                    avatarPos.x(), avatarPos.y(), avatarPos.z(), 1
                };

                // Create physics object for face-down fall
                PhysicsObject avatarPhysicsObject = physicsEngine.addSphereObject(
                    physicsEngine.nextUID(),
                    0.1f, // Light mass for responsive fall
                    xform,
                    0.2f  // Approximate rabbit radius
                );

                if (avatarPhysicsObject == null) {
                    System.err.println("Failed to create avatar physics object for animal collision!");
                    return;
                }

                // Configure physics properties
                avatarPhysicsObject.setBounciness(0.3f);
                avatarPhysicsObject.setFriction(0.2f);
                avatarPhysicsObject.setDamping(0.5f, 0.5f);

                // Apply angular velocity to rotate forward (face down)
                avatarPhysicsObject.setAngularVelocity(new float[]{2.0f, 0, 0}); // Rotate around X-axis

                System.out.println("Collision with pig/chicken! Applied angular velocity for face-down fall.");
                System.out.println("Physics object created at: " + avatarPos);

                // Store physics object and activate physics in MyGame
                game.setAvatarPhysicsObject(avatarPhysicsObject);
                game.setAvatarPhysicsActive(true);
                game.setPhysicsActivateTime(System.currentTimeMillis());
                avatar.setPhysicsObject(avatarPhysicsObject);
                return;
            }

            // Regular move if no collision and no active physics
            if (!game.isAvatarPhysicsActive()) {
                avatar.setLocalLocation(newPosition);
            }

            if (game.getProtocolClient() != null && game.isConnected()) {
                game.getProtocolClient().sendMoveMessage(newPosition);
            }
        }
    }
}
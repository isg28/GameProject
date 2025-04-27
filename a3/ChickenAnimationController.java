package a3;

import tage.*;
import tage.shapes.AnimatedShape;
import tage.physics.PhysicsObject;
import org.joml.*;
import java.util.Random;

/**
 * ChickenAnimationController manages the animation and movement of a chicken GameObject.
 * The chicken walks for 30 seconds to cover longer distances, then stalls for a random
 * duration (1-3 seconds). During some stalls (30% chance), it plays a curious animation.
 * The controller ensures the chicken avoids collisions with the market, house, and terrain
 * borders, and adjusts its height to the terrain. It uses its own timer for state transitions.
 */
public class ChickenAnimationController {
    private GameObject chicken;
    private AnimatedShape animatedShape;
    private MyGame game;
    private Random random;
    private boolean enabled = true; // Controller enabled state
    
    // Timing variables
    private float timer = 0f; // Custom timer in seconds
    private float walkDuration = 0f; // Walk for 5 seconds
    private float stallDuration = 0f; // Current stall duration
    private boolean isWalking = true; // Current state (walking or stalling)
    
    // Movement parameters
    private float moveSpeed = 0.2f; // Increased temporarily to test movement (was 0.1f)
    private final float NORMAL_SPEED = 0.5f;
    private final float SLOW_SPEED = 0.2f;
    private Vector3f forwardDir; // Current forward direction
    
    // Animation names
    private static final String WALK_ANIMATION = "WALK";
    private static final String CURIOUS_ANIMATION = "CURIOUS";
    
    // Collision parameters
    private float blockRadius = 1.0f; // Radius to avoid house/market
    private float minX = -12f, maxX = 12f, minZ = -12f, maxZ = 12f; // Terrain bounds

    /**
     * Constructor initializes the controller with the chicken GameObject and game instance.
     * @param chicken The chicken GameObject with an AnimatedShape.
     * @param game The MyGame instance for accessing home, market, and terrain.
     */
    public ChickenAnimationController(GameObject chicken, MyGame game) {
        this.chicken = chicken;
        this.game = game;
        this.random = new Random();
        
        // Get the animated shape and load animations
        this.animatedShape = (AnimatedShape) chicken.getShape();
        try {
            animatedShape.loadAnimation(WALK_ANIMATION, "chickenwalk.rka");
            animatedShape.loadAnimation(CURIOUS_ANIMATION, "chickencurious.rka");
        } catch (RuntimeException e) {
            System.err.println("Failed to load chicken animations: " + e.getMessage());
            throw e;
        }
        
        // Start with walking animation
        animatedShape.playAnimation(WALK_ANIMATION, 0.4f, AnimatedShape.EndType.LOOP, 0);
        
        // Initialize forward direction (random to start)
        updateForwardDirection();
    }

    /**
     * Sets whether the controller is enabled.
     * @param enabled True to enable, false to disable.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Updates the forward direction to a new random direction and rotates the chicken.
     */
    private void updateForwardDirection() {
        float angle = (float) (random.nextFloat() * 2 * java.lang.Math.PI); // Random angle in radians
        forwardDir = new Vector3f((float) java.lang.Math.cos(angle), 0, (float) java.lang.Math.sin(angle)).normalize();
        
        // Update chicken's rotation to face the new direction
        Matrix4f rotation = new Matrix4f().rotationY((float) (-angle + java.lang.Math.PI / 2)); // Adjust for model orientation
        chicken.setLocalRotation(rotation);
        System.out.println("Chicken rotation updated to face direction: " + forwardDir);
    }

    /**
     * Checks if the proposed new location is valid (avoids house, market, and borders).
     * @param newLoc The proposed new location.
     * @return True if the location is valid, false otherwise.
     */
    private boolean isValidMove(Vector3f newLoc) {
        // Check collision with house
        if (game.getHome() != null) {
            float distanceToHome = newLoc.distance(game.getHome().getWorldLocation());
            if (distanceToHome < blockRadius) {
                System.out.println("Invalid move: Too close to house (distance: " + distanceToHome + ")");
                return false;
            }
        }
        
        // Check collision with market
        if (game.getMarket() != null) {
            float distanceToMarket = newLoc.distance(game.getMarket().getWorldLocation());
            if (distanceToMarket < blockRadius) {
                System.out.println("Invalid move: Too close to market (distance: " + distanceToMarket + ")");
                return false;
            }
        }
        
        // Check terrain boundaries
        if (newLoc.x() < minX || newLoc.x() > maxX || newLoc.z() < minZ || newLoc.z() > maxZ) {
            System.out.println("Invalid move: Outside terrain bounds (x: " + newLoc.x() + ", z: " + newLoc.z() + ")");
            return false;
        }
        
        return true;
    }

    /**
     * Updates the controller, managing movement, animations, and terrain height.
     * @param deltaTimeMs Time since last frame in milliseconds.
     */
    public void update(float deltaTimeMs) {
        if (!enabled) return;
    
        try {
            float deltaTimeSec = deltaTimeMs / 1000f; // Convert to seconds
            timer += deltaTimeSec;
    
            animatedShape.updateAnimation();
    
            if (isWalking) {
                if (timer >= walkDuration) {
                    // Done walking, start stalling
                    isWalking = false;
                    timer = 0f;
                    stallDuration = 1f + random.nextFloat() * 2f; // 1-3 sec stall
    
                    animatedShape.stopAnimation();
                    if (random.nextFloat() < 0.3f) {
                        animatedShape.playAnimation(CURIOUS_ANIMATION, 0.4f, AnimatedShape.EndType.LOOP, 0);
                        System.out.println("Playing curious animation");
                    } else {
                        System.out.println("Stalling without curious animation");
                    }
                } else {
                    // Move the chicken
                    Vector3f currentLoc = chicken.getWorldLocation();
                    Vector3f moveVec = new Vector3f(forwardDir).mul(moveSpeed * deltaTimeSec);
                    Vector3f newLoc = new Vector3f(currentLoc).add(moveVec);
    
                    if (isValidMove(newLoc)) {
                        float newHeight = game.getTerr().getHeight(newLoc.x(), newLoc.z());
                        newLoc.set(newLoc.x(), newHeight + 0.1f, newLoc.z());
                        chicken.setLocalLocation(newLoc);
    
                        PhysicsObject phys = chicken.getPhysicsObject();
                        if (phys != null) {
                            double[] xform = {
                                1, 0, 0, 0,
                                0, 1, 0, 0,
                                0, 0, 1, 0,
                                newLoc.x(), newLoc.y(), newLoc.z(), 1
                            };
                            phys.setTransform(xform);
                        }
                    } else {
                        updateForwardDirection();
                    }
                }
            } else {
                // Stalling
                if (timer >= stallDuration) {
                    // Start walking again
                    isWalking = true;
                    timer = 0f;
    
                    animatedShape.stopAnimation();
                    animatedShape.playAnimation(WALK_ANIMATION, 0.4f, AnimatedShape.EndType.LOOP, 0);
    
                    // Randomize walk duration (1-5 seconds)
                    walkDuration = 1f + random.nextFloat() * 4f;
    
                    // Randomize movement speed (slow or normal)
                    if (random.nextBoolean()) {
                        moveSpeed = NORMAL_SPEED;
                        System.out.println("Walking at NORMAL speed (" + moveSpeed + ")");
                    } else {
                        moveSpeed = SLOW_SPEED;
                        System.out.println("Walking at SLOW speed (" + moveSpeed + ")");
                    }
    
                    updateForwardDirection();
                }
            }
        } catch (Exception e) {
            System.err.println("Error in ChickenAnimationController.update: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
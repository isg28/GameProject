package a3;

import tage.*;
import tage.shapes.AnimatedShape;
import tage.physics.PhysicsObject;
import org.joml.*;
import java.util.Random;

/**
 * PigAnimationController manages the animation and movement behavior of a pig GameObject.
 * The pig walks for a duration, then stalls for a random period, sometimes playing a curious animation.
 * It avoids collisions with the house, market, and terrain borders. It adjusts its height to match terrain.
 */
public class PigAnimationController {
    private GameObject pig;
    private AnimatedShape animatedShape;
    private MyGame game;
    private Random random;
    private boolean enabled = true;

    // Timing variables
    private float timer = 0f;
    private float walkDuration = 0f;
    private float stallDuration = 0f;
    private boolean isWalking = true;

    // Movement parameters
    private float moveSpeed = 0.15f;
    private final float NORMAL_SPEED = 0.25f;
    private final float SLOW_SPEED = 0.15f;
    private Vector3f forwardDir;

    // Animation names
    private static final String WALK_ANIMATION = "WALK";
    private static final String CURIOUS_ANIMATION = "CURIOUS";

    // Collision parameters
    private float blockRadius = 1.0f;
    private float minX = -12f, maxX = 12f, minZ = -12f, maxZ = 12f;

    public PigAnimationController(GameObject pig, MyGame game) {
        this.pig = pig;
        this.game = game;
        this.random = new Random();

        // Get the animated shape and load animations
        this.animatedShape = (AnimatedShape) pig.getShape();
        try {
            animatedShape.loadAnimation(WALK_ANIMATION, "pigwalk.rka");
            animatedShape.loadAnimation(CURIOUS_ANIMATION, "pigcurious.rka");
        } catch (RuntimeException e) {
            System.err.println("Failed to load pig animations: " + e.getMessage());
            throw e;
        }

        // Start with walk animation
        animatedShape.playAnimation(WALK_ANIMATION, 0.4f, AnimatedShape.EndType.LOOP, 0);

        updateForwardDirection();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private void updateForwardDirection() {
        float angle = (float) (random.nextFloat() * 2 * java.lang.Math.PI);
        forwardDir = new Vector3f((float) java.lang.Math.cos(angle), 0, (float) java.lang.Math.sin(angle)).normalize();

        Matrix4f rotation = new Matrix4f().rotationY((float) (-angle + java.lang.Math.PI / 2));
        pig.setLocalRotation(rotation);
        System.out.println("Pig rotation updated to face direction: " + forwardDir);
    }

    private boolean isValidMove(Vector3f newLoc) {
        if (game.getHome() != null && newLoc.distance(game.getHome().getWorldLocation()) < blockRadius) {
            return false;
        }
        if (game.getMarket() != null && newLoc.distance(game.getMarket().getWorldLocation()) < blockRadius) {
            return false;
        }
        if (newLoc.x() < minX || newLoc.x() > maxX || newLoc.z() < minZ || newLoc.z() > maxZ) {
            return false;
        }
        return true;
    }

    public void update(float deltaTimeMs) {
        if (!enabled) return;

        try {
            float deltaTimeSec = deltaTimeMs / 1000f;
            timer += deltaTimeSec;

            animatedShape.updateAnimation();

            if (isWalking) {
                if (timer >= walkDuration) {
                    isWalking = false;
                    timer = 0f;
                    stallDuration = 1f + random.nextFloat() * 2f; // 1-3 sec stall

                    animatedShape.stopAnimation();
                    if (random.nextFloat() < 0.3f) {
                        animatedShape.playAnimation(CURIOUS_ANIMATION, 0.4f, AnimatedShape.EndType.LOOP, 0);
                        System.out.println("Pig playing curious animation");
                    } else {
                        System.out.println("Pig stalling without curious animation");
                    }
                } else {
                    Vector3f currentLoc = pig.getWorldLocation();
                    Vector3f moveVec = new Vector3f(forwardDir).mul(moveSpeed * deltaTimeSec);
                    Vector3f newLoc = new Vector3f(currentLoc).add(moveVec);

                    if (isValidMove(newLoc)) {
                        float newHeight = game.getTerr().getHeight(newLoc.x(), newLoc.z());
                        newLoc.set(newLoc.x(), newHeight + 0.1f, newLoc.z());
                        pig.setLocalLocation(newLoc);

                        PhysicsObject phys = pig.getPhysicsObject();
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
                if (timer >= stallDuration) {
                    isWalking = true;
                    timer = 0f;

                    animatedShape.stopAnimation();
                    animatedShape.playAnimation(WALK_ANIMATION, 0.4f, AnimatedShape.EndType.LOOP, 0);

                    walkDuration = 1f + random.nextFloat() * 4f;

                    if (random.nextBoolean()) {
                        moveSpeed = NORMAL_SPEED;
                        System.out.println("Pig walking at NORMAL speed (" + moveSpeed + ")");
                    } else {
                        moveSpeed = SLOW_SPEED;
                        System.out.println("Pig walking at SLOW speed (" + moveSpeed + ")");
                    }

                    updateForwardDirection();
                }
            }
        } catch (Exception e) {
            System.err.println("Error in PigAnimationController.update: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

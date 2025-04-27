package a3;

import tage.*;
import tage.shapes.AnimatedShape;
import java.util.Random;

/**
 * PlantAnimationController manages the wind-like animation of a planted crop.
 * After being planted, the plant will randomly flap its leaves at random intervals,
 * creating a natural effect as if wind occasionally moves the plant.
 */
public class PlantAnimationController {
    private GameObject plant;
    private AnimatedShape animatedShape;
    private Random random;
    private boolean enabled = true;

    // Timing
    private float timer = 0f;
    private float nextFlapTime = 0f; // How long until next flap

    // Animation settings
    private static final String FLAP_ANIMATION = "plantmove"; // your "plantmove.rka"
    private static final float FLAP_SPEED = 0.5f; // Adjust if needed

    /**
     * Creates a new controller for a plant GameObject.
     * @param plant the GameObject representing the plant (must have AnimatedShape)
     */
    public PlantAnimationController(GameObject plant) {
        this.plant = plant;
        this.random = new Random();

        if (!(plant.getShape() instanceof AnimatedShape)) {
            throw new IllegalArgumentException("Plant must have an AnimatedShape!");
        }

        this.animatedShape = (AnimatedShape) plant.getShape();

        // Try loading the animation
        try {
            animatedShape.loadAnimation(FLAP_ANIMATION, "plantmove.rka");
        } catch (RuntimeException e) {
            System.err.println("Failed to load plant animation: " + e.getMessage());
            throw e;
        }

        // Initialize first random time to flap
        scheduleNextFlap();
    }

    private void scheduleNextFlap() {
        nextFlapTime = 3f + random.nextFloat() * 7f; // Random between 3-10 seconds
        timer = 0f;
    }

    /**
     * Enables or disables this controller.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Updates the animation controller. Should be called every frame.
     * @param deltaTimeMs elapsed time since last frame in milliseconds
     */
    public void update(float deltaTimeMs) {
        if (!enabled) return;

        float deltaTimeSec = deltaTimeMs / 1000f;
        timer += deltaTimeSec;

        animatedShape.updateAnimation();

        if (timer >= nextFlapTime) {
            // Play the flap animation once
            animatedShape.playAnimation(FLAP_ANIMATION, FLAP_SPEED, AnimatedShape.EndType.STOP, 0);
            System.out.println("Plant flapping!");
            scheduleNextFlap(); // Set a new random time for next flap
        }
    }
}

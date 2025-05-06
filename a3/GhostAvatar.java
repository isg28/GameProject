package a3;

import org.joml.Vector3f;
import org.joml.Matrix4f;
import tage.GameObject;
import tage.Light;
import tage.ObjShape;
import tage.TextureImage;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

/**
 * GhostAvatar represents a remote player’s avatar in the game world, complete with
 * simulated watering-can droplet physics and torch-lighting behavior.
 * <p>
 * It extends GameObject and manages:
 * <ul>
 *   <li>Unique client ID and position updates</li>
 *   <li>Watering-can visuals and droplet spawning/synchronization</li>
 *   <li>Torch object and positional light for torch effects</li>
 * </ul>
 * 
 * @author Isabel Santoyo-Garcia
 */
public class GhostAvatar extends GameObject {
    private UUID id;
    private GameObject wateringCan;
    private boolean watering = false;
    private float dropletTimer = 0f;
    private static final float DROP_INTERVAL = 0.05f;
    private static final float DROPLET_LIFETIME = 2.0f;
    private GameObject torchObject;
    private boolean torchOn = false;
    private Light torchLight;
    private GameObject torchGO;
    private MyGame game;

    private List<GameObject> ghostDrops = new ArrayList<>();
    private List<PhysicsObject> ghostDropPhysics = new ArrayList<>();
    private List<Boolean> grounded = new ArrayList<>();
    private List<Float> groundTime = new ArrayList<>();
    private List<Boolean> hasBounced = new ArrayList<>();
    
    /**
     * Constructs a GhostAvatar at the given position with specified shape and texture.
     *
     * @param game reference to the main game instance
     * @param id unique UUID of the remote client
     * @param s shape used for the avatar model
     * @param t texture applied to the avatar model
     * @param p initial world position for the avatar
     */
    public GhostAvatar(MyGame game, UUID id, ObjShape s, TextureImage t, Vector3f p) {
        super(GameObject.root(), s, t);
        this.game = game;             
        setId(id);
        setLocalTranslation(new Matrix4f().translation(p));
    }

    /** Sets the unique ID for this ghost avatar. */
    public void setId(UUID id) { this.id = id; }
    /** Retrieves the ghost avatar’s UUID. */
    public UUID getId() { return id; }
    /** Checks if avatar is using the watering can */
    public boolean isWatering() { return watering; }
    /**
     * Initializes the watering-can child object with provided shape and texture.
     * The can starts hidden until watering is enabled.
     *
     * @param canShape shape for the watering can
     * @param canTex texture for the watering can
    */
    public void initWateringCan(ObjShape canShape, TextureImage canTex) {
        wateringCan = new GameObject(this, canShape, canTex);
        wateringCan.getRenderStates().disableRendering();
        wateringCan.setLocalScale(new Matrix4f().scaling(0.4f));
    }
    /** Retrieves the watering can GameObject. */
    public GameObject getWateringCanObject() { return wateringCan; }
    /** Enables or disables watering visuals and droplet spawning. */
    public void setWatering(boolean on) {
        watering = on;
        if (wateringCan != null) {
            if (on) wateringCan.getRenderStates().enableRendering();
            else    wateringCan.getRenderStates().disableRendering();
        }
    }
    /**
     * Called each frame to spawn new droplets and sync their physics.
     */
    public void updateDroplets(PhysicsEngine physics, float dtSec, ObjShape dropletShape) {
        if (watering) {
            dropletTimer += dtSec;
            while (dropletTimer >= DROP_INTERVAL) {
                dropletTimer -= DROP_INTERVAL;
                Vector3f canPos   = wateringCan.getWorldLocation();
                Vector3f fwd      = wateringCan.getWorldForwardVector().normalize();
                Vector3f up       = wateringCan.getWorldUpVector().normalize();
                Vector3f right    = wateringCan.getWorldRightVector().normalize();
                Vector3f offset   = new Vector3f(fwd).mul(0.07f)
                                   .add(new Vector3f(up).mul(0.1f))
                                   .add(new Vector3f(right).mul(0f));
                Vector3f spawnPos = canPos.add(offset);

                GameObject drop = new GameObject(GameObject.root(), dropletShape, null);
                drop.getRenderStates().enableRendering();
                drop.getRenderStates().setColor(new Vector3f(0f,0.7f,1f));
                drop.setLocalScale(new Matrix4f().scaling(0.01f));
                drop.setLocalTranslation(new Matrix4f().translation(spawnPos));
                double[] xform = {
                    1,0,0,0,  0,1,0,0,  0,0,1,0,
                    spawnPos.x, spawnPos.y, spawnPos.z, 1
                };
                PhysicsObject phys = physics.addSphereObject(
                    physics.nextUID(), 0.05f, xform, 0.01f
                );
                phys.setLinearVelocity(new float[]{0f, -2f, 0f});

                drop.setPhysicsObject(phys);
                ghostDrops.add(drop);
                ghostDropPhysics.add(phys);
                grounded.add(false);
                groundTime.add(0f);
            }
        }
        for (int i = 0; i < ghostDrops.size(); i++) {
            GameObject drop = ghostDrops.get(i);
            PhysicsObject phys = ghostDropPhysics.get(i);
            double[] t = phys.getTransform();
            float y = (float)t[13];
            drop.setLocalTranslation(
                new Matrix4f().translation((float)t[12], y, (float)t[14])
            );

            if (!grounded.get(i) && y <= 0.01f) {
                grounded.set(i, true);
                phys.setLinearVelocity(new float[]{
                    (float)(Math.random()-0.5)*2f, 1.5f, (float)(Math.random()-0.5)*2f
                });
            }
            if (grounded.get(i)) {
                float age = groundTime.get(i) + dtSec;
                groundTime.set(i, age);
                if (age >= DROPLET_LIFETIME) {
                    physics.removeObject(phys.getUID());
                    drop.getRenderStates().disableRendering();
                    ghostDrops.remove(i);
                    ghostDropPhysics.remove(i);
                    grounded.remove(i);
                    groundTime.remove(i);
                    i--;
                }
            }
        }
    }
    /**
     * Sets the avatar’s world position .
     *
     * @param p new world position as a Vector3f
     */
    public void setPosition(Vector3f p) {
        Matrix4f m = new Matrix4f().translation(p.x, p.y, p.z);
        setLocalTranslation(m);
    }
    /**
     * Initializes a torch object and its positional light for this avatar.
     *
     * @param torchShape shape for the torch
     * @param torchTex texture for the torch
    */
    public void initTorch(ObjShape torchShape, TextureImage torchTex) {
        torchObject = new GameObject(this, torchShape, torchTex);
        torchObject.setLocalScale(new Matrix4f().scaling(0.7f));     
        torchObject.setLocalTranslation(new Matrix4f().translation(0.1f,0.1f,0.1f));
        torchObject.getRenderStates().disableRendering();

        torchLight = new Light();
        torchLight.setType(Light.LightType.POSITIONAL);
        torchLight.setAmbient(0.2f, 0.1f, 0.0f);
        torchLight.setDiffuse(1.0f, 0.8f, 0.3f);
        torchLight.setSpecular(1.0f, 0.8f, 0.3f);
        torchLight.setRange(2.0f);
        torchLight.setConstantAttenuation(1f);
        torchLight.setLinearAttenuation(0.8f);
        torchLight.setQuadraticAttenuation(0.2f);
        torchLight.disable();
        game.getEngine().getSceneGraph().addLight(torchLight);
    }
    /** Retrieves the torch’s GameObject. */
    public GameObject getTorchObject() { return torchObject; }
    /** Turns the torch (and its light) on or off. */
    public void setTorchOn(boolean on) {
        this.torchOn = on;
        if (torchObject != null) {
            if (on) {
                torchObject.getRenderStates().enableRendering();
                torchLight.enable();         
            }
            else {
                torchObject.getRenderStates().disableRendering();
                torchLight.disable();        
            }
        }
    }
    /** Checks if the torch is currently on. */
    public boolean isTorchOn() { return torchOn; }
    /** Retrieves the torch’s torchGO. */
    public GameObject getTorchGO() { return torchGO; }
    /** Retrieves the torch’s positional Light object. */
    public Light getTorchLight() { return torchLight; }



}

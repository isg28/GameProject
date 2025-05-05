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
 * Represents a ghost avatar in the game world, with proper droplet physics.
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
    private Light       torchLight;
    private GameObject torchGO;
    private MyGame game;



    private List<GameObject>   ghostDrops        = new ArrayList<>();
    private List<PhysicsObject> ghostDropPhysics = new ArrayList<>();
    private List<Boolean>       grounded          = new ArrayList<>();
    private List<Float>         groundTime        = new ArrayList<>();
    private List<Boolean>       hasBounced        = new ArrayList<>();
    

    public GhostAvatar(MyGame game, UUID id, ObjShape s, TextureImage t, Vector3f p) {
        super(GameObject.root(), s, t);
        this.game = game;             
        setId(id);
        setLocalTranslation(new Matrix4f().translation(p));
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public boolean isWatering() { return watering; }

    public void initWateringCan(ObjShape canShape, TextureImage canTex) {
        wateringCan = new GameObject(this, canShape, canTex);
        wateringCan.getRenderStates().disableRendering();
        wateringCan.setLocalScale(new Matrix4f().scaling(0.4f));
    }

    public GameObject getWateringCanObject() { return wateringCan; }

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
        // spawn new droplets
        if (watering) {
            dropletTimer += dtSec;
            while (dropletTimer >= DROP_INTERVAL) {
                dropletTimer -= DROP_INTERVAL;
                // compute spawn position at can spout
                Vector3f canPos   = wateringCan.getWorldLocation();
                Vector3f fwd      = wateringCan.getWorldForwardVector().normalize();
                Vector3f up       = wateringCan.getWorldUpVector().normalize();
                Vector3f right    = wateringCan.getWorldRightVector().normalize();
                Vector3f offset   = new Vector3f(fwd).mul(0.07f)
                                   .add(new Vector3f(up).mul(0.1f))
                                   .add(new Vector3f(right).mul(0f));
                Vector3f spawnPos = canPos.add(offset);

                // visual
                GameObject drop = new GameObject(GameObject.root(), dropletShape, null);
                drop.getRenderStates().enableRendering();
                drop.getRenderStates().setColor(new Vector3f(0f,0.7f,1f));
                drop.setLocalScale(new Matrix4f().scaling(0.01f));
                drop.setLocalTranslation(new Matrix4f().translation(spawnPos));

                // physics
                double[] xform = {
                    1,0,0,0,  0,1,0,0,  0,0,1,0,
                    spawnPos.x, spawnPos.y, spawnPos.z, 1
                };
                PhysicsObject phys = physics.addSphereObject(
                    physics.nextUID(), 0.05f, xform, 0.01f
                );
                phys.setLinearVelocity(new float[]{0f, -2f, 0f});

                // link and store
                drop.setPhysicsObject(phys);
                ghostDrops.add(drop);
                ghostDropPhysics.add(phys);
                grounded.add(false);
                groundTime.add(0f);
            }
        }

        // sync existing droplets
        for (int i = 0; i < ghostDrops.size(); i++) {
            GameObject drop = ghostDrops.get(i);
            PhysicsObject phys = ghostDropPhysics.get(i);
            double[] t = phys.getTransform();
            float y = (float)t[13];
            drop.setLocalTranslation(
                new Matrix4f().translation((float)t[12], y, (float)t[14])
            );

            // ground bounce
            if (!grounded.get(i) && y <= 0.01f) {
                grounded.set(i, true);
                phys.setLinearVelocity(new float[]{
                    (float)(Math.random()-0.5)*2f, 1.5f, (float)(Math.random()-0.5)*2f
                });
            }
            // lifetime
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

    // helper to position avatar
    public void setPosition(Vector3f p) {
        Matrix4f m = new Matrix4f().translation(p.x, p.y, p.z);
        setLocalTranslation(m);
    }
    public void initTorch(ObjShape torchShape, TextureImage torchTex) {
        torchObject = new GameObject(this, torchShape, torchTex);
        torchObject.setLocalScale(new Matrix4f().scaling(0.7f));      // match your MyGame scale
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

    public GameObject getTorchObject() { return torchObject; }
    public void setTorchOn(boolean on) {
        this.torchOn = on;
        if (torchObject != null) {
            if (on) {
                torchObject.getRenderStates().enableRendering();
                torchLight.enable();         // turn on the glow
            }
            else {
                torchObject.getRenderStates().disableRendering();
                torchLight.disable();        // turn off the glow
            }
        }
    }
    
    public boolean isTorchOn() { return torchOn; }
    public GameObject getTorchGO() { return torchGO; }
    public Light      getTorchLight() { return torchLight; }



}

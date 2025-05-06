package a3;

import java.io.IOException;
import java.util.UUID;
import java.util.Vector;
import java.util.Iterator;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import tage.VariableFrameRateGame;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;


/**
 * Manages all ghost avatars in the multiplayer environment.
 * <p>
 * GhostManager is responsible for creating, updating, and removing 
 * ghost avatars which represent other players in the game world.
 * It maintains a list of currently active ghost avatars and ensures that each
 * is uniquely identified by a UUID.
 * </p>
 * 
 * @author Isabel Santoyo-Garcia
 */
public class GhostManager {
    private MyGame game;
    private Vector<GhostAvatar> ghostAvs = new Vector<>();
    private Map<UUID, List<Crop>> ghostCrops = new HashMap<>();
    private Map<UUID,String> ghostColors = new HashMap<>();
    private HashMap<UUID, GhostAvatar> ghostAvatars = new HashMap<>();
    private static final float CLEANUP_INTERVAL = 5.0f;
    private float cleanupTimer = 0f;



    /**
     * Constructs a GhostManager linked to the current game instance.
     *
     * @param vfrg The main game engine instance (casted to {@link MyGame}).
    */
    public GhostManager(VariableFrameRateGame vfrg) {
        game = (MyGame) vfrg;
    }

    /**
     * Creates and adds a new ghost avatar to the scene.
     *
     * @param id The unique identifier for the ghost avatar.
     * @param p The initial position of the ghost in world coordinates.
     * @throws IOException if the shape or texture loading fails.
    */
    public void createGhost(UUID id, Vector3f p, String color) throws IOException {
        if (ghostAvatars.containsKey(id)) {
            System.out.println("[GhostManager] Ghost avatar already exists for client ID: " + id + ", ignoring create request");
            return;
        }
        ghostColors.put(id, color);
    
        ObjShape s = game.getGhostShape();
        TextureImage t = game.getGhostTexture(color);
        GhostAvatar newAvatar = new GhostAvatar(game, id, s, t, p);
        newAvatar.initWateringCan(game.getWateringCanShape(), game.getWateringCanTexture());
        newAvatar.initTorch(game.getTorchShape(), game.getTorchTexture());
        newAvatar.setLocalScale(new Matrix4f(game.getAvatar().getLocalScale()));
        
        ghostAvs.add(newAvatar);
        ghostAvatars.put(id, newAvatar); 
    }
      
    /**
     * Removes a ghost avatar from the scene and internal list.
     *
     * @param id The UUID of the ghost avatar to remove.
    */
    public void removeGhostAvatar(UUID id) {
        GhostAvatar ghostAvatar = ghostAvatars.get(id); 
        if (ghostAvatar != null) {
            GameObject wateringCan = ghostAvatar.getWateringCanObject();
            GameObject torch = ghostAvatar.getTorchObject();
            if (wateringCan != null) {
                game.getEngine().getSceneGraph().removeGameObject(wateringCan);
            }
            if (torch != null) {
                game.getEngine().getSceneGraph().removeGameObject(torch);
            }
            if (ghostAvatar.getTorchLight() != null) {
                ghostAvatar.getTorchLight().disable();
            }
            game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
            ghostAvs.remove(ghostAvatar);
            ghostAvatars.remove(id); 
            ghostColors.remove(id);
            ghostCrops.remove(id);
        } else {
            System.out.println("[GhostManager] Unable to find ghost with ID: " + id);
        }
    }
        /**
     * Periodically cleans up stale or invalid ghost avatars.
     *
     * @param dtSec Elapsed time in seconds since the last update.
     */
    public void cleanGhostAvatars(float dtSec) {
        cleanupTimer += dtSec;
        if (cleanupTimer >= CLEANUP_INTERVAL) {
            cleanupTimer = 0f;
            List<GhostAvatar> toRemove = new ArrayList<>();
            for (GhostAvatar ghost : ghostAvs) {
                if (ghost == null || !ghost.getRenderStates().renderingEnabled()) { 
                    toRemove.add(ghost);
                }
            }
            for (GhostAvatar ghost : toRemove) {
                if (ghost != null) {
                    UUID id = ghost.getId();
                    System.out.println("[GhostManager] Cleaning up stale ghost avatar ID: " + id);
                    removeGhostAvatar(id);
                } else {
                    ghostAvs.remove(null); 
                    System.out.println("[GhostManager] Removed null ghost avatar entry");
                }
            }
        }
    }

    // Find a ghost avatar by ID
    private GhostAvatar findAvatar(UUID id) {
        return ghostAvatars.get(id); 
    }
    
    /**
     * Updates the position of a ghost avatar.
     * If the ghost doesn't exist yet, this method creates it.
     *
     * @param ghostID The UUID of the ghost avatar.
     * @param position The new position to set.
     */
    public void updateGhostPosition(UUID ghostID, Vector3f position) {
        GhostAvatar ghost = findAvatar(ghostID);
        
        if (ghost != null) {
            ghost.setPosition(position);
        } else {
            System.out.println("[Client] Ghost " + ghostID + " not found; creating with default color");
            try {
                String color = ghostColors.getOrDefault(ghostID, "White");
                createGhost(ghostID, position, color);            
            } catch (IOException e) {
                System.out.println("[Client] ERROR: Failed to create ghost avatar for " + ghostID);
                e.printStackTrace();
            }
        }
    }
    /**
     * Retrieves an existing ghost avatar instance.
     *
     * @param id The UUID of the ghost.
     * @return The GhostAvatar, or null if not found.
     */
    public GhostAvatar getGhostAvatar(UUID id) {
        return findAvatar(id);
    }
    
    /**  
     * Updates a ghost’s orientation. 
     * @param id The UUID of the ghost.
     * @param q  The new rotation quaternion. 
     */
    public void updateGhostRotation(UUID id, Quaternionf q) {
        GhostAvatar ghost = findAvatar(id);
        if (ghost != null) {
            Matrix4f m = new Matrix4f().rotation(q);
            ghost.setLocalRotation(m);
        }
    }
    /**
     * Sets whether a ghost avatar is currently watering.
     *
     * @param id The UUID of the ghost.
     * @param on True to show watering-can and spawn droplets.
     */
    public void setGhostWatering(UUID id, boolean on) {
        GhostAvatar g = findAvatar(id);
        if (g != null) g.setWatering(on);
    }

    /**
     * Called each frame from MyGame.update() to position watering-cans.
     */    
    public void updateAllGhostCans() {
        for (GhostAvatar g : ghostAvs) {
            if (!g.isWatering()) continue;
            GameObject can = g.getWateringCanObject();
            Vector3f fwd   = can.getWorldForwardVector().normalize();
            Vector3f right = can.getWorldRightVector().normalize();
            Vector3f up    = can.getWorldUpVector().normalize();
            Vector3f offset = fwd.mul(0.1f)
                              .add(right.mul(0.1f))
                              .add(up.mul(0.1f));
            can.setLocalTranslation(new Matrix4f().translation(offset));
            can.setLocalRotation(new Matrix4f());
        }
    }

   /**
     * Called each frame from MyGame.update() to spawn and update droplets.
     *
     * @param dtSec elapsed time in seconds since last update
     */    
    public void updateAllGhostDroplets(float dtSec) {
        UUID me = game.getProtocolClient().getClientId();
        for (GhostAvatar g : ghostAvs) {
           if (g.getId().equals(me))
               continue;
            g.updateDroplets(game.getPhysicsEngine(), dtSec, game.getWaterCubeShape());
        }
    }
    /**
     * Helper to create a new visual GameObject for a ghost crop.
     */    
    private GameObject createGhostCropObject(Vector3f pos) {
        ObjShape plantShape   = game.plantS;
        TextureImage plantTex = game.planttx;
        GameObject planted = new GameObject(GameObject.root(), plantShape, plantTex);
        planted.setLocalTranslation(new Matrix4f().translation(pos));
        planted.setLocalScale(new Matrix4f().scaling(0.02f));
        return planted;
    }

    /**
     * Handles a ghost planting action by creating a Crop and GameObject.
     *
     * @param who     The avatar UUID who planted.
     * @param cropId  The UUID of the new crop.
     * @param pos     The planting position.
     * @param type    "Carrot" or "Wheat".
    */
    public void ghostPlant(UUID who, UUID cropId, Vector3f pos, String type) {
        ObjShape targetShape = type.equals("Carrot") ? game.carrotS  : game.wheatS;
        TextureImage targetTexture = type.equals("Carrot") ? game.carrottx : game.wheattx;
        double growTimeSec = type.equals("Carrot") ? 45 : 30;
        Crop c = new Crop(type, growTimeSec, targetShape, targetTexture);
        c.setId(cropId);                     
        GameObject obj = createGhostCropObject(pos);
        c.setPlantedObject(obj);
        ghostCrops.computeIfAbsent(who, k->new ArrayList<>()).add(c);
        game.activeCrops.add(c);
    }
    
    /**
     * Handles a ghost harvesting action, removing the crop.
     *
     * @param who    The avatar UUID who harvested.
     * @param cropId The UUID of the harvested crop.
     */
    public void ghostHarvest(UUID who, UUID cropId) {
        for (List<Crop> list : ghostCrops.values()) {
            Iterator<Crop> iter = list.iterator();
            while(iter.hasNext()) {
                Crop c = iter.next();
                if (c.getId().equals(cropId)) {
                    c.markHarvested();
                    if (c.getPlantedObject() != null)
                        c.getPlantedObject().getRenderStates().disableRendering();
                    iter.remove();
                    return;
                }
            }
        }
    }
    /**
     * Handles a ghost growth event, forcing the crop to mature.
     *
     * @param who    The avatar UUID whose crop grew.
     * @param cropId The UUID of the growing crop.
    */
    public void ghostGrow(UUID who, UUID cropId, Vector3f pos, String type) {
        List<Crop> list = ghostCrops.get(who);
        if (list==null) return;
        for(Crop c:list) if(c.getId().equals(cropId)) {
          c.forceGrowNow();    
          break;
        }
    }
    /**
     * Called each frame from MyGame.update() to advance growth on all ghost crops.
    */
    public void updateAllGhostCrops() {
        for(List<Crop> list : ghostCrops.values())
            for(Crop c : list)
                c.update();
    }
    /**
     * Retrieves the color name assigned to a ghost.
     *
     * @param id The UUID of the ghost.
     * @return Color name, or "White" if not set.
    */
    public String getColor(UUID id) {
        return ghostColors.getOrDefault(id, "White");
    }
    /**
     * Toggles a ghost’s torch GameObject and its positional light.
     *
     * @param id The UUID of the ghost.
     * @param on True to enable torch.
     */
    public void setGhostTorch(UUID id, boolean on) {
        GhostAvatar g = findAvatar(id);
        if (g != null) {
            g.setTorchOn(on);                 
            if (on)   g.getTorchLight().enable();
            else      g.getTorchLight().disable();
        }
    }
    
    /**
     * Called each frame from MyGame.update() to position torch objects.
     */
    public void updateAllGhostTorches() {
        for (GhostAvatar g : ghostAvs) {
            if (!g.isTorchOn()) continue;
            GameObject t = g.getTorchObject();
            Vector3f fwd = t.getWorldForwardVector().normalize();
            Vector3f up = t.getWorldUpVector().normalize();
            Vector3f right = t.getWorldRightVector().normalize();
            Vector3f offset = fwd.mul(0.05f).add(up.mul(0.10f)).add(right.mul(0.05f));
            t.setLocalTranslation(new Matrix4f().translation(offset));
        }
    }
}
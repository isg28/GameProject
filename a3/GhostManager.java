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
 * @author 
 */
public class GhostManager {
    private MyGame game;
    private Vector<GhostAvatar> ghostAvs = new Vector<>();
    private Map<UUID, List<Crop>> ghostCrops = new HashMap<>();
    private Map<UUID,String> ghostColors = new HashMap<>();


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
        ghostColors.put(id, color);

        ObjShape s = game.getGhostShape();
        TextureImage t = game.getGhostTexture(color);
        GhostAvatar newAvatar = new GhostAvatar(game, id, s, t, p);
        newAvatar.initWateringCan(
            game.getWateringCanShape(),
            game.getWateringCanTexture()
        );
        newAvatar.initTorch(
            game.getTorchShape(),
            game.getTorchTexture()
          );
      
        // copy your player’s scale:
        newAvatar.setLocalScale(new Matrix4f(game.getAvatar().getLocalScale()));
      ghostAvs.add(newAvatar);
    }
      

    /**
     * Removes a ghost avatar from the scene and internal list.
     *
     * @param id The UUID of the ghost avatar to remove.
    */
    public void removeGhostAvatar(UUID id) {
        GhostAvatar ghostAvatar = findAvatar(id);
        if (ghostAvatar != null) {
            game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
            ghostAvs.remove(ghostAvatar); 
        } else {
            System.out.println("Unable to find ghost in list");
        }
    }

    // Find a ghost avatar by ID
    private GhostAvatar findAvatar(UUID id) {
        for (GhostAvatar ghostAvatar : ghostAvs) {
            if (ghostAvatar.getId().compareTo(id) == 0) {  
                return ghostAvatar;
            }
        }
        return null;
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
    public GhostAvatar getGhostAvatar(UUID id) {
        return findAvatar(id);
    }
    
    /**  
     * Updates a ghost’s orientation.  
     */
    public void updateGhostRotation(UUID id, Quaternionf q) {
        GhostAvatar ghost = findAvatar(id);
        if (ghost != null) {
            Matrix4f m = new Matrix4f().rotation(q);
            ghost.setLocalRotation(m);
        }
    }
    public void setGhostWatering(UUID id, boolean on) {
        GhostAvatar g = findAvatar(id);
        if (g != null) g.setWatering(on);
    }

    /** call each frame from MyGame.update() */
    public void updateAllGhostCans() {
        for (GhostAvatar g : ghostAvs) {
            if (!g.isWatering()) continue;
            GameObject can = g.getWateringCanObject();
            // same offset logic as local:
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

    /** call each frame from MyGame.update() */
    public void updateAllGhostDroplets(float dtSec) {
        UUID me = game.getProtocolClient().getClientId();
        for (GhostAvatar g : ghostAvs) {
           if (g.getId().equals(me))
               continue;
            g.updateDroplets(game.getPhysicsEngine(), dtSec, game.getWaterCubeShape());
        }
    }
    /** place this somewhere in GhostManager **/
    private GameObject createGhostCropObject(Vector3f pos) {
        // use your game’s plant shape & texture
        ObjShape plantShape   = game.plantS;
        TextureImage plantTex = game.planttx;
        GameObject planted = new GameObject(GameObject.root(), plantShape, plantTex);
        planted.setLocalTranslation(new Matrix4f().translation(pos));
        planted.setLocalScale(new Matrix4f().scaling(0.02f));
        return planted;
    }

    
    public void ghostPlant(UUID who, UUID cropId, Vector3f pos, String type) {
        ObjShape     targetShape   = type.equals("Carrot") ? game.carrotS  : game.wheatS;
        TextureImage targetTexture = type.equals("Carrot") ? game.carrottx : game.wheattx;
        double       growTimeSec   = type.equals("Carrot") ? 45            : 30;
        Crop c = new Crop(type, growTimeSec, targetShape, targetTexture);
        c.setId(cropId);                     
        GameObject obj = createGhostCropObject(pos);
        c.setPlantedObject(obj);
        ghostCrops.computeIfAbsent(who, k->new ArrayList<>()).add(c);
        game.activeCrops.add(c);
    }
    
    public void ghostHarvest(UUID who, UUID cropId) {
        // find and remove that cropId no matter who planted it
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
    public void ghostGrow(UUID who, UUID cropId, Vector3f pos, String type) {
        List<Crop> list = ghostCrops.get(who);
        if (list==null) return;
        for(Crop c:list) if(c.getId().equals(cropId)) {
          c.forceGrowNow();    // or: replace its plantedObject with grown shape/tex
          break;
        }
    }
    public void updateAllGhostCrops() {
        for(List<Crop> list : ghostCrops.values())
            for(Crop c : list)
                c.update();
    }
    public String getColor(UUID id) {
        return ghostColors.getOrDefault(id, "White");
    }
    /** toggle a ghost’s torch GameObject on/off */
    public void setGhostTorch(UUID id, boolean on) {
        GhostAvatar g = findAvatar(id);
        if (g != null) {
            g.setTorchOn(on);                 
            if (on)   g.getTorchLight().enable();
            else      g.getTorchLight().disable();
        }
    }
    
    /** call each frame from MyGame.update() */
    public void updateAllGhostTorches() {
        for (GhostAvatar g : ghostAvs) {
            if (!g.isTorchOn()) continue;
            GameObject t = g.getTorchObject();
            Vector3f fwd   = t.getWorldForwardVector().normalize();
            Vector3f up    = t.getWorldUpVector().normalize();
            Vector3f right = t.getWorldRightVector().normalize();
            Vector3f offset = fwd.mul(0.05f).add(up.mul(0.10f)).add(right.mul(0.05f));
            t.setLocalTranslation(new Matrix4f().translation(offset));

        }
    }
    

    
    
    
}
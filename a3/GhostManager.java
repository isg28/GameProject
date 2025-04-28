package a3;

import java.io.IOException;
import java.util.UUID;
import java.util.Vector;
import java.util.Iterator;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import tage.VariableFrameRateGame;

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
    public void createGhost(UUID id, Vector3f p) throws IOException {
        ObjShape s = game.getGhostShape();
        TextureImage t = game.getGhostTexture();
        GhostAvatar newAvatar = new GhostAvatar(id, s, t, p);
        newAvatar.initWateringCan(
            game.getWateringCanShape(),
            game.getWateringCanTexture()
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
            System.out.println("[Client] Ghost with ID " + ghostID + " not found. Creating new ghost...");
            try {
                createGhost(ghostID, position);
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
    
    
    
    
}
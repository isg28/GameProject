package a3;

import java.io.IOException;
import java.util.UUID;
import java.util.Vector;
import java.util.Iterator;

import org.joml.Matrix4f;  
import org.joml.Vector3f;
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
        GhostAvatar newAvatar = new GhostAvatar(id, s, null, p); // No texture, just color
    
        // Assign a unique color based on UUID
        Vector3f uniqueColor = game.getUniqueColorForClient(id);
        newAvatar.getRenderStates().setColor(uniqueColor);
        newAvatar.getRenderStates().setHasSolidColor(true); // Enable solid color rendering
    
        Matrix4f initialScale = new Matrix4f().scaling(0.25f);
        newAvatar.setLocalScale(initialScale);
    
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
    
}

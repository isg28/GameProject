package a3;
import org.joml.Vector3f; 
import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import org.joml.Matrix4f;
import java.util.UUID;

/**
 * Represents a ghost avatar in the game world, which is a visual representation of another player's avatar.
 * <p>
 * A GhostAvatar is a subclass of {@link GameObject} and includes a unique identifier (UUID)
 * to distinguish between different players in a multiplayer session.
 * It holds information about its shape, optional texture, and 3D position.
 * </p>  
 * 
 *  @author 
 */

public class GhostAvatar extends GameObject {
    private UUID id;

    /**
     * Constructs a new GhostAvatar.
     *
     * @param id The unique identifier for this ghost avatar (usually the remote client's UUID).
     * @param s The shape of the avatar.
     * @param t The texture image to apply (can be null if using color).
     * @param p The initial 3D position of the avatar in the world.
    */
    public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p) {
        super(GameObject.root(), s, t);
        this.id = id;
        setPosition(p);  
    }

    /**
     * Retrieves the UUID associated with the ghost avatar.
     *
     * @return The UUID of the ghost.
    */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the UUID for this ghost avatar.
     *
     * @param id The new UUID to assign.
    */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the current position of the ghost avatar in world space.
     *
     * @return A Vector3f representing the current position.
    */
    public Vector3f getPosition() {
        Matrix4f transform = getLocalTranslation();
        Vector3f position = new Vector3f();
        transform.getTranslation(position);  
        return position;
    }

    /**
     * Sets the position of the ghost avatar in world space.
     *
     * @param p The new position to apply.
    */
    public void setPosition(Vector3f p) {
        Matrix4f transform = new Matrix4f();
        transform.translation(p.x, p.y, p.z);  
        setLocalTranslation(transform);
    }
}

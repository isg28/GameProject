package a3;

import java.util.HashMap;
import tage.TextureImage;
import tage.GameObject;

/**
 * Handles dynamic management of terrain textures in the game.
 * 
 * @author
 */
public class TerrainManager {
    private GameObject terrain;
    private HashMap<String, TextureImage> textureMap;

    /**
     * Constructs a new TerrainManager with a given terrain GameObject.
     *
     * @param terrainObj The GameObject representing the terrain.
    */
    public TerrainManager(GameObject terrainObj) {
        this.terrain = terrainObj;
        this.textureMap = new HashMap<>();
    }

    /**
     * Registers a texture under a given name (typically matching skybox names).
     * @param name     The unique key/name for the texture.
     * @param texture  The texture to associate with the name.
     */
    public void registerTerrainTexture(String name, TextureImage texture) {
        textureMap.put(name, texture);
    }

    /**
     * Applies a registered texture to the terrain based on its name.
     *
     * @param name The name of the texture to apply.
     */
    public void setTerrainTexture(String name) {
        TextureImage tex = textureMap.get(name);
        if (tex != null && terrain != null) {
            terrain.setTextureImage(tex);
            System.out.println("[TerrainManager] Set terrain texture to: " + name);
        } else {
            System.out.println("[TerrainManager] Warning: Texture not found for name: " + name);
        }
    }
}

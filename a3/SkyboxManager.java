package a3;

import tage.SceneGraph;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the skybox and terrain transitions in the game environment.
 * <p>
 * SkyboxManager cycles through a predefined list of skybox textures
 * to simulate time-of-day transitions (e.g., day, evening, sunset, night).
 * It also updates the terrain texture to match the skybox for visual consistency.
 * </p>
 *
 * <p>The manager automatically cycles through skyboxes every 30 seconds by default.
 * Once the "night" skybox is reached, the cycle stops unless reset manually.</p>
 *
 * <p>This class is also responsible for broadcasting skybox changes to other clients
 * in a multiplayer game using {@link MyGame#getProtocolClient()}.</p>
 *
 * @author 
 */
public class SkyboxManager {
    private final String[] skyboxes = {
        "dayOne", "dayTwo", "dayThree", "dayFour",
        "eveningOne", "eveningTwo", "sunset", "night"
    };

    private final long interval = 30000; // 30 secs WILL CHANGE LATER
    private long lastSwitchTime = System.currentTimeMillis();
    private boolean isCycling = true;
    private int currentIndex = 0;

    private final SceneGraph sceneGraph;
    private final TerrainManager terrainManager;
    private final MyGame game;

    private final Map<String, Integer> skyboxTextures = new HashMap<>();

    /**
     * Constructs a new SkyboxManager and initializes the first skybox.
     *
     * @param sg   The scene graph used to apply skybox textures.
     * @param tm   The terrain manager used to sync terrain with the skybox.
     * @param game The game instance for network communication.
    */
    public SkyboxManager(SceneGraph sg, TerrainManager tm, MyGame game) {
        this.sceneGraph = sg;
        this.terrainManager = tm;
        this.game = game;
    
        preloadSkyboxes();
        loadAndSetSkybox(currentIndex);
    }
    // Preloads all cube map textures associated with each skybox name.
    // Stores them in a map for fast lookup when switching.
    private void preloadSkyboxes() {
        for (String name : skyboxes) {
            int texId = sceneGraph.loadCubeMap(name);
            skyboxTextures.put(name, texId);
        }
    }

    /**
     * Should be called once per frame.
     * Handles automatic skybox transitions based on elapsed time.
    */
    public void update() {
        if (!isCycling) return;
        if (System.currentTimeMillis() - lastSwitchTime >= interval) {
            advanceSkybox();
            lastSwitchTime = System.currentTimeMillis();
        }
    }
    // When the "night" skybox is reached, automatic cycling stops.
    private void advanceSkybox() {
        if (currentIndex < skyboxes.length - 1) {
            currentIndex++;
            loadAndSetSkybox(currentIndex);
            if (skyboxes[currentIndex].equals("night")) isCycling = false;
    
            game.getProtocolClient().sendSkyboxIndex(currentIndex);

        }
    }

    /**
     * Resets the skybox cycle back to the beginning ("dayOne").
     * Also resumes automatic cycling and broadcasts the change.
    */
    public void resetCycle() {
        currentIndex = 0;
        isCycling = true;
        lastSwitchTime = System.currentTimeMillis();
        loadAndSetSkybox(currentIndex);
    
        game.getProtocolClient().sendSkyboxIndex(currentIndex);
    }
    
    /**
     * Loads the specified skybox by index and applies it to the scene.
     * Also syncs the terrain texture with the skybox.
     *
     * @param index The index of the skybox to load.
    */
    private void loadAndSetSkybox(int index) {
        String name = skyboxes[index];  // get current skybox name
    
        int skyboxTex = skyboxTextures.get(name);
        sceneGraph.setActiveSkyBoxTexture(skyboxTex);
        sceneGraph.setSkyBoxEnabled(true);
        sceneGraph.getSkyBoxObject().getRenderStates().setTiling(1);
        sceneGraph.getSkyBoxObject().getRenderStates().setTileFactor(4);
    
        // Sync terrain to match the skybox
        if (terrainManager != null) {
            terrainManager.setTerrainTexture(name);  // pass matching key
        }
    
        System.out.println("[SkyboxManager] Switched to: " + name);
    }
    /**
     * Manually sets the skybox using an index from the skybox array.
     * Also updates the terrain and stops cycling if the skybox is "night".
     *
     * @param index The index of the skybox in the array.
     */
    public void setSkyboxByIndex(int index) {
        if (index >= 0 && index < skyboxes.length) {
            currentIndex = index;
            isCycling = !skyboxes[currentIndex].equals("night");
            loadAndSetSkybox(currentIndex);
        }
    }
    
    
}

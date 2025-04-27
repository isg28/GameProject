package a3;

import tage.*;
import tage.shapes.*;
import java.lang.Math;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.awt.event.*;
import java.io.IOException;

import org.joml.*;
import org.joml.sampling.BestCandidateSampling.Quad;

import net.java.games.input.Event;
import tage.input.*;
import tage.input.action.AbstractInputAction;
import tage.networking.IGameConnection.ProtocolType;
import tage.networking.server.ProtocolClient;
import tage.nodeControllers.BouncingController;
import tage.nodeControllers.RotationController;
import tage.nodeControllers.StretchController;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.rml.Vector3;

/**
 * MyGame is the main entry point for the game application and manages core gameplay mechanics.
 * <p>
 * While currently a prototype, this evolving project is designed as a 3D farming simulation,
 * featuring environmental interactions, terrain systems, and multiplayer support.
 * 
 * Current features include:
 * <ul>
 *   <li>Real-time player control using keyboard/gamepad input</li>
 *   <li>Dynamic terrain and skybox transitions simulating time of day</li>
 *   <li>Camera orbit system and multi-viewport support</li>
 *   <li>Multiplayer ghost avatars with position syncing</li>
 *   <li>Networking setup for joining, leaving, and syncing skybox state</li>
 * </ul>
 * 
 * Planned features will include planting systems, animals, and crop mechanics.
 * 
 */


public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	private boolean paused=false;
	private int counter=0;
	private double lastFrameTime, currFrameTime, elapsTime;

	protected GameObject dol, avatar, x, y, z, terr, pig, chicken, rabbit, carrot, home, tree, plant, market, wheat, wateringcan;
	protected ObjShape dolS, linxS, linyS, linzS, ghostS, terrS, borderShape, pigS, chickenS, rabbitS, carrotS, homeS, treeS, plantS, marketS,
																wheatS, wateringcanS, waterCubeS;
	protected TextureImage doltx, pigtx, chickentx, rabbittx, carrottx, hometx, treetx, planttx, markettx, wheattx, wateringcantx;
	private Light light1; 

	private InputManager im;
	private Camera mainCamera;
	private TurnAction turnAction;
	private final float movementSpeed = 0.05f;
	private CameraOrbit3D orbitController;
	private Vector3f rightViewportOffset;
	private Vector3f leftViewportOffset;
    private TextureImage ghostT, hills, dayOneTerrain, dayTwoTerrain, dayThreeTerrain, dayFourTerrain, 
								eveningOneTerrain, eveningTwoTerrain, sunsetTerrain, nightTerrain;
	private boolean linesVisible = true; 
	private StretchController stretchController;
	private BouncingController bouncingController;
	private int dayLand, nightMode;

	private GhostManager gm;
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isConnected = false;
	private SkyboxManager skyboxManager;
	private boolean shouldResetSkybox = false;
	private TerrainManager terrainManager;
	private Integer pendingSkyboxIndex = null;  
	private int coins = 10;
	private String[] inventory = new String[5];
	private int inventoryCount = 0;
	private List<Crop> activeCrops = new ArrayList<>();
	private Vector3f plantedPos = new Vector3f(0, 0, -2); // Just an example location
	private boolean isBuyingSeeds = false;
	private String selectedSeedType = "";
	public static enum MarketMode { NONE, CHOOSING, BUYING, SELLING }
	private MarketMode marketMode = MarketMode.NONE;
	private boolean showNotEnoughCoinsMessage = false;
	private boolean shouldAttachWateringCan = false;
	private boolean shouldDetachWateringCan = false;
	private List<Crop> cropsToRemove = new ArrayList<>();
	private List<GameObject> objectsToDisable = new ArrayList<>();
	private boolean initializationComplete = false;
	private List<Runnable> renderStateQueue = new ArrayList<>(); // New queue for render state changes
	private boolean isWatering = false; 
	private List<GameObject> waterDroplets = new ArrayList<>();
	private List<PhysicsObject> waterDropletPhysics = new ArrayList<>();
	PhysicsEngine physicsEngine;
	private static final float DROP_INTERVAL = 0.05f;
	private float spawnTimer = 0f;
	private List<Boolean> waterGrounded   = new ArrayList<>();
	private List<Float>   waterGroundTime = new ArrayList<>();
	private static final float DROPLET_LIFETIME = 2.0f;  // seconds
	private List<Boolean> hasBouncedOffPlant = new ArrayList<>();
	private boolean avatarPhysicsActive = false;
	private PhysicsObject avatarPhysicsObject = null;
	private long physicsActivateTime = 0;
	private static final long PHYSICS_DURATION_MS = 1000; // Physics active duration
	private boolean isFaceDown = false;
	private long faceDownStartTime = 0;
	private static final long FACE_DOWN_DURATION_MS = 2000;
	private ChickenAnimationController chickenController; 
	private PigAnimationController pigController;
	private PlantAnimationController plantController;
	private float deltaTime = 0f; // Delta time in milliseconds
    private long lastUpdateTime = System.nanoTime();
	private List<PlantAnimationController> plantControllers = new ArrayList<>();



	/**
    * Constructs the game instance and initializes the game loop.
    */
    public MyGame() {
        super();
        // Initialize physicsEngine in constructor
        physicsEngine = new tage.physics.JBullet.JBulletPhysicsEngine();
        physicsEngine.initSystem();
        physicsEngine.setGravity(new float[]{0f, -9.8f, 0f});
    }
	
    public MyGame(String serverAddress, int serverPort, String protocol) {
        super();
        gm = new GhostManager(this);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        if (protocol.toUpperCase().compareTo("TCP") == 0)
            this.serverProtocol = ProtocolType.TCP;
        else
            this.serverProtocol = ProtocolType.UDP;
        // Initialize physicsEngine in constructor
        physicsEngine = new tage.physics.JBullet.JBulletPhysicsEngine();
        physicsEngine.initSystem();
        physicsEngine.setGravity(new float[]{0f, -9.8f, 0f});
    }

	/**
     * Initializes the game's lighting system.
     */
	@Override
	public void initializeLights() {
		Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
	
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		light1.setDiffuse(1.0f, 1.0f, 1.0f);
		light1.setAmbient(0.3f, 0.3f, 0.3f);
		engine.getSceneGraph().addLight(light1);
	
		for (int i = 0; i < 5; i++) {
			Light dummyLight = new Light();
			dummyLight.setLocation(new Vector3f(0, -10, 0)); 
			dummyLight.setDiffuse(0, 0, 0);
			dummyLight.setAmbient(0, 0, 0);
			engine.getSceneGraph().addLight(dummyLight);
		}
	}
	


	/**
     * Main constructor for the game.
    */
	public static void main(String[] args)
	{	
		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.initializeLights();
		game.game_loop();
	}
	/**
	 * Loads 3D models for key scene elements, including the player avatar and axis lines.
	 * This is where future shapes for crops, tools, and animals will be loaded.
	 */
	@Override
	public void loadShapes() {
		dolS = new ImportedModel("dolphinHighPoly.obj");
		pigS = new AnimatedShape("pig.rkm", "pig.rks");
		chickenS = new AnimatedShape("chicken.rkm", "chicken.rks");		
		rabbitS = new ImportedModel("rabbit.obj");
		carrotS = new ImportedModel("carrot.obj");
		homeS = new ImportedModel("home.obj");
		marketS = new ImportedModel("market.obj");
		plantS = new AnimatedShape("plant.rkm", "plant.rks");
		wheatS = new ImportedModel("wheat.obj");
	
		// Robustly load watering can
		try {
			wateringcanS = new ImportedModel("watercan.obj");
			if (wateringcanS == null || 
				wateringcanS.getVertices() == null || wateringcanS.getVertices().length == 0 ||
				wateringcanS.getTexCoords() == null || wateringcanS.getTexCoords().length == 0 ||
				wateringcanS.getNormals() == null || wateringcanS.getNormals().length == 0) {
				System.err.println("Watering can model is invalid (null or empty vertices/texcoords/normals) - using Cube fallback");
				wateringcanS = new Cube();
			}
		} catch (Exception e) {
			System.err.println("Error loading watering can model: " + e.getMessage() + " - using Cube fallback");
			wateringcanS = new Cube();
		}

		try {
            waterCubeS = new Sphere();
            if (waterCubeS == null || 
                waterCubeS.getVertices() == null || waterCubeS.getVertices().length == 0 ||
                waterCubeS.getTexCoords() == null || waterCubeS.getTexCoords().length == 0 ||
                waterCubeS.getNormals() == null || waterCubeS.getNormals().length == 0) {
                System.err.println("Sphere shape is invalid - using Cube fallback");
                waterCubeS = new Cube();
            }
        } catch (Exception e) {
            System.err.println("Error loading Sphere shape: " + e.getMessage() + " - using Cube fallback");
            waterCubeS = new Cube();
        }
	
		linxS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(10f, 0f, 0f));
		linyS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 10f, 0f));
		linzS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, -10f));
		ghostS = new Sphere();
		terrS = new TerrainPlane(1000);
		borderShape = new Torus();
		if (borderShape == null || borderShape.getVertices() == null || borderShape.getVertices().length == 0) {
			System.err.println("Border shape failed to load correctly. Using Cube instead.");
			borderShape = new Cube();
		}
	}
    /**
     * Loads and assigns texture images to various objects in the game.
     * Includes textures for the dolphins & terrain.
     */
	@Override
	public void loadTextures()
	{	
		doltx = new TextureImage("Dolphin_HighPolyUV.png");
		pigtx = new TextureImage("pigtx2.jpg");
		chickentx = new TextureImage("chickentx.jpg");
		rabbittx = new TextureImage("rabbittx.jpg");
		carrottx = new TextureImage("carrottx2.jpg");
		hometx = new TextureImage("hometx.jpg");
		markettx = new TextureImage("markettx.jpg");
		//treetx = new TextureImage("treetx.jpg");
		planttx = new TextureImage("planttx.jpg");
		wheattx = new TextureImage("wheattx.jpg");
		wateringcantx = new TextureImage("watercantx.jpg");
		
		dayOneTerrain = new TextureImage("dayOneTerrain.jpg");
		dayTwoTerrain = new TextureImage("dayTwoTerrain.jpg");
		dayThreeTerrain = new TextureImage("dayThreeTerrain.jpg");
		dayFourTerrain = new TextureImage("dayFourTerrain.jpg");
		eveningOneTerrain = new TextureImage("eveningOneTerrain.jpg");
		eveningTwoTerrain = new TextureImage("eveningTwoTerrain.jpg");
		sunsetTerrain = new TextureImage("sunsetTerrain.jpg");
		nightTerrain = new TextureImage("nightTerrain.jpg");

		try {
			hills = new TextureImage("hills.jpg");
		} catch (Exception e) {
			System.err.println("Failed to load hills.jpg: " + e.getMessage() + " - height map will be disabled");
			hills = null;
		}		

	}
	
	/**
	 * Constructs and initializes scene objects, including the avatar, terrain, and axis indicators.
	 * Currently sets up the terrain with support for dynamic texture updates.
	 */
	@Override
	public void buildObjects()
	{	
		System.out.println("Starting buildObjects...");
		Matrix4f initialTranslation, initialScale;
		if (engine == null) {
			System.out.println("Engine not initialized yet.");
			return;
		}
		stretchController = new StretchController(2000); 
		engine.getSceneGraph().addNodeController(stretchController);
		bouncingController = new BouncingController(2000); 
		engine.getSceneGraph().addNodeController(bouncingController);

/*  		dol = new GameObject(GameObject.root(), dolS, doltx);
		initialTranslation = (new Matrix4f()).translation(0,0,0);
		dol.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.25f));
		dol.setLocalTranslation(initialTranslation);
		dol.setLocalScale(initialScale);  */

		pig = new GameObject(GameObject.root(), pigS, pigtx);
		initialTranslation = (new Matrix4f()).translation(2,0,0);
		pig.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.1f));
		pig.setLocalTranslation(initialTranslation);
		pig.setLocalScale(initialScale);

		// Add physics object for pig
		Vector3f pigPos = pig.getWorldLocation();
		double[] pigXform = {
				1, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 1, 0,
				pigPos.x(), pigPos.y(), pigPos.z(), 1
		};
		
		PhysicsObject pigPhys = physicsEngine.addSphereObject(
			physicsEngine.nextUID(),
			0f, // Static
			pigXform,
			0.3f // Radius
		);
		pig.setPhysicsObject(pigPhys);

		chicken = new GameObject(GameObject.root(), chickenS, chickentx);
		initialTranslation = (new Matrix4f()).translation(0,0,1);
		chicken.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.1f));
		chicken.setLocalTranslation(initialTranslation);
		chicken.setLocalScale(initialScale);
		
		chickenController = new ChickenAnimationController(chicken, this); // Assign to field
		chickenController.setEnabled(true);

 		// Add physics object for chicken
		Vector3f chickenPos = chicken.getWorldLocation();
		double[] chickenXform = {
					1, 0, 0, 0,
					0, 1, 0, 0,
					0, 0, 1, 0,
					chickenPos.x(), chickenPos.y(), chickenPos.z(), 1
		};
		PhysicsObject chickenPhys = physicsEngine.addSphereObject(
				physicsEngine.nextUID(),
				0f, // Static
				chickenXform,
				0.3f // Radius
			);
		chicken.setPhysicsObject(chickenPhys); 

		rabbit = new GameObject(GameObject.root(), rabbitS, rabbittx);
		initialTranslation = (new Matrix4f()).translation(0,0 ,2);
		rabbit.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.1f));
		rabbit.setLocalTranslation(initialTranslation);
		rabbit.setLocalScale(initialScale);

		home = new GameObject(GameObject.root(), homeS, hometx);
		initialTranslation = (new Matrix4f()).translation(-4,0,0);
		home.setLocalTranslation(initialTranslation);
		Matrix4f rotateHome = new Matrix4f().rotateY((float) Math.toRadians(180));
		home.setLocalRotation(rotateHome);
		initialScale = (new Matrix4f().scaling(0.1f));
		home.setLocalTranslation(initialTranslation);
		home.setLocalScale(initialScale);

		Vector3f homePos = home.getWorldLocation();

		// 2) build a column-major 4×4 = 16-entry transform array
		double[] homeXform = {
			1, 0, 0, 0,    // col 0
			0, 1, 0, 0,    // col 1
			0, 0, 1, 0,    // col 2
			homePos.x(), homePos.y(), homePos.z(), 1  // col 3 (translation)
		};
		float[] homeHalfExtents = { 0.5f, 1.0f, 0.5f };  
		PhysicsObject homePhys = physicsEngine.addBoxObject(
			physicsEngine.nextUID(),
			0f,              // zero mass = static
			homeXform,
			homeHalfExtents
		);
		home.setPhysicsObject(homePhys);


/*     	tree = new GameObject(GameObject.root(), treeS, treetx);
		initialTranslation = (new Matrix4f()).translation(1,0,-1);
		tree.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.1f));
		tree.setLocalTranslation(initialTranslation);
		tree.setLocalScale(initialScale);   

		tree.getRenderStates().setTiling(1);
		tree.getRenderStates().setTileFactor(4);  */

		market = new GameObject(GameObject.root(), marketS, markettx);

		initialTranslation = new Matrix4f().translation(-2, 0, -1);
		market.setLocalTranslation(initialTranslation);
		Matrix4f rotation = new Matrix4f().rotateY((float) Math.toRadians(90));
		market.setLocalRotation(rotation);
		initialScale = new Matrix4f().scaling(0.2f);
		market.setLocalScale(initialScale);

		Vector3f marketPos = market.getWorldLocation();
		double[] marketXform = {
			1, 0, 0, 0,       // column 0
			0, 1, 0, 0,       // column 1
			0, 0, 1, 0,       // column 2
			marketPos.x(),    // column 3 (translation.x)
			marketPos.y(),    // column 3 (translation.y)
			marketPos.z(),    // column 3 (translation.z)
			1                 // column 3 (w)
		};
		float[] marketHalfExtents = { 0.5f, 1f, 0.5f };
		PhysicsObject marketPhys = physicsEngine.addBoxObject(
			physicsEngine.nextUID(),
			0f,              // static
			marketXform,
			marketHalfExtents
		);
		market.setPhysicsObject(marketPhys);		

		wateringcan = new GameObject(rabbit, wateringcanS, wateringcantx);
		if (wateringcanS == null || wateringcanS.getVertices() == null || wateringcanS.getVertices().length == 0) {
			System.err.println("Watering can shape invalid at initialization, using Cube fallback");
			wateringcan.setShape(new Cube());
		}
		wateringcan.setLocalTranslation(new Matrix4f().translation(0.1f, 0.1f, 0.1f));
		// Set no rotation (identity matrix) to prevent rotation
		wateringcan.setLocalRotation(new Matrix4f()); // Identity matrix, no rotation
		wateringcan.setLocalScale(new Matrix4f().scaling(0.4f));
		wateringcan.getRenderStates().disableRendering();

		x = new GameObject(GameObject.root(), linxS);
		y = new GameObject(GameObject.root(), linyS);
		z = new GameObject(GameObject.root(), linzS);
		(x.getRenderStates()).setColor(new Vector3f(1f,0f,0f));
		(y.getRenderStates()).setColor(new Vector3f(0f,1f,0f));
		(z.getRenderStates()).setColor(new Vector3f(0f,0f,1f));


		// Terrain setup
		terr = new GameObject(GameObject.root(), terrS, dayOneTerrain);
		Matrix4f terrTrans = new Matrix4f().translation(0f, 0f, 0f);
		Matrix4f terrScale = new Matrix4f().scaling(15.0f, 0.3f, 20.0f);
		terr.setLocalTranslation(terrTrans);
		terr.setLocalScale(terrScale);
		terr.setHeightMap(hills);

		terr.getRenderStates().setTiling(1);
		terr.getRenderStates().setTileFactor(4);

		// Optional tiling
		terr.getRenderStates().setTiling(1);
		terr.getRenderStates().setTileFactor(2);

		terrainManager = new TerrainManager(terr);
		terrainManager.registerTerrainTexture("dayOne", dayOneTerrain);
		terrainManager.registerTerrainTexture("dayTwo", dayTwoTerrain);
		terrainManager.registerTerrainTexture("dayThree", dayThreeTerrain);
		terrainManager.registerTerrainTexture("dayFour", dayFourTerrain);
		terrainManager.registerTerrainTexture("eveningOne", eveningOneTerrain);
		terrainManager.registerTerrainTexture("eveningTwo", eveningTwoTerrain);
		terrainManager.registerTerrainTexture("sunset", sunsetTerrain);
		terrainManager.registerTerrainTexture("night", nightTerrain);

		// Replace your current terrain physics setup with this:
		double[] planeTransform = new double[] {
			1, 0, 0, 0,  // First row
			0, 1, 0, 0,  // Second row
			0, 0, 1, 0,  // Third row
			0, 0, 0, 1   // Fourth row
		};

		PhysicsObject terrainPhys = physicsEngine.addStaticPlaneObject(
			physicsEngine.nextUID(),
			planeTransform,
			new float[]{0, 1, 0},  // Up vector
			0f                     // Plane constant
		);
		terr.setPhysicsObject(terrainPhys);
		// Create border toruses around the terrain edges
		for (float x = -15; x <= 15; x += 3f) {
			createBorderTorus(x, 0, -15); // back
			createBorderTorus(x, 0, 15);  // front
		}
		for (float z = -12; z <= 12; z += 3f) {
			createBorderTorus(-15, 0, z); // left
			createBorderTorus(15, 0, z);  // right
		}
		avatar = rabbit;

		// Queue rendering enablement for after initialization
		renderStateQueue.add(() -> {
			pig.getRenderStates().enableRendering();
			chicken.getRenderStates().enableRendering();
			rabbit.getRenderStates().enableRendering();
			home.getRenderStates().enableRendering();
			market.getRenderStates().enableRendering();
			terr.getRenderStates().enableRendering();
			x.getRenderStates().enableRendering();
			y.getRenderStates().enableRendering();
			z.getRenderStates().enableRendering();
		});

		System.out.println("Finished buildObjects, activeCrops size: " + activeCrops.size());

	}

	/**
	 * Configures the game's runtime state, including input handling, camera setup,
	 * and network client initialization. This method also associates keyboard and gamepad
	 * inputs to control movement and actions within the game world.
	*/
	@Override
	public void initializeGame()
	{
		System.out.println("Starting initializeGame...");
		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		setupNetworking();
		inventory[0] = "Seed_Wheat";
		inventoryCount = 1;
		pigController = new PigAnimationController(pig, this);
		pigController.setEnabled(true);



		// ------------- initialize camera -------------
		im = engine.getInputManager();
		Camera c = engine.getRenderSystem().getViewport("MAIN").getCamera();
		orbitController = new CameraOrbit3D(c, avatar, engine);

		Camera mini = (engine.getRenderSystem().getViewport("LEFT").getCamera());
		orbitController = new CameraOrbit3D(mini, avatar, engine);


		// ----------------- INPUTS SECTION -----------------------------
		FwdAction fwdAction = new FwdAction(this, protClient);
		turnAction = new TurnAction(this);
		
		im.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._1, 
				fwdAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.Y, 
				fwdAction, 
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._0, 
				fwdAction, 
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._1, 
				fwdAction, 
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.W, 
				fwdAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.S, 
				fwdAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.A, 
			turnAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.D, 
			turnAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.X, 
			turnAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.X, 
			turnAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.RY, 
			turnAction, 
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.RX, turnAction, 
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.LEFT,
			new AbstractInputAction() {
				public void performAction(float time, Event event) {
					orbitController.orbitAzimuth(-2.0f); 
				}
			},
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.RIGHT,
			new AbstractInputAction() {
				public void performAction(float time, Event event) {
					orbitController.orbitAzimuth(2.0f); 
				}
			},
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

			im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.UP,
				new AbstractInputAction() {
					public void performAction(float time, Event event) {
						orbitController.orbitElevation(2.0f);
					}
				},
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
			);
			im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.DOWN,
				new AbstractInputAction() {
					public void performAction(float time, Event event) {
						orbitController.orbitElevation(-2.0f); 
					}
				},
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
			);
				im.associateActionWithAllKeyboards(
					net.java.games.input.Component.Identifier.Key.COMMA,
					new AbstractInputAction() {
						public void performAction(float time, Event event) {
							orbitController.zoom(-0.5f); 
						}
					},
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
				);
				im.associateActionWithAllKeyboards(
					net.java.games.input.Component.Identifier.Key.PERIOD,
					new AbstractInputAction() {
						public void performAction(float time, Event event) {
							orbitController.zoom(0.5f); 
						}
					},
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
				);

		synchronized (renderStateQueue) {
			for (Runnable task : renderStateQueue) {
				task.run();
			}
			renderStateQueue.clear();
		}

        initializationComplete = true; // Mark initialization as complete
		System.out.println("Finished initializeGame, rendering enabled");
	}

	private void setupNetworking() {
		isConnected = false;
		try { 
			protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} catch (IOException e) { 
			e.printStackTrace();
		}
		
		if (protClient == null) {
			System.out.println("Missing protocol client! Unable to send network messages.");
		} else {
			protClient.sendJoinMessage();
			isConnected = true;
		}
	}
	
	/**
	 * Updates the game logic every frame. Handles avatar movement, HUD updates,
	 * camera positioning, terrain height adjustment, and multiplayer state.
	 * Will later be extended to include simulation logic for farming mechanics.
	 */
	@Override
	public void update()
	{
		long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastUpdateTime) / 1_000_000f; // Convert nanoseconds to milliseconds
        lastUpdateTime = currentTime;

		if (!initializationComplete) {
            return; // Skip updates until initialization is complete
        }

		if (skyboxManager == null && terrainManager != null) {
			skyboxManager = new SkyboxManager(engine.getSceneGraph(), terrainManager, this);
			return; 
		}
	
		if (skyboxManager != null) {
			skyboxManager.update();
		}
	
		if (pendingSkyboxIndex != null) {
			skyboxManager.setSkyboxByIndex(pendingSkyboxIndex);
			pendingSkyboxIndex = null;
		}	

		if (chickenController != null) {
			chickenController.update(deltaTime);
		} else {
			System.out.println("ChickenAnimationController is null!");
		}

		if (pigController != null) {
			pigController.update(deltaTime);
		}
		((AnimatedShape) pig.getShape()).updateAnimation();


		
		for (PlantAnimationController controller : plantControllers) {
			controller.update(deltaTime);
		}
		

		// Handle terrain height adjustment only if physics is not active
		if (terr != null && !avatarPhysicsActive && !isFaceDown) {
			Vector3f loc = avatar.getWorldLocation();
			float targetY = 0.1f;
			if (terr.getHeightMap() != null) {
				try {
					float height = terr.getHeight(loc.x(), loc.z());
					targetY = height + 0.1f;
				} catch (Exception e) {
					System.err.println("Error getting terrain height: " + e.getMessage());
				}
			}
			float currentY = loc.y();
			if (Math.abs(currentY - targetY) > 0.01f) {
				avatar.setLocalLocation(new Vector3f(loc.x(), targetY, loc.z()));
			}
		}
		
		// Update watering can position to maintain consistent offset
		if (wateringcan != null && wateringcan.getRenderStates().renderingEnabled()) {
			Vector3f rabbitForward = rabbit.getWorldForwardVector().normalize();
			Vector3f rabbitRight = rabbit.getWorldRightVector().normalize();
			Vector3f rabbitUp = rabbit.getWorldUpVector().normalize();
			// Desired offset: slightly in front (0.15f), to the right (0.1f), and above (0.1f)
			Vector3f offset = rabbitForward.mul(0.1f)
							.add(rabbitRight.mul(0.1f))
							.add(rabbitUp.mul(0.1f));
			wateringcan.setLocalTranslation(new Matrix4f().translation(offset));
			wateringcan.setLocalRotation(new Matrix4f()); // Ensure no rotation
		}

		if (shouldResetSkybox && skyboxManager != null) {
			skyboxManager.resetCycle();
			shouldResetSkybox = false;
		}
		

		Vector3f loc, fwd, up, right, newLocation;
		Camera cam;

		if (orbitController != null) {
			orbitController.updateCameraPosition();
		}

		loc = avatar.getWorldLocation() ;
 		fwd = avatar.getWorldForwardVector();
		up = avatar.getWorldUpVector();
		right = avatar.getWorldRightVector();
	
		Vector3f pos = avatar.getWorldLocation();
		float distToMarket = pos.distance(market.getWorldLocation());
		float distToHome   = pos.distance(home  .getWorldLocation());
		
		String hudMessage;
		if (isWatering) {
			hudMessage = "Status: Watering Crops";
		}
		else if (distToHome < 1.0f) {
			hudMessage = "Status: Near the House";
		}
		else if (distToMarket < 1.0f) {
			hudMessage = "Status: Near the Market";
		}
		else {
			hudMessage = "Status: Roaming the Fields";
		}
		
		im.update((float)elapsTime);
		processNetworking((float)elapsTime);

		// build and set HUD
    	String dispStr1 = "Objective: Buy seeds, plant crops with E, harvest with H, and sell them at the market to earn coins!";
		String dispStr3 = "Surveillance Camera on Dolphin";
		String coinStr = "Coins: " + coins;

		Vector3f hud1Color = new Vector3f(1,0,0);
		Vector3f hud2Color = new Vector3f(0,0,1);
		Vector3f hud3Color = new Vector3f(0, 0, 1);
		Vector3f hud4Color = new Vector3f(0, 1, 0);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 20, 20);
		(engine.getHUDmanager()).setHUD7(coinStr, hud2Color, 15, 55);
		(engine.getHUDmanager()).setHUD3(hudMessage, hud3Color, 15, 80);


		// To create Viewport 
        float rightNormX = 0.75f;
        float rightNormY = 0.0f;
        float rightNormWidth = 0.25f;
        float rightNormHeight = 0.25f;

        int windowWidth = engine.getRenderSystem().getWidth();
        int windowHeight = engine.getRenderSystem().getHeight();

        int hudX = (int)((rightNormX + 0.1f * rightNormWidth) * windowWidth);
        int hudY = (int)((rightNormY + 0.15f * rightNormHeight) * windowHeight);

        (engine.getHUDmanager()).setHUD4(dispStr3, hud4Color, hudX, hudY);

		// Build inventory string
		StringBuilder inventoryStr = new StringBuilder("Inventory: ");
		for (int i = 0; i < 5; i++) {
			if (inventory[i] != null) {
				inventoryStr.append("[").append(inventory[i]).append("] ");
			} else {
				inventoryStr.append("[ ] ");
			}
		}

		StringBuilder invStr = new StringBuilder("Inventory: ");
		for (int i = 0; i < 5; i++) {
			if (inventory[i] != null) {
				invStr.append("[").append(inventory[i]).append("] ");
			} else {
				invStr.append("[ ] ");
			}
		}
		engine.getHUDmanager().setHUD6(invStr.toString(), new Vector3f(0.9f, 1f, 1f), 20, 110);

		if (marketMode == MarketMode.CHOOSING) {
			int baseX = 600;
			int baseY = 200;
			engine.getHUDmanager().setHUD8("+--------------------------+", new Vector3f(1f, 1f, 1f), baseX, baseY);
			engine.getHUDmanager().setHUD9("| Press X to Cancel         |", new Vector3f(1f, 0.5f, 0.5f), baseX, baseY + 30);
			engine.getHUDmanager().setHUD10("| Press S to Sell         |", new Vector3f(0.9f, 1f, 0.6f), baseX, baseY + 60);
			engine.getHUDmanager().setHUD11("| Press B to Buy       |", new Vector3f(0.9f, 1f, 0.6f), baseX, baseY + 90);
			engine.getHUDmanager().setHUD12("+--------------------------+", new Vector3f(1f, 1f, 1f), baseX, baseY + 120);
		}
		if (marketMode == MarketMode.SELLING) {
			int baseX = 600;
			int baseY = 200;
			engine.getHUDmanager().setHUD8("+------------------------------+", new Vector3f(1f, 1f, 1f), baseX, baseY);
			engine.getHUDmanager().setHUD9("| Press X to exit  |", new Vector3f(1f, 0.5f, 0.5f), baseX, baseY + 30);
			engine.getHUDmanager().setHUD10("| Press A to sell all          |", new Vector3f(1f, 1f, 1f), baseX, baseY + 60);
			engine.getHUDmanager().setHUD11("| Press 1-5 to sell slot items        |", new Vector3f(1f, 1f, 1f), baseX, baseY + 90);
			engine.getHUDmanager().setHUD12("+------------------------------+", new Vector3f(1f, 1f, 1f), baseX, baseY + 120);
		}
		
		if (isBuyingSeeds) {
			int baseX = 600;
			int baseY = 200;
	
			engine.getHUDmanager().setHUD8("+----------------------+ ", new Vector3f(1f, 1f, 1f), baseX, baseY);
			engine.getHUDmanager().setHUD9("| [2] Buy Carrot - 2 Coins      |", new Vector3f(1f, 1f, 1f), baseX, baseY + 30);
			engine.getHUDmanager().setHUD10("| [1] Buy Wheat - 2 Coins  |", new Vector3f(1f, 1f, 1f), baseX, baseY + 60);
			engine.getHUDmanager().setHUD11("|  SEED SHOP  |", new Vector3f(0.9f, 1f, 0.6f), baseX, baseY + 90);
			engine.getHUDmanager().setHUD12("+----------------------+ ", new Vector3f(1f, 1f, 1f), baseX, baseY + 120);
			
		}
		
		if (marketMode == MarketMode.NONE && !isBuyingSeeds) {
			engine.getHUDmanager().setHUD8("", new Vector3f(0, 0, 0), 0, 0);
			engine.getHUDmanager().setHUD9("", new Vector3f(0, 0, 0), 0, 0);
			engine.getHUDmanager().setHUD10("", new Vector3f(0, 0, 0), 0, 0);
			engine.getHUDmanager().setHUD11("", new Vector3f(0, 0, 0), 0, 0);
			engine.getHUDmanager().setHUD12("", new Vector3f(0, 0, 0), 0, 0);

		}

		if (showNotEnoughCoinsMessage) {
			int baseX = 600;
			int baseY = 200;
			engine.getHUDmanager().setHUD8("+----------------------+ ", new Vector3f(1f, 0.2f, 0.2f), baseX, baseY);
			engine.getHUDmanager().setHUD9("|  Press X to close    |", new Vector3f(1f, 0.2f, 0.2f), baseX, baseY + 30);
			engine.getHUDmanager().setHUD10("|  Not enough coins!   |", new Vector3f(1f, 0.5f, 0.5f), baseX, baseY + 60);
			engine.getHUDmanager().setHUD11("", new Vector3f(0, 0, 0), 0, 0);
			engine.getHUDmanager().setHUD12("+----------------------+ ", new Vector3f(1f, 1f, 1f), baseX, baseY + 90);
		}
		
		cropsToRemove.clear();
		objectsToDisable.clear();

		// Sync avatar position with physics object if active
		// Sync avatar position with physics object if active
		if (avatarPhysicsActive && avatarPhysicsObject != null) {
			double[] physTransform = avatarPhysicsObject.getTransform();
			Vector3f physPos = new Vector3f(
				(float) physTransform[12],
				(float) physTransform[13],
				(float) physTransform[14]
			);
			avatar.setLocalLocation(physPos);
			Matrix4f rotation = new Matrix4f(
				(float) physTransform[0], (float) physTransform[4], (float) physTransform[8], 0,
				(float) physTransform[1], (float) physTransform[5], (float) physTransform[9], 0,
				(float) physTransform[2], (float) physTransform[6], (float) physTransform[10], 0,
				0, 0, 0, 1
			);
			avatar.setLocalRotation(rotation);
			System.out.println("Avatar physics active, position updated to: " + physPos);

			// Check if physics duration has expired
			if (System.currentTimeMillis() - physicsActivateTime > PHYSICS_DURATION_MS) {
				physicsEngine.removeObject(avatarPhysicsObject.getUID());
				avatar.setPhysicsObject(null);
				avatarPhysicsObject = null;
				avatarPhysicsActive = false;
				isFaceDown = true; // Enter face-down state
				faceDownStartTime = System.currentTimeMillis();
				System.out.println("Physics deactivated, entering face-down state");
			}
		}

		// Handle face-down state
		if (isFaceDown) {
			// Clamp position to ground
			Vector3f loc1 = avatar.getWorldLocation();
			float targetY = 0.1f;
			if (terr.getHeightMap() != null) {
				try {
					float height = terr.getHeight(loc1.x(), loc1.z());
					targetY = height + 0.1f;
				} catch (Exception e) {
					System.err.println("Error getting terrain height: " + e.getMessage());
				}
			}
			avatar.setLocalLocation(new Vector3f(loc1.x(), targetY, loc1.z()));
	
			// Check if 4 seconds have passed
			if (System.currentTimeMillis() - faceDownStartTime > FACE_DOWN_DURATION_MS) {
				isFaceDown = false;
				// Reset rotation to upright
				Matrix4f uprightRotation = new Matrix4f().identity();
				avatar.setLocalRotation(uprightRotation);
				System.out.println("Rabbit stood back up");
			}
		}
				
    // ==== 1) WATER→PLANT COLLISIONS ====

		// inside update(), in your water–plant collision pass:
		for (int i = 0; i < waterDroplets.size(); i++) {
			PhysicsObject phys   = waterDropletPhysics.get(i);
			GameObject   droplet = waterDroplets.get(i);
			double[]     t       = phys.getTransform();
			Vector3f     dropPos = new Vector3f((float)t[12], (float)t[13], (float)t[14]);
	
			Crop hitCrop = null;
			// find the first plant under this droplet
			for (Crop crop : activeCrops) {
				GameObject planted = crop.getPlantedObject();
				if (!crop.isReadyToHarvest() && planted != null) {
					float collisionRadius = 0.2f;
					if (dropPos.distance(planted.getWorldLocation()) < collisionRadius) {
						hitCrop = crop;
						break;
					}
				}
			}
			if (hitCrop != null && !hasBouncedOffPlant.get(i)) {
				// water the crop once
				hitCrop.water(10.0);
	
				// hop off the plant
				float upVel = 0.5f;    // gentle hop
				float horiz = 0.1f;    // slight scatter
				float vx = ((float)Math.random() - 0.5f) * horiz;
				float vz = ((float)Math.random() - 0.5f) * horiz;
				phys.setLinearVelocity(new float[]{ vx, upVel, vz });
	
				// mark so we don’t bounce again
				hasBouncedOffPlant.set(i, true);
	
				waterGrounded.set(i, false);
				waterGroundTime.set(i, 0f);
			}
		}

		
		// Update crops
		for (Crop crop : activeCrops) {
			crop.update();
		}

		// Process crop removals and object disabling
		synchronized (activeCrops) {
			for (Crop crop : cropsToRemove) {
				activeCrops.remove(crop);
			}
			cropsToRemove.clear();
		}

		synchronized (objectsToDisable) {
			for (GameObject obj : objectsToDisable) {
				renderStateQueue.add(() -> obj.getRenderStates().disableRendering());
			}
			objectsToDisable.clear();
		}

		if (shouldAttachWateringCan) {
			wateringcan.getRenderStates().enableRendering();
			shouldAttachWateringCan = false;
		}
		
		if (shouldDetachWateringCan) {
			wateringcan.getRenderStates().disableRendering();
			shouldDetachWateringCan = false;
		}

		// in update(), after you compute elapsTime:

    // ==== 2) PHYSICS STEP ====

	// 1) recompute delta-time
	// compute dt in ms
	currFrameTime = System.currentTimeMillis();
	float dtMs = (float)(currFrameTime - lastFrameTime);
	lastFrameTime = currFrameTime;

	// 2) spawn new drops *before* you step the world
	if (isWatering && spawnTimer >= DROP_INTERVAL) {
		spawnWaterDroplet();
		spawnTimer = 0f;
	}
	

	// step physics in ms
	physicsEngine.update(dtMs);

	// Sync avatar position with physics object if active
	if (avatarPhysicsActive && avatarPhysicsObject != null) {
		double[] physTransform = avatarPhysicsObject.getTransform();
		Vector3f physPos = new Vector3f(
			(float) physTransform[12],
			(float) physTransform[13],
			(float) physTransform[14]
		);
		avatar.setLocalLocation(physPos);
		System.out.println("Avatar physics active, position updated to: " + physPos);
		System.out.println("Physics velocity: " + Arrays.toString(avatarPhysicsObject.getLinearVelocity()));

		// Check if physics duration has expired
		if (System.currentTimeMillis() - physicsActivateTime > PHYSICS_DURATION_MS) {
			physicsEngine.removeObject(avatarPhysicsObject.getUID());
			avatar.setPhysicsObject(null);
			avatarPhysicsObject = null;
			avatarPhysicsActive = false;
			System.out.println("Physics deactivated, resuming regular movement");
		}
	}

	// if you still need seconds for spawnTimer:
	float dtSec = dtMs * 0.001f;
	spawnTimer += dtSec;

    // ==== 3) DROPLET→GROUND BOUNCE & LIFETIME ====

	// 3) sync all droplets to their physics body transforms and handle cleanup
	// after physicsEngine.update(dt);
	// 4) sync all droplets from their physics bodies into your GameObjects
    for (int i = 0; i < waterDroplets.size(); i++) {
        PhysicsObject phys = waterDropletPhysics.get(i);
        GameObject droplet = waterDroplets.get(i);
        double[] t = phys.getTransform();
        float y = (float)t[13];
        droplet.setLocalTranslation(new Matrix4f().translation((float)t[12], y, (float)t[14]));

        // first hit ground?
        if (!waterGrounded.get(i) && y <= 0.01f) {
            waterGrounded.set(i, true);
            // toss it a little
            phys.setLinearVelocity(new float[]{
                (float)(Math.random() - 0.5) * 2f,
                1.5f,
                (float)(Math.random() - 0.5) * 2f
            });
        }

        // once grounded, age it out
        if (waterGrounded.get(i)) {
            float age = waterGroundTime.get(i) + dtSec;
            waterGroundTime.set(i, age);
            if (age >= DROPLET_LIFETIME) {
                // cleanup
                physicsEngine.removeObject(phys.getUID());
                droplet.getRenderStates().disableRendering();
                waterDroplets.remove(i);
                waterDropletPhysics.remove(i);
                waterGrounded.remove(i);
                waterGroundTime.remove(i);
                hasBouncedOffPlant.remove(i);
                i--;
            }
        }
    }

    }
	// Process packets received by the client from the server
	protected void processNetworking(float elapsTime)
	{ 
		if (protClient != null)
		protClient.processPackets();
	}

    /**
    * Handles key inputs for movement, interactions, and game resets.
    *
    * @param e The KeyEvent triggered by the player's input.
    */
	@Override
	public void keyPressed(KeyEvent e)
	{
		int keyCode = e.getKeyCode();

		boolean inMarketUI = marketMode != MarketMode.NONE || isBuyingSeeds;
		boolean allowedWhileMarket =
			keyCode == KeyEvent.VK_1 ||
			keyCode == KeyEvent.VK_2 ||
			keyCode == KeyEvent.VK_3 ||
			keyCode == KeyEvent.VK_4 ||
			keyCode == KeyEvent.VK_5 ||
			keyCode == KeyEvent.VK_B ||
			keyCode == KeyEvent.VK_S ||
			keyCode == KeyEvent.VK_A ||
			keyCode == KeyEvent.VK_X;
	
		if (inMarketUI && !allowedWhileMarket) return; // block all other

		switch (keyCode)
		{	
			case KeyEvent.VK_0:
				shouldResetSkybox = true;
				break;
			
			case KeyEvent.VK_1:
				if (isBuyingSeeds && coins >= 2) {
					selectedSeedType = "Wheat";
					addToInventory("Seed_Wheat");
					coins -= 2;
					isBuyingSeeds = false;
					showNotEnoughCoinsMessage = false;
				} else if (isBuyingSeeds){
					showNotEnoughCoinsMessage = true;
				}
				else if (marketMode == MarketMode.SELLING) {
					if (inventory[0] != null) {
						if (inventory[0].startsWith("Seed")) {
							coins += 1; 
						} else if (inventory[0].equals("Wheat")) {
							coins += 5;
						} else if (inventory[0].equals("Carrot")) {
							coins += 10;
						}
						inventory[0] = null;
						inventoryCount--;
						compactInventory();
					}
				}
            break;

			case KeyEvent.VK_2:
				if (isBuyingSeeds && coins >= 5) {
					selectedSeedType = "Carrot";
					addToInventory("Seed_Carrot");
					coins -= 5;
					isBuyingSeeds = false;
				} else if (isBuyingSeeds) {
					showNotEnoughCoinsMessage = true;
				}else if (marketMode == MarketMode.SELLING) {
					if (inventory[1] != null) {
						if (inventory[1].startsWith("Seed")) {
							coins += 1;
						} else if (inventory[1].equals("Wheat")) {
							coins += 5;
						} else if (inventory[1].equals("Carrot")) {
							coins += 10;
						}
						inventory[1] = null;
						inventoryCount--;
						compactInventory();
					}
				}
            break;

			case KeyEvent.VK_3: case KeyEvent.VK_4: case KeyEvent.VK_5:
				if (marketMode == MarketMode.SELLING) {
					int index = e.getKeyCode() - KeyEvent.VK_1;
					if (inventory[index] != null) {
						if (inventory[index].startsWith("Seed")) {
							coins += 1;
						} else if (inventory[index].equals("Wheat")) {
							coins += 5;
						} else if (inventory[index].equals("Carrot")) {
							coins += 10;
						}
						inventory[index] = null;
						inventoryCount--;
						compactInventory();
					}
				}
            break;


			case KeyEvent.VK_W:
				if (protClient != null && isConnected) {
					protClient.sendMoveMessage(avatar.getWorldLocation());
				} else {
					System.out.println("Warning: Cannot send move message, client not connected.");
				}	
				moveAvatar(1);
				break;
			case KeyEvent.VK_S:
				if (marketMode == MarketMode.CHOOSING) {
					marketMode = MarketMode.SELLING;
				} else{
					moveAvatar(-1);
				}
				break;
			case KeyEvent.VK_A: 
				if (marketMode == MarketMode.SELLING) {
					for (int i = 0; i < inventory.length; i++) {
						if (inventory[i] != null) {
							if (inventory[i].startsWith("Seed")) {
								coins += 1;
							} else if (inventory[i].equals("Wheat")) {
								coins += 5;
							} else if (inventory[i].equals("Carrot")) {
								coins += 10;
							}
							inventory[i] = null;
						}
					}
					compactInventory();
					inventoryCount = 0;
					for (String item : inventory) {
						if (item != null) inventoryCount++;
					}
				} else {
					turnAction.yawDolphin(0.02f);
				}
            break;
			case KeyEvent.VK_D:
				turnAction.yawDolphin(-0.02f);
			break;
			case KeyEvent.VK_P:
				Camera rightCam = engine.getRenderSystem().getViewport("RIGHT").getCamera();
				Camera leftCam = engine.getRenderSystem().getViewport("LEFT").getCamera();
				if (rightCam != null) {
					resetViewportCamera(rightCam, rightViewportOffset);
				}
				if (leftCam != null) {
					resetViewportCamera(leftCam, leftViewportOffset);
				}
				break;
			case KeyEvent.VK_J: 
				panRightViewportCamera(-0.1f, 0);
				break;
			case KeyEvent.VK_L: 
				panRightViewportCamera(0.1f, 0);
				break;
			case KeyEvent.VK_I: 
				panRightViewportCamera(0, 0.1f);
				break;
			case KeyEvent.VK_K: 
				panRightViewportCamera(0, -0.1f);
				break;
			case KeyEvent.VK_U: 
				zoomRightViewportCamera(-0.1f);
				break;
			case KeyEvent.VK_O: 
				zoomRightViewportCamera(0.1f);
				break;			
			case KeyEvent.VK_Q:  
				toggleLines();
			break;

			case KeyEvent.VK_ESCAPE:
				if (protClient != null) {
					System.out.println("Sending bye manually...");
					protClient.sendByeMessage();
				}
				System.exit(0); // Exit after sending
			break;

			case KeyEvent.VK_H:
				if (marketMode!=MarketMode.NONE || isBuyingSeeds) break;
				Crop nearest = null;
				float bestDist = Float.MAX_VALUE;
				// find the ready, unharvested crop closest to you
				for (Crop c: activeCrops) {
					if (!c.isHarvested() && c.isReadyToHarvest() && c.hasGrown()) {
						GameObject p = c.getPlantedObject();
						if (p==null) continue;
						float d = avatar.getWorldLocation().distance(p.getWorldLocation());
						if (d < 1.5f && d < bestDist) {
							bestDist = d;
							nearest = c;
						}
					}
				}
				if (nearest != null && inventoryCount < 5) {
					GameObject p = nearest.getPlantedObject();
					synchronized(cropsToRemove)   { cropsToRemove.add(nearest); }
					synchronized(objectsToDisable){ objectsToDisable.add(p);       }
					p.getRenderStates().disableRendering();
					nearest.markHarvested();
					inventory[inventoryCount++] = nearest.getType();
					System.out.println("Harvested " + nearest.getType());
				} else if (nearest!=null) {
					System.out.println("Inventory full!");
				}
			break;
			
			
			case KeyEvent.VK_M:
				Vector3f pos = avatar.getWorldLocation();
				float distToMarket = pos.distance(market.getWorldLocation());
				float distToHome   = pos.distance(home.getWorldLocation());
				if (distToMarket < 1.0f) {
					marketMode = MarketMode.CHOOSING;
				}
				else if (distToHome < 1.0f) {
					shouldResetSkybox = true;
				}
			break;
		
			
			case KeyEvent.VK_E:
			for (int i = 0; i < inventory.length; i++) {
				if (inventory[i] != null && inventory[i].startsWith("Seed_")) {
					String cropType = inventory[i].split("_")[1];
					inventory[i] = null;
					inventoryCount--;
		
					Vector3f forward = avatar.getWorldForwardVector().normalize();
					Vector3f position = avatar.getWorldLocation().add(forward.mul(0.5f));
		
					GameObject planted = new GameObject(GameObject.root(), plantS, planttx);
					planted.setLocalTranslation(new Matrix4f().translation(position.x(), 0, position.z()));
					planted.setLocalScale(new Matrix4f().scaling(0.020f));
		
					double growTime = cropType.equals("Carrot") ? 45 : 30;
					ObjShape targetShape = cropType.equals("Carrot") ? carrotS : wheatS;
					TextureImage targetTexture = cropType.equals("Carrot") ? carrottx : wheattx;
		
					Crop crop = new Crop(cropType, growTime, targetShape, targetTexture);
					crop.setPlantedObject(planted);
					activeCrops.add(crop);
		
					compactInventory(); // after removing seed
		
					// Only add a PlantAnimationController if the planted object has AnimatedShape
					if (planted.getShape() instanceof AnimatedShape) {
						PlantAnimationController plantController = new PlantAnimationController(planted);
						plantControllers.add(plantController);
					}
		
					break; // Only plant once per keypress
				}
			}
			break;
		
		
			case KeyEvent.VK_B:
				if (marketMode == MarketMode.CHOOSING) {
					isBuyingSeeds = true;
					marketMode = MarketMode.NONE;
				}
			break;
			case KeyEvent.VK_X:
				if (marketMode != MarketMode.NONE || isBuyingSeeds || showNotEnoughCoinsMessage) {
					marketMode = MarketMode.NONE;
					isBuyingSeeds = false;
					showNotEnoughCoinsMessage = false;
				}
			break;
			case KeyEvent.VK_SPACE:
				isWatering = !isWatering;
				if (isWatering) {
					shouldAttachWateringCan = true;
				} else {
					shouldDetachWateringCan = true;
				}
            break;
		
		}
		super.keyPressed(e);
	}

	/**
     * Retrieves the dolphin object.
     *
     * @return The GameObject representing the player's avatar.
     */
	public GameObject getAvatar() { 
		if (avatar == null) {
			System.out.println("Rabbit object is null!");
		}
		return avatar; 
	}
	/**
    * Moves the rabbit forward or backward based on player input.
    *
    * @param direction 1 for forward, -1 for backward.
    */
	public void moveAvatar(int direction) {
		// 1) compute your candidate new position exactly as before
		Vector3f fwd = avatar.getWorldForwardVector().normalize();
		Vector3f loc = avatar.getWorldLocation();
		float step = movementSpeed * direction * (float)elapsTime;
		Vector3f newLoc = loc.add(new Vector3f(fwd).mul(step));
	
		// 2) choose a blocking radius that covers each model’s footprint in XZ
		//    (your home & market boxes use half-extents of 0.5 in X and Z → radius ~0.75)
		float blockRadius = 0.50f;
	
		// 3) if that new position would be too close to the house, cancel the move
		if (newLoc.distance(home.getWorldLocation()) < blockRadius) {
			return;
		}
		// 4) same check for the market
		if (newLoc.distance(market.getWorldLocation()) < blockRadius) {
			return;
		}
	
		// 5) your existing world bounds check
		float minX = -12, maxX = 12, minZ = -12, maxZ = 12;
		if (newLoc.x() < minX || newLoc.x() > maxX || newLoc.z() < minZ || newLoc.z() > maxZ) {
			return;
		}
	
		// 6) if we passed all tests, commit the move
		avatar.setLocalLocation(newLoc);
	}
	
	
    /**
     * Increments the player's score upon successfully disarming a satellite.
     */
	public void incrementScore()
	{
		counter++;  
		System.out.println("Score: " + counter);
		if (counter >= 3) {
			System.out.println("All objects disarmed! You win!");
		}
	}	
	/**
     * Retrieves the main camera used in the game.
     *
     * @return The main Camera object.
    */
	public Camera getMainCamera() {
		return mainCamera;
	}
	
	/**
	 * Initializes and configures multiple camera viewports.
	 * Each viewport is assigned a camera with specific positioning and orientation.
	 */
	@Override
	public void createViewports() {
		RenderSystem renderSystem = engine.getRenderSystem();
		if (renderSystem == null) {
			System.out.println("RenderSystem is not initialized!");
			return;
		}
	
		// Add viewports
		renderSystem.addViewport("MAIN", 0, 0, 1f, 1f);
		renderSystem.addViewport("LEFT", 0, 0, 1f, 1f);
		renderSystem.addViewport("RIGHT", 0.75f, 0, 0.25f, 0.25f);
	
		// Retrieve viewports and check for null
		Viewport mainVp = renderSystem.getViewport("MAIN");
		Viewport leftVp = renderSystem.getViewport("LEFT");
		Viewport rightVp = renderSystem.getViewport("RIGHT");
	
		if (mainVp == null || leftVp == null || rightVp == null) {
			System.out.println("Failed to create viewports!");
			return;
		}
	
		// Configure MAIN viewport camera
		mainVp.getCamera().setLocation(new Vector3f(0, 2, 0));
		mainVp.getCamera().setU(new Vector3f(1, 0, 0));
		mainVp.getCamera().setV(new Vector3f(0, 1, 0));
		mainVp.getCamera().setN(new Vector3f(0, 0, -1));
	
		// Configure LEFT viewport camera
		leftVp.getCamera().setLocation(new Vector3f(-2, 0, 2));
		leftVp.getCamera().setU(new Vector3f(1, 0, 0));
		leftVp.getCamera().setV(new Vector3f(0, 1, 0));
		leftVp.getCamera().setN(new Vector3f(0, 0, -1));
	
		// Configure RIGHT viewport camera
		rightVp.setHasBorder(true);
		rightVp.setBorderWidth(4);
		rightVp.setBorderColor(0.0f, 1.0f, 0.0f);
		rightVp.getCamera().setLocation(new Vector3f(0, 2, 0));
		rightVp.getCamera().setU(new Vector3f(1, 0, 0));
		rightVp.getCamera().setV(new Vector3f(0, 0, -1));
		rightVp.getCamera().setN(new Vector3f(0, -1, 0));

		rightViewportOffset = new Vector3f(rightVp.getCamera().getLocation()).sub(avatar.getWorldLocation());
		leftViewportOffset = new Vector3f(leftVp.getCamera().getLocation()).sub(avatar.getWorldLocation());	
	}

	/**
	 * Moves the RIGHT viewport camera in the X and Y directions.
	 * This allows for panning adjustments when the user presses arrow keys.
	 *
	 * @param deltaX Movement along the X-axis (left/right)
	 * @param deltaY Movement along the Y-axis (up/down)
	 */
	private void panRightViewportCamera(float deltaX, float deltaY) {
		Viewport rightVp = engine.getRenderSystem().getViewport("RIGHT");
		if (rightVp != null) {
			Camera rightCamera = rightVp.getCamera();
			Vector3f location = rightCamera.getLocation();
			Vector3f right = rightCamera.getU();
			Vector3f up = rightCamera.getV();
	
			// Pan the camera
			Vector3f panDelta = new Vector3f(right).mul(deltaX).add(new Vector3f(up).mul(deltaY));
			rightCamera.setLocation(new Vector3f(location).add(panDelta)); 
		}
	}
	
	/**
	 * Adjusts the zoom level of the RIGHT viewport camera.
	 * Allows zooming in and out by moving the camera forward/backward.
	 *
	 * @param delta Amount to zoom (positive to zoom out, negative to zoom in)
	 */
	private void zoomRightViewportCamera(float delta) {
		Viewport rightVp = engine.getRenderSystem().getViewport("RIGHT");
		if (rightVp != null) {
			Camera rightCamera = rightVp.getCamera();
			Vector3f location = rightCamera.getLocation();
			Vector3f forward = rightCamera.getN().mul(-1); 
	
			Vector3f zoomDelta = new Vector3f(forward).mul(delta);
			// Zoom the camera
			rightCamera.setLocation(new Vector3f(location).add(zoomDelta));
		}
	}

	/**
	 * Resets the given camera so that its location is:
	 * (dolphin's current position + original offset).
	 * 
	 * The camera's orientation (U, V, N vectors) remains unchanged.
	 *
	 * @param cam Camera to reset
	 * @param offset The original offset to maintain relative positioning
	 */
	private void resetViewportCamera(Camera cam, Vector3f offset) {
		Vector3f newPos = new Vector3f(avatar.getWorldLocation()).add(offset);
		cam.setLocation(newPos);
		cam.lookAt(avatar);
	}

	/**
 	* Toggles the visibility of the X, Y, and Z axis lines in the scene.
     */
	public void toggleLines() {
		linesVisible = !linesVisible;  
	
		if (linesVisible) {
			x.getRenderStates().enableRendering();
			y.getRenderStates().enableRendering();
			z.getRenderStates().enableRendering();
		} else {
			x.getRenderStates().disableRendering();
			y.getRenderStates().disableRendering();
			z.getRenderStates().disableRendering();
		}
	}
	/**
	 * Retrieves the shape used for representing ghost avatars (other players).
	*/
	public ObjShape getGhostShape() { return ghostS; }
	/**
	 * Retrieves the texture assigned to ghost avatars (if any).
	*/
	public TextureImage getGhostTexture() { return ghostT; }
	/**
	 * Provides access to the {@link GhostManager}, which handles ghost avatar tracking and updates.
	*/
	public GhostManager getGhostManager() { return gm; }
	/**
	 * Returns the main engine instance managing the game.
	 */
	public Engine getEngine() { return engine; }
	/**
	 * Gets the current world location of the player's avatar.
	*/
	public Vector3f getPlayerPosition(){ return avatar.getWorldLocation(); }
	/**
	 * Updates the connection status of the client.
	 */
	public void setIsConnected(boolean status) {
		isConnected = status;
	}
	/**
	 * Checks whether the client is connected to the server.
	 */
	public boolean isConnected() {
		return isConnected;
	}
	/**
	 * Retrieves the active {@link ProtocolClient} handling network communication.
	 */
	public ProtocolClient getProtocolClient() {
		return protClient;
	}	

	/**
	 * Generates a unique RGB color based on a client UUID.
	 * This is used to visually distinguish ghost avatars in the game world.
	 *
	 * @param clientId the UUID of the client.
	 * @return a {@link Vector3f} representing the RGB color.
	 */
	public Vector3f getUniqueColorForClient(UUID clientId) {
		int hash = Math.abs(clientId.hashCode());
		float r = (hash % 256) / 255.0f; 
		float g = ((hash / 256) % 256) / 255.0f; 
		float b = ((hash / 65536) % 256) / 255.0f; 
	
		return new Vector3f(r, g, b);
	}
	/**
	 * Gets the {@link SkyboxManager} responsible for handling skybox transitions.
	 */
	public SkyboxManager getSkyboxManager() {
		return skyboxManager;
	}

	public void setPendingSkyboxIndex(int index) {
		pendingSkyboxIndex = index;
	}	

	private void addToInventory(String item) {
		for (int i = 0; i < inventory.length; i++) {
			if (inventory[i] == null) {
				inventory[i] = item;
				inventoryCount++;
				break;
			}
		}
	}
	private void compactInventory() {
		String[] newInventory = new String[5];
		int newCount = 0;
		for (int i = 0; i < inventory.length; i++) {
			if (inventory[i] != null) {
				newInventory[newCount++] = inventory[i];
			}
		}
		inventory = newInventory;
		inventoryCount = newCount;
	}
	
	public MarketMode getMarketMode() {
		return marketMode;
	}
	
	public boolean isBuyingSeeds() {
		return isBuyingSeeds;
	}

	private void createBorderTorus(float x, float y, float z) {
		GameObject torus = new GameObject(GameObject.root(), borderShape, null);
		Matrix4f pos = new Matrix4f().translation(x, y + 0.05f, z);
		Matrix4f scale = new Matrix4f().scaling(0.4f);
		torus.setLocalTranslation(pos);
		torus.setLocalScale(scale);
		torus.getRenderStates().setColor(new Vector3f(1f, 0.5f, 0.2f)); // optional: orange borders

		if (borderShape == null || borderShape.getVertices() == null || borderShape.getVertices().length == 0) {
			System.err.println("Invalid border shape! Skipping border torus creation.");
			return;
		}
		
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		super.keyReleased(e);
	}
	
	private void spawnWaterDroplet() {
		// 1. Create visual droplet with very small size
		GameObject droplet = new GameObject(GameObject.root(), waterCubeS, null);
		droplet.getRenderStates().enableRendering();
		droplet.getRenderStates().setColor(new Vector3f(0.0f, 0.7f, 1.0f)); // Blue water color
		
		// 2. Calculate exact spawn position at watering can spout
		Vector3f canPos = wateringcan.getWorldLocation();
		Vector3f canForward = wateringcan.getWorldForwardVector();
		Vector3f canUp = wateringcan.getWorldUpVector();
		
		// Adjust these values to match your watering can model's spout position
		float spoutForwardOffset = 0.07f; // How far forward from can center
		float spoutUpOffset = 0.1f;      // How high from can center
		float spoutRightOffset = 0.0f;  // How far to the side
		
		Vector3f spawnPos = canPos
			.add(canForward.mul(spoutForwardOffset))
			.add(canUp.mul(spoutUpOffset))
			.add(wateringcan.getWorldRightVector().mul(spoutRightOffset));
		
		// 3. Set very small scale for droplet (0.01f = 1cm)
		float dropletSize = 0.01f; 
		droplet.setLocalScale(new Matrix4f().scaling(dropletSize));
		
		// 4. Create physics object at correct position
		double[] xform = new double[] {
			// col 0        col 1        col 2        col 3 (translation)
			  1, 0, 0,      0,           0,  1, 0,     0,           0,  0, 1,     0,
			spawnPos.x, spawnPos.y, spawnPos.z,      1
		};
		
		
		PhysicsObject phys = physicsEngine.addSphereObject(
			physicsEngine.nextUID(),
			0.05f, // Very light mass (0.05kg)
			xform,
			dropletSize // Match visual size
		);
		
		// 5. Configure physics properties
		phys.setBounciness(0.2f); // Slight bounce
		phys.setDamping(0.1f, 0.1f); // Some damping
		phys.setFriction(0.1f); // Slight friction
		
		// 6. Apply initial velocity - mostly downward with slight randomness
		float initialDownwardVelocity = -2.0f; // Fast enough to look natural
		phys.setLinearVelocity(new float[]{
			(float)(Math.random() - 0.5f) * 0.2f, // Small horizontal randomness
			initialDownwardVelocity,
			(float)(Math.random() - 0.5f) * 0.2f
		});
		
		// 7. Link physics to visual object
		droplet.setLocalTranslation(new Matrix4f().translation(spawnPos));
		droplet.setPhysicsObject(phys);
		
		// 8. Add to tracking lists
		waterDroplets.add(droplet);
		waterDropletPhysics.add(phys);
		waterGrounded.add(false);
		waterGroundTime.add(0f);
		hasBouncedOffPlant.add(false);
		
		}
		public GameObject getHome()   { return home; }
		public GameObject getMarket() { return market; }
		public PhysicsEngine getPhysicsEngine() { return physicsEngine; }
		public boolean isAvatarPhysicsActive() { return avatarPhysicsActive; }
		public void setAvatarPhysicsActive(boolean active) { avatarPhysicsActive = active; }
		public void setAvatarPhysicsObject(PhysicsObject physObj) { avatarPhysicsObject = physObj; }
		public void setPhysicsActivateTime(long time) { physicsActivateTime = time; }
		public GameObject getPig() { return pig; }
		public GameObject getChicken() { return chicken; }
		public boolean isFaceDown() { return isFaceDown; }
		public float getDeltaTime() { return deltaTime ; }
		public GameObject getTerr() { return terr;}
		
}
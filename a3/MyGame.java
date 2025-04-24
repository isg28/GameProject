package a3;

import tage.*;
import tage.shapes.*;
import java.lang.Math;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
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

	private GameObject dol, avatar, x, y, z, terr, pig, chicken, rabbit, carrot, home, tree, plant, market, wheat, wateringcan;
	private ObjShape dolS, linxS, linyS, linzS, ghostS, terrS, borderShape, pigS, chickenS, rabbitS, carrotS, homeS, treeS, plantS, marketS,
																wheatS, wateringcanS;
	private TextureImage doltx, pigtx, chickentx, rabbittx, carrottx, hometx, treetx, planttx, markettx, wheattx, wateringcantx;
	private Light light1; 

	private InputManager im;
	private Camera mainCamera;
	private TurnAction turnAction;
	private final float movementSpeed = 0.05f;
	private boolean gameOver = false;
	private boolean victory = false;
	private double victoryCountdown = -1;
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

	/**
    * Constructs the game instance and initializes the game loop.
    */
	public MyGame() { super(); }

	
	public MyGame(String serverAddress, int serverPort, String protocol)
	{ 
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
		this.serverProtocol = ProtocolType.UDP;
	}

	/**
     * Initializes the game's lighting system.
     */
	@Override
	public void initializeLights()
	{	
		Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(light1);
	}


	/**
     * Main constructor for the game.
    */
	public static void main(String[] args)
	{	
		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
		game.initializeLights();
	}
	/**
	 * Loads 3D models for key scene elements, including the player avatar and axis lines.
	 * This is where future shapes for crops, tools, and animals will be loaded.
	 */
	@Override
	public void loadShapes()
	{	
		dolS = new ImportedModel("dolphinHighPoly.obj");
		pigS = new ImportedModel("pig.obj");
		chickenS = new ImportedModel("chicken.obj");
		rabbitS = new ImportedModel("rabbit.obj");
		carrotS = new ImportedModel("carrot.obj");
 		homeS = new ImportedModel("home.obj");
		//treeS = new ImportedModel("tree.obj");
		marketS = new ImportedModel("market.obj");
		plantS = new ImportedModel("plant.obj");
		wheatS = new ImportedModel("wheat.obj");
		wateringcanS = new ImportedModel("watercan.obj");
		linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(10f,0f,0f));  
		linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,10f,0f));  
		linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-10f)); 
		ghostS = new Sphere();
		terrS = new TerrainPlane(1000);
		hills = new TextureImage("border2.jpg");
		
	}
    /**
     * Loads and assigns texture images to various objects in the game.
     * Includes textures for the dolphins & terrain.
     */
	@Override
	public void loadTextures()
	{	
		doltx = new TextureImage("Dolphin_HighPolyUV.png");
		pigtx = new TextureImage("pigtx.jpg");
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

		//hills = new TextureImage("hills.jpg");
		

	}
	
	/**
	 * Constructs and initializes scene objects, including the avatar, terrain, and axis indicators.
	 * Currently sets up the terrain with support for dynamic texture updates.
	 */
	@Override
	public void buildObjects()
	{	
		Matrix4f initialTranslation, initialScale;
		if (engine == null) {
			System.out.println("Engine not initialized yet.");
			return;
		}
		stretchController = new StretchController(2000); 
		engine.getSceneGraph().addNodeController(stretchController);
		bouncingController = new BouncingController(2000); 
		engine.getSceneGraph().addNodeController(bouncingController);

		dol = new GameObject(GameObject.root(), dolS, doltx);
		initialTranslation = (new Matrix4f()).translation(0,0,0);
		dol.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.25f));
		dol.setLocalTranslation(initialTranslation);
		dol.setLocalScale(initialScale);

		pig = new GameObject(GameObject.root(), pigS, pigtx);
		initialTranslation = (new Matrix4f()).translation(2,0,0);
		pig.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.1f));
		pig.setLocalTranslation(initialTranslation);
		pig.setLocalScale(initialScale);

		chicken = new GameObject(GameObject.root(), chickenS, chickentx);
		initialTranslation = (new Matrix4f()).translation(0,0,1);
		chicken.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.1f));
		chicken.setLocalTranslation(initialTranslation);
		chicken.setLocalScale(initialScale);

		rabbit = new GameObject(GameObject.root(), rabbitS, rabbittx);
		initialTranslation = (new Matrix4f()).translation(0,0 ,2);
		rabbit.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.1f));
		rabbit.setLocalTranslation(initialTranslation);
		rabbit.setLocalScale(initialScale);

		carrot = new GameObject(GameObject.root(), carrotS, carrottx);
		initialTranslation = (new Matrix4f()).translation(1,1,0);
		carrot.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.3f));
		carrot.setLocalTranslation(initialTranslation);
		carrot.setLocalScale(initialScale);

		home = new GameObject(GameObject.root(), homeS, hometx);
		initialTranslation = (new Matrix4f()).translation(-3,0,0);
		home.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.1f));
		home.setLocalTranslation(initialTranslation);
		home.setLocalScale(initialScale);

   		plant = new GameObject(GameObject.root(), plantS, planttx);
		initialTranslation = (new Matrix4f()).translation(0,0,-1);
		plant.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.05f));
		plant.setLocalTranslation(initialTranslation);
		plant.setLocalScale(initialScale);  

		plant.getRenderStates().setTiling(1);
		plant.getRenderStates().setTileFactor(4);

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
		
        
		wheat = new GameObject(GameObject.root(), wheatS, wheattx);
		initialTranslation = (new Matrix4f()).translation(0,0,-2);
		wheat.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.2f));
		wheat.setLocalTranslation(initialTranslation);
		wheat.setLocalScale(initialScale);

		wateringcan = new GameObject(GameObject.root(), wateringcanS, wateringcantx);
		initialTranslation = (new Matrix4f()).translation(-1,1,1);
		wateringcan.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.2f));
		wateringcan.setLocalTranslation(initialTranslation);
		wateringcan.setLocalScale(initialScale);

		x = new GameObject(GameObject.root(), linxS);
		y = new GameObject(GameObject.root(), linyS);
		z = new GameObject(GameObject.root(), linzS);
		(x.getRenderStates()).setColor(new Vector3f(1f,0f,0f));
		(y.getRenderStates()).setColor(new Vector3f(0f,1f,0f));
		(z.getRenderStates()).setColor(new Vector3f(0f,0f,1f));

		Vector3f dolPosition = dol.getWorldLocation();

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
	}

	/**
	 * Configures the game's runtime state, including input handling, camera setup,
	 * and network client initialization. This method also associates keyboard and gamepad
	 * inputs to control movement and actions within the game world.
	*/
	@Override
	public void initializeGame()
	{
		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		setupNetworking();
		inventory[0] = "Seed_Wheat";
		inventoryCount = 1;


		// ------------- initialize camera -------------
		im = engine.getInputManager();
		avatar = dol; 
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
		
		// Handle terrain height adjustment
		if (terr != null) {
			Vector3f loc = dol.getWorldLocation();
			float height = terr.getHeight(loc.x(), loc.z());
			float currentY = loc.y();
			float targetY = height + 0.1f;
			
			if (Math.abs(currentY - targetY) > 0.01f) {
				dol.setLocalLocation(new Vector3f(loc.x(), targetY, loc.z()));
			}
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

		loc = dol.getWorldLocation() ;
 		fwd = dol.getWorldForwardVector();
		up = dol.getWorldUpVector();
		right = dol.getWorldRightVector();
	
		String hudMessage = "Status: Roaming the Fields";
		
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
		
		

		for (int i = 0; i < activeCrops.size(); i++) {
			Crop crop = activeCrops.get(i);
			crop.update();
		
			if (crop.isReadyToHarvest() && !crop.hasGrown()) {
				GameObject currentObj = crop.getPlantedObject();
		
				if (currentObj != null) {
					Vector3f growPos = currentObj.getWorldLocation();
					currentObj.getRenderStates().disableRendering();
		
					// Create the grown crop based on type
					GameObject grownObj;
					if (crop.getType().equals("Wheat")) {
						grownObj = new GameObject(GameObject.root(), wheatS, wheattx);
					} else if (crop.getType().equals("Carrot")) {
						grownObj = new GameObject(GameObject.root(), carrotS, carrottx);
					} else {
						System.out.println("Unknown crop type: " + crop.getType());
						continue; // Skip unknown crops
					}
		
					grownObj.setLocalTranslation(new Matrix4f().translation(growPos));
					grownObj.setLocalScale(new Matrix4f().scaling(0.15f));
					crop.setPlantedObject(grownObj);
					crop.setHasGrown(true);
		
					System.out.println(crop.getType() + " has grown!");
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
				moveDolphin(1);
				break;
			case KeyEvent.VK_S:
				if (marketMode == MarketMode.CHOOSING) {
					marketMode = MarketMode.SELLING;
				} else{
					moveDolphin(-1);
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

			case KeyEvent.VK_H:
				for (int i = 0; i < activeCrops.size(); i++) {
					Crop crop = activeCrops.get(i);
			
					if (crop.isReadyToHarvest() && crop.hasGrown()) {
						GameObject plantedObj = crop.getPlantedObject();
						if (plantedObj != null) {
							float distance = dol.getWorldLocation().sub(plantedObj.getWorldLocation()).length();
			
							if (distance < 1.5f) {
								plantedObj.getRenderStates().disableRendering();
			
								if (inventoryCount < 5) {
									inventory[inventoryCount++] = crop.getType();
									System.out.println("Harvested " + crop.getType() + "!");
								}
			
								activeCrops.remove(i); // remove the harvested crop
								break; 
							}
						}
					}
				}
			break;
			
			
			
			case KeyEvent.VK_M:
				float distToMarket = dol.getWorldLocation().sub(market.getWorldLocation()).length();
				if (distToMarket < 2.0f) {
					marketMode = MarketMode.CHOOSING;
				}
			break;
			
			
			case KeyEvent.VK_E:
			for (int i = 0; i < inventory.length; i++) {
				if (inventory[i] != null && inventory[i].startsWith("Seed_")) {
					String cropType = inventory[i].split("_")[1];
					inventory[i] = null;
					inventoryCount--;
		
					Vector3f forward = dol.getWorldForwardVector().normalize();
					Vector3f position = dol.getWorldLocation().add(forward.mul(0.5f));
		
					GameObject planted = new GameObject(GameObject.root(), plantS, planttx);
					planted.setLocalTranslation(new Matrix4f().translation(position.x(), 0, position.z()));
					planted.setLocalScale(new Matrix4f().scaling(0.020f));
		
					double growTime = cropType.equals("Carrot") ? 20 : 30;
		
					Crop crop = new Crop(cropType, growTime);
					crop.setPlantedObject(planted);
					activeCrops.add(crop);
		
					break;
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
		
		}
		super.keyPressed(e);
	}

	/**
     * Retrieves the dolphin object.
     *
     * @return The GameObject representing the player's avatar.
     */
	public GameObject getAvatar() { 
		if (dol == null) {
			System.out.println("Dolphin object is null!");
		}
		return dol; 
	}
	/**
    * Moves the dolphin forward or backward based on player input.
    *
    * @param direction 1 for forward, -1 for backward.
    */
	public void moveDolphin(int direction)
	{
		Vector3f fwd = dol.getWorldForwardVector();
		Vector3f loc = dol.getWorldLocation();
		Vector3f newLocation = loc.add(fwd.mul(movementSpeed * direction * (float) elapsTime));
	
		// Set tight bounds near the terrain center (adjust as needed)
		float minX = -15.0f;
		float maxX = 15.0f;
		float minZ = -15.0f;
		float maxZ = 15.0f;
	
		if (newLocation.x() < minX || newLocation.x() > maxX || 
			newLocation.z() < minZ || newLocation.z() > maxZ) {
			return;
		}
	
		dol.setLocalLocation(newLocation);
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
     * Resets the game state to its initial conditions.
     * This includes resetting object positions, node controller effects, score, lighting, and camera settings.
    */
	public void resetGame() {
		gameOver = false;
		victory = false;
		counter = 0;  
		elapsTime = 0.0;

		// Reset dolphin
		dol.setLocalTranslation(new Matrix4f().translation(0, 0, 0));
		dol.setLocalScale(new Matrix4f().scaling(0.25f));
		dol.setLocalRotation(new Matrix4f().rotateY((float) Math.PI / 2));

		// Attach the camera back to the dolphin
		Camera cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		if (cam != null) {
			Vector3f dolLoc = dol.getWorldLocation();
			Vector3f newFwd = new Vector3f(-1, 0, 0); 
			Vector3f newRight = new Vector3f(0, 0, -1); 
			Vector3f newUp = new Vector3f(0, 1, 0);
			cam.setU(newRight);
			cam.setV(newUp);
			cam.setN(newFwd);
			cam.setLocation(dolLoc.add(newUp.mul(0.1f)).add(newFwd.mul(-0.3f)));
		}
	
		// Reset lights
		light1.setDiffuse(1.0f, 1.0f, 1.0f);
		light1.setAmbient(0.3f, 0.3f, 0.3f);

		dol.getRenderStates().hasLighting(true);
		
		System.out.println("Game Reset!");
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

		rightViewportOffset = new Vector3f(rightVp.getCamera().getLocation()).sub(dol.getWorldLocation());
		leftViewportOffset = new Vector3f(leftVp.getCamera().getLocation()).sub(dol.getWorldLocation());	
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
		Vector3f newPos = new Vector3f(dol.getWorldLocation()).add(offset);
		cam.setLocation(newPos);
		cam.lookAt(dol);
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
	
}
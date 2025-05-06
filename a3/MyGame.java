package a3;

import tage.*;
import tage.audio.AudioResource;
import tage.audio.AudioResourceType;
import tage.audio.IAudioManager;
import tage.audio.Sound;
import tage.audio.SoundType;
import tage.shapes.*;
import java.lang.Math;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.awt.event.*;
import java.io.IOException;

import org.joml.*;
import org.joml.sampling.BestCandidateSampling.Quad;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLRunnable;

import net.java.games.input.Component;
import net.java.games.input.Event;
import tage.input.*;
import tage.input.action.AbstractInputAction;
import tage.networking.IGameConnection.ProtocolType;
import tage.networking.server.GameAIServerUDP;
import tage.networking.server.ProtocolClient;
import tage.nodeControllers.BouncingController;
import tage.nodeControllers.OrbitAroundController;
import tage.nodeControllers.OrbitAroundController;
import tage.nodeControllers.OrbitAroundController;
import tage.nodeControllers.OrbitAroundController;
import tage.nodeControllers.RotationController;
import tage.nodeControllers.StretchController;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.rml.Vector3;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLRunnable;
import java.lang.reflect.Method;
import javax.swing.*;


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
 *  @author Isabel Santoyo-Garcia & Danica Galang
 */


public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	private boolean paused=false;
	private int counter=0;
	private double lastFrameTime, currFrameTime, elapsTime;

	protected GameObject dol, avatar, x, y, z, terr, pig, chicken, rabbit, carrot, home, tree, plant, market, wheat, wateringcan, bee,
																radio, lampLeft, lampRight, torch;
	protected ObjShape dolS, linxS, linyS, linzS, terrS, borderShape, pigS, chickenS, rabbitS, carrotS, homeS, treeS, plantS, marketS,
																wheatS, wateringcanS, waterCubeS, beeS, radioS, lampS, torchS;
	protected TextureImage doltx, pigtx, chickentx, rabbittx, carrottx, hometx, treetx, planttx, markettx, wheattx, wateringcantx,
																grayrabbittx, yellowrabbittx, purplerabbittx, pinkrabbittx, orangerabbittx, 
																lavenderrabbittx, greenrabbittx, bluerabbittx, brownrabbittx,
																beetx, radiotx, lamptx, torchtx;

	private Light light1, chaseLight, lampLeftLight, lampRightLight, torchLight; 

	private InputManager im;
	private Camera mainCamera;
	private TurnAction turnAction;
	private final float movementSpeed = 0.05f;
	private CameraOrbit3D orbitController;
	private Vector3f rightViewportOffset;
	private Vector3f leftViewportOffset;
    private TextureImage hills, dayOneTerrain, dayTwoTerrain, dayThreeTerrain, dayFourTerrain, 
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
	List<Crop> activeCrops = new ArrayList<>();
	private Vector3f plantedPos = new Vector3f(0, 0, -2); 
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
	private List<Runnable> renderStateQueue = new ArrayList<>(); 
	private boolean isWatering = false; 
	private List<GameObject> waterDroplets = new ArrayList<>();
	private List<PhysicsObject> waterDropletPhysics = new ArrayList<>();
	PhysicsEngine physicsEngine;
	private static final float DROP_INTERVAL = 0.05f;
	private float spawnTimer = 0f;
	private List<Boolean> waterGrounded   = new ArrayList<>();
	private List<Float>   waterGroundTime = new ArrayList<>();
	private static final float DROPLET_LIFETIME = 2.0f; 
	private List<Boolean> hasBouncedOffPlant = new ArrayList<>();
	private boolean avatarPhysicsActive = false;
	private PhysicsObject avatarPhysicsObject = null;
	private long physicsActivateTime = 0;
	private static final long PHYSICS_DURATION_MS = 1000; 
	private boolean isFaceDown = false;
	private long faceDownStartTime = 0;
	private static final long FACE_DOWN_DURATION_MS = 2000;
	private ChickenAnimationController chickenController; 
	private PigAnimationController pigController;
	private PlantAnimationController plantController;
	private float deltaTime = 0f; 
    private long lastUpdateTime = System.nanoTime();
	private List<PlantAnimationController> plantControllers = new ArrayList<>();
	private static MyGame instance;
	private NPCcontroller npcCtrl;
	private GameAIServerUDP aiServer;
	private IAudioManager audioMgr;
	private Sound beeBuzzSound, pigOinkSound, wateringSound, backgroundMusic, fireSound;
	private boolean isBuzzing = false;
	private Sound chickenCluckSound;
	private float pigSoundCooldown = 0;
	private float chickenSoundCooldown = 0;
	private Vector3f lastPigPos = new Vector3f();
	private Vector3f lastChickenPos = new Vector3f();
	private Map<UUID,Light> plantLights = new HashMap<>();
	private boolean radioOn = false;
	public static enum Tool { NONE, WATERING_CAN, TORCH }
	private Tool activeTool = Tool.NONE;
	public static String selectedRabbitColor = "white";
	public static final Map<String,String> rabbitColorFiles = new HashMap<>();
	static {
		rabbitColorFiles.put("White",       "rabbittx.jpg");
		rabbitColorFiles.put("Gray",        "grayrabbittx.jpg");
		rabbitColorFiles.put("Yellow",      "yellowrabbittx.jpg");
		rabbitColorFiles.put("Purple",      "purplerabbittx.jpg");
		rabbitColorFiles.put("Pink",        "pinkrabbittx.jpg");
		rabbitColorFiles.put("Orange",      "orangerabbittx.jpg");
		rabbitColorFiles.put("Lavender",    "lavenderrabbittx.jpg");
		rabbitColorFiles.put("Green",       "greenrabbittx.jpg");
		rabbitColorFiles.put("Brown",    "brownrabbittx.jpg");
		rabbitColorFiles.put("Blue",     "bluerabbittx.jpg");
	}
	private int selectedMenuOption = 0;

	/**
    * Constructs the game instance and initializes the game loop.
    */
    public MyGame() {
        super();
		instance = this;
        physicsEngine = new tage.physics.JBullet.JBulletPhysicsEngine();
        physicsEngine.initSystem();
        physicsEngine.setGravity(new float[]{0f, -9.8f, 0f});
		npcCtrl = new NPCcontroller(this);
    }
	/**
     * Constructs the game instance with networking parameters.
     *
     * @param serverAddress the address of the game server
     * @param serverPort the port number of the game server
     * @param protocol the protocol ("TCP" or "UDP") to use for network communication
    */
    public MyGame(String serverAddress, int serverPort, String protocol) {
        super();
		instance = this;

        gm = new GhostManager(this);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        if (protocol.toUpperCase().compareTo("TCP") == 0)
            this.serverProtocol = ProtocolType.TCP;
        else
            this.serverProtocol = ProtocolType.UDP;

		npcCtrl = new NPCcontroller(this);
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
	
        chaseLight = new Light();
        chaseLight.setLocation(new Vector3f(0,5,0));    
        chaseLight.setDiffuse(1,0,0);                    
        chaseLight.setAmbient(0.3f,0,0);
		chaseLight.disable();
        engine.getSceneGraph().addLight(chaseLight);

		lampLeftLight = new Light();
		lampLeftLight.setAmbient(0.2f, 0.1f, 0.05f);
		lampLeftLight.setDiffuse(1.0f, 0.8f, 0.6f);
		lampLeftLight.setSpecular(1.0f, 0.8f, 0.6f);
		lampLeftLight.setRange(2.0f);
		lampLeftLight.setConstantAttenuation(1.0f);
		lampLeftLight.setLinearAttenuation(1.5f);
		lampLeftLight.setQuadraticAttenuation(0.5f);
		engine.getSceneGraph().addLight(lampLeftLight);

		lampRightLight = new Light();
		lampRightLight.setAmbient(0.2f, 0.1f, 0.05f);
		lampRightLight.setDiffuse(1.0f, 0.8f, 0.6f);
		lampRightLight.setSpecular(1.0f, 0.8f, 0.6f);
		lampRightLight.setRange(2.0f);
		lampRightLight.setConstantAttenuation(1.0f);
		lampRightLight.setLinearAttenuation(1.5f);
		lampRightLight.setQuadraticAttenuation(0.5f);
		engine.getSceneGraph().addLight(lampRightLight);

		lampLeftLight.setLocation( lampLeft .getWorldLocation() );
		lampRightLight.setLocation( lampRight.getWorldLocation() );

		torchLight = new Light();
		torchLight.setType(Light.LightType.POSITIONAL);
		torchLight.setAmbient(0.2f, 0.1f, 0.0f);
		torchLight.setDiffuse(1.0f, 0.8f, 0.3f);
		torchLight.setSpecular(1.0f, 0.8f, 0.3f);
		torchLight.setRange(2.0f);
		torchLight.setConstantAttenuation(1.0f);
		torchLight.setLinearAttenuation(0.8f);
		torchLight.setQuadraticAttenuation(0.2f);

		torchLight.disable();            
		engine.getSceneGraph().addLight(torchLight);		
	}

	/**
     * Main constructor for the game.
    */
	public static void main(String[] args)
	{	
		String[] options = rabbitColorFiles.keySet().toArray(new String[0]);
		int choice = JOptionPane.showOptionDialog(
			null,
			"Pick your rabbit's color",
			"Rabbit Color Picker",
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			null,
			options,
			options[0]
		);
		if (choice>=0) selectedRabbitColor = options[choice];

		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.loadShapes();
		game.loadTextures();
		game.loadSounds();
		
		game.initializeSystem();
		game.initializeLights();
		game.game_loop();
		game.beeBuzzSound.setLocation(game.bee.getWorldLocation());
		game.beeBuzzSound.play();
		

	}
	/**
     * Loads and configures all sound resources used in the game.
    */
	@Override
	public void loadSounds() {
		audioMgr = engine.getAudioManager();
		AudioResource buzzRes = audioMgr.createAudioResource("beebuzz.wav", AudioResourceType.AUDIO_SAMPLE);
		
		beeBuzzSound = new Sound(buzzRes, SoundType.SOUND_EFFECT, 300, true);
		beeBuzzSound.initialize(audioMgr);
		
		beeBuzzSound.setMaxDistance(20.0f);
		beeBuzzSound.setMinDistance(1.0f);
		beeBuzzSound.setRollOff(2.0f);

		AudioResource pigRes = audioMgr.createAudioResource("pigoink.wav", AudioResourceType.AUDIO_SAMPLE);
		AudioResource chickRes = audioMgr.createAudioResource("chickennoise.wav", AudioResourceType.AUDIO_SAMPLE);
	
		pigOinkSound = new Sound(pigRes, SoundType.SOUND_EFFECT, 300, false);
		chickenCluckSound = new Sound(chickRes, SoundType.SOUND_EFFECT, 200, false);
	
		pigOinkSound.initialize(audioMgr);
		chickenCluckSound.initialize(audioMgr);
	
		pigOinkSound.setMaxDistance(15.0f);
		chickenCluckSound.setMaxDistance(15.0f);

		AudioResource waterRes = audioMgr.createAudioResource(
			"water.wav",
			AudioResourceType.AUDIO_SAMPLE
		);
		wateringSound = new Sound(
			waterRes,
			SoundType.SOUND_EFFECT,
			50,   
			true   
		);
		wateringSound.initialize(audioMgr);
		wateringSound.setMaxDistance(15.0f);
		wateringSound.setMinDistance(1.0f);
		wateringSound.setRollOff(2.0f);

		AudioResource bgRes = audioMgr.createAudioResource(
		"backgroundMusic.wav",
		AudioResourceType.AUDIO_STREAM
		);
		backgroundMusic = new Sound(bgRes, SoundType.SOUND_EFFECT, 50, true);
		backgroundMusic.initialize(audioMgr);
		backgroundMusic.setMinDistance(0f);
		backgroundMusic.setMaxDistance(100f);
		backgroundMusic.setRollOff(1.0f);

		AudioResource fireRes = audioMgr.createAudioResource("fireSound.wav", AudioResourceType.AUDIO_SAMPLE);
		fireSound = new Sound(fireRes, SoundType.SOUND_EFFECT, 30, true);
		fireSound.initialize(audioMgr);
		fireSound.setMaxDistance(10.0f); 
		fireSound.setMinDistance(1.0f);
		fireSound.setRollOff(1.0f);
	}

	/**
     * Loads and assigns texture images for all game objects and terrain.
	 */
	@Override
	public void loadShapes() {
		try {
			dolS = new ImportedModel("dolphinHighPoly.obj");
		} catch(Exception e) {
			System.err.println("Failed to load dolphinHighPoly.obj: " + e.getMessage());
			dolS = new Cube();
		}
		if (dolS.getVertices() == null || dolS.getVertices().length == 0) {
			System.err.println("Dolphin model has no vertices — using Cube fallback");
			dolS = new Cube();
		}	
		try {
			pigS = new AnimatedShape("pig.rkm", "pig.rks");
		} catch(Exception e) {
			System.err.println("Failed to load pig animation: " + e.getMessage());
			pigS = new Cube();
		}
		if (pigS.getVertices() == null || pigS.getVertices().length == 0) {
			System.err.println("Pig shape has no vertices — using Cube fallback");
			pigS = new Cube();
		}	
		try {
			chickenS = new AnimatedShape("chicken.rkm", "chicken.rks");
		} catch(Exception e) {
			System.err.println("Failed to load chicken animation: " + e.getMessage());
			chickenS = new Cube();
		}
		if (chickenS.getVertices() == null || chickenS.getVertices().length == 0) {
			System.err.println("Chicken shape has no vertices — using Cube fallback");
			chickenS = new Cube();
		}	
		try {
			rabbitS = new ImportedModel("rabbit.obj");
		} catch(Exception e) {
			System.err.println("Failed to load rabbit.obj: " + e.getMessage());
			rabbitS = new Cube();
		}
		if (rabbitS.getVertices() == null || rabbitS.getVertices().length == 0) {
			System.err.println("Rabbit model has no vertices — using Cube fallback");
			rabbitS = new Cube();
		}	
		try {
			carrotS = new ImportedModel("carrot.obj");
		} catch(Exception e) {
			System.err.println("Failed to load carrot.obj: " + e.getMessage());
			carrotS = new Cube();
		}
		if (carrotS.getVertices() == null || carrotS.getVertices().length == 0) {
			System.err.println("Carrot model has no vertices — using Cube fallback");
			carrotS = new Cube();
		}	
		try {
			homeS = new ImportedModel("home.obj");
		} catch(Exception e) {
			System.err.println("Failed to load home.obj: " + e.getMessage());
			homeS = new Cube();
		}
		if (homeS.getVertices() == null || homeS.getVertices().length == 0) {
			System.err.println("Home model has no vertices — using Cube fallback");
			homeS = new Cube();
		}	
		try {
			marketS = new ImportedModel("market.obj");
		} catch(Exception e) {
			System.err.println("Failed to load market.obj: " + e.getMessage());
			marketS = new Cube();
		}
		if (marketS.getVertices() == null || marketS.getVertices().length == 0) {
			System.err.println("Market model has no vertices — using Cube fallback");
			marketS = new Cube();
		}	
		try {
			plantS = new AnimatedShape("plant.rkm", "plant.rks");
		} catch(Exception e) {
			System.err.println("Failed to load plant animation: " + e.getMessage());
			plantS = new Cube();
		}
		if (plantS.getVertices() == null || plantS.getVertices().length == 0) {
			System.err.println("Plant shape has no vertices — using Cube fallback");
			plantS = new Cube();
		}	
		try {
			wheatS = new ImportedModel("wheat.obj");
		} catch(Exception e) {
			System.err.println("Failed to load wheat.obj: " + e.getMessage());
			wheatS = new Cube();
		}
		if (wheatS.getVertices() == null || wheatS.getVertices().length == 0) {
			System.err.println("Wheat model has no vertices — using Cube fallback");
			wheatS = new Cube();
		}	
		try {
			wateringcanS = new ImportedModel("watercan.obj");
		} catch (Exception e) {
			System.err.println("Error loading watercan.obj: " + e.getMessage());
			wateringcanS = new Cube();
		}
		if (wateringcanS.getVertices() == null || wateringcanS.getVertices().length == 0) {
			System.err.println("Watering can has no vertices — using Cube fallback");
			wateringcanS = new Cube();
		}
		try {
			treeS = new ImportedModel("tree.obj");
		} catch(Exception e) {
			System.err.println("Failed to load tree.obj: " + e.getMessage());
			treeS = new Cube();
		}
		if (treeS.getVertices() == null || treeS.getVertices().length == 0) {
			System.err.println("Tree model has no vertices — using Cube fallback");
			treeS = new Cube();
		}
		try {
			beeS = new AnimatedShape("bee.rkm", "bee.rks");
		} catch(Exception e) {
			System.err.println("Failed to load bee.obj: " + e.getMessage());
			beeS = new Cube();
		}
		if (beeS.getVertices() == null || beeS.getVertices().length == 0) {
			System.err.println("Bee model has no vertices — using Cube fallback");
			beeS = new Cube();
		}
	
		try {
			waterCubeS = new Sphere();
		} catch (Exception e) {
			System.err.println("Error creating Sphere: " + e.getMessage());
			waterCubeS = new Cube();
		}
		if (waterCubeS.getVertices() == null || waterCubeS.getVertices().length == 0) {
			System.err.println("Sphere shape has no vertices — using Cube fallback");
			waterCubeS = new Cube();
		}
		try {
			radioS = new ImportedModel("radio.obj");
		} catch(Exception e) {
			System.err.println("Failed to load radio.obj: " + e.getMessage());
			radioS = new Cube();
		}
		try {
			lampS = new ImportedModel("lamp.obj");
		} catch(Exception e) {
			System.err.println("Failed to load lamp.obj: " + e.getMessage());
			lampS = new Cube();
		}
		if (lampS.getVertices() == null || lampS.getVertices().length == 0) {
			System.err.println("Lamp model has no vertices — using Cube fallback");
			lampS = new Cube();
		}
		try {
			torchS = new ImportedModel("torch.obj");
		} catch(Exception e) {
			System.err.println("Failed to load torch.obj: " + e.getMessage());
			torchS = new Cube();
		}		
	
		linxS = new Line(new Vector3f(0,0,0), new Vector3f(10,0,0));
		linyS = new Line(new Vector3f(0,0,0), new Vector3f(0,10,0));
		linzS = new Line(new Vector3f(0,0,0), new Vector3f(0,0,-10));
	
		terrS = new TerrainPlane(1000);	
		try {
			borderShape = new Torus();
		} catch(Exception e) {
			System.err.println("Error creating Torus: " + e.getMessage());
			borderShape = new Cube();
		}
		if (borderShape.getVertices() == null || borderShape.getVertices().length == 0) {
			System.err.println("Torus has no vertices — using Cube fallback");
			borderShape = new Cube();
		}
	}
	
    /**
     * Loads and assigns texture images to various objects in the game.
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
		treetx = new TextureImage("treetx2.jpg");
		planttx = new TextureImage("planttx.jpg");
		wheattx = new TextureImage("wheattx.jpg");
		wateringcantx = new TextureImage("watercantx.jpg");
		beetx = new TextureImage("beetx.jpeg");
		radiotx = new TextureImage("radiotx.jpeg");
		lamptx = new TextureImage("lamptx.jpeg");
		torchtx = new TextureImage("torchtx.jpeg");

		grayrabbittx      = new TextureImage("grayrabbittx.jpg");
		yellowrabbittx    = new TextureImage("yellowrabbittx.jpg");
		purplerabbittx    = new TextureImage("purplerabbittx.jpg");
		pinkrabbittx      = new TextureImage("pinkrabbittx.jpg");
		orangerabbittx    = new TextureImage("orangerabbittx.jpg");
		lavenderrabbittx  = new TextureImage("lavenderrabbittx.jpg");
		greenrabbittx     = new TextureImage("greenrabbittx.jpg");
		brownrabbittx = new TextureImage("brownrabbittx.jpg");
		bluerabbittx  = new TextureImage("bluerabbittx.jpg");
		rabbittx          = new TextureImage("rabbittx.jpg");  // white
	
		String file = rabbitColorFiles.get(selectedRabbitColor);
		if (file != null) {
			rabbittx = new TextureImage(file);
		}
		
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
	private TextureImage getChosenRabbitTexture() {
		switch(selectedRabbitColor) {
			case "Gray":     return grayrabbittx;
			case "Yellow":   return yellowrabbittx;
			case "Purple":   return purplerabbittx;
			case "Pink":     return pinkrabbittx;
			case "Orange":   return orangerabbittx;
			case "Lavender": return lavenderrabbittx;
			case "Green":    return greenrabbittx;
			case "Brown": return brownrabbittx;
			case "Blue":  return bluerabbittx;
			default:         return rabbittx;        // white
		}
	}
	
	/**
     * Constructs and initializes scene objects including avatar, terrain, animals, and tools.
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

		pig = new GameObject(GameObject.root(), pigS, pigtx);
		initialTranslation = (new Matrix4f()).translation(2,0,0);
		pig.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.1f));
		pig.setLocalTranslation(initialTranslation);
		pig.setLocalScale(initialScale);

		Vector3f pigPos = pig.getWorldLocation();
		double[] pigXform = {
				1, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 1, 0,
				pigPos.x(), pigPos.y(), pigPos.z(), 1
		};
		
		PhysicsObject pigPhys = physicsEngine.addSphereObject( physicsEngine.nextUID(), 0f, pigXform, 0.3f);
		pig.setPhysicsObject(pigPhys);

		chicken = new GameObject(GameObject.root(), chickenS, chickentx);
		initialTranslation = (new Matrix4f()).translation(0,0,1);
		chicken.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.05f));
		chicken.setLocalTranslation(initialTranslation);
		chicken.setLocalScale(initialScale);
		
		chickenController = new ChickenAnimationController(chicken, this); 
		chickenController.setEnabled(true);
		Vector3f chickenPos = chicken.getWorldLocation();
		double[] chickenXform = {
					1, 0, 0, 0,
					0, 1, 0, 0,
					0, 0, 1, 0,
					chickenPos.x(), chickenPos.y(), chickenPos.z(), 1
		};
		PhysicsObject chickenPhys = physicsEngine.addSphereObject( physicsEngine.nextUID(), 0f, chickenXform, 0.3f );
		chicken.setPhysicsObject(chickenPhys); 

		rabbit = new GameObject(GameObject.root(), rabbitS, getChosenRabbitTexture());
		initialTranslation = (new Matrix4f()).translation(0,0 ,2);
		rabbit.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f().scaling(0.1f));
		rabbit.setLocalTranslation(initialTranslation);
		rabbit.setLocalScale(initialScale);
		avatar = rabbit;

		home = new GameObject(GameObject.root(), homeS, hometx);
		initialTranslation = (new Matrix4f()).translation(-4,0,0);
		home.setLocalTranslation(initialTranslation);
		Matrix4f rotateHome = new Matrix4f().rotateY((float) Math.toRadians(180));
		home.setLocalRotation(rotateHome);
		initialScale = (new Matrix4f().scaling(0.1f));
		home.setLocalTranslation(initialTranslation);
		home.setLocalScale(initialScale);

		Vector3f homePos = home.getWorldLocation();

		double[] homeXform = {
			1, 0, 0, 0,    
			0, 1, 0, 0,    
			0, 0, 1, 0,    
			homePos.x(), homePos.y(), homePos.z(), 1  
		};
		float[] homeHalfExtents = { 0.5f, 1.0f, 0.5f };  
		PhysicsObject homePhys = physicsEngine.addBoxObject(
			physicsEngine.nextUID(),
			0f,              
			homeXform,
			homeHalfExtents
		);
		home.setPhysicsObject(homePhys);

		market = new GameObject(GameObject.root(), marketS, markettx);
		initialTranslation = new Matrix4f().translation(-2, 0, -1);
		market.setLocalTranslation(initialTranslation);
		Matrix4f rotation = new Matrix4f().rotateY((float) Math.toRadians(90));
		market.setLocalRotation(rotation);
		initialScale = new Matrix4f().scaling(0.2f);
		market.setLocalScale(initialScale);

		Vector3f marketPos = market.getWorldLocation();
		double[] marketXform = {
			1, 0, 0, 0,       
			0, 1, 0, 0,       
			0, 0, 1, 0,       
			marketPos.x(), marketPos.y(), marketPos.z(), 1                 
		};
		float[] marketHalfExtents = { 0.5f, 1f, 0.5f };
		PhysicsObject marketPhys = physicsEngine.addBoxObject( physicsEngine.nextUID(),0f, marketXform, marketHalfExtents);
		market.setPhysicsObject(marketPhys);	

		Vector3f homePos1 = home.getWorldLocation();
		radio = new GameObject(GameObject.root(), radioS, radiotx);
		Matrix4f radioTrans = new Matrix4f()
			.translation(homePos1.x(), homePos1.y(), homePos1.z()+ 1.0f);
		Matrix4f rotateRadio = new Matrix4f().rotateY((float) Math.toRadians(180));
		radio.setLocalRotation(rotateRadio);
		radio.setLocalTranslation(radioTrans);
		radio.setLocalScale(new Matrix4f().scaling(0.1f));
	
		double[] xf = {
			1,0,0,0,
			0,1,0,0,
			0,0,1,0,
			homePos.x()+1, homePos.y(), homePos.z(), 1
		};
		float[] halfExtents = { 0.3f, 0.3f, 0.3f };
		PhysicsObject radioPhys = physicsEngine.addBoxObject( physicsEngine.nextUID(), 0f, xf, halfExtents);
		radio.setPhysicsObject(radioPhys);

		Vector3f mpos = market.getWorldLocation();
		float   lampSideOffset   = 0.4f;
		float   lampScale        = 0.1f;

		lampLeft = new GameObject(GameObject.root(), lampS, lamptx);
		lampLeft.setLocalScale(new Matrix4f().scaling(lampScale));
		lampLeft.setLocalTranslation(
			new Matrix4f().translation(
				mpos.x() - lampSideOffset, mpos.y(), mpos.z()
			)
		);
		lampRight = new GameObject(GameObject.root(), lampS, lamptx);
		lampRight.setLocalScale(new Matrix4f().scaling(lampScale));
		lampRight.setLocalTranslation(
			new Matrix4f().translation(
				mpos.x() + lampSideOffset, mpos.y() ,mpos.z()
			)
		);

		tree = new GameObject(GameObject.root(), treeS, treetx);
		initialTranslation = new Matrix4f().translation(3, 0, 1);
		tree.setLocalTranslation(initialTranslation);
		initialScale = new Matrix4f().scaling(0.3f);
		tree.setLocalScale(initialScale);

		torch = new GameObject(rabbit, torchS, torchtx);
		torch.setLocalScale(new Matrix4f().scaling(0.7f));
		torch.setLocalTranslation(new Matrix4f().translation(0.1f, 0.1f, 0.1f));
		torch.getRenderStates().disableRendering();

		bee = new GameObject(GameObject.root(), beeS, beetx);
		initialTranslation = new Matrix4f().translation(2, 0, 1);
		bee.setLocalTranslation(initialTranslation);
		initialScale = new Matrix4f().scaling(0.1f);
		bee.setLocalScale(initialScale);

		((AnimatedShape) bee.getShape()).loadAnimation("FLY", "beeFly.rka");
		((AnimatedShape) bee.getShape()).playAnimation("FLY", 2.0f, AnimatedShape.EndType.LOOP, 0);
		npcCtrl.getNPC().setLocation(2, 0, 1);
		
		Vector3f treeCenter = tree.getWorldLocation();
		Vector3f higherCenter = new Vector3f(treeCenter.x(), treeCenter.y() + 0.5f, treeCenter.z());
		OrbitAroundController beeController = new OrbitAroundController(higherCenter, 0.6f, 0.0005f, bee);
		engine.getSceneGraph().addNodeController(beeController);
		beeController.enable();
		npcCtrl.setOrbitController(beeController);

        try {
			aiServer = new GameAIServerUDP(0, npcCtrl);
			npcCtrl.start(aiServer);
			System.out.println("AI server listening on port " + aiServer.getLocalPort());
					
        } catch (IOException e) {
            e.printStackTrace();
        }

		wateringcan = new GameObject(rabbit, wateringcanS, wateringcantx);
		if (wateringcanS == null || wateringcanS.getVertices() == null || wateringcanS.getVertices().length == 0) {
			System.err.println("Watering can shape invalid at initialization, using Cube fallback");
			wateringcan.setShape(new Cube());
		}
		wateringcan.setLocalTranslation(new Matrix4f().translation(0.1f, 0.1f, 0.1f));
		wateringcan.setLocalRotation(new Matrix4f()); 
		wateringcan.setLocalScale(new Matrix4f().scaling(0.4f));
		wateringcan.getRenderStates().disableRendering();

		x = new GameObject(GameObject.root(), linxS);
		y = new GameObject(GameObject.root(), linyS);
		z = new GameObject(GameObject.root(), linzS);
		(x.getRenderStates()).setColor(new Vector3f(1f,0f,0f));
		(y.getRenderStates()).setColor(new Vector3f(0f,1f,0f));
		(z.getRenderStates()).setColor(new Vector3f(0f,0f,1f));


		terr = new GameObject(GameObject.root(), terrS, dayOneTerrain);
		Matrix4f terrTrans = new Matrix4f().translation(0f, 0f, 0f);
		Matrix4f terrScale = new Matrix4f().scaling(15.0f, 0.3f, 20.0f);
		terr.setLocalTranslation(terrTrans);
		terr.setLocalScale(terrScale);
		terr.setHeightMap(hills);

		terr.getRenderStates().setTiling(1);
		terr.getRenderStates().setTileFactor(4);
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

		double[] planeTransform = new double[] {
			1, 0, 0, 0,  
			0, 1, 0, 0,  
			0, 0, 1, 0, 
			0, 0, 0, 1   
		};

		PhysicsObject terrainPhys = physicsEngine.addStaticPlaneObject( physicsEngine.nextUID(), planeTransform, new float[]{0, 1, 0}, 0f);
		terr.setPhysicsObject(terrainPhys);
		for (float x = -15; x <= 15; x += 3f) {
			createBorderTorus(x, 0, -15); 
			createBorderTorus(x, 0, 15);  
		}
		for (float z = -12; z <= 12; z += 3f) {
			createBorderTorus(-15, 0, z); 
			createBorderTorus(15, 0, z);  
		}
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
			bee.getRenderStates().enableRendering();
			tree.getRenderStates().enableRendering();
			radio.getRenderStates().enableRendering();
			lampLeft.getRenderStates().enableRendering();
			lampRight.getRenderStates().enableRendering();
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
		GLCanvas canvas = engine.getRenderSystem().getGLCanvas();
		canvas.invoke(true, new GLRunnable() {
			@Override
			public boolean run(GLAutoDrawable drawable) {
				try {
					RenderSystem rs = engine.getRenderSystem();
					Method loadVBOs = rs.getClass().getDeclaredMethod("loadVBOs");
					loadVBOs.setAccessible(true);
					loadVBOs.invoke(rs);
					//  generate & load the SSBO once and for all
					LightManager lm = engine.getLightManager();
					Method loadLights = lm.getClass()
										 .getDeclaredMethod("loadLightsSSBOinitial");
					loadLights.setAccessible(true);
					loadLights.invoke(lm);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				return true; 
			}
		});
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

		im.associateActionWithAllKeyboards( net.java.games.input.Component.Identifier.Key.COMMA,
			new AbstractInputAction() {
				public void performAction(float time, Event event) {
					orbitController.zoom(-0.5f); 
				}
			},
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllKeyboards(	net.java.games.input.Component.Identifier.Key.PERIOD,
			new AbstractInputAction() {
				public void performAction(float time, Event event) {
					orbitController.zoom(0.5f); 
				}
			},
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.UP,
			new AbstractInputAction() {
				@Override
				public void performAction(float time, Event event) {
					if (marketMode == MarketMode.CHOOSING || marketMode == MarketMode.SELLING || isBuyingSeeds) {
						int maxOption;
						if (marketMode == MarketMode.CHOOSING) {
							maxOption = 2; 
						} else if (marketMode == MarketMode.SELLING) {
							maxOption = 2; 
						} else { 
							maxOption = 2; 
						}
						selectedMenuOption = (selectedMenuOption + 1) % (maxOption + 1);
					} else {
						orbitController.orbitElevation(2.0f);
					}
				}
			},
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
		);
		
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.DOWN,
			new AbstractInputAction() {
				@Override
				public void performAction(float time, Event event) {
					if (marketMode == MarketMode.CHOOSING || marketMode == MarketMode.SELLING || isBuyingSeeds) {
						int maxOption;
						if (marketMode == MarketMode.CHOOSING) {
							maxOption = 2; 
						} else if (marketMode == MarketMode.SELLING) {
							maxOption = 2; 
						} else {
							maxOption = 2; 
						}
						selectedMenuOption = (selectedMenuOption - 1 + maxOption + 1) % (maxOption + 1);
					} else {
						orbitController.orbitElevation(-2.0f);
					}
				}
			},
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
		);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.SPACE,
			new AbstractInputAction() {
				@Override
				public void performAction(float time, Event event) {
					if (marketMode != MarketMode.NONE || isBuyingSeeds || showNotEnoughCoinsMessage) {
						selectMenuOption();
					}
				}
			},
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
		);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.Y, 
			fwdAction, 
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllGamepads(
		Component.Identifier.Axis.X,
		turnAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Button._0, //X
			new AbstractInputAction() {
				public void performAction(float time, Event e) {
					if (e.getValue() > 0.5f)    
						doPlant();
				}
			},
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
		);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Button._2, //B
			new AbstractInputAction() {
				public void performAction(float time, Event e) {
					if (e.getValue() > 0.5f)
						doHarvest();
				}
			},
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
		);
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Button._7,  //   Z
			new AbstractInputAction() {
				@Override public void performAction(float time, Event e) {
					if (e.getValue() > 0.5f)
						cycleTool();
				}
			},
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
		);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Button._3,  // Y 
			new AbstractInputAction() {
				@Override
				public void performAction(float time, Event e) {
					if (e.getValue() > 0.5f) 
						doUseTool();
				}
			},
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
		);
        im.associateActionWithAllGamepads(
                Component.Identifier.Button._1, //A
                new AbstractInputAction() {
                    @Override
                    public void performAction(float time, Event e) {
                        if (e.getValue() > 0.5f) {
                            if (marketMode == MarketMode.CHOOSING || marketMode == MarketMode.SELLING || isBuyingSeeds) {
                                selectMenuOption();
                            } else {
                                openMenu();
                            }
                        }
                    }
                },
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY
        );
		im.associateActionWithAllGamepads(
			Component.Identifier.Axis.POV,
			new AbstractInputAction() {
				@Override public void performAction(float time, Event e) {
					float pov = e.getValue();
		
					if (marketMode == MarketMode.CHOOSING
					 || marketMode == MarketMode.SELLING
					 || isBuyingSeeds) {
		
						int maxOption = 2;  
						if      (pov == Component.POV.UP) {
						   selectedMenuOption = (selectedMenuOption + 1) % (maxOption + 1);
						}
						else if (pov == Component.POV.DOWN) {
						   selectedMenuOption = (selectedMenuOption - 1 + maxOption + 1) % (maxOption + 1);
						}
					}
					else {
						if      (pov == Component.POV.RIGHT) panRightViewportCamera( 0.1f,  0f);
						else if (pov == Component.POV.LEFT ) panRightViewportCamera(-0.1f,  0f);
						else if (pov == Component.POV.UP   ) panRightViewportCamera( 0f,    0.1f);
						else if (pov == Component.POV.DOWN ) panRightViewportCamera( 0f,   -0.1f);
					}
				}
			},
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllGamepads(
			Component.Identifier.Axis.RZ,
			new AbstractInputAction() {
				@Override public void performAction(float time, Event e) {
					float zoomAmount = e.getValue() * 0.1f;  
					zoomRightViewportCamera(zoomAmount);
				}
			},
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);
		im.associateActionWithAllGamepads(
			Component.Identifier.Axis.RY,
			new AbstractInputAction() {
				boolean hasReset = false;
				@Override public void performAction(float time, Event e) {
					float v = e.getValue();
					if (!hasReset && v > 0.5f) {
						Camera r = engine.getRenderSystem().getViewport("RIGHT").getCamera();
						Camera l = engine.getRenderSystem().getViewport("LEFT") .getCamera();
						if (r != null) resetViewportCamera(r);
						if (l != null) resetViewportCamera(l);
						hasReset = true;
					}
					else if (hasReset && Math.abs(v) < 0.1f) {
						hasReset = false;
					}
				}
			},
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE
		);
		
		synchronized (renderStateQueue) {
			for (Runnable task : renderStateQueue) {
				task.run();
			}
			renderStateQueue.clear();
		}

        initializationComplete = true;
		System.out.println("Finished initializeGame, rendering enabled");
	}

	// In setupNetworking(), add client ID logging after ProtocolClient creation
	private void setupNetworking() {
		isConnected = false;
		System.out.println("[MyGame] Attempting to initialize networking with serverAddress: " + serverAddress + ", serverPort: " + serverPort + ", protocol: " + serverProtocol);
		
		if (serverAddress == null || serverAddress.isEmpty()) {
			System.out.println("[MyGame] Error: serverAddress is null or empty");
			return;
		}
		if (serverPort <= 0 || serverPort > 65535) {
			System.out.println("[MyGame] Error: Invalid serverPort: " + serverPort);
			return;
		}
		
		try {
			InetAddress inetAddress = InetAddress.getByName(serverAddress);
			protClient = new ProtocolClient(inetAddress, serverPort, serverProtocol, this);
			System.out.println("[MyGame] ProtocolClient initialized with client ID: " + protClient.getClientId());
			protClient.sendJoinMessage();
			ProtocolClient aiClient = new ProtocolClient(inetAddress, serverPort + 1, serverProtocol, this);
			aiClient.sendJoinMessage();
			isConnected = true;
			sendTextureSelection();
			System.out.println("[MyGame] Successfully connected to server");
		} catch (UnknownHostException e) {
			System.out.println("[MyGame] Error: Unknown host - " + serverAddress);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("[MyGame] Error: Failed to initialize ProtocolClient - " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("[MyGame] Unexpected error during networking setup: " + e.getMessage());
			e.printStackTrace();
		}
		
		if (protClient == null || !isConnected) {
			System.out.println("[MyGame] Failed to establish network connection. protClient is null: " + (protClient == null));
		}
	}
	/**
	*  New method to log client ID from NPCcontroller
	*/
	public void logNPCClientId(UUID clientId) {
		System.out.println("[MyGame] NPCcontroller received client ID: " + clientId);
	}
	
	/**
     * Updates game logic every frame: animations, physics, AI, HUD, and network state..
	 */
	@Override
	public void update()
	{
		long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastUpdateTime) / 1_000_000f; 
        lastUpdateTime = currentTime;

		if (!initializationComplete) {
            return; 
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
		((AnimatedShape) bee.getShape()).updateAnimation();
		
		for (PlantAnimationController controller : plantControllers) {
			controller.update(deltaTime);
		}
		
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
		
		if (wateringcan != null && wateringcan.getRenderStates().renderingEnabled()) {
			Vector3f rabbitForward = rabbit.getWorldForwardVector().normalize();
			Vector3f rabbitRight = rabbit.getWorldRightVector().normalize();
			Vector3f rabbitUp = rabbit.getWorldUpVector().normalize();
			Vector3f offset = rabbitForward.mul(0.1f)
							.add(rabbitRight.mul(0.1f))
							.add(rabbitUp.mul(0.1f));
			wateringcan.setLocalTranslation(new Matrix4f().translation(offset));
			wateringcan.setLocalRotation(new Matrix4f()); 
		}
		if (torch.getRenderStates().renderingEnabled()) {
			Vector3f fwd   = rabbit.getWorldForwardVector().normalize();
			Vector3f up    = rabbit.getWorldUpVector().normalize();
			Vector3f right = rabbit.getWorldRightVector().normalize();
		
			Vector3f offset = new Vector3f(fwd).mul(0.05f)    
								  .add(new Vector3f(up).mul(0.10f))     
								  .add(new Vector3f(right).mul(0.05f)); 
		
			torch.setLocalTranslation(new Matrix4f().translation(offset));
		
			float yaw = (float)Math.atan2(fwd.x(), fwd.z());
			torch.setLocalRotation(new Matrix4f().rotateY(yaw));
		}
		if (torch.getRenderStates().renderingEnabled()) {
			torchLight.enable();
			torchLight.setLocation(torch.getWorldLocation());
		} else {
			torchLight.disable();
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
		float distToRadio = pos.distance(radio.getWorldLocation());
		
		String hudMessage;
		if (isWatering) {
			hudMessage = "Status: Watering Crops";
		}
		else if (distToHome < 1.0f) {
			hudMessage = "Status: Near the House, click M";
		}
		else if (distToMarket < 1.0f) {
			hudMessage = "Status: Near the Market, click M";
		}
		else if (distToRadio < 1.0f) {
			hudMessage = "Status: Near the Radio, click M";
		}
		else {
			hudMessage = "Status: Roaming the Fields";
		}
		
		im.update((float)elapsTime);
		processNetworking((float)elapsTime);

    	String dispStr1 = "Objective: Buy seeds, plant crops with E, harvest with H, and sell them at the market to earn coins!";
		String dispStr3 = "Surveillance Camera on Rabbit";
		String coinStr = "Coins: " + coins;
		Vector3f hud1Color = new Vector3f(1,0,0); 
		Vector3f hud2Color = new Vector3f(0,0,1);
		Vector3f hud3Color = new Vector3f(0, 0, 1);
		Vector3f hud4Color = new Vector3f(0, 1, 0);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 20, 20);
		(engine.getHUDmanager()).setHUD7(coinStr, hud2Color, 15, 55);
		(engine.getHUDmanager()).setHUD3(hudMessage, hud3Color, 15, 80);
 
        float rightNormX = 0.75f;
        float rightNormY = 0.0f;
        float rightNormWidth = 0.25f;
        float rightNormHeight = 0.25f;

        int windowWidth = engine.getRenderSystem().getWidth();
        int windowHeight = engine.getRenderSystem().getHeight();

        int hudX = (int)((rightNormX + 0.1f * rightNormWidth) * windowWidth);
        int hudY = (int)((rightNormY + 0.15f * rightNormHeight) * windowHeight);

        (engine.getHUDmanager()).setHUD4(dispStr3, hud4Color, hudX, hudY);

		String toolStr = "Tool: " + (activeTool == Tool.TORCH ? "Torch" :
				activeTool == Tool.WATERING_CAN ? "Watering Can" :
				"None");
		engine.getHUDmanager().setHUD5(toolStr, new Vector3f(1f,1f,0f), 20, 140);

		String instr = "Press 1 -> Watering Can   2 -> Torch   SPACE -> Use";
		engine.getHUDmanager().setHUD13(instr, new Vector3f(1f,1f,1f), 20, 170);

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
        Vector3f highlightColor = new Vector3f(1f, 1f, 0f);
        Vector3f normalColor = new Vector3f(1f, 1f, 1f);  
        int baseX = 600;
        int baseY = 200;

        if (marketMode == MarketMode.CHOOSING) {
            engine.getHUDmanager().setHUD8("+--------------------------+", normalColor, baseX, baseY);
            engine.getHUDmanager().setHUD9("| Press to Cancel         |", selectedMenuOption == 0 ? highlightColor : normalColor, baseX, baseY + 30);
            engine.getHUDmanager().setHUD10("| Press to Sell         |", selectedMenuOption == 1 ? highlightColor : normalColor, baseX, baseY + 60);
            engine.getHUDmanager().setHUD11("| Press to Buy       |", selectedMenuOption == 2 ? highlightColor : normalColor, baseX, baseY + 90);
            engine.getHUDmanager().setHUD12("+--------------------------+", normalColor, baseX, baseY + 120);
        } else if (marketMode == MarketMode.SELLING) {
			engine.getHUDmanager().setHUD8("+------------------------------+", normalColor, baseX, baseY);
			engine.getHUDmanager().setHUD9("| Press to exit               |", selectedMenuOption == 0 ? highlightColor : normalColor, baseX, baseY + 30);
			engine.getHUDmanager().setHUD10("| Press to sell everything   |", selectedMenuOption == 1 ? highlightColor : normalColor, baseX, baseY + 60);
			engine.getHUDmanager().setHUD11("| Press to sell only crops   |", selectedMenuOption == 2 ? highlightColor : normalColor, baseX, baseY + 90);
			engine.getHUDmanager().setHUD12("+------------------------------+", normalColor, baseX, baseY + 120);
		} else if (isBuyingSeeds) {
            engine.getHUDmanager().setHUD8("+----------------------+ ", normalColor, baseX, baseY);
            engine.getHUDmanager().setHUD9("| Buy Carrot - 2 Coins      |", selectedMenuOption == 0 ? highlightColor : normalColor, baseX, baseY + 30);
            engine.getHUDmanager().setHUD10("| Buy Wheat - 2 Coins  |", selectedMenuOption == 1 ? highlightColor : normalColor, baseX, baseY + 60);
            engine.getHUDmanager().setHUD11("|  Press to Cancel  |", selectedMenuOption == 2 ? highlightColor : normalColor, baseX, baseY + 90);
            engine.getHUDmanager().setHUD12("+----------------------+ ", normalColor, baseX, baseY + 120);
        } else if (showNotEnoughCoinsMessage) {
            engine.getHUDmanager().setHUD8("+----------------------+ ", new Vector3f(1f, 0.2f, 0.2f), baseX, baseY);
            engine.getHUDmanager().setHUD9("|  Press X to close    |", new Vector3f(1f, 0.2f, 0.2f), baseX, baseY + 30);
            engine.getHUDmanager().setHUD10("|  Not enough coins!   |", new Vector3f(1f, 0.5f, 0.5f), baseX, baseY + 60);
            engine.getHUDmanager().setHUD11("", new Vector3f(0, 0, 0), 0, 0);
            engine.getHUDmanager().setHUD12("+----------------------+ ", new Vector3f(1f, 1f, 1f), baseX, baseY + 90);
        } else {
            engine.getHUDmanager().setHUD8("", new Vector3f(0, 0, 0), 0, 0);
            engine.getHUDmanager().setHUD9("", new Vector3f(0, 0, 0), 0, 0);
            engine.getHUDmanager().setHUD10("", new Vector3f(0, 0, 0), 0, 0);
            engine.getHUDmanager().setHUD11("", new Vector3f(0, 0, 0), 0, 0);
            engine.getHUDmanager().setHUD12("", new Vector3f(0, 0, 0), 0, 0);
        }
		
		cropsToRemove.clear();
		objectsToDisable.clear();

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

			if (System.currentTimeMillis() - physicsActivateTime > PHYSICS_DURATION_MS) {
				physicsEngine.removeObject(avatarPhysicsObject.getUID());
				avatar.setPhysicsObject(null);
				avatarPhysicsObject = null;
				avatarPhysicsActive = false;
				isFaceDown = true; 
				faceDownStartTime = System.currentTimeMillis();
				System.out.println("Physics deactivated, entering face-down state");
			}
		}

		if (isFaceDown) {
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
	
			if (System.currentTimeMillis() - faceDownStartTime > FACE_DOWN_DURATION_MS) {
				isFaceDown = false;
				Matrix4f uprightRotation = new Matrix4f().identity();
				avatar.setLocalRotation(uprightRotation);
				System.out.println("Rabbit stood back up");
			}
		}
				
		boolean chasing = npcCtrl.isPursuingAvatar();
		if (chasing) {
			chaseLight.enable();
			Vector3f avPos = avatar.getWorldLocation();
			chaseLight.setLocation(new Vector3f(avPos.x(), avPos.y() + 2f, avPos.z()));
		} else{
			chaseLight.disable();
		}

		for (int i = 0; i < waterDroplets.size(); i++) {
			PhysicsObject phys = waterDropletPhysics.get(i);
			GameObject droplet = waterDroplets.get(i);
			double[] t = phys.getTransform();
			Vector3f dropPos = new Vector3f((float)t[12], (float)t[13], (float)t[14]);
	
			Crop hitCrop = null;
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
				hitCrop.water(10.0);
	
				float upVel = 0.5f;    
				float horiz = 0.1f;   
				float vx = ((float)Math.random() - 0.5f) * horiz;
				float vz = ((float)Math.random() - 0.5f) * horiz;
				phys.setLinearVelocity(new float[]{ vx, upVel, vz });	
				hasBouncedOffPlant.set(i, true);
	
				waterGrounded.set(i, false);
				waterGroundTime.set(i, 0f);
			}
		}

		for (Crop crop : activeCrops) {
			if (crop.updateAndCheckReady()) {
				UUID id = crop.getId();
				if (!plantLights.containsKey(id)) {
					spawnSpotlightFor(crop);
					if (protClient != null && isConnected) {
						Vector3f pos2 = crop.getPlantedObject().getWorldLocation();
						try {
							protClient.sendGrowMessage(
								crop.getId().toString(),
								pos2,
								crop.getType()
							);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		
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
		cleanActiveCrops();
		gm.cleanGhostAvatars(deltaTime / 1000f);


		if (shouldAttachWateringCan) {
			wateringcan.getRenderStates().enableRendering();
			shouldAttachWateringCan = false;
		}
		
		if (shouldDetachWateringCan) {
			wateringcan.getRenderStates().disableRendering();
			shouldDetachWateringCan = false;
		}

		currFrameTime = System.currentTimeMillis();
		float dtMs = (float)(currFrameTime - lastFrameTime);
		lastFrameTime = currFrameTime;

		if (isWatering && spawnTimer >= DROP_INTERVAL) {
			spawnWaterDroplet();
			spawnTimer = 0f;
		}
		
		physicsEngine.update(dtMs);
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

			if (System.currentTimeMillis() - physicsActivateTime > PHYSICS_DURATION_MS) {
				physicsEngine.removeObject(avatarPhysicsObject.getUID());
				avatar.setPhysicsObject(null);
				avatarPhysicsObject = null;
				avatarPhysicsActive = false;
				System.out.println("Physics deactivated, resuming regular movement");
			}
		}

		float dtSec = dtMs * 0.001f;
		spawnTimer += dtSec;
		gm.updateAllGhostCans();
		gm.updateAllGhostDroplets(dtSec);
		gm.updateAllGhostCrops();
		gm.updateAllGhostTorches();

		if (npcCtrl.isPursuingAvatar()) {
			NPC theBee = npcCtrl.getNPC();
			Vector3f p = new Vector3f((float) theBee.getX(), (float) theBee.getY(), (float) theBee.getZ());
			bee.setLocalTranslation(new Matrix4f().translation(p.x(), p.y(), p.z()));
		}

		float distToBee = avatar.getWorldLocation().distance(bee.getWorldLocation());

		if (distToBee < 15.0f) {
			if (!isBuzzing) {
				beeBuzzSound.setLocation(bee.getWorldLocation());
				beeBuzzSound.play();
				isBuzzing = true;
			} else {
				beeBuzzSound.setLocation(bee.getWorldLocation()); 
			}
		} else {
			if (isBuzzing) {
				beeBuzzSound.stop();
				isBuzzing = false;
			}
		}
		setEarParameters();
		Vector3f currentPigPos = pig.getWorldLocation();
		Vector3f currentChickPos = chicken.getWorldLocation();
		
		float pigMoved = currentPigPos.distance(lastPigPos);
		float chickMoved = currentChickPos.distance(lastChickenPos);
		
		if (pigSoundCooldown <= 0 && pigMoved > 0.01f && Math.random() < 0.01) {
			pigOinkSound.setLocation(currentPigPos);
			pigOinkSound.play();
			pigSoundCooldown = 3000 + (float)(Math.random() * 2000);
		}
		
		if (chickenSoundCooldown <= 0 && chickMoved > 0.01f && Math.random() < 0.01) {
			chickenCluckSound.setLocation(currentChickPos);
			chickenCluckSound.play();
			chickenSoundCooldown = 3000 + (float)(Math.random() * 2000);
		}
		
		lastPigPos = new Vector3f(currentPigPos);
		lastChickenPos = new Vector3f(currentChickPos);
		if (pigSoundCooldown > 0) pigSoundCooldown -= dtMs;
		if (chickenSoundCooldown > 0) chickenSoundCooldown -= dtMs;

		for (int i = 0; i < waterDroplets.size(); i++) {
			PhysicsObject phys = waterDropletPhysics.get(i);
			GameObject droplet = waterDroplets.get(i);
			double[] t = phys.getTransform();
			float y = (float)t[13];
			droplet.setLocalTranslation(new Matrix4f().translation((float)t[12], y, (float)t[14]));

			if (!waterGrounded.get(i) && y <= 0.01f) {
				waterGrounded.set(i, true);
				phys.setLinearVelocity(new float[]{
					(float)(Math.random() - 0.5) * 2f,
					1.5f,
					(float)(Math.random() - 0.5) * 2f
				});
			}

			if (waterGrounded.get(i)) {
				float age = waterGroundTime.get(i) + dtSec;
				waterGroundTime.set(i, age);
				if (age >= DROPLET_LIFETIME) {
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
		if (wateringSound != null && isWatering) {
			wateringSound.setLocation(wateringcan.getWorldLocation());
		}
		if (torch.getRenderStates().renderingEnabled() && fireSound != null) {
			fireSound.setLocation(torch.getWorldLocation());
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
	
		if (inMarketUI && !allowedWhileMarket) return; 

		switch (keyCode)
		{	
			case KeyEvent.VK_0:
				shouldResetSkybox = true;
				break;


			case KeyEvent.VK_1:
				if(!inMarketUI){
					activeTool = Tool.WATERING_CAN;
					torch.getRenderStates().disableRendering();
					wateringcan.getRenderStates().enableRendering();
				}
			break;

			case KeyEvent.VK_2:
				if(!inMarketUI){
					activeTool = Tool.TORCH;
					wateringcan.getRenderStates().disableRendering();
					torch.getRenderStates().enableRendering();
				}

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
					resetViewportCamera(rightCam);
				}
				if (leftCam != null) {
					resetViewportCamera(leftCam);
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
				System.exit(0); 
			break;

			case KeyEvent.VK_H:
				doHarvest();
			break;
			
			case KeyEvent.VK_M:
				openMenu();
			break;
			
			case KeyEvent.VK_E:
				doPlant(); 
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
				System.out.println("[MyGame] SPACE key pressed. MarketMode: " + marketMode + ", isBuyingSeeds: " + isBuyingSeeds + ", showNotEnoughCoinsMessage: " + showNotEnoughCoinsMessage + ", selectedMenuOption: " + selectedMenuOption);
				if (marketMode != MarketMode.NONE || isBuyingSeeds || showNotEnoughCoinsMessage) {
					System.out.println("[MyGame] Calling selectMenuOption for menu interaction");
					selectMenuOption();
					break;
				}
				switch(activeTool){
					case WATERING_CAN:
						isWatering = !isWatering;
						if (protClient != null && isConnected) {
							protClient.sendWateringMessage(isWatering);
						}
						if (isWatering) {
							shouldAttachWateringCan = true;
							wateringSound.setLocation(avatar.getWorldLocation());
							wateringSound.play();
						} else {
							shouldDetachWateringCan = true;
							wateringSound.stop();
						}
					break;
					case TORCH:
						boolean newTorchState = !torch.getRenderStates().renderingEnabled();
						if (newTorchState) {
							torch.getRenderStates().enableRendering();
							fireSound.play();
						} else {
							torch.getRenderStates().disableRendering();
							fireSound.stop();
						}
						protClient.sendTorchMessage(newTorchState);
					break;
					default:
						break;
				}
				break;
		}
		super.keyPressed(e);
	}

	/**
	 * Exactly the same as SPACE: toggles use of the active tool (watering can or torch).
	 */
	public void doUseTool() {
		switch(activeTool) {
			case WATERING_CAN:
				isWatering = !isWatering;             
				if (isWatering) {
					shouldAttachWateringCan = true;
					wateringSound.setLocation(avatar.getWorldLocation());
					wateringSound.play();
				} else {
					shouldDetachWateringCan = true;
					wateringSound.stop();
				}
				break;

			case TORCH:
				if (torch.getRenderStates().renderingEnabled()) {
					torch.getRenderStates().disableRendering();
					fireSound.stop();
				} else {
					torch.getRenderStates().enableRendering();
					fireSound.play();
				}
				break;

			default:
				break;
		}
	}

	/**
     * Plants the first available seed from inventory, creating a Crop instance at the rabbit's position. Same logic as clicking E on the keyboard.
	 */
	public void doPlant() {
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
				double growTime       = cropType.equals("Carrot") ? 45 : 30;
				ObjShape targetShape = cropType.equals("Carrot") ? carrotS : wheatS;
				TextureImage targetTexture = cropType.equals("Carrot") ? carrottx : wheattx;

				Crop crop = new Crop(cropType, growTime, targetShape, targetTexture);
				crop.setPlantedObject(planted);
				activeCrops.add(crop);

				compactInventory();

				if (planted.getShape() instanceof AnimatedShape) {
					PlantAnimationController pac = new PlantAnimationController(planted);
					plantControllers.add(pac);
				}

				if (protClient != null && isConnected) {
					try {
						protClient.sendPlantMessage(position, crop.getId().toString(), cropType);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}

				break; 
			}
		}
	}
	private void openMenu() {
		Vector3f pos = avatar.getWorldLocation();
		float distToMarket = pos.distance(market.getWorldLocation());
		float distToHome   = pos.distance(home .getWorldLocation());
		float distToRadio  = pos.distance(radio.getWorldLocation());
	
		if (distToMarket < 1.0f) {
			marketMode = MarketMode.CHOOSING;
		}
		else if (distToHome < 1.0f) {
			shouldResetSkybox = true;
		}
		else if (distToRadio < 1.0f) {
			if (!radioOn) {
				backgroundMusic.setLocation(radio.getWorldLocation());
				backgroundMusic.play();
			} else {
				backgroundMusic.stop();
			}
			radioOn = !radioOn;
		}
	}
	
	/**
     * Harvests the nearest ready crop and adds it to the player's inventory. Same logic as clicking H on the keyboard.
	 */
    public void doHarvest() {
        if (marketMode != MarketMode.NONE || isBuyingSeeds) {
            System.out.println("[MyGame] Harvest aborted: In market mode or buying seeds");
            return;
        }

        Crop nearest = null;
        float bestDist = Float.MAX_VALUE;
        Vector3f avatarPos = avatar.getWorldLocation();
        synchronized (activeCrops) {
            for (Crop c : activeCrops) {
                if (c == null) {
                    System.out.println("[MyGame] Warning: Null crop found in activeCrops");
                    continue;
                }
                if (!c.isHarvested() && c.isReadyToHarvest() && c.hasGrown()) {
                    GameObject p = c.getPlantedObject();
                    if (p == null) {
                        System.out.println("[MyGame] Warning: Crop ID " + c.getId() + " has null GameObject");
                        continue;
                    }
                    float d = avatarPos.distance(p.getWorldLocation());
                    if (d < 2.0f && d < bestDist) { // Increased range to 2.0
                        bestDist = d;
                        nearest = c;
                    }
                } else {
                    System.out.println("[MyGame] Crop ID " + c.getId() + " skipped: " +
                        "isHarvested=" + c.isHarvested() + ", isReadyToHarvest=" + c.isReadyToHarvest() +
                        ", hasGrown=" + c.hasGrown());
                }
            }
        }

        if (nearest == null) {
            System.out.println("[MyGame] No harvestable crop found within 2.0 units");
            return;
        }

        if (inventoryCount >= 5) {
            System.out.println("[MyGame] Inventory full!");
            return;
        }

        GameObject p = nearest.getPlantedObject();
        synchronized (cropsToRemove) { cropsToRemove.add(nearest); }
        synchronized (objectsToDisable) { objectsToDisable.add(p); }

        // Disable and remove the spotlight for this crop
        synchronized (plantLights) {
            Light spot = plantLights.get(nearest.getId());
            if (spot != null) {
                System.out.println("[MyGame] Disabling light for crop ID: " + nearest.getId());
                spot.disable();
                try {
                    engine.getSceneGraph().removeLight(spot);
                    System.out.println("[MyGame] Light removed from scene graph for crop ID: " + nearest.getId());
                } catch (Exception e) {
                    System.err.println("[MyGame] Error removing light from scene graph for crop ID: " + nearest.getId() + ": " + e.getMessage());
                }
                plantLights.remove(nearest.getId());
                System.out.println("[MyGame] Light removed from plantLights, map size: " + plantLights.size());
            } else {
                System.out.println("[MyGame] No light found for crop ID: " + nearest.getId());
            }
        }

        p.getRenderStates().disableRendering();
        nearest.markHarvested();
        inventory[inventoryCount++] = nearest.getType();

        if (protClient != null && isConnected) {
            try {
                protClient.sendHarvestMessage(nearest.getId().toString());
                System.out.println("[MyGame] Sent harvest message for crop ID: " + nearest.getId());
            } catch (IOException ex) {
                System.err.println("[MyGame] Error sending harvest message: " + ex.getMessage());
            }
        }
        System.out.println("[MyGame] Harvested " + nearest.getType());
    }
	/**
     * Retrieves the player's avatar GameObject.
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
		Vector3f fwd = avatar.getWorldForwardVector().normalize();
		Vector3f loc = avatar.getWorldLocation();
		float step = movementSpeed * direction * (float)elapsTime;
		Vector3f newLoc = loc.add(new Vector3f(fwd).mul(step));
		float blockRadius = 0.50f;
	
		if (newLoc.distance(home.getWorldLocation()) < blockRadius) {
			return;
		}
		if (newLoc.distance(market.getWorldLocation()) < blockRadius) {
			return;
		}
	
		float minX = -12, maxX = 12, minZ = -12, maxZ = 12;
		if (newLoc.x() < minX || newLoc.x() > maxX || newLoc.z() < minZ || newLoc.z() > maxZ) {
			return;
		}	
		avatar.setLocalLocation(newLoc);
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
	
		renderSystem.addViewport("MAIN", 0, 0, 1f, 1f);
		renderSystem.addViewport("LEFT", 0, 0, 1f, 1f);
		renderSystem.addViewport("RIGHT", 0.75f, 0, 0.25f, 0.25f);
	
		Viewport mainVp = renderSystem.getViewport("MAIN");
		Viewport leftVp = renderSystem.getViewport("LEFT");
		Viewport rightVp = renderSystem.getViewport("RIGHT");
		if (mainVp == null || leftVp == null || rightVp == null) {
			System.out.println("Failed to create viewports!");
			return;
		}
	
		mainVp.getCamera().setLocation(new Vector3f(0, 2, 0));
		mainVp.getCamera().setU(new Vector3f(1, 0, 0));
		mainVp.getCamera().setV(new Vector3f(0, 1, 0));
		mainVp.getCamera().setN(new Vector3f(0, 0, -1));
	
		leftVp.getCamera().setLocation(new Vector3f(-2, 0, 2));
		leftVp.getCamera().setU(new Vector3f(1, 0, 0));
		leftVp.getCamera().setV(new Vector3f(0, 1, 0));
		leftVp.getCamera().setN(new Vector3f(0, 0, -1));
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
	 * (avatar's current position + original offset).
	 * 
	 * The camera's orientation (U, V, N vectors) remains unchanged.
	 *
	 * @param cam Camera to reset
	 * @param offset The original offset to maintain relative positioning
	 */
	private void resetViewportCamera(Camera cam) {
		Vector3f avatarPos = avatar.getWorldLocation();
		
		float cameraHeight = avatarPos.y() + 2.0f; 
		cam.setLocation(new Vector3f(avatarPos.x(), cameraHeight, avatarPos.z()));
		
		cam.setN(new Vector3f(0, -1, 0)); 
		cam.setV(new Vector3f(0, 0, -1)); 
		cam.setU(new Vector3f(1, 0, 0));  
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
	public ObjShape getGhostShape() { return rabbitS; }
	/**
	 * Retrieves the texture assigned to ghost avatars (if any).
	*/
	public TextureImage getGhostTexture() { return rabbittx; }
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
	/**
	 * Sets the Skybox indexes for networking.
	 */
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
	/**
	 * Gets the status of whether the avatar is in market mode.
	*/
	public MarketMode getMarketMode() { return marketMode; }
	/**
	 * Gets the status of whether the avatar is currently buying seeds via interactions with HUD message.
	*/
	public boolean isBuyingSeeds() { return isBuyingSeeds; }

	private void createBorderTorus(float x, float y, float z) {
		GameObject torus = new GameObject(GameObject.root(), borderShape, null);
		Matrix4f pos = new Matrix4f().translation(x, y + 0.05f, z);
		Matrix4f scale = new Matrix4f().scaling(0.4f);
		torus.setLocalTranslation(pos);
		torus.setLocalScale(scale);
		torus.getRenderStates().setColor(new Vector3f(1f, 0.5f, 0.2f)); 

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
		GameObject droplet = new GameObject(GameObject.root(), waterCubeS, null);
		droplet.getRenderStates().enableRendering();
		droplet.getRenderStates().setColor(new Vector3f(0.0f, 0.7f, 1.0f)); 
		
		Vector3f canPos = wateringcan.getWorldLocation();
		Vector3f canForward = wateringcan.getWorldForwardVector();
		Vector3f canUp = wateringcan.getWorldUpVector();
		
		float spoutForwardOffset = 0.07f; 
		float spoutUpOffset = 0.1f;      
		float spoutRightOffset = 0.0f;  
		
		Vector3f spawnPos = canPos
			.add(canForward.mul(spoutForwardOffset))
			.add(canUp.mul(spoutUpOffset))
			.add(wateringcan.getWorldRightVector().mul(spoutRightOffset));
		
		float dropletSize = 0.01f; 
		droplet.setLocalScale(new Matrix4f().scaling(dropletSize));
		
		double[] xform = new double[] {
			  1, 0, 0,      0,           0,  1, 0,     0,           0,  0, 1,     0,
			spawnPos.x, spawnPos.y, spawnPos.z,      1
		};
		
		
		PhysicsObject phys = physicsEngine.addSphereObject( physicsEngine.nextUID(), 0.05f, xform, dropletSize);
		phys.setBounciness(0.2f); 
		phys.setDamping(0.1f, 0.1f); 
		phys.setFriction(0.1f); 
		
		float initialDownwardVelocity = -2.0f; 
		phys.setLinearVelocity(new float[]{
			(float)(Math.random() - 0.5f) * 0.2f, 
			initialDownwardVelocity,
			(float)(Math.random() - 0.5f) * 0.2f
		});
		
		droplet.setLocalTranslation(new Matrix4f().translation(spawnPos));
		droplet.setPhysicsObject(phys);
		
		waterDroplets.add(droplet);
		waterDropletPhysics.add(phys);
		waterGrounded.add(false);
		waterGroundTime.add(0f);
		hasBouncedOffPlant.add(false);
		
		}
		/**
		 * Allows GhostManager to grab the watering can shape.
		 */
		public ObjShape getWateringCanShape() {  return wateringcanS; }
		/**
		 * Allows GhostManager to grab the watering can texture.
		 */
		public TextureImage getWateringCanTexture() {  return wateringcantx; }
        /** so GhostManager can spawn droplets */
		public ObjShape getWaterCubeShape() {
			return waterCubeS;
		}
		/** 
		 * Called whenever any client (you or someone else) harvests a crop.
		 * Removes that crop from your local activeCrops list and hides its object.
		 */
		public void onCropHarvested(UUID cropId) {
			synchronized (activeCrops) {
				Iterator<Crop> it = activeCrops.iterator();
				while (it.hasNext()) {
					Crop c = it.next();
					if (c == null) {
						System.out.println("[MyGame] Warning: Null crop found in activeCrops during harvest");
						continue;
					}
					if (c.getId().equals(cropId)) {
						GameObject p = c.getPlantedObject();
						if (p != null) {
							p.getRenderStates().disableRendering();
							synchronized (objectsToDisable) { objectsToDisable.add(p); }
						} else {
							System.out.println("[MyGame] Warning: Crop ID " + cropId + " has null GameObject in onCropHarvested");
						}
						// Remove associated light
						synchronized (plantLights) {
							Light spot = plantLights.get(cropId);
							if (spot != null) {
								System.out.println("[MyGame] Disabling light for remotely harvested crop ID: " + cropId);
								spot.disable();
								try {
									engine.getSceneGraph().removeLight(spot);
									System.out.println("[MyGame] Light removed from scene graph for remotely harvested crop ID: " + cropId);
								} catch (Exception e) {
									System.err.println("[MyGame] Error removing light for remotely harvested crop ID: " + cropId + ": " + e.getMessage());
								}
								plantLights.remove(cropId);
								System.out.println("[MyGame] Light removed from plantLights for crop ID: " + cropId + ", map size: " + plantLights.size());
							} else {
								System.out.println("[MyGame] No light found for remotely harvested crop ID: " + cropId);
							}
						}
						it.remove();
						System.out.println("[MyGame] Removed crop ID " + cropId + " from activeCrops");
						break;
					}
				}
			}
		}
	
		/** Called by ProtocolClient when you’re the target of a bee attack. */
		public void applyBeeKnockback(Vector3f impulse) {
			System.out.println("[MyGame] applyBeeKnockback() invoked with impulse: " + impulse);
			if (avatar == null) {
				System.out.println("[MyGame] Error: avatar is null");
				return;
			}
		
			if (!avatarPhysicsActive) {
				Vector3f p = avatar.getWorldLocation();
				double[] xform = {
					1,0,0,0,
					0,1,0,0,
					0,0,1,0,
					p.x(),p.y(),p.z(),1
				};
				avatarPhysicsObject = physicsEngine.addSphereObject( physicsEngine.nextUID(), 1f, xform, 0.3f);
				avatar.setPhysicsObject(avatarPhysicsObject);
				avatarPhysicsActive = true;
				physicsActivateTime = System.currentTimeMillis();
				System.out.println("[MyGame] Physics body created for avatar");
			}
		
			float fx = impulse.x;
			float fz = impulse.z;
			avatarPhysicsObject.applyForce( fx, 0f, fz,  0f, 0f, 0f );
			System.out.println("[MyGame] Applied force to avatar physics body: " + impulse);
			isFaceDown = false;
		}
		/**
		 * Updates the audio listener parameters each frame.
		 */		
		public void setEarParameters() {
			Camera camera = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
			audioMgr.getEar().setLocation(avatar.getWorldLocation());
			audioMgr.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
		}
		/**
		 * Cycles through available tools (none, watering can, torch).
		 */		
		public void cycleTool() {
			wateringcan.getRenderStates().disableRendering();
			torch.getRenderStates().disableRendering();
		
			switch(activeTool) {
				case NONE:
					activeTool = Tool.WATERING_CAN;
					wateringcan.getRenderStates().enableRendering();
					break;
				case WATERING_CAN:
					activeTool = Tool.TORCH;
					torch.getRenderStates().enableRendering();
					break;
				case TORCH:
					activeTool = Tool.NONE;
					break;
			}
		}

		// Method to handle menu option selection
		private void selectMenuOption() {
			System.out.println("[MyGame] selectMenuOption called. MarketMode: " + marketMode + ", isBuyingSeeds: " + isBuyingSeeds + ", showNotEnoughCoinsMessage: " + showNotEnoughCoinsMessage + ", selectedMenuOption: " + selectedMenuOption);
			if (marketMode == MarketMode.CHOOSING) {
				switch (selectedMenuOption) {
					case 0: 
						System.out.println("[MyGame] Selected Cancel, closing market menu");
						marketMode = MarketMode.NONE;
						selectedMenuOption = 0;
					break;
					case 1: 
						System.out.println("[MyGame] Selected Sell, entering SELLING mode");
						marketMode = MarketMode.SELLING;
						selectedMenuOption = 0;
					break;
					case 2: 
						System.out.println("[MyGame] Selected Buy, entering seed shop");
						isBuyingSeeds = true;
						marketMode = MarketMode.NONE;
						selectedMenuOption = 0;
					break;
				}
			} else if (marketMode == MarketMode.SELLING) {
				switch (selectedMenuOption) {
					case 0: 
						System.out.println("[MyGame] Selected Exit, closing sell menu");
						marketMode = MarketMode.NONE;
						selectedMenuOption = 0;
						break;
					case 1: 
						System.out.println("[MyGame] Selected Sell Everything, selling all items in inventory");
						for (int i = 0; i < inventory.length; i++) {
							if (inventory[i] != null) {
								if (inventory[i].equals("Wheat")) {
									coins += 5;
								} else if (inventory[i].equals("Carrot")) {
									coins += 10;
								} else if (inventory[i].startsWith("Seed")) {
									coins += 1;
								}
								inventory[i] = null;
							}
						}
						compactInventory();
						inventoryCount = 0;
						break;
					case 2: 
						System.out.println("[MyGame] Selected Sell Only Crops, selling all crops in inventory");
						for (int i = 0; i < inventory.length; i++) {
							if (inventory[i] != null && !inventory[i].startsWith("Seed")) {
								if (inventory[i].equals("Wheat")) {
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
					break;
				}
			} else if (isBuyingSeeds) {
				switch (selectedMenuOption) {
					case 0: 
						System.out.println("[MyGame] Selected Buy Carrot, coins: " + coins + ", inventoryCount: " + inventoryCount);
						if (coins >= 2 && inventoryCount < inventory.length) {
							selectedSeedType = "Seed_Carrot";
							for (int i = 0; i < inventory.length; i++) {
								if (inventory[i] == null) {
									inventory[i] = selectedSeedType;
									inventoryCount++;
									coins -= 2;
									break;
								}
							}
							isBuyingSeeds = false;
							selectedMenuOption = 0;
							System.out.println("[MyGame] Bought Seed_Carrot, new coins: " + coins);
						} else {
							showNotEnoughCoinsMessage = true;
							System.out.println("[MyGame] Cannot buy Carrot: " + (coins < 2 ? "Not enough coins" : "Inventory full"));
						}
						break;
					case 1: 
						System.out.println("[MyGame] Selected Buy Wheat, coins: " + coins + ", inventoryCount: " + inventoryCount);
						if (coins >= 2 && inventoryCount < inventory.length) {
							selectedSeedType = "Seed_Wheat";
							for (int i = 0; i < inventory.length; i++) {
								if (inventory[i] == null) {
									inventory[i] = selectedSeedType;
									inventoryCount++;
									coins -= 2;
									break;
								}
							}
							isBuyingSeeds = false;
							selectedMenuOption = 0;
							System.out.println("[MyGame] Bought Seed_Wheat, new coins: " + coins);
						} else {
							showNotEnoughCoinsMessage = true;
							System.out.println("[MyGame] Cannot buy Wheat: " + (coins < 2 ? "Not enough coins" : "Inventory full"));
						}
						break;
					case 2: 
						System.out.println("[MyGame] Selected Exit Seed Shop");
						isBuyingSeeds = false;
						selectedMenuOption = 0;
						break;
				}
			}
			if (showNotEnoughCoinsMessage) {
				System.out.println("[MyGame] Showing not enough coins message, closing with SPACE");
				showNotEnoughCoinsMessage = false;
				selectedMenuOption = 0;
			}
		}
		/**
		 * Sends the selected rabbit texture color to the server via a details-for message.
		 */
		private void sendTextureSelection() {
			if (protClient != null && isConnected) {
				try {
					UUID clientId = protClient.getClientId();
					Vector3f pos = avatar.getWorldLocation();
					if (clientId == null) {
						throw new IllegalStateException("Client ID is null");
					}
					if (pos == null) {
						throw new IllegalStateException("Avatar position is null");
					}
					protClient.sendCreateMessage(pos);
					System.out.println("[MyGame] Sent create message with texture: create," + clientId + "," + 
									 pos.x() + "," + pos.y() + "," + pos.z() + "," + selectedRabbitColor);
				} catch (IllegalStateException e) {
					System.out.println("[MyGame] Invalid state for sending texture selection: " + e.getMessage());
					e.printStackTrace();
				}
			} else {
				System.out.println("[MyGame] Cannot send texture selection, client not connected.");
			}
		}
		/**
		 * Processes incoming create packets from the server for ghost creation.
		 *
		 * @param message the raw packet string
		 */
		public void handleCreatePacket(String message) {
			try {
				String[] parts = message.split(",");
				if (parts.length < 6) {
					if (message.equals("create,success")) {
						System.out.println("[MyGame] Received create,success packet, ignoring");
						return;
					}
					throw new IllegalArgumentException("Invalid create packet format: " + message);
				}
				UUID clientId = UUID.fromString(parts[1]);
				float x = Float.parseFloat(parts[2]);
				float y = Float.parseFloat(parts[3]);
				float z = Float.parseFloat(parts[4]);
				String color = parts[5];
				Vector3f position = new Vector3f(x, y, z);
				System.out.println("[MyGame] Processing create packet for client " + clientId + " at " + position + " with color " + color);
				gm.createGhost(clientId, position, color);
			} catch (Exception e) {
				System.out.println("[MyGame] Error processing create packet: " + message + ", error: " + e.getMessage());
				e.printStackTrace();
			}
		}
		/**
		 * Retrieves the texture for a specified ghost color name.
		 *
		 * @param color the color name string
		 * @return the corresponding TextureImage
		 */
		public TextureImage getGhostTexture(String color) {
			String file = rabbitColorFiles.getOrDefault(color, "rabbittx.jpg");
			switch (file) {
				case "grayrabbittx.jpg": return grayrabbittx;
				case "yellowrabbittx.jpg": return yellowrabbittx;
				case "purplerabbittx.jpg": return purplerabbittx;
				case "pinkrabbittx.jpg": return pinkrabbittx;
				case "orangerabbittx.jpg": return orangerabbittx;
				case "lavenderrabbittx.jpg": return lavenderrabbittx;
				case "greenrabbittx.jpg": return greenrabbittx;
				case "brownrabbittx.jpg": return brownrabbittx;
				case "bluerabbittx.jpg": return bluerabbittx;
				default: return rabbittx; // White
			}
		}
		private void spawnSpotlightFor(Crop crop) {
			if (crop == null) {
				System.out.println("[MyGame] Error: Attempted to spawn spotlight for null crop");
				return;
			}
			if (crop.isHarvested() || !crop.hasGrown()) {
				System.out.println("[MyGame] Skipping spotlight spawn for crop ID: " + crop.getId() +
					", isHarvested=" + crop.isHarvested() + ", hasGrown=" + crop.hasGrown());
				return;
			}
			synchronized (plantLights) {
				if (plantLights.containsKey(crop.getId())) {
					System.out.println("[MyGame] Spotlight already exists for crop ID: " + crop.getId());
					return;
				}
				GameObject plantGO = crop.getPlantedObject();
				if (plantGO == null) {
					System.out.println("[MyGame] Error: No GameObject for crop ID: " + crop.getId());
					return;
				}
				Vector3f pos = plantGO.getWorldLocation();
				Light spot = new Light();
				spot.setType(Light.LightType.SPOTLIGHT);
				spot.setLocation(new Vector3f(pos.x(), pos.y() + 1.5f, pos.z()));
				spot.setDirection(new Vector3f(0, -1, 0));
				spot.setCutoffAngle(20f);
				spot.setOffAxisExponent(5f);
				spot.setAmbient(0.05f, 0.1f, 0.05f);
				spot.setDiffuse(0.5f, 1.0f, 0.5f);
				spot.setSpecular(0.5f, 1.0f, 0.5f);
				spot.setRange(4.0f);
				spot.setConstantAttenuation(1.0f);
				spot.setLinearAttenuation(0.2f);
				spot.setQuadraticAttenuation(0.05f);
				try {
					engine.getSceneGraph().addLight(spot);
					plantLights.put(crop.getId(), spot);
					System.out.println("[MyGame] Added light for crop ID: " + crop.getId());
				} catch (Exception e) {
					System.err.println("[MyGame] Error adding light for crop ID: " + crop.getId() + ": " + e.getMessage());
				}
			}
		}
	
		/**
		 * Handles a remote crop growth event, updating local state and visuals.
		 *
		 * @param cropId the UUID of the crop that grew
		 * @param pos the world position of the crop
		 * @param type the crop type string
		*/			
		public void onRemoteGrow(UUID cropId, Vector3f pos, String type) {
			synchronized (activeCrops) {
				for (Crop c : activeCrops) {
					if (c == null) {
						System.out.println("[MyGame] Warning: Null crop found in activeCrops during onRemoteGrow");
						continue;
					}
					if (c.getId().equals(cropId)) {
						if (c.isHarvested()) {
							System.out.println("[MyGame] Ignoring grow message for harvested crop ID: " + cropId);
							return;
						}
						c.forceGrowNow();
						spawnSpotlightFor(c);
						System.out.println("[MyGame] Updated existing crop ID " + cropId + " to grown state");
						return;
					}
				}
				System.out.println("[MyGame] Crop ID " + cropId + " not found locally, creating new crop");
				ObjShape targetShape = type.equals("Carrot") ? carrotS : wheatS;
				TextureImage targetTexture = type.equals("Carrot") ? carrottx : wheattx;
				GameObject planted = new GameObject(GameObject.root(), plantS, planttx);
				planted.setLocalTranslation(new Matrix4f().translation(pos.x(), 0, pos.z()));
				planted.setLocalScale(new Matrix4f().scaling(0.020f));
				double growTime = type.equals("Carrot") ? 45 : 30;
				Crop crop = new Crop(type, growTime, targetShape, targetTexture);
				crop.setPlantedObject(planted);
				crop.setId(cropId);
				crop.forceGrowNow();
				activeCrops.add(crop);
				spawnSpotlightFor(crop);
				System.out.println("[MyGame] Created new crop ID " + cropId + " of type " + type + " at " + pos);
			}
		}
		   /**
     * Cleans up invalid or null crops from activeCrops to prevent errors.
     */
    public void cleanActiveCrops() {
        synchronized (activeCrops) {
            Iterator<Crop> it = activeCrops.iterator();
            while (it.hasNext()) {
                Crop c = it.next();
                if (c == null || c.getPlantedObject() == null) {
                    if (c != null) {
                        System.out.println("[MyGame] Removing invalid crop ID: " + c.getId());
                        synchronized (plantLights) {
                            Light spot = plantLights.get(c.getId());
                            if (spot != null) {
                                spot.disable();
                                try {
                                    engine.getSceneGraph().removeLight(spot);
                                    System.out.println("[MyGame] Removed light for invalid crop ID: " + c.getId());
                                } catch (Exception e) {
                                    System.err.println("[MyGame] Error removing light for invalid crop ID: " + c.getId() + ": " + e.getMessage());
                                }
                                plantLights.remove(c.getId());
                            }
                        }
                    }
                    it.remove();
                }
            }
        }
    }

		/**
		 * Allows GhostManager to grab the torch shape.
		 */
		public ObjShape getTorchShape() { return torchS; }
		/**
		 * Allows GhostManager to grab the torch texture.
		 */
		public TextureImage getTorchTexture() { return torchtx; }
		/**
		* Getter for the home GameObject.
		*
		* @return the home GameObject
	   */
	   public GameObject getHome()   { return home; }
	   /**
		* Getter for the market GameObject.
		*
		* @return the market GameObject
	   */
	   public GameObject getMarket() { return market; }
	   /**
		* Getter for the physics engine instance.
		*
		* @return the PhysicsEngine in use
		*/
	   public PhysicsEngine getPhysicsEngine() { return physicsEngine; }
	   /**
		* Checks whether the avatar physics body is active.
		*
		* @return true if physics simulation is active on avatar
	   */
	   public boolean isAvatarPhysicsActive() { return avatarPhysicsActive; }
	   /**
		* Sets the avatar physics active flag.
		*
		* @param active true to enable physics on avatar
	   */
	   public void setAvatarPhysicsActive(boolean active) { avatarPhysicsActive = active; }
	   /**
		* Sets the current avatar PhysicsObject reference.
		*
		* @param physObj the PhysicsObject instance
		*/
	   public void setAvatarPhysicsObject(PhysicsObject physObj) { avatarPhysicsObject = physObj; }
	   /**
		* Sets the timestamp when physics was activated on the avatar.
		*
		* @param time epoch milliseconds of activation
	   */
	   public void setPhysicsActivateTime(long time) { physicsActivateTime = time; }
	   /**
		* Getter for the pig GameObject.
		*
		* @return the pig GameObject
	   */
	   public GameObject getPig() { return pig; }
	   /**
		* Getter for the chicken GameObject.
		*
		* @return the chicken GameObject
		*/
	   public GameObject getChicken() { return chicken; }
	   /**
		* Checks if the avatar is in a face-down state.
		*
		* @return true if face-down
	   */
	   public boolean isFaceDown() { return isFaceDown; }
	   /**
		* Retrieves the last computed delta time between frames.
		*
		* @return delta time in milliseconds
	   */
	   public float getDeltaTime() { return deltaTime ; }
	   /**
		* Getter for the terrain GameObject.
		*
		* @return the terrian  GameObject
		*/
	   public GameObject getTerr() { return terr;}
	   /**
		* Getter for the MyGame instance.
		*
		* @return the MyGame instance
	   */
	   public static  MyGame getInstance() { return instance; }
	   /**
		* Getter for the bee GameObject.
		*
		* @return the bee GameObject
		*/
	   public GameObject getBee() { return bee; }
	   /**
		* Getter for the radio GameObject.
		*
		* @return the radio GameObject
		*/
	   public GameObject getRadio() { return radio; }
   
		
}
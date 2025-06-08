package io.github.mygame;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle; // Rectangle 클래스 import
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import java.util.ArrayList;
import java.util.List;

public class    FinalScreen implements Screen {
    private Game game;
    private OrthographicCamera camera;
    private FitViewport viewport;

    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;

    private Texture playerTexture;
    private Vector2 playerPosition;
    private float playerSpeed = 100f;

    private SpriteBatch batch;

    // Animation variables
    private Animation<TextureRegion> walkUpAnimation;
    private Animation<TextureRegion> walkDownAnimation;
    private Animation<TextureRegion> walkLeftAnimation;
    private Animation<TextureRegion> walkRightAnimation;

    private Animation<TextureRegion> idleUpAnimation;
    private Animation<TextureRegion> idleDownAnimation;
    private Animation<TextureRegion> idleLeftAnimation;
    private Animation<TextureRegion> idleRightAnimation;

    private float stateTime;
    private Direction currentDirection;
    private boolean isMoving;

    private Joystick joystick;

    private float playerScale = 1.0f; // 플레이어 크기 조정

    private static final float PPM = 32f; // Pixels Per Meter

    private enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        IDLE_UP,
        IDLE_DOWN,
        IDLE_LEFT,
        IDLE_RIGHT
    }

    // Physics and Lighting
    private World world;
    private RayHandler rayHandler;
    private PointLight flashlight;

    // Doors for screen transitions
    private List<Door> doors;

    // Fade effect variables
    private Texture blackTexture;
    private float fadeInTime = 1.0f;
    private float fadeOutTime = 1.0f;
    private float fadeInElapsed = 0f;
    private float fadeOutElapsed = 0f;
    private boolean isFadingIn = true;
    private boolean isFadingOut = false;

    // Sound variables
    private Music backgroundMusic;
    private Sound transitionSound;

    // Next screen reference
    private Screen nextScreen = null;

    // Inventory variables
    private Stage stage;
    private Window inventoryWindow;
    private Table inventoryTable;
    private ScrollPane inventoryScrollPane;
    private Skin skin;
    private boolean isInventoryOpen = false;

    // Inventory button
    private Texture inventoryButtonTexture;
    private ImageButton inventoryButton;

    // Message label
    private Label messageLabel;
    private float messageDisplayTime = 3.0f;
    private float messageTimer = 0f;

    // Door class for managing screen transitions
    private class Door {
        Rectangle rectangle;
        String type;

        Door(Rectangle rectangle, String type) {
            this.rectangle = rectangle;
            this.type = type;
        }
    }

    public FinalScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        // 파일 존재 여부 확인
        if (Gdx.files.internal("maze_32x32_with_entrance_exit.tmx").exists()) {
            Gdx.app.log("FinalScreen", "Map file exists.");
        } else {
            Gdx.app.error("FinalScreen", "Map file does not exist!");
        }

        // Initialize Stage and Skin for UI
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        stage = new Stage(new FitViewport(800, 600));
        Gdx.input.setInputProcessor(stage);

        // Initialize Inventory UI
        setupInventoryUI();

        // Initialize Message Label
        setupMessageLabel();

        // Initialize SpriteBatch
        batch = new SpriteBatch();

        // Initialize Camera and Viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(1024 / PPM, 768 / PPM, camera);
        viewport.apply();

        // Load Background Music
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();

        // Load Transition Sound
        transitionSound = Gdx.audio.newSound(Gdx.files.internal("transition_sound.mp3"));

        // Create Black Texture for Fade Effect
        createBlackTexture();

        // Load Tiled Map
        tiledMap = new TmxMapLoader().load("maze_32x32_with_entrance_exit.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1 / PPM);

        // Initialize Player Position
        playerPosition = getSpawnPosition();
        camera.position.set(playerPosition.x, playerPosition.y, 0);
        camera.update();

        // Load Player Textures and Animations
        loadPlayerAnimations();

        // Initialize Joystick
        setupJoystick();

        // Initialize Box2D World and Lighting
        setupBox2D();

        // Initialize Doors
        setupDoors();

        // Initialize Fade Effect Variables
        isFadingIn = true;
        fadeInElapsed = 0f;

        // Initialize currentDirection
        currentDirection = Direction.IDLE_DOWN; // 초기 방향 설정
    }

    private void setupInventoryUI() {
        // Create Inventory Table
        inventoryTable = new Table(skin);
        inventoryTable.top().left();

        // Create ScrollPane for Inventory
        inventoryScrollPane = new ScrollPane(inventoryTable, skin);
        inventoryScrollPane.setFadeScrollBars(false);

        // Create Inventory Window
        inventoryWindow = new Window("Inventory", skin);
        inventoryWindow.setSize(300, 400);
        inventoryWindow.setPosition(stage.getWidth() / 2 - 150, stage.getHeight() / 2 - 200);
        inventoryWindow.add(inventoryScrollPane).expand().fill();
        inventoryWindow.setVisible(false);
        stage.addActor(inventoryWindow);

        // Create Inventory Button
        inventoryButtonTexture = new Texture(Gdx.files.internal("inventory_button.png"));
        TextureRegionDrawable inventoryButtonDrawable = new TextureRegionDrawable(new TextureRegion(inventoryButtonTexture));
        inventoryButton = new ImageButton(inventoryButtonDrawable);
        inventoryButton.setSize(80, 80);
        inventoryButton.setPosition(stage.getWidth() - 90, stage.getHeight() - 90);
        stage.addActor(inventoryButton);

        inventoryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleInventory();
            }
        });
    }

    private void setupMessageLabel() {
        // Create Message Label
        messageLabel = new Label("", skin);
        messageLabel.setVisible(false);

        // Add Message Label to Stage
        Table messageTable = new Table();
        messageTable.setFillParent(true);
        messageTable.top().center();
        messageTable.add(messageLabel).padTop(20);
        stage.addActor(messageTable);
    }

    private void createBlackTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 1); // Black color
        pixmap.fill();
        blackTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void loadPlayerAnimations() {
        walkUpAnimation = loadAnimation("player_walk_up.png", 4, 1, 0.1f);
        walkDownAnimation = loadAnimation("player_walk_down.png", 4, 1, 0.1f);
        walkLeftAnimation = loadAnimation("player_walk_left.png", 4, 1, 0.1f);
        walkRightAnimation = loadAnimation("player_walk_right.png", 4, 1, 0.1f);

        idleUpAnimation = loadIdleAnimation("player_idle_up.png");
        idleDownAnimation = loadIdleAnimation("player_idle_down.png");
        idleLeftAnimation = loadIdleAnimation("player_idle_left.png");
        idleRightAnimation = loadIdleAnimation("player_idle_right.png");
    }

    private void setupJoystick() {
        Texture joystickBase = new Texture(Gdx.files.internal("joystick_base.png"));
        Texture joystickKnob = new Texture(Gdx.files.internal("joystick_knob.png"));
        joystick = new Joystick(joystickBase, joystickKnob, 50, 50);
        joystick.setScale(1.0f);
        stage.addActor(joystick);
    }

    private void setupBox2D() {
        world = new World(new Vector2(0, 0), true);
        RayHandler.setGammaCorrection(true);
        RayHandler.useDiffuseLight(true);
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.1f);

        flashlight = new PointLight(rayHandler, 128, new Color(1, 1, 1, 1), 300 / PPM, playerPosition.x, playerPosition.y);
        flashlight.setXray(true);
        flashlight.setSoftnessLength(10f);
    }

    private void setupDoors() {
        doors = new ArrayList<>();
        MapLayer doorLayer = tiledMap.getLayers().get("Doors");
        if (doorLayer != null) {
            for (MapObject object : doorLayer.getObjects()) {
                if (object instanceof RectangleMapObject) {
                    RectangleMapObject rectObject = (RectangleMapObject) object;
                    Rectangle rect = rectObject.getRectangle();
                    String type = rectObject.getName(); // Assuming the door type is set as the object's name
                    doors.add(new Door(new Rectangle(rect.x / PPM, rect.y / PPM, rect.width / PPM, rect.height / PPM), type));
                }
            }
        }
    }

    private Animation<TextureRegion> loadAnimation(String fileName, int frameCols, int frameRows, float frameDuration) {
        Texture sheet = new Texture(Gdx.files.internal(fileName));
        TextureRegion[][] tmp = TextureRegion.split(sheet, sheet.getWidth() / frameCols, sheet.getHeight() / frameRows);

        TextureRegion[] frames = new TextureRegion[frameCols * frameRows];
        int index = 0;
        for (int i = 0; i < frameRows; i++) {
            for (int j = 0; j < frameCols; j++) {
                frames[index++] = tmp[i][j];
            }
        }

        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames);
        animation.setPlayMode(Animation.PlayMode.LOOP);
        return animation;
    }

    private Animation<TextureRegion> loadIdleAnimation(String fileName) {
        Texture idleTexture = new Texture(Gdx.files.internal(fileName));
        TextureRegion idleRegion = new TextureRegion(idleTexture);
        Animation<TextureRegion> idleAnimation = new Animation<>(0.1f, idleRegion);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP);
        return idleAnimation;
    }

    private Vector2 getSpawnPosition() {
        MapLayer spawnLayer = tiledMap.getLayers().get("Spawn");
        if (spawnLayer != null) {
            for (MapObject object : spawnLayer.getObjects()) {
                if ("Spawn".equalsIgnoreCase(object.getName())) {
                    RectangleMapObject rectObject = (RectangleMapObject) object;
                    return new Vector2(rectObject.getRectangle().x / PPM, rectObject.getRectangle().y / PPM);
                }
            }
        }
        // Default spawn position if none found
        return new Vector2(50f, 50f);
    }

    @Override
    public void render(float delta) {
        float deltaTime = delta;

        // Update Stage
        stage.act(deltaTime);
        stage.draw();

        // Handle Fade In
        if (isFadingIn) {
            fadeInElapsed += deltaTime;
            float alpha = 1f - Math.min(fadeInElapsed / fadeInTime, 1f);

            // Render Fade Effect
            Gdx.gl.glEnable(GL20.GL_BLEND);
            batch.begin();
            batch.setColor(0, 0, 0, alpha);
            batch.draw(blackTexture, camera.position.x - viewport.getWorldWidth() / 2,
                camera.position.y - viewport.getWorldHeight() / 2,
                viewport.getWorldWidth(), viewport.getWorldHeight());
            batch.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            batch.setColor(1, 1, 1, 1);

            if (fadeInElapsed >= fadeInTime) {
                isFadingIn = false;
            }
        }

        // Handle Fade Out
        if (isFadingOut) {
            fadeOutElapsed += deltaTime;
            float alpha = Math.min(fadeOutElapsed / fadeOutTime, 1f);

            // Render Fade Effect
            Gdx.gl.glEnable(GL20.GL_BLEND);
            batch.begin();
            batch.setColor(0, 0, 0, alpha);
            batch.draw(blackTexture, camera.position.x - viewport.getWorldWidth() / 2,
                camera.position.y - viewport.getWorldHeight() / 2,
                viewport.getWorldWidth(), viewport.getWorldHeight());
            batch.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            batch.setColor(1, 1, 1, 1);

            if (fadeOutElapsed >= fadeOutTime) {
                // Transition to Next Screen
                if (transitionSound != null) {
                    transitionSound.dispose();
                    transitionSound = null;
                }
                game.setScreen(nextScreen);
                return;
            }
        }

        // Update Box2D World
        world.step(deltaTime, 6, 2);

        // Handle Player Movement
        handlePlayerMovement(deltaTime);

        // Update Camera Position
        camera.position.set(playerPosition.x, playerPosition.y, 0);
        camera.update();

        // Clear Screen and Render Map
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mapRenderer.setView(camera);
        mapRenderer.render();

        // Render Player
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        TextureRegion currentFrame = getCurrentAnimationFrame();
        float drawWidth = (currentFrame.getRegionWidth() / PPM) * playerScale;
        float drawHeight = (currentFrame.getRegionHeight() / PPM) * playerScale;

        batch.draw(currentFrame,
            playerPosition.x - drawWidth / 2,
            playerPosition.y - drawHeight / 2,
            drawWidth,
            drawHeight);

        batch.end();

        // Update and Render Lighting
        rayHandler.setCombinedMatrix(camera);
        rayHandler.updateAndRender();

        // Render Stage on top of Lighting
        stage.act(deltaTime);
        stage.draw();

        // Check for Door Collisions
        checkDoorCollisions();
    }

    private void handlePlayerMovement(float deltaTime) {
        if (currentDirection == null) {
            Gdx.app.error("FinalScreen", "currentDirection is null!");
            // Set to default direction to prevent crash
            currentDirection = Direction.IDLE_DOWN;
        }

        Vector2 directionVector = joystick.getDirection();
        Vector2 velocity = directionVector.cpy().scl(playerSpeed * deltaTime / PPM);
        playerPosition.add(velocity);

        // Update Lighting Position
        flashlight.setPosition(playerPosition.x, playerPosition.y);

        // Determine Movement Direction
        isMoving = directionVector.len() > 0.1f;

        if (isMoving) {
            if (Math.abs(directionVector.x) > Math.abs(directionVector.y)) {
                currentDirection = directionVector.x > 0 ? Direction.RIGHT : Direction.LEFT;
            } else {
                currentDirection = directionVector.y > 0 ? Direction.UP : Direction.DOWN;
            }
            stateTime += deltaTime;
        } else {
            switch (currentDirection) {
                case UP:
                    currentDirection = Direction.IDLE_UP;
                    break;
                case DOWN:
                    currentDirection = Direction.IDLE_DOWN;
                    break;
                case LEFT:
                    currentDirection = Direction.IDLE_LEFT;
                    break;
                case RIGHT:
                    currentDirection = Direction.IDLE_RIGHT;
                    break;
                default:
                    currentDirection = Direction.IDLE_DOWN;
                    break;
            }
            stateTime += deltaTime;
        }
    }

    private TextureRegion getCurrentAnimationFrame() {
        switch (currentDirection) {
            case UP:
                return walkUpAnimation.getKeyFrame(stateTime, true);
            case DOWN:
                return walkDownAnimation.getKeyFrame(stateTime, true);
            case LEFT:
                return walkLeftAnimation.getKeyFrame(stateTime, true);
            case RIGHT:
                return walkRightAnimation.getKeyFrame(stateTime, true);
            case IDLE_UP:
                return idleUpAnimation.getKeyFrame(stateTime, true);
            case IDLE_DOWN:
                return idleDownAnimation.getKeyFrame(stateTime, true);
            case IDLE_LEFT:
                return idleLeftAnimation.getKeyFrame(stateTime, true);
            case IDLE_RIGHT:
                return idleRightAnimation.getKeyFrame(stateTime, true);
            default:
                return idleDownAnimation.getKeyFrame(stateTime, true);
        }
    }

    private void checkDoorCollisions() {
        for (Door door : doors) {
            if (playerPosition.x > door.rectangle.x && playerPosition.x < door.rectangle.x + door.rectangle.width &&
                playerPosition.y > door.rectangle.y && playerPosition.y < door.rectangle.y + door.rectangle.height) {
                initiateScreenTransition(door.type);
                break;
            }
        }
    }

    private void initiateScreenTransition(String doorType) {
        if (!isFadingOut) {
            isFadingOut = true;
            fadeOutElapsed = 0f;

            // Play Transition Sound
            if (transitionSound != null) {
                transitionSound.play();
            }

            // Set Next Screen Based on Door Type
            switch (doorType) {
                case "Door1":
                    nextScreen = new FourthScreen(game, new Vector2(1400f / PPM, 520f / PPM));
                    break;
                case "Door3":
                    nextScreen = new SixthScreen(game, new Vector2(950f / PPM, 900f / PPM));
                    break;
                // Add more cases as needed
                default:
                    nextScreen = new FifthScreen(game, new Vector2(100f / PPM, 100f / PPM));
                    break;
            }

            // Stop Background Music
            if (backgroundMusic != null) {
                backgroundMusic.stop();
                backgroundMusic.dispose();
                backgroundMusic = null;
            }
        }
    }

    private void toggleInventory() {
        isInventoryOpen = !isInventoryOpen;
        inventoryWindow.setVisible(isInventoryOpen);

        if (isInventoryOpen) {
            updateInventoryUI();
        }
    }

    private void updateInventoryUI() {
        // Clear Existing Items
        inventoryTable.clear();

        // Add Items to Inventory
        Inventory inventory = Inventory.getInstance();
        for (String item : inventory.getItems()) {
            Label itemLabel = new Label(item, skin);
            inventoryTable.add(itemLabel).left().pad(5);
            inventoryTable.row();
        }

        // Refresh Table
        inventoryTable.invalidateHierarchy();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageTimer = 0f;
    }

    // Example method to handle item collection or other interactions
    private void handleInteractions() {
        // Implement interaction logic here, such as collecting items
        // 예시:
        // if (player overlaps with an item) { Inventory.getInstance().addItem("ItemName"); showMessage("Item collected!"); }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
        rayHandler.useCustomViewport(0, 0, width, height);
    }

    @Override
    public void pause() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    @Override
    public void resume() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        batch.dispose();
        stage.dispose();
        if (joystick != null) {
            joystick.dispose(); // Joystick 클래스에 dispose() 메서드가 추가되어 이제 오류가 발생하지 않습니다.
        }
        if (playerTexture != null) {
            playerTexture.dispose();
        }
        if (tiledMap != null) {
            tiledMap.dispose();
        }
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        if (world != null) {
            world.dispose();
        }
        if (rayHandler != null) {
            rayHandler.dispose();
        }
        if (blackTexture != null) {
            blackTexture.dispose();
        }

        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }

        if (transitionSound != null) {
            transitionSound.dispose();
        }

        if (inventoryButtonTexture != null) {
            inventoryButtonTexture.dispose();
        }

        if (skin != null) {
            skin.dispose();
        }

        // Dispose all animation textures
        disposeAnimation(walkUpAnimation);
        disposeAnimation(walkDownAnimation);
        disposeAnimation(walkLeftAnimation);
        disposeAnimation(walkRightAnimation);
        disposeAnimation(idleUpAnimation);
        disposeAnimation(idleDownAnimation);
        disposeAnimation(idleLeftAnimation);
        disposeAnimation(idleRightAnimation);
    }

    private void disposeAnimation(Animation<TextureRegion> animation) {
        for (TextureRegion frame : animation.getKeyFrames()) {
            if (frame.getTexture() != null) {
                frame.getTexture().dispose();
            }
        }
    }
}

package io.github.mygame;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.util.List;
import java.util.ArrayList;

public class SixthScreen implements Screen {
    private Game game;

    // 인벤토리 UI 변수
    private Window inventoryWindow;
    private Table inventoryTable;
    private Skin skin;
    private boolean isInventoryOpen = false;

    // 인벤토리 버튼
    private Texture inventoryButtonTexture;
    private ImageButton inventoryButton;

    // 메시지 라벨
    private Label messageLabel;
    private float messageDisplayTime = 1.0f; // 메시지가 표시되는 시간 (초)
    private float messageTimer = 0f;

    private SpriteBatch batch;
    private Stage stage;
    private OrthographicCamera camera;
    private FitViewport viewport; // Viewport 타입을 FitViewport으로 명확히 설정
    private Music backgroundMusic;

    private Joystick joystick;

    private Vector2 playerPosition; // 플레이어 위치를 저장하는 변수
    private float playerSpeed;
    private Texture playerTexture;

    private World world;
    private Body playerBody;

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

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;

    private float playerScale;
    private static final float PPM = 100f; // Pixels Per Meter

    // 문 객체를 저장할 리스트 (문 속성 포함)
    private List<Door> doors;

    private class Door {
        Rectangle rectangle;
        String type;

        Door(Rectangle rectangle, String type) {
            this.rectangle = rectangle;
            this.type = type;
        }
    }

    private RayHandler rayHandler;
    private PointLight flashlight;

    private Texture blackTexture;
    private float fadeInTime = 1.0f;
    private float fadeOutTime = 1.0f;
    private float fadeInElapsed = 0f;
    private float fadeOutElapsed = 0f;
    private boolean isFadingIn = true;
    private boolean isFadingOut = false;

    private Sound transitionSound;
    private Screen nextScreen = null;

    private Texture investigateButtonTexture;
    private ImageButton investigateButton;

    private List<Rectangle> quiz5Areas;

    // --- 챗봇 관련 필드 추가 ---
    private Window chatWindow;
    private TextField chatInput;
    private TextButton sendButton;
    private Table chatMessages;
    private ScrollPane chatScrollPane;
    private boolean isChatVisible = false;

    // 챗봇 ImageButton 관련 필드 추가
    private Texture mailButtonTexture; // 챗봇 버튼 이미지 텍스처 필드
    // --- 챗봇 관련 필드 끝 ---

    public SixthScreen(Game game, Vector2 spawnPosition) {
        this.game = game;
        this.playerPosition = spawnPosition.cpy();
    }

    public SixthScreen(Game game) {
        this(game, new Vector2(950f / PPM, 900f / PPM));
    }

    @Override
    public void show() {
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 인벤토리 설정
        inventoryTable = new Table(skin);
        inventoryTable.top().left();

        int inventoryWindowWidth = 300;
        int inventoryWindowHeight = 400;

        // 그라데이션 배경 생성
        Drawable inventoryBackgroundDrawable = createVerticalGradientDrawable(
            new Color(0.2f, 0f, 0.0f, 0.7f), // 시작 색상 (어두운 빨간색)
            new Color(0.0f, 0f, 0f, 0.7f),   // 끝 색상 (검은색)
            inventoryWindowWidth,
            inventoryWindowHeight
        );

        Window.WindowStyle inventoryWindowStyle = new Window.WindowStyle();
        inventoryWindowStyle.background = inventoryBackgroundDrawable;
        inventoryWindowStyle.titleFont = skin.getFont("default-font");
        inventoryWindowStyle.titleFontColor = Color.RED;

        inventoryWindow = new Window("인벤토리", inventoryWindowStyle);
        inventoryWindow.setSize(inventoryWindowWidth, inventoryWindowHeight);
        inventoryWindow.setPosition(stage.getWidth() / 2 - inventoryWindowWidth / 2, stage.getHeight() / 2 - inventoryWindowHeight / 2);
        inventoryWindow.pad(10, 10, 10, 10);
        inventoryWindow.add(inventoryTable).expand().fill();
        inventoryWindow.setVisible(false);

        inventoryWindow.getTitleTable().setBackground((Drawable) null);
        inventoryWindow.getTitleLabel().setColor(Color.RED);

        stage.addActor(inventoryWindow);

        // 인벤토리 버튼 설정
        inventoryButtonTexture = new Texture(Gdx.files.internal("inventory_button.png"));
        TextureRegionDrawable inventoryButtonDrawable = new TextureRegionDrawable(new TextureRegion(inventoryButtonTexture));
        inventoryButton = new ImageButton(inventoryButtonDrawable);
        inventoryButton.setSize(200, 150);
        inventoryButton.setPosition(stage.getWidth() - inventoryButton.getWidth() - 20, stage.getHeight() - inventoryButton.getHeight() - 20);
        stage.addActor(inventoryButton);

        inventoryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleInventory();
            }
        });

        // 조사하기 버튼 설정
        investigateButtonTexture = new Texture(Gdx.files.internal("investigate_button.png"));
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(new TextureRegion(investigateButtonTexture));
        investigateButton = new ImageButton(buttonDrawable);
        investigateButton.setSize(450, 75);
        investigateButton.setPosition(stage.getWidth() - investigateButton.getWidth() - 20, 70);
        stage.addActor(investigateButton);

        investigateButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleInvestigateAction();
            }
        });

        // 메시지 라벨 설정
        messageLabel = new Label("", skin);
        messageLabel.setVisible(false);

        Table messageTable = new Table();
        messageTable.setFillParent(true);
        messageTable.top().center();
        messageTable.add(messageLabel).padTop(20);
        stage.addActor(messageTable);

        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        viewport = new FitViewport(800 / PPM, 480 / PPM, camera);
        viewport.apply();

        camera.zoom = 1f;

        // 배경 음악 설정
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("3f_bgm.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();

        transitionSound = Gdx.audio.newSound(Gdx.files.internal("stairs.mp3"));

        // 검은색 텍스처 생성 (페이드 효과용)
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 1);
        pixmap.fill();
        blackTexture = new Texture(pixmap);
        pixmap.dispose();

        // 맵 로드
        map = new TmxMapLoader().load("3f.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1 / PPM);

        playerSpeed = 100f;
        playerScale = 2f;

        camera.position.set(playerPosition.x, playerPosition.y, 0);
        camera.update();

        // 조이스틱 설정
        Texture joystickBase = new Texture("joystick_base.png");
        Texture joystickKnob = new Texture("joystick_knob.png");

        float joystickX = 50;
        float joystickY = 50;

        joystick = new Joystick(joystickBase, joystickKnob, joystickX, joystickY);
        float joystickScale = 0.8f;
        joystick.setScale(joystickScale);
        stage.addActor(joystick);

        // 플레이어 텍스처 설정
        playerTexture = new Texture("player_idle_down.png");

        stateTime = 0f;
        isMoving = false;
        currentDirection = Direction.IDLE_DOWN;

        // 애니메이션 로드
        walkUpAnimation = loadAnimation("player_walk_up.png", 4, 1, 0.1f);
        walkDownAnimation = loadAnimation("player_walk_down.png", 4, 1, 0.1f);
        walkLeftAnimation = loadAnimation("player_walk_left.png", 4, 1, 0.1f);
        walkRightAnimation = loadAnimation("player_walk_right.png", 4, 1, 0.1f);

        idleUpAnimation = loadIdleAnimation("player_idle_up.png");
        idleDownAnimation = loadIdleAnimation("player_idle_down.png");
        idleLeftAnimation = loadIdleAnimation("player_idle_left.png");
        idleRightAnimation = loadIdleAnimation("player_idle_right.png");

        // 월드 설정
        world = new World(new Vector2(0, 0), true);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(playerPosition);

        playerBody = world.createBody(bodyDef);

        CircleShape shape = new CircleShape();
        shape.setRadius((16f) / PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;

        playerBody.createFixture(fixtureDef);
        shape.dispose();

        createObstacleBodiesFromTiles();

        doors = new ArrayList<>();
        quiz5Areas = new ArrayList<>();
        MapLayers mapLayers = map.getLayers();
        for (MapLayer layer : mapLayers) {
            if (layer instanceof TiledMapTileLayer) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                String layerName = tileLayer.getName();

                // 문 설정
                if (layerName.equals("Door")) {
                    String type = null;
                    if (tileLayer.getProperties().containsKey("type")) {
                        type = tileLayer.getProperties().get("type", String.class);
                    }

                    for (int x = 0; x < tileLayer.getWidth(); x++) {
                        for (int y = 0; y < tileLayer.getHeight(); y++) {
                            TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                            if (cell != null && cell.getTile() != null && cell.getTile().getId() != 0) {
                                float doorX = (x * tileLayer.getTileWidth()) / PPM;
                                float doorY = (y * tileLayer.getTileHeight()) / PPM;
                                float doorWidth = tileLayer.getTileWidth() / PPM;
                                float doorHeight = tileLayer.getTileHeight() / PPM;
                                Rectangle doorRect = new Rectangle(doorX, doorY, doorWidth, doorHeight);
                                doors.add(new Door(doorRect, type));
                            }
                        }
                    }
                }

                // Quiz5 영역 설정
                if (layerName.equals("Quiz5")) {
                    for (int x = 0; x < tileLayer.getWidth(); x++) {
                        for (int y = 0; y < tileLayer.getHeight(); y++) {
                            TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                            if (cell != null && cell.getTile() != null && cell.getTile().getId() != 0) {
                                float tileX = (x * tileLayer.getTileWidth()) / PPM;
                                float tileY = (y * tileLayer.getTileHeight()) / PPM;
                                float tileWidth = tileLayer.getTileWidth() / PPM;
                                float tileHeight = tileLayer.getTileHeight() / PPM;
                                Rectangle tileRect = new Rectangle(tileX, tileY, tileWidth, tileHeight);
                                quiz5Areas.add(tileRect);
                            }
                        }
                    }
                }
            }
        }

        // 라이팅 설정
        RayHandler.setGammaCorrection(true);
        RayHandler.useDiffuseLight(true);
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.1f);

        flashlight = new PointLight(rayHandler, 128, new Color(1, 1, 1, 1), 300 / PPM, playerPosition.x, playerPosition.y);
        flashlight.setXray(true);
        flashlight.setSoftnessLength(10f);

        // --- 챗봇 기능 추가 ---
        // 그라데이션 배경 생성 메서드 사용
        Drawable customChatBackground = createVerticalGradientDrawable(
            new Color(0.2f, 0f, 0f, 0.9f), // 시작 색상 (어두운 빨간색)
            new Color(0f, 0f, 0f, 0.9f),   // 끝 색상 (검은색)
            400, // 너비
            300  // 높이
        );

        // 챗봇 창 스타일 설정
        Window.WindowStyle customChatStyle = new Window.WindowStyle();
        customChatStyle.background = customChatBackground;
        customChatStyle.titleFont = skin.getFont("default-font");
        customChatStyle.titleFontColor = Color.RED;

        // 챗봇 창 생성에 커스텀 스타일 적용
        chatWindow = new Window("챗봇", customChatStyle);
        chatWindow.setSize(1000, 400);
        chatWindow.setPosition((Gdx.graphics.getWidth() - chatWindow.getWidth()) / 2,
            (Gdx.graphics.getHeight() - chatWindow.getHeight()) / 2);
        chatWindow.setVisible(false);

        // 챗봇 메시지 영역 설정
        chatMessages = new Table();
        chatMessages.top().left();
        chatMessages.pad(10); // 패딩 설정 (옵션)

        chatScrollPane = new ScrollPane(chatMessages, skin);
        chatScrollPane.setFadeScrollBars(false);
        chatScrollPane.setScrollingDisabled(true, false);

        // 챗봇 입력 및 전송 설정
        chatInput = new TextField("", skin);
        chatInput.setMessageText("메시지를 입력하세요...");

        sendButton = new TextButton("전송", skin);
        sendButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sendMessage();
            }
        });

        // 챗봇 창 내 테이블 설정
        Table mainChatTable = new Table();
        mainChatTable.setFillParent(true);
        mainChatTable.add(chatScrollPane).expand().fill().padBottom(10).row();

        Table inputChatTable = new Table();
        inputChatTable.add(chatInput).expandX().fillX().padRight(10).height(50);
        inputChatTable.add(sendButton).size(80, 50);
        mainChatTable.add(inputChatTable).expandX().fillX().align(Align.bottom);

        chatWindow.add(mainChatTable).expand().fill();
        stage.addActor(chatWindow);

        // 챗봇 ImageButton 생성
        if (mailButtonTexture == null) {
            // mail.png 텍스처 로드
            mailButtonTexture = new Texture(Gdx.files.internal("mail.png"));
        }

        // Drawable 생성
        TextureRegionDrawable mailButtonDrawable = new TextureRegionDrawable(new TextureRegion(mailButtonTexture));

        // ImageButton 생성
        ImageButton chatBotImageButton = new ImageButton(mailButtonDrawable);

        // 버튼 크기 설정
        chatBotImageButton.setSize(200, 150);

        // 버튼 위치 설정
        chatBotImageButton.setPosition(20, stage.getHeight() - chatBotImageButton.getHeight() - 20);

        // ClickListener 추가 (챗봇 창 토글)
        chatBotImageButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleChatWindow();
            }
        });

        // Stage에 ImageButton 추가
        stage.addActor(chatBotImageButton);
        // --- 챗봇 ImageButton 생성 끝 ---
    }

    /**
     * 그라데이션을 생성하는 메서드
     *
     * @param startColor 그라데이션 시작 색상 (위쪽)
     * @param endColor 그라데이션 끝 색상 (아래쪽)
     * @param width 그라데이션 이미지의 너비
     * @param height 그라데이션 이미지의 높이
     * @return 생성된 TextureRegionDrawable
     */
    private TextureRegionDrawable createVerticalGradientDrawable(Color startColor, Color endColor, int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            float ratio = (float) y / (float) height;
            Color color = new Color(
                startColor.r + ratio * (endColor.r - startColor.r),
                startColor.g + ratio * (endColor.g - startColor.g),
                startColor.b + ratio * (endColor.b - startColor.b),
                startColor.a + ratio * (endColor.a - startColor.a)
            );
            pixmap.setColor(color);
            pixmap.drawLine(0, y, width, y);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private Animation<TextureRegion> loadAnimation(String fileName, int frameCols, int frameRows, float frameDuration) {
        Texture sheet = new Texture(Gdx.files.internal(fileName));
        TextureRegion[][] tmp = TextureRegion.split(sheet,
            sheet.getWidth() / frameCols,
            sheet.getHeight() / frameRows);

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

    private void createObstacleBodiesFromTiles() {
        for (MapLayer layer : map.getLayers()) {
            if (layer.getName().equals("obstacle5") || layer.getName().equals("obstacle6")) {
                if (layer instanceof TiledMapTileLayer) {
                    TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;

                    int layerWidth = tileLayer.getWidth();
                    int layerHeight = tileLayer.getHeight();
                    float tileWidth = tileLayer.getTileWidth() / PPM;
                    float tileHeight = tileLayer.getTileHeight() / PPM;

                    for (int x = 0; x < layerWidth; x++) {
                        for (int y = 0; y < layerHeight; y++) {
                            TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                            if (cell != null && cell.getTile() != null) {
                                if (cell.getTile().getId() != 0) {
                                    createStaticBodyForTile(x, y, tileWidth, tileHeight);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void createStaticBodyForTile(int x, int y, float tileWidth, float tileHeight) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;

        float posX = (x + 0.5f) * tileWidth;
        float posY = (y + 0.5f) * tileHeight;
        bodyDef.position.set(posX, posY);

        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(tileWidth / 2, tileHeight / 2);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.5f;

        body.createFixture(fixtureDef);
        shape.dispose();
    }

    @Override
    public void render(float delta) {
        float deltaTime = delta;

        // 메시지 라벨 타이머 처리
        if (messageLabel.isVisible()) {
            messageTimer += deltaTime;
            if (messageTimer >= messageDisplayTime) {
                messageLabel.setVisible(false);
                messageTimer = 0f;
            }
        }

        if (!isFadingIn && !isFadingOut) {
            world.step(deltaTime, 6, 2);

            Vector2 direction = joystick.getDirection();
            Vector2 velocity = direction.cpy().scl(playerSpeed / PPM);
            playerBody.setLinearVelocity(velocity);

            isMoving = direction.len() > 0.1f;
            if (isMoving) {
                if (Math.abs(direction.x) > Math.abs(direction.y)) {
                    currentDirection = direction.x > 0 ? Direction.RIGHT : Direction.LEFT;
                } else {
                    currentDirection = direction.y > 0 ? Direction.UP : Direction.DOWN;
                }
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
            }

            stateTime += deltaTime;
            playerPosition.set(playerBody.getPosition());
            flashlight.setPosition(playerPosition.x, playerPosition.y);

            float drawWidth = (playerTexture.getWidth() / PPM) * playerScale;
            float drawHeight = (playerTexture.getHeight() / PPM) * playerScale;

            Rectangle playerRect = new Rectangle(
                playerPosition.x - (drawWidth / 2),
                playerPosition.y - (drawHeight / 2),
                drawWidth,
                drawHeight
            );

            // 문과의 충돌 처리
            for (Door door : doors) {
                if (playerRect.overlaps(door.rectangle)) {
                    if (!isFadingOut) {
                        isFadingOut = true;
                        fadeOutElapsed = 0f;

                        playerBody.setLinearVelocity(0, 0);
                        playerBody.setActive(false);
                        if (transitionSound != null) {
                            transitionSound.play();
                        }
                        if ("door".equals(door.type)) {
                            Vector2 spawnPoint = new Vector2(1480f / PPM, 900f / PPM);
                            Gdx.app.log("SixthScreen", "전환: FifthScreen, 스폰 포인트: " + spawnPoint);
                            nextScreen = new FifthScreen(game, spawnPoint);
                        }
                        if (backgroundMusic != null) {
                            backgroundMusic.stop();
                            backgroundMusic.dispose();
                            backgroundMusic = null;
                        }
                    }
                    break;
                }
            }

            // Quiz5 영역에서의 상호작용 처리
            if (isPlayerInQuiz5Area()) {
                // 예: 특정 퀴즈 영역에 들어갔을 때 수행할 로직
                // 현재 예제에서는 handleInvestigateAction()에서 처리하므로 별도로 처리하지 않음
            }
        }

        camera.position.set(playerPosition.x, playerPosition.y, 0);
        camera.update();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mapRenderer.setView(camera);
        mapRenderer.render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        TextureRegion currentFrame;
        switch (currentDirection) {
            case UP:
                currentFrame = walkUpAnimation.getKeyFrame(stateTime, true);
                break;
            case DOWN:
                currentFrame = walkDownAnimation.getKeyFrame(stateTime, true);
                break;
            case LEFT:
                currentFrame = walkLeftAnimation.getKeyFrame(stateTime, true);
                break;
            case RIGHT:
                currentFrame = walkRightAnimation.getKeyFrame(stateTime, true);
                break;
            case IDLE_UP:
                currentFrame = idleUpAnimation.getKeyFrame(stateTime, true);
                break;
            case IDLE_DOWN:
                currentFrame = idleDownAnimation.getKeyFrame(stateTime, true);
                break;
            case IDLE_LEFT:
                currentFrame = idleLeftAnimation.getKeyFrame(stateTime, true);
                break;
            case IDLE_RIGHT:
                currentFrame = idleRightAnimation.getKeyFrame(stateTime, true);
                break;
            default:
                currentFrame = idleDownAnimation.getKeyFrame(stateTime, true);
                break;
        }

        float playerDrawWidth = (currentFrame.getRegionWidth() / PPM) * playerScale;
        float playerDrawHeight = (currentFrame.getRegionHeight() / PPM) * playerScale;

        batch.draw(currentFrame,
            playerPosition.x - playerDrawWidth / 2,
            playerPosition.y - playerDrawHeight / 2,
            playerDrawWidth,
            playerDrawHeight);

        batch.end();

        rayHandler.setCombinedMatrix(camera);
        rayHandler.updateAndRender();

        stage.act(deltaTime);
        stage.draw();

        // 페이드 인 처리
        if (isFadingIn) {
            fadeInElapsed += deltaTime;
            float alpha = 1f - Math.min(fadeInElapsed / fadeInTime, 1f);

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

        // 페이드 아웃 처리
        if (isFadingOut) {
            fadeOutElapsed += deltaTime;
            float alpha = Math.min(fadeOutElapsed / fadeOutTime, 1f);

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
                if (transitionSound != null) {
                    transitionSound.dispose();
                    transitionSound = null;
                }
                game.setScreen(nextScreen);
                return;
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
        if (rayHandler != null) {
            rayHandler.useCustomViewport(0, 0, width, height);
        }
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
        if (batch != null) batch.dispose();
        if (stage != null) stage.dispose();
        if (joystick != null) {
            if (joystick.baseTexture != null) joystick.baseTexture.dispose();
            if (joystick.knobTexture != null) joystick.knobTexture.dispose();
        }
        if (playerTexture != null) playerTexture.dispose();
        if (world != null) world.dispose();
        if (backgroundMusic != null) backgroundMusic.dispose();
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();

        disposeAnimation(walkUpAnimation);
        disposeAnimation(walkDownAnimation);
        disposeAnimation(walkLeftAnimation);
        disposeAnimation(walkRightAnimation);

        disposeAnimation(idleUpAnimation);
        disposeAnimation(idleDownAnimation);
        disposeAnimation(idleLeftAnimation);
        disposeAnimation(idleRightAnimation);

        if (rayHandler != null) rayHandler.dispose();

        if (blackTexture != null) blackTexture.dispose();

        if (transitionSound != null) transitionSound.dispose();

        if (inventoryButtonTexture != null) inventoryButtonTexture.dispose();
        if (investigateButtonTexture != null) investigateButtonTexture.dispose();

        if (inventoryWindow != null && inventoryWindow.getStyle().background instanceof TextureRegionDrawable) {
            TextureRegionDrawable drawable = (TextureRegionDrawable) inventoryWindow.getStyle().background;
            if (drawable.getRegion().getTexture() != null) {
                drawable.getRegion().getTexture().dispose();
            }
        }

        if (chatWindow != null && chatWindow.getStyle().background instanceof TextureRegionDrawable) {
            TextureRegionDrawable drawable = (TextureRegionDrawable) chatWindow.getStyle().background;
            if (drawable.getRegion().getTexture() != null) {
                drawable.getRegion().getTexture().dispose();
            }
        }

        if (mailButtonTexture != null) {
            mailButtonTexture.dispose();
        }

        if (skin != null) skin.dispose();
    }

    private void disposeAnimation(Animation<TextureRegion> animation) {
        if (animation != null) {
            for (TextureRegion frame : animation.getKeyFrames()) {
                if (frame.getTexture() != null) {
                    frame.getTexture().dispose();
                }
            }
        }
    }

    private boolean isPlayerInQuiz5Area() {
        float drawWidth = (playerTexture.getWidth() * playerScale) / PPM;
        float drawHeight = (playerTexture.getHeight() * playerScale) / PPM;

        Rectangle playerRect = new Rectangle(
            playerPosition.x - (drawWidth / 2),
            playerPosition.y - (drawHeight / 2),
            drawWidth,
            drawHeight
        );

        for (Rectangle area : quiz5Areas) {
            if (playerRect.overlaps(area)) {
                return true;
            }
        }
        return false;
    }

    private void showMessage(String message) {
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = Color.WHITE;

        Label messageLabel = new Label(message, labelStyle);

        Table messageTable = new Table();
        messageTable.setFillParent(true);
        messageTable.top().center();
        messageTable.padTop(20);

        messageTable.add(messageLabel).center();
        stage.addActor(messageTable);

        messageLabel.getColor().a = 0f;
        messageLabel.addAction(
            Actions.sequence(
                Actions.fadeIn(0.5f),
                Actions.delay(messageDisplayTime),
                Actions.fadeOut(0.5f),
                Actions.removeActor()
            )
        );
    }

    private void handleInvestigateAction() {
        if (isPlayerInQuiz5Area()) {
            if (!isFadingOut) {
                isFadingOut = true;
                fadeOutElapsed = 0f;

                playerBody.setLinearVelocity(0, 0);
                playerBody.setActive(false);

                Vector2 spawnPosition = playerPosition.cpy();
                nextScreen = new DiaryScreen(game, spawnPosition);

                if (nextScreen instanceof FifthScreen && transitionSound != null) {
                    transitionSound.play();
                }

                if (backgroundMusic != null) {
                    backgroundMusic.stop();
                    backgroundMusic.dispose();
                    backgroundMusic = null;
                }
            }
        } else {
            showMessage("아무 것도 찾을 수 없습니다.");
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
        inventoryTable.clear();

        Inventory inventory = Inventory.getInstance();
        for (String item : inventory.getItems()) {
            Label.LabelStyle labelStyle = new Label.LabelStyle();
            labelStyle.font = skin.getFont("default-font");
            labelStyle.fontColor = Color.WHITE;
            Label itemLabel = new Label(item, labelStyle);
            inventoryTable.add(itemLabel).left().pad(10, 10, 10, 10).width(280);
            inventoryTable.row();
        }

        inventoryTable.invalidateHierarchy();
    }

    // ---------------------------
    // 여기서부터 챗봇 관련 메서드 추가
    // ---------------------------

    private void toggleChatWindow() {
        isChatVisible = !isChatVisible;
        chatWindow.setVisible(isChatVisible);
        if (isChatVisible) {
            Gdx.input.setInputProcessor(stage);
        } else {
            Gdx.input.setInputProcessor(stage);
        }
    }

    private void sendMessage() {
        String userMessage = chatInput.getText().trim();
        if (!userMessage.isEmpty()) {
            addMessage("나: " + userMessage);
            chatInput.setText("");
            sendToChatBot(userMessage);
        }
    }

    private void addMessage(String message) {
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = Color.WHITE;

        Label messageLabel = new Label(message, labelStyle);
        messageLabel.setWrap(true);
        messageLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        messageLabel.setWidth(chatScrollPane.getWidth() - 20); // 패딩을 고려하여 너비 설정

        // 메시지를 테이블의 새로운 행으로 추가
        chatMessages.add(messageLabel).left().expandX().fillX().row();

        // 스크롤페인 레이아웃 갱신 및 최신 메시지로 스크롤
        chatScrollPane.layout();
        chatScrollPane.scrollTo(0, chatMessages.getHeight(), 0, 0);
    }

    private void sendToChatBot(String message) {
        Gdx.app.log("ChatBot", "Sending message: " + message);
        HttpRequest request = new HttpRequest(HttpMethods.POST);
        request.setUrl("http://icechatapi.duckdns.org:5000/chat");
        request.setHeader("Content-Type", "application/json");

        String jsonData = "{\"user_input\": \"" + message + "\"}";
        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
            @Override
            public void handleHttpResponse(HttpResponse httpResponse) {
                if (httpResponse.getStatus().getStatusCode() == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    Json json = new Json();
                    ChatResponse chatResponse = json.fromJson(ChatResponse.class, responseJson);
                    if (chatResponse != null && chatResponse.response != null) {
                        addMessage("챗봇: " + chatResponse.response);
                    } else {
                        addMessage("챗봇: 응답을 처리할 수 없습니다.");
                    }
                } else {
                    addMessage("챗봇: 오류가 발생했습니다. 상태 코드: " + httpResponse.getStatus().getStatusCode());
                }
            }

            @Override
            public void failed(Throwable t) {
                addMessage("챗봇: 요청 실패. 네트워크를 확인하세요.");
            }

            @Override
            public void cancelled() {
                addMessage("챗봇: 요청이 취소되었습니다.");
            }
        });
    }

    private static class ChatResponse {
        public String response;
    }

    // ---------------------------
    // 챗봇 관련 메서드 추가 끝
    // ---------------------------
}

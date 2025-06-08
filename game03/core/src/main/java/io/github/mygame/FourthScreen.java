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
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import java.util.List;
import java.util.ArrayList;
import com.badlogic.gdx.math.Rectangle;

/**
 * FourthScreen 클래스는 게임의 4번째 화면을 담당합니다.
 */
public class FourthScreen implements Screen {
    private Game game;

    private Window inventoryWindow;
    private Table inventoryTable;
    private Skin skin;
    private boolean isInventoryOpen = false;

    private Texture inventoryButtonTexture;
    private ImageButton inventoryButton;

    private Label messageLabel;
    private float messageDisplayTime = 1.0f;
    private float messageTimer = 0f;

    private SpriteBatch batch;
    private Stage stage;
    private OrthographicCamera camera;
    private Viewport viewport;
    private Music backgroundMusic;

    private Joystick joystick;
    private Vector2 playerPosition;
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
        UP, DOWN, LEFT, RIGHT, IDLE_UP, IDLE_DOWN, IDLE_LEFT, IDLE_RIGHT
    }

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;

    private float playerScale;

    private static final float PPM = 100f;

    private List<Door> doors;

    private class Door {
        Rectangle rectangle;
        String type;

        Door(Rectangle rectangle, String type) {
            this.rectangle = rectangle;
            this.type = type;
        }
    }

    private List<Rectangle> quiz2Areas;
    private List<Rectangle> quiz1Areas;

    private RayHandler rayHandler;
    private PointLight flashlight;

    private Texture blackTexture;
    private float fadeInTime = 3.0f;
    private float fadeOutTime = 1.0f;
    private float fadeInElapsed = 0f;
    private float fadeOutElapsed = 0f;
    private boolean isFadingIn = true;
    private boolean isFadingOut = false;

    private Sound transitionSound;
    private Screen nextScreen = null;

    private Texture investigateButtonTexture;
    private ImageButton investigateButton;

    private ComputerLock computerLockScreen;

    private Texture inventoryWindowBackgroundTexture;

    // **추가된 부분: mailButtonTexture**
    private Texture mailButtonTexture; // 챗봇 버튼 이미지 텍스처 필드

    // 챗봇 관련 필드
    private Window chatWindow;
    private com.badlogic.gdx.scenes.scene2d.ui.TextField chatInput;
    private com.badlogic.gdx.scenes.scene2d.ui.TextButton sendButton;
    private com.badlogic.gdx.scenes.scene2d.ui.Table chatMessages;
    private com.badlogic.gdx.scenes.scene2d.ui.ScrollPane chatScrollPane;
    private boolean isChatVisible = false;

    /**
     * 그라데이션을 생성하는 메서드
     *
     * @param startColor 그라데이션 시작 색상 (위쪽)
     * @param endColor 그라데이션 끝 색상 (아래쪽)
     * @param height 그라데이션 이미지의 높이
     * @return 생성된 TextureRegionDrawable
     */
    private TextureRegionDrawable createGradientDrawable(Color startColor, Color endColor, int height) {
        Pixmap pixmap = new Pixmap(1, height, Pixmap.Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            float ratio = (float) y / (float) height;
            Color color = new Color(
                startColor.r + (endColor.r - startColor.r) * ratio,
                startColor.g + (endColor.g - startColor.g) * ratio,
                startColor.b + (endColor.b - startColor.b) * ratio,
                startColor.a + (endColor.a - startColor.a) * ratio
            );
            pixmap.setColor(color);
            pixmap.drawPixel(0, y);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    public FourthScreen(Game game, Vector2 spawnPosition) {
        this.game = game;
        this.playerPosition = spawnPosition.cpy();
    }

    public FourthScreen(Game game) {
        this(game, new Vector2(770f / PPM, 70f / PPM));
    }

    @Override
    public void show() {
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        // default-font 확인 및 없으면 폰트 추가
        if (skin.getFont("default-font") == null) {
            BitmapFont defaultFont = new BitmapFont();
            skin.add("default-font", defaultFont, BitmapFont.class);
        }

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        inventoryTable = new Table(skin);
        inventoryTable.top().left();

        int windowWidth = 300;
        int windowHeight = 400;

        // 인벤토리 창 배경 그라데이션 생성
        Pixmap pixmap = new Pixmap(1, windowHeight, Pixmap.Format.RGBA8888);
        for (int y = 0; y < pixmap.getHeight(); y++) {
            float ratio = (float) y / (float) pixmap.getHeight();
            Color color = new Color(
                0.2f * (1 - ratio),
                0f,
                0f,
                0.7f
            );
            pixmap.setColor(color);
            pixmap.drawPixel(0, y);
        }

        inventoryWindowBackgroundTexture = new Texture(pixmap);
        pixmap.dispose();

        Drawable inventoryWindowBackgroundDrawable = new TextureRegionDrawable(new TextureRegion(inventoryWindowBackgroundTexture));

        Window.WindowStyle inventoryWindowStyle = new Window.WindowStyle();
        inventoryWindowStyle.background = inventoryWindowBackgroundDrawable;
        inventoryWindowStyle.titleFont = skin.getFont("default-font");
        inventoryWindowStyle.titleFontColor = Color.RED;

        inventoryWindow = new Window("인벤토리", inventoryWindowStyle);
        inventoryWindow.setSize(windowWidth, windowHeight);
        inventoryWindow.setPosition(stage.getWidth() / 2 - windowWidth / 2, stage.getHeight() / 2 - windowHeight / 2);
        inventoryWindow.pad(10, 10, 10, 10);
        inventoryWindow.add(inventoryTable).expand().fill();
        inventoryWindow.setVisible(false);

        inventoryWindow.getTitleTable().setBackground((Drawable) null);
        inventoryWindow.getTitleLabel().setColor(Color.RED);

        stage.addActor(inventoryWindow);

        // 인벤토리 버튼 생성
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

        // 메시지 라벨 설정
        messageLabel = new Label("", new Label.LabelStyle(skin.getFont("default-font"), Color.WHITE));
        messageLabel.setVisible(false);

        // 메시지 테이블 설정
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
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();

        // 전환 소리 설정
        transitionSound = Gdx.audio.newSound(Gdx.files.internal("door.mp3"));

        // 검은색 텍스처 생성 (페이드 인/아웃용)
        Pixmap blackPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        blackPixmap.setColor(0, 0, 0, 1);
        blackPixmap.fill();
        blackTexture = new Texture(blackPixmap);
        blackPixmap.dispose();

        // 맵 로드 및 렌더러 설정
        map = new TmxMapLoader().load("1f.tmx");
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

        // 플레이어 텍스처 및 애니메이션 설정
        playerTexture = new Texture("player_idle_down.png");

        stateTime = 0f;
        isMoving = false;
        currentDirection = Direction.IDLE_DOWN;

        walkUpAnimation = loadAnimation("player_walk_up.png", 4, 1, 0.1f);
        walkDownAnimation = loadAnimation("player_walk_down.png", 4, 1, 0.1f);
        walkLeftAnimation = loadAnimation("player_walk_left.png", 4, 1, 0.1f);
        walkRightAnimation = loadAnimation("player_walk_right.png", 4, 1, 0.1f);

        idleUpAnimation = loadIdleAnimation("player_idle_up.png");
        idleDownAnimation = loadIdleAnimation("player_idle_down.png");
        idleLeftAnimation = loadIdleAnimation("player_idle_left.png");
        idleRightAnimation = loadIdleAnimation("player_idle_right.png");

        // 월드 및 플레이어 바디 설정
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

        // 도어 및 퀴즈 영역 설정
        doors = new ArrayList<>();
        quiz2Areas = new ArrayList<>();
        quiz1Areas = new ArrayList<>();
        MapLayers mapLayers = map.getLayers();
        for (MapLayer layer : mapLayers) {
            if (layer instanceof TiledMapTileLayer) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                String layerName = tileLayer.getName();

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

                if (layerName.equals("Quiz2")) {
                    for (int x = 0; x < tileLayer.getWidth(); x++) {
                        for (int y = 0; y < tileLayer.getHeight(); y++) {
                            TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                            if (cell != null && cell.getTile() != null && cell.getTile().getId() != 0) {
                                float tileX = (x * tileLayer.getTileWidth()) / PPM;
                                float tileY = (y * tileLayer.getTileHeight()) / PPM;
                                float tileWidth = tileLayer.getTileWidth() / PPM;
                                float tileHeight = tileLayer.getTileHeight() / PPM;
                                Rectangle tileRect = new Rectangle(tileX, tileY, tileWidth, tileHeight);
                                quiz2Areas.add(tileRect);
                            }
                        }
                    }
                }

                if (layerName.equals("Quiz1")) {
                    for (int x = 0; x < tileLayer.getWidth(); x++) {
                        for (int y = 0; y < tileLayer.getHeight(); y++) {
                            TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                            if (cell != null && cell.getTile() != null && cell.getTile().getId() != 0) {
                                float tileX = (x * tileLayer.getTileWidth()) / PPM;
                                float tileY = (y * tileLayer.getTileHeight()) / PPM;
                                float tileWidth = tileLayer.getTileWidth() / PPM;
                                float tileHeight = tileLayer.getTileHeight() / PPM;
                                Rectangle tileRect = new Rectangle(tileX, tileY, tileWidth, tileHeight);
                                quiz1Areas.add(tileRect);
                            }
                        }
                    }
                }
            }
        }

        // 라이트 설정
        RayHandler.setGammaCorrection(true);
        RayHandler.useDiffuseLight(true);
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.1f);

        flashlight = new PointLight(rayHandler, 128, new Color(1, 1, 1, 1), 300 / PPM, playerPosition.x, playerPosition.y);
        flashlight.setXray(true);
        flashlight.setSoftnessLength(10f);

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

        // 챗봇 필드 초기화
        chatMessages = new Table();
        chatMessages.top().left();
        chatMessages.pad(10); // 테이블 패딩 설정 (옵션)

        chatScrollPane = new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane(chatMessages, skin);
        chatScrollPane.setFadeScrollBars(false);
        chatScrollPane.setScrollingDisabled(true, false);

        chatInput = new com.badlogic.gdx.scenes.scene2d.ui.TextField("", skin);
        chatInput.setMessageText("메시지를 입력하세요...");

        sendButton = new com.badlogic.gdx.scenes.scene2d.ui.TextButton("전송", skin);
        sendButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sendMessage();
            }
        });

        // **수정된 부분: 챗봇 창에 그라데이션 배경 적용**
        // 그라데이션 배경 생성 (예: 어두운 빨간색에서 검은색으로)
        Drawable customChatBackground = createGradientDrawable(
            new Color(0.2f, 0f, 0f, 0.9f), // 시작 색상 (어두운 빨간색)
            new Color(0f, 0f, 0f, 0.9f),   // 끝 색상 (검은색)
            100 // 그라데이션 높이
        );

        // 커스텀 DialogStyle 생성
        Window.WindowStyle customChatStyle = new Window.WindowStyle();
        customChatStyle.background = customChatBackground;
        customChatStyle.titleFont = skin.getFont("default-font");
        customChatStyle.titleFontColor = Color.RED; // 제목 글자색을 붉게 설정

        // Skin에 커스텀 ChatStyle 추가
        skin.add("custom-chat", customChatStyle);
        Gdx.app.log("Skin", "custom-chat style added to skin");

        chatWindow = new Window("챗봇", skin, "custom-chat"); // "custom-chat" 스타일 사용
        chatWindow.setSize(1000, 400);
        chatWindow.setPosition((Gdx.graphics.getWidth() - chatWindow.getWidth()) / 2, (Gdx.graphics.getHeight() - chatWindow.getHeight()) / 2);
        chatWindow.setVisible(false);

        Table mainTableChat = new Table();
        mainTableChat.setFillParent(true);
        mainTableChat.add(chatScrollPane).expand().fill().padBottom(10).row();

        Table inputTableChat = new Table();
        inputTableChat.add(chatInput).expandX().fillX().padRight(10).height(50);
        inputTableChat.add(sendButton).size(80, 50);
        mainTableChat.add(inputTableChat).expandX().fillX().align(com.badlogic.gdx.utils.Align.bottom);

        chatWindow.add(mainTableChat).expand().fill();
        stage.addActor(chatWindow);

        // **수정된 부분: 챗봇 버튼을 ImageButton으로 변경**
        // 기존의 TextButton을 제거하고 ImageButton으로 대체

        // 1. mail.png 텍스처 로드
        this.mailButtonTexture = new Texture(Gdx.files.internal("mail.png"));

        // 2. Drawable 생성
        TextureRegionDrawable mailButtonDrawable = new TextureRegionDrawable(new TextureRegion(mailButtonTexture));

        // 3. ImageButtonStyle 생성 및 드로어블 설정
        ImageButton.ImageButtonStyle chatBotImageButtonStyle = new ImageButton.ImageButtonStyle();
        chatBotImageButtonStyle.up = mailButtonDrawable;
        chatBotImageButtonStyle.down = mailButtonDrawable;  // 클릭 시 동일한 이미지 사용
        chatBotImageButtonStyle.over = mailButtonDrawable;  // 마우스 오버 시 동일한 이미지 사용

        // 4. ImageButton 생성
        ImageButton chatBotImageButton = new ImageButton(chatBotImageButtonStyle);

        // 5. 버튼 크기 설정 (너비 200, 높이 150)
        chatBotImageButton.setSize(150, 150);

        // 6. 버튼 위치 설정 (화면 좌측 상단에 배치)
        chatBotImageButton.setPosition(20, stage.getHeight() - chatBotImageButton.getHeight() - 20);

        // 7. 클릭 리스너 추가 (챗봇 창 토글)
        chatBotImageButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleChatWindow();
            }
        });

        // 8. Stage에 ImageButton 추가
        stage.addActor(chatBotImageButton);
    }

    /**
     * 애니메이션을 로드하는 메서드
     *
     * @param fileName 애니메이션 스프라이트 시트 파일 이름
     * @param frameCols 프레임의 열 수
     * @param frameRows 프레임의 행 수
     * @param frameDuration 각 프레임의 지속 시간
     * @return 로드된 애니메이션
     */
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

    /**
     * 아이들 애니메이션을 로드하는 메서드
     *
     * @param fileName 아이들 프레임 이미지 파일 이름
     * @return 로드된 아이들 애니메이션
     */
    private Animation<TextureRegion> loadIdleAnimation(String fileName) {
        Texture idleTexture = new Texture(Gdx.files.internal(fileName));
        TextureRegion idleRegion = new TextureRegion(idleTexture);
        Animation<TextureRegion> idleAnimation = new Animation<>(0.1f, idleRegion);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP);
        return idleAnimation;
    }

    /**
     * 타일맵의 장애물 층에서 Box2D 바디를 생성하는 메서드
     */
    private void createObstacleBodiesFromTiles() {
        for (MapLayer layer : map.getLayers()) {
            if (layer.getName().equals("obstacle1") || layer.getName().equals("obstacle2")) {
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

    /**
     * 단일 타일에 대한 정적 바디를 생성하는 메서드
     *
     * @param x 타일의 x 인덱스
     * @param y 타일의 y 인덱스
     * @param tileWidth 타일의 너비
     * @param tileHeight 타일의 높이
     */
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

        // 메시지 라벨 타이머 업데이트
        if (messageLabel.isVisible()) {
            messageTimer += deltaTime;
            if (messageTimer >= messageDisplayTime) {
                messageLabel.setVisible(false);
                messageTimer = 0f;
            }
        }

        // 페이드 인/아웃 처리 및 게임 로직 업데이트
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

            // 도어와의 충돌 체크
            for (Door door : doors) {
                if (playerRect.overlaps(door.rectangle)) {
                    if (!isFadingOut) {
                        Inventory inventory = Inventory.getInstance();

                        if ("door".equals(door.type)) {
                            if (inventory.hasItem("2층 열쇠")) {
                                isFadingOut = true;
                                fadeOutElapsed = 0f;
                                playerBody.setLinearVelocity(0, 0);
                                playerBody.setActive(false);
                                if (transitionSound != null) {
                                    transitionSound.play();
                                }
                                Vector2 spawnPoint = new Vector2(160f / PPM, 820f / PPM);
                                Gdx.app.log("FourthScreen", "FifthScreen으로 전환, 스폰 포인트: " + spawnPoint);
                                nextScreen = new FifthScreen(game);
                                if (backgroundMusic != null) {
                                    backgroundMusic.stop();
                                    backgroundMusic.dispose();
                                    backgroundMusic = null;
                                }
                            } else {
                                showMessage("2층 열쇠가 필요합니다.");
                            }
                        }
                    }
                    break;
                }
            }
        }

        // 카메라 업데이트
        camera.position.set(playerPosition.x, playerPosition.y, 0);
        camera.update();

        // 화면 클리어
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 맵 렌더링
        mapRenderer.setView(camera);
        mapRenderer.render();

        // 플레이어 그리기
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

        float drawWidthPlayer = (currentFrame.getRegionWidth() / PPM) * playerScale;
        float drawHeightPlayer = (currentFrame.getRegionHeight() / PPM) * playerScale;

        batch.draw(currentFrame,
            playerPosition.x - drawWidthPlayer / 2,
            playerPosition.y - drawHeightPlayer / 2,
            drawWidthPlayer,
            drawHeightPlayer);

        batch.end();

        // 라이트 업데이트 및 렌더링
        rayHandler.setCombinedMatrix(camera);
        rayHandler.updateAndRender();

        // Stage 업데이트 및 그리기
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
        viewport.update(width, height);
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
        if (inventoryWindowBackgroundTexture != null) {
            inventoryWindowBackgroundTexture.dispose();
            inventoryWindowBackgroundTexture = null;
        }

        if (transitionSound != null) transitionSound.dispose();
        if (investigateButtonTexture != null) investigateButtonTexture.dispose();
        if (inventoryButtonTexture != null) inventoryButtonTexture.dispose();
        if (skin != null) skin.dispose();

        // **추가된 부분: mail.png 텍스처 해제**
        if (mailButtonTexture != null) {
            mailButtonTexture.dispose();
        }
    }

    /**
     * 애니메이션을 해제하는 메서드
     *
     * @param animation 해제할 애니메이션
     */
    private void disposeAnimation(Animation<TextureRegion> animation) {
        if (animation != null) {
            for (TextureRegion frame : animation.getKeyFrames()) {
                Texture tex = frame.getTexture();
                if (tex != null) tex.dispose();
            }
        }
    }

    /**
     * 플레이어가 Quiz2 영역 내에 있는지 확인하는 메서드
     *
     * @return 플레이어가 Quiz2 영역 내에 있으면 true, 아니면 false
     */
    private boolean isPlayerInQuiz2Area() {
        float drawWidth = (playerTexture.getWidth() / PPM) * playerScale;
        float drawHeight = (playerTexture.getHeight() / PPM) * playerScale;

        Rectangle playerRect = new Rectangle(
            playerPosition.x - (drawWidth / 2),
            playerPosition.y - (drawHeight / 2),
            drawWidth,
            drawHeight
        );

        for (Rectangle area : quiz2Areas) {
            if (playerRect.overlaps(area)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 플레이어가 Quiz1 영역 내에 있는지 확인하는 메서드
     *
     * @return 플레이어가 Quiz1 영역 내에 있으면 true, 아니면 false
     */
    private boolean isPlayerInQuiz1Area() {
        float drawWidth = (playerTexture.getWidth() / PPM) * playerScale;
        float drawHeight = (playerTexture.getHeight() / PPM) * playerScale;

        Rectangle playerRect = new Rectangle(
            playerPosition.x - (drawWidth / 2),
            playerPosition.y - (drawHeight / 2),
            drawWidth,
            drawHeight
        );

        for (Rectangle area : quiz1Areas) {
            if (playerRect.overlaps(area)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 메시지를 화면에 표시하는 메서드
     *
     * @param message 표시할 메시지 텍스트
     */
    private void showMessage(String message) {
        // default-font 폰트 존재 확인 후 Label 생성
        BitmapFont font = skin.getFont("default-font");
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
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

    /**
     * 조사하기 버튼을 클릭했을 때 호출되는 메서드
     * 해당 영역에 따라 다른 행동을 수행합니다.
     */
    private void handleInvestigateAction() {
        Inventory inventory = Inventory.getInstance();

        if (isPlayerInQuiz2Area()) {
            if (!inventory.hasItem("2층 열쇠")) {
                game.setScreen(new SlidePuzzleScreen(game, this));
            } else {
                showMessage("이미 2층 열쇠를 획득했습니다.");
            }
        } else if (isPlayerInQuiz1Area()) {
            computerLockScreen = new ComputerLock(game, this);
            game.setScreen(computerLockScreen);
        } else {
            showMessage("아무 것도 찾을 수 없습니다.");
        }
    }

    /**
     * 인벤토리 창의 가시성을 토글하는 메서드
     */
    private void toggleInventory() {
        isInventoryOpen = !isInventoryOpen;
        inventoryWindow.setVisible(isInventoryOpen);

        if (isInventoryOpen) {
            updateInventoryUI();
        }
    }

    /**
     * 인벤토리 UI를 업데이트하는 메서드
     */
    private void updateInventoryUI() {
        inventoryTable.clear();

        Inventory inventory = Inventory.getInstance();
        BitmapFont font = skin.getFont("default-font"); // 폰트 확인
        for (String item : inventory.getItems()) {
            Label.LabelStyle labelStyle = new Label.LabelStyle();
            labelStyle.font = font;
            labelStyle.fontColor = Color.WHITE;
            Label itemLabel = new Label(item, labelStyle);
            inventoryTable.add(itemLabel).left().pad(10, 10, 10, 10).width(280);
            inventoryTable.row();
        }

        inventoryTable.invalidateHierarchy();
    }

    /**
     * 플레이어를 Quiz1 영역의 첫 번째 위치로 이동시키는 메서드
     */
    public void returnToQuiz1Area() {
        if (!quiz1Areas.isEmpty()) {
            Rectangle firstQuiz1Area = quiz1Areas.get(0);
            playerPosition.set(firstQuiz1Area.x + firstQuiz1Area.width / 2, firstQuiz1Area.y + firstQuiz1Area.height / 2);
            playerBody.setTransform(playerPosition, 0);
            camera.position.set(playerPosition.x, playerPosition.y, 0);
            camera.update();
        }
    }

    /**
     * 챗봇 창의 가시성을 토글하는 메서드
     */
    private void toggleChatWindow() {
        isChatVisible = !isChatVisible;
        chatWindow.setVisible(isChatVisible);
        if (isChatVisible) {
            Gdx.input.setInputProcessor(stage);
        } else {
            Gdx.input.setInputProcessor(stage);
        }
    }

    /**
     * 챗봇에 메시지를 전송하는 메서드
     */
    private void sendMessage() {
        String userMessage = chatInput.getText().trim();
        if (!userMessage.isEmpty()) {
            addMessage("나: " + userMessage);
            chatInput.setText("");
            sendToChatBot(userMessage);
        }
    }

    /**
     * 챗봇 메시지를 화면에 추가하는 메서드
     *
     * @param message 추가할 메시지 텍스트
     */
    private void addMessage(String message) {
        BitmapFont font = skin.getFont("default-font");
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;

        Label messageLabel = new Label(message, labelStyle);
        messageLabel.setWrap(true);
        messageLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        messageLabel.setWidth(chatScrollPane.getWidth() - 20); // 패딩 고려

        // 메시지를 테이블의 새로운 행으로 추가
        chatMessages.add(messageLabel).left().expandX().fillX().row();

        // 스크롤페인 레이아웃 갱신 및 최신 메시지로 스크롤
        chatScrollPane.layout();
        chatScrollPane.scrollTo(0, chatMessages.getHeight(), 0, 0);
    }

    /**
     * 챗봇 API로 메시지를 전송하는 메서드
     *
     * @param message 전송할 사용자 메시지
     */
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

    /**
     * 챗봇 응답 클래스
     */
    private static class ChatResponse {
        public String response;
    }
}

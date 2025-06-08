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
import com.badlogic.gdx.audio.Sound; // 효과음을 위해 추가
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap; // Pixmap을 사용하기 위해 추가
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable; // Drawable 임포트 추가
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import java.util.List;
import java.util.ArrayList;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Json; // 챗봇 JSON 파싱 위해 추가

public class FifthScreen implements Screen {
    private Game game;

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

    // Door 클래스 정의
    private class Door {
        Rectangle rectangle;
        String type;

        Door(Rectangle rectangle, String type) {
            this.rectangle = rectangle;
            this.type = type;
        }
    }

    // Quiz2, Quiz3, Quiz4 영역을 저장할 리스트
    private List<Rectangle> quiz2Areas;
    private List<Rectangle> quiz3Areas;
    private List<Rectangle> quiz4Areas;

    // Box2DLights 관련 변수 추가
    private RayHandler rayHandler;
    private PointLight flashlight;

    // 페이드 효과 관련 변수 추가
    private Texture blackTexture;
    private float fadeInTime = 1.0f; // 페이드 인 지속 시간 (초)
    private float fadeOutTime = 1.0f; // 페이드 아웃 지속 시간 (초)
    private float fadeInElapsed = 0f;
    private float fadeOutElapsed = 0f;
    private boolean isFadingIn = true;
    private boolean isFadingOut = false;

    // 효과음 변수 추가
    private Sound transitionSound;

    // 다음 스크린 정보를 저장할 변수 추가
    private Screen nextScreen = null;

    // 인벤토리 관련 변수 추가
    private Window inventoryWindow;
    private Table inventoryTable;
    private Skin skin;
    private boolean isInventoryOpen = false;

    // 인벤토리 버튼 관련 변수 추가
    private Texture inventoryButtonTexture;
    private ImageButton inventoryButton;

    // 조사하기 버튼 관련 변수 추가
    private Texture investigateButtonTexture;
    private ImageButton investigateButton;

    private float messageDisplayTime = 1.0f; // 메시지가 표시되는 시간 (초)
    private float messageTimer = 0f;

    // --- 챗봇 관련 필드 추가 ---
    private Window chatWindow;
    private com.badlogic.gdx.scenes.scene2d.ui.TextField chatInput;
    private com.badlogic.gdx.scenes.scene2d.ui.TextButton sendButton;
    private com.badlogic.gdx.scenes.scene2d.ui.Table chatMessages;
    private com.badlogic.gdx.scenes.scene2d.ui.ScrollPane chatScrollPane;
    private boolean isChatVisible = false;
    // --- 챗봇 관련 필드 끝 ---

    // **추가된 부분: 그라데이션 생성 메서드**
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

    public FifthScreen(Game game, Vector2 spawnPosition) {
        this.game = game;
        this.playerPosition = spawnPosition.cpy(); // 안전하게 복사
    }

    public FifthScreen(Game game) {
        this(game, new Vector2(160f / PPM, 820f / PPM));
    }

    @Override
    public void show() {
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        inventoryTable = new Table(skin);
        inventoryTable.top().left();

        int windowWidth = 300;
        int windowHeight = 400;

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

        Texture inventoryWindowBackgroundTexture = new Texture(pixmap);
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

        // --- 챗봇 기능 추가 ---
        // 챗봇 관련 필드 초기화
        chatMessages = new com.badlogic.gdx.scenes.scene2d.ui.Table();
        chatMessages.top().left();
        chatMessages.pad(10); // 패딩 설정 (옵션)


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

        // 챗봇 창 생성에 커스텀 스타일 적용
        chatWindow = new Window("챗봇", skin, "custom-chat"); // "custom-chat" 스타일 사용
        chatWindow.setSize(1000, 400);
        chatWindow.setPosition((Gdx.graphics.getWidth() - chatWindow.getWidth()) / 2, (Gdx.graphics.getHeight() - chatWindow.getHeight()) / 2);
        chatWindow.setVisible(false);

        Table mainChatTable = new Table();
        mainChatTable.setFillParent(true);
        mainChatTable.add(chatScrollPane).expand().fill().padBottom(10).row();

        Table inputChatTable = new Table();
        inputChatTable.add(chatInput).expandX().fillX().padRight(10).height(50);
        inputChatTable.add(sendButton).size(80, 50);
        mainChatTable.add(inputChatTable).expandX().fillX().align(com.badlogic.gdx.utils.Align.bottom);

        chatWindow.add(mainChatTable).expand().fill();
        stage.addActor(chatWindow);

        // **수정된 부분: 챗봇 버튼에 그라데이션 스타일 적용**
        // 그라데이션 배경 생성 (예: 어두운 빨간색에서 더 밝은 빨간색으로)
        Drawable chatBotUpDrawable = createGradientDrawable(new Color(0.2f, 0f, 0f, 1f), new Color(0.4f, 0f, 0f, 0.7f), 50);
        Drawable chatBotDownDrawable = createGradientDrawable(new Color(0.4f, 0f, 0f, 0.7f), new Color(0.6f, 0f, 0f, 0.5f), 50);
        Drawable chatBotOverDrawable = createGradientDrawable(new Color(0.6f, 0f, 0f, 0.5f), new Color(0.8f, 0f, 0f, 0.3f), 50);

        // 새로운 TextButtonStyle 생성
        com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle chatBotButtonStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle();
        chatBotButtonStyle.up = chatBotUpDrawable;
        chatBotButtonStyle.down = chatBotDownDrawable;
        chatBotButtonStyle.over = chatBotOverDrawable;
        chatBotButtonStyle.font = skin.getFont("default-font");
        chatBotButtonStyle.fontColor = Color.WHITE;
        chatBotButtonStyle.overFontColor = Color.ORANGE;

        // Skin에 커스텀 ChatBotButtonStyle 추가
        skin.add("custom-chatbot", chatBotButtonStyle);

        // 챗봇 버튼 생성에 커스텀 스타일 적용
        com.badlogic.gdx.scenes.scene2d.ui.TextButton chatBotButton = new com.badlogic.gdx.scenes.scene2d.ui.TextButton("챗봇", skin, "custom-chatbot");
        chatBotButton.setSize(200, 75);
        chatBotButton.setPosition(20, stage.getHeight() - chatBotButton.getHeight() - 20);
        chatBotButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleChatWindow();
            }
        });
        stage.addActor(chatBotButton);
        // --- 챗봇 기능 추가 끝 ---

        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        viewport = new FitViewport(800 / PPM, 480 / PPM, camera);
        viewport.apply();

        camera.zoom = 1f;

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();

        transitionSound = Gdx.audio.newSound(Gdx.files.internal("stairs.mp3"));

        Pixmap pixmapBlack = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmapBlack.setColor(0, 0, 0, 1);
        pixmapBlack.fill();
        blackTexture = new Texture(pixmapBlack);
        pixmapBlack.dispose();

        map = new TmxMapLoader().load("2f.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1 / PPM);

        playerSpeed = 100f;
        playerScale = 2f;

        camera.position.set(playerPosition.x, playerPosition.y, 0);
        camera.update();

        Texture joystickBase = new Texture("joystick_base.png");
        Texture joystickKnob = new Texture("joystick_knob.png");
        float joystickX = 50;
        float joystickY = 50;

        joystick = new Joystick(joystickBase, joystickKnob, joystickX, joystickY);
        float joystickScale = 0.8f;
        joystick.setScale(joystickScale);
        stage.addActor(joystick);

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
        quiz2Areas = new ArrayList<>();
        quiz3Areas = new ArrayList<>();
        quiz4Areas = new ArrayList<>();
        MapLayers mapLayers = map.getLayers();
        for (MapLayer layer : mapLayers) {
            if (layer instanceof TiledMapTileLayer) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                String layerName = tileLayer.getName();

                if (layerName.equals("Door1") || layerName.equals("Door3")) {
                    String type = null;
                    if (layerName.equals("Door1")) {
                        type = "door1";
                    } else if (layerName.equals("Door3")) {
                        type = "door3";
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

                if (layerName.equals("Quiz3")) {
                    for (int x = 0; x < tileLayer.getWidth(); x++) {
                        for (int y = 0; y < tileLayer.getHeight(); y++) {
                            TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                            if (cell != null && cell.getTile() != null && cell.getTile().getId() != 0) {
                                float tileX = (x * tileLayer.getTileWidth()) / PPM;
                                float tileY = (y * tileLayer.getTileHeight()) / PPM;
                                float tileWidth = tileLayer.getTileWidth() / PPM;
                                float tileHeight = tileLayer.getTileHeight() / PPM;
                                Rectangle tileRect = new Rectangle(tileX, tileY, tileWidth, tileHeight);
                                quiz3Areas.add(tileRect);
                            }
                        }
                    }
                }

                if (layerName.equals("Quiz4")) {
                    for (int x = 0; x < tileLayer.getWidth(); x++) {
                        for (int y = 0; y < tileLayer.getHeight(); y++) {
                            TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                            if (cell != null && cell.getTile() != null && cell.getTile().getId() != 0) {
                                float tileX = (x * tileLayer.getTileWidth()) / PPM;
                                float tileY = (y * tileLayer.getTileHeight()) / PPM;
                                float tileWidth = tileLayer.getTileWidth() / PPM;
                                float tileHeight = tileLayer.getTileHeight() / PPM;
                                Rectangle tileRect = new Rectangle(tileX, tileY, tileWidth, tileHeight);
                                quiz4Areas.add(tileRect);
                            }
                        }
                    }
                }
            }
        }

        RayHandler.setGammaCorrection(true);
        RayHandler.useDiffuseLight(true);
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.1f);

        flashlight = new PointLight(rayHandler, 128, new Color(1, 1, 1, 1), 300 / PPM, playerPosition.x, playerPosition.y);
        flashlight.setXray(true);
        flashlight.setSoftnessLength(10f);

        // --- 챗봇 기능 추가 끝 ---

        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        viewport = new FitViewport(800 / PPM, 480 / PPM, camera);
        viewport.apply();

        camera.zoom = 1f;

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();

        transitionSound = Gdx.audio.newSound(Gdx.files.internal("stairs.mp3"));

        pixmapBlack = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmapBlack.setColor(0, 0, 0, 1);
        pixmapBlack.fill();
        blackTexture = new Texture(pixmapBlack);
        pixmapBlack.dispose();
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
            if (layer.getName().equals("obstacle3") || layer.getName().equals("obstacle4")){
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

            Inventory inventory = Inventory.getInstance();

            for (Door door : doors) {
                if (playerRect.overlaps(door.rectangle)) {
                    if (!isFadingOut) {
                        if ("door1".equals(door.type)) {
                            if (inventory.hasItem("2층 열쇠")) {
                                isFadingOut = true;
                                fadeOutElapsed = 0f;
                                playerBody.setLinearVelocity(0, 0);
                                playerBody.setActive(false);
                                if (transitionSound != null) {
                                    transitionSound.play();
                                }
                                Vector2 spawnPoint = new Vector2(1400f / PPM, 520f / PPM);
                                nextScreen = new FourthScreen(game, spawnPoint);
                                if (backgroundMusic != null) {
                                    backgroundMusic.stop();
                                    backgroundMusic.dispose();
                                    backgroundMusic = null;
                                }
                            } else {
                                showMessage("2층 열쇠가 필요합니다.");
                            }
                        } else if ("door3".equals(door.type)) {
                            if (inventory.hasItem("3층 열쇠")) {
                                isFadingOut = true;
                                fadeOutElapsed = 0f;
                                playerBody.setLinearVelocity(0, 0);
                                playerBody.setActive(false);
                                if (transitionSound != null) {
                                    transitionSound.play();
                                }
                                Vector2 spawnPoint = new Vector2(950f / PPM, 900f / PPM);
                                nextScreen = new SixthScreen(game, spawnPoint);
                                if (backgroundMusic != null) {
                                    backgroundMusic.stop();
                                    backgroundMusic.dispose();
                                    backgroundMusic = null;
                                }
                            } else {
                                showMessage("3층 열쇠가 필요합니다.");
                            }
                        }
                    }
                    break;
                }
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

        stage.act(deltaTime);
        stage.draw();
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
        joystick.baseTexture.dispose();
        joystick.knobTexture.dispose();
        playerTexture.dispose();
        world.dispose();
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
        map.dispose();
        mapRenderer.dispose();

        disposeAnimation(walkUpAnimation);
        disposeAnimation(walkDownAnimation);
        disposeAnimation(walkLeftAnimation);
        disposeAnimation(walkRightAnimation);

        disposeAnimation(idleUpAnimation);
        disposeAnimation(idleDownAnimation);
        disposeAnimation(idleLeftAnimation);
        disposeAnimation(idleRightAnimation);

        rayHandler.dispose();

        if (blackTexture != null) {
            blackTexture.dispose();
        }

        if (transitionSound != null) {
            transitionSound.dispose();
        }

        if (inventoryButtonTexture != null) {
            inventoryButtonTexture.dispose();
        }

        if (investigateButtonTexture != null) {
            investigateButtonTexture.dispose();
        }

        if (skin != null) {
            skin.dispose();
        }

        // Dispose of chat window background texture if any
        // (그라데이션 텍스처는 WindowStyle에 의해 관리되므로 별도로 dispose 할 필요 없음)
    }

    private void disposeAnimation(Animation<TextureRegion> animation) {
        for (TextureRegion frame : animation.getKeyFrames()) {
            frame.getTexture().dispose();
        }
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

    private void handleInvestigateAction() {
        Inventory inventory = Inventory.getInstance();

        boolean found = false;

        if (isPlayerInQuiz2Area() && !inventory.hasItem("열쇠")) {
            inventory.addItem("열쇠");
            showMessage("열쇠를 획득했습니다!");
            updateInventoryUI();
            found = true;
        }

        if (isPlayerInQuiz3Area()) {
            // Quiz3 -> AnalogClockScreen으로 전환하는 로직
            isFadingOut = true;
            fadeOutElapsed = 0f;

            playerBody.setLinearVelocity(0, 0);
            playerBody.setActive(false);

            Vector2 spawnPointForQuiz3 = playerPosition.cpy();
            nextScreen = new AnalogClockScreen(game, spawnPointForQuiz3);

            if (backgroundMusic != null) {
                backgroundMusic.stop();
                backgroundMusic.dispose();
                backgroundMusic = null;
            }

            found = true;
        }

        if (isPlayerInQuiz4Area()) {
            if (!inventory.hasItem("깃펜")) {
                showMessage("쓸 것이 필요합니다.");
            } else {
                Vector2 spawnPointForQuiz4 = playerPosition.cpy();
                game.setScreen(new DrawingPuzzleScreen(game, spawnPointForQuiz4));
            }
            found = true;
        }

        if (!found) {
            showMessage("아무 것도 찾을 수 없습니다.");
        }
    }

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

    private boolean isPlayerInQuiz3Area() {
        float drawWidth = (playerTexture.getWidth() / PPM) * playerScale;
        float drawHeight = (playerTexture.getHeight() / PPM) * playerScale;

        Rectangle playerRect = new Rectangle(
            playerPosition.x - (drawWidth / 2),
            playerPosition.y - (drawHeight / 2),
            drawWidth,
            drawHeight
        );

        for (Rectangle area : quiz3Areas) {
            if (playerRect.overlaps(area)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPlayerInQuiz4Area() {
        float drawWidth = (playerTexture.getWidth() / PPM) * playerScale;
        float drawHeight = (playerTexture.getHeight() / PPM) * playerScale;

        Rectangle playerRect = new Rectangle(
            playerPosition.x - (drawWidth / 2),
            playerPosition.y - (drawHeight / 2),
            drawWidth,
            drawHeight
        );

        for (Rectangle area : quiz4Areas) {
            if (playerRect.overlaps(area)) {
                return true;
            }
        }
        return false;
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
}

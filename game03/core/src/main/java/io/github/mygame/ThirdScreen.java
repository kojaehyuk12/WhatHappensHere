package io.github.mygame;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.List;

/**
 * ThirdScreen 클래스는 게임의 3번째 화면을 담당합니다.
 * 이 화면에서도 조사하기 버튼을 클릭할 때 텍스트 메시지만 표시되도록 구현합니다.
 */
public class ThirdScreen implements Screen {
    private Game game;
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
    private RayHandler rayHandler;
    private PointLight flashlight;

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
    private float playerScale = 1.5f;
    private static final float PPM = 100f;
    private Texture blackTexture;
    private float fadeOutTime = 1.0f;
    private float fadeOutElapsed = 0f;
    private boolean isTransitioning = false;
    private Sound transitionSound;

    private final Vector2 investigationPosition = new Vector2(5, 3); // 돌의 위치
    private final float investigationRadius = 0.8f; // 돌 조사 범위
    private final Vector2 treePosition = new Vector2(8, 5); // 나무의 위치
    private final float treeInvestigationRadius = 1.2f; // 나무 조사 범위

    // 챗봇 관련 필드
    private Window chatWindow;
    private TextField chatInput;
    private TextButton sendButton;
    private Table chatMessages; // VerticalGroup에서 Table로 변경
    private ScrollPane chatScrollPane;
    private boolean isChatVisible = false;
    private Skin skin; // 한 번만 로드
    private BitmapFont koreanFont; // 추가
    private final Vector2 doorPosition = new Vector2(8, 6); // 문 앞 위치
    private final float doorInvestigationRadius = 1.0f; // 문 앞 범위
    private boolean doorInvestigated = false; // 자물쇠 조사 여부

    // "조사하기" 버튼을 ImageButton으로 변경하기 위해 추가된 변수
    private Texture investigateButtonTexture;
    private ImageButton investigationButton;

    // 클래스 필드로 helpButtonStyle과 chatBotButtonStyle 추가
    private TextButton.TextButtonStyle helpButtonStyle;
    // private TextButton.TextButtonStyle chatBotButtonStyle; // 더 이상 필요 없음

    // ImageButton을 위한 텍스처 필드 추가
    private Texture mailButtonTexture;
    private Texture mailButtonDownTexture; // 클릭 상태 이미지 (옵션)
    private Texture mailButtonOverTexture; // 마우스 오버 상태 이미지 (옵션)

    // 문 객체를 저장할 리스트
    private List<Door> doors;

    /**
     * Door 클래스 정의
     */
    private class Door {
        Rectangle rectangle;
        String type;

        Door(Rectangle rectangle, String type) {
            this.rectangle = rectangle;
            this.type = type;
        }
    }

    public ThirdScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(800 / PPM, 480 / PPM, camera);
        viewport.apply();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();

        map = new TmxMapLoader().load("untitled.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1 / PPM);
        playerPosition = new Vector2(510 / PPM, 200 / PPM);
        playerSpeed = 100f;
        camera.position.set(playerPosition.x, playerPosition.y, 0);
        camera.update();

        Texture joystickBase = new Texture("joystick_base.png");
        Texture joystickKnob = new Texture("joystick_knob.png");
        joystick = new Joystick(joystickBase, joystickKnob, 50, 50);
        joystick.setScale(0.8f);
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
        shape.setRadius((16f * playerScale) / PPM);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        playerBody.createFixture(fixtureDef);
        shape.dispose();

        createObstacleBodiesFromTiles();

        RayHandler.setGammaCorrection(true);
        RayHandler.useDiffuseLight(true);
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.1f);
        flashlight = new PointLight(rayHandler, 128, new Color(1, 1, 1, 1), 500 / PPM, playerPosition.x, playerPosition.y);
        flashlight.setXray(true);
        flashlight.setSoftnessLength(10f);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 1);
        pixmap.fill();
        blackTexture = new Texture(pixmap);
        pixmap.dispose();

        // FreeTypeFontGenerator를 사용하여 한국어 폰트 생성
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/NotoSansKR-Regular.ttf")); // 폰트 파일 경로
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 30; // 원하는 폰트 크기
        parameter.color = Color.WHITE;

        // 모든 한글 음절을 포함하도록 수정
        StringBuilder characters = new StringBuilder(FreeTypeFontGenerator.DEFAULT_CHARS + "한글을지원합니다.");
        for (char c = '\uAC00'; c <= '\uD7A3'; c++) {
            characters.append(c);
        }
        parameter.characters = characters.toString();

        koreanFont = generator.generateFont(parameter);
        generator.dispose();

        // Skin 로드
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // 그라데이션 배경 생성 (공포 컨셉에 맞게 어두운 빨간색에서 검은색으로)
        Drawable customDialogBackground = createGradientDrawable(
            new Color(0.2f, 0f, 0f, 0.9f), // 시작 색상 (어두운 빨간색)
            new Color(0f, 0f, 0f, 0.9f),   // 끝 색상 (검은색)
            100 // 그라데이션 높이
        );

        // 커스텀 DialogStyle 생성
        Window.WindowStyle customDialogStyle = new Window.WindowStyle();
        customDialogStyle.background = customDialogBackground;
        customDialogStyle.titleFont = koreanFont;
        customDialogStyle.titleFontColor = Color.RED; // 제목 글자색을 붉게 설정

        // Skin에 커스텀 DialogStyle 추가
        skin.add("custom-dialog", customDialogStyle);
        Gdx.app.log("Skin", "custom-dialog style added to skin");

        // "도움말" 버튼 스타일 커스터마이징 시작
        // 그라데이션 배경 생성 (공포 컨셉에 맞게 어두운 빨간색에서 더 어두운 빨간색으로)
        TextureRegionDrawable helpUpDrawable = createGradientDrawable(new Color(0.3f, 0f, 0f, 1f), new Color(0.5f, 0f, 0f, 0.7f), 50);
        TextureRegionDrawable helpDownDrawable = createGradientDrawable(new Color(0.5f, 0f, 0f, 0.7f), new Color(0.7f, 0f, 0f, 0.5f), 50);
        TextureRegionDrawable helpOverDrawable = createGradientDrawable(new Color(0.7f, 0f, 0f, 0.5f), new Color(0.9f, 0f, 0f, 0.3f), 50);

        // 새로운 TextButtonStyle 생성 및 클래스 필드에 할당
        helpButtonStyle = new TextButton.TextButtonStyle();
        helpButtonStyle.up = helpUpDrawable;
        helpButtonStyle.down = helpDownDrawable;
        helpButtonStyle.over = helpOverDrawable;
        helpButtonStyle.font = koreanFont;
        helpButtonStyle.fontColor = Color.WHITE;
        helpButtonStyle.overFontColor = Color.RED; // 마우스를 올렸을 때 텍스트 색상 변경

        // 커스터마이징된 "도움말" 버튼 생성 및 추가
        TextButton helpButton = new TextButton("도움말", helpButtonStyle);
        helpButton.setSize(200, 75); // **수정된 부분: 버튼 크기 키우기**
        helpButton.setPosition(Gdx.graphics.getWidth() - helpButton.getWidth() - 20, Gdx.graphics.getHeight() - helpButton.getHeight() - 20); // 버튼 위치 설정
        helpButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showHelpDialog();
            }
        });
        stage.addActor(helpButton);

        // 챗봇 버튼을 ImageButton으로 대체
        // 1. 텍스처 로드
        mailButtonTexture = new Texture(Gdx.files.internal("mail.png"));
        // 클릭 및 오버 상태 이미지를 별도로 사용하려면 아래 라인의 주석을 제거하고, 해당 이미지를 프로젝트에 추가하세요.
        // mailButtonDownTexture = new Texture(Gdx.files.internal("mail_down.png"));
        // mailButtonOverTexture = new Texture(Gdx.files.internal("mail_over.png"));

        // 2. Drawable 생성
        TextureRegionDrawable mailButtonDrawable = new TextureRegionDrawable(new TextureRegion(mailButtonTexture));
        // 클릭 및 오버 상태 드로어블을 별도로 사용하는 경우
        // TextureRegionDrawable mailButtonDownDrawable = new TextureRegionDrawable(new TextureRegion(mailButtonDownTexture));
        // TextureRegionDrawable mailButtonOverDrawable = new TextureRegionDrawable(new TextureRegion(mailButtonOverTexture));

        // 3. ImageButtonStyle 생성 및 드로어블 설정
        ImageButton.ImageButtonStyle chatBotButtonStyle = new ImageButton.ImageButtonStyle();
        chatBotButtonStyle.up = mailButtonDrawable;
        // 클릭 및 오버 상태 드로어블 설정 (별도의 이미지를 사용하는 경우)
        // chatBotButtonStyle.down = mailButtonDownDrawable;
        // chatBotButtonStyle.over = mailButtonOverDrawable;

        // 동일한 이미지를 클릭 및 오버 상태로 사용하는 경우
        chatBotButtonStyle.down = mailButtonDrawable;
        chatBotButtonStyle.over = mailButtonDrawable;

        // 4. ImageButton 생성
        ImageButton chatBotImageButton = new ImageButton(chatBotButtonStyle);

        // 5. 버튼 크기 설정 (요청에 따라 너비 200, 높이 150으로 변경)
        chatBotImageButton.setSize(150, 150); // **수정된 부분: 버튼 크기 변경**

        // 6. 버튼 위치 설정 (기존 챗봇 버튼과 동일한 위치로 설정)
        // 기존 위치는 화면 좌측 상단에 설정되어 있으므로, 높이 증가에 따라 Y 좌표를 재조정할 수 있습니다.
        chatBotImageButton.setPosition(20, Gdx.graphics.getHeight() - chatBotImageButton.getHeight() - 20);

        // 7. 클릭 리스너 추가 (기존 챗봇 버튼과 동일한 기능)
        chatBotImageButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("ChatBotButton", "챗봇 버튼 클릭됨");
                toggleChatWindow();
            }
        });

        // 8. Stage에 ImageButton 추가
        stage.addActor(chatBotImageButton);

        // "조사하기" 버튼을 ImageButton으로 변경
        investigateButtonTexture = new Texture(Gdx.files.internal("investigate_button.png")); // 조사하기 버튼 이미지 로드
        TextureRegionDrawable investigateButtonDrawable = new TextureRegionDrawable(new TextureRegion(investigateButtonTexture));
        investigationButton = new ImageButton(investigateButtonDrawable);
        investigationButton.setSize(450, 75); // **수정된 부분: 버튼 크기 키우기**
        investigationButton.setPosition(Gdx.graphics.getWidth() - investigationButton.getWidth() - 20, 70); // 위치 설정
        investigationButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                checkInvestigation();
            }
        });
        stage.addActor(investigationButton);

        // 챗봇 창 생성 (초기에는 숨김)
        chatWindow = new Window("챗봇", skin, "custom-dialog"); // "custom-dialog" 스타일 사용
        chatWindow.setSize(1000, 400); // 적절한 크기로 조정 (예: 500x400)
        chatWindow.setPosition((Gdx.graphics.getWidth() - chatWindow.getWidth()) / 2, (Gdx.graphics.getHeight() - chatWindow.getHeight()) / 2);
        chatWindow.setVisible(false);

        // 채팅 메시지를 표시할 Table과 ScrollPane
        chatMessages = new Table();
        chatMessages.top().left();
        chatMessages.pad(10); // 패딩 설정

        chatScrollPane = new ScrollPane(chatMessages, skin);
        chatScrollPane.setFadeScrollBars(false);
        chatScrollPane.setScrollingDisabled(true, false); // 수평 스크롤 비활성화, 수직 스크롤 활성화
        chatScrollPane.setForceScroll(false, true); // 수직 스크롤 강제

        // 입력 필드와 전송 버튼
        chatInput = new TextField("", skin);
        chatInput.setMessageText("메시지를 입력하세요...");
        chatInput.setMaxLength(200); // 메시지 길이 제한 (선택사항)

        sendButton = new TextButton("전송", skin);
        sendButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sendMessage();
            }
        });

        // 창에 패딩 추가
        chatWindow.pad(10);

        // 메인 Table 생성
        Table mainTable = new Table();
        mainTable.setFillParent(true);

        // 채팅 메시지 스크롤 패널을 위쪽에 추가
        mainTable.add(chatScrollPane).expand().fill().padBottom(10).row();

        // 입력 필드와 전송 버튼을 담을 Table
        Table inputTable = new Table();
        inputTable.add(chatInput).expandX().fillX().padRight(10).height(50);
        inputTable.add(sendButton).size(80, 50);
        // 입력 테이블을 mainTable의 마지막 행에 추가하여 하단으로 배치
        mainTable.add(inputTable).expandX().fillX().align(Align.bottom); // 하단 정렬

        // mainTable을 chatWindow에 추가
        chatWindow.add(mainTable).expand().fill();

        stage.addActor(chatWindow);
    }

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

    // 챗봇 창 토글 메서드
    private void toggleChatWindow() {
        isChatVisible = !isChatVisible;
        chatWindow.setVisible(isChatVisible);
        Gdx.app.log("ChatWindow", "챗봇 창 가시성: " + isChatVisible);
        if (isChatVisible) {
            Gdx.input.setInputProcessor(stage);
        } else {
            Gdx.input.setInputProcessor(stage);
        }
    }

    // 메시지 전송 메서드
    private void sendMessage() {
        String userMessage = chatInput.getText().trim();
        if (!userMessage.isEmpty()) {
            Gdx.app.log("Chat", "Sending user message: " + userMessage);
            addMessage("나: " + userMessage, true); // 사용자 메시지 추가
            chatInput.setText("");
            sendToChatBot(userMessage);
        }
    }

    /**
     * 메시지를 채팅창에 추가하는 메서드
     *
     * @param message 메시지 텍스트
     * @param isUser  true이면 사용자의 메시지, false이면 챗봇의 메시지
     */
    private void addMessage(String message, boolean isUser) {
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = koreanFont; // skin을 통해서가 아니라 직접 koreanFont 사용
        labelStyle.fontColor = Color.WHITE;
        Label messageLabel = new Label(message, labelStyle);
        messageLabel.setWrap(true); // 줄 바꿈 활성화
        messageLabel.setAlignment(Align.left); // 메시지 정렬 설정
        messageLabel.setWidth(chatScrollPane.getWidth() - 20); // 패딩을 고려하여 너비 설정

        // 메시지를 테이블의 새로운 행으로 추가
        chatMessages.add(messageLabel).left().expandX().fillX().padBottom(5).row();

        // 스크롤페인 레이아웃 갱신 및 최신 메시지로 스크롤
        chatScrollPane.layout();
        chatScrollPane.scrollTo(0, chatMessages.getHeight(), 0, 0);
    }

    // 챗봇 API로 메시지 전송
    private void sendToChatBot(String message) {
        Gdx.app.log("ChatBot", "Sending message to server: " + message);
        HttpRequest request = new HttpRequest(HttpMethods.GET);
        request.setUrl("http://icechatapi.duckdns.org:5000/chat");
        request.setHeader("Content-Type", "application/json");

        // JSON 객체 사용
        // JSON 형식으로 데이터 전송
        String jsonData = "{\"user_input\": \"" + message + "\"}";
        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
            @Override
            public void handleHttpResponse(HttpResponse httpResponse) {
                Gdx.app.log("ChatBot", "Response status: " + httpResponse.getStatus().getStatusCode());
                Gdx.app.log("ChatBot", "Response content: " + httpResponse.getResultAsString());
                if (httpResponse.getStatus().getStatusCode() == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    // JSON 파싱
                    Json json = new Json();
                    ChatResponse chatResponse = json.fromJson(ChatResponse.class, responseJson);
                    if (chatResponse != null && chatResponse.response != null) {
                        addMessage("챗봇: " + chatResponse.response, false); // 챗봇 메시지 추가
                    } else {
                        addMessage("챗봇: 응답을 처리할 수 없습니다.", false);
                    }
                } else {
                    addMessage("챗봇: 오류가 발생했습니다. 상태 코드: " + httpResponse.getStatus().getStatusCode(), false);
                }
            }

            @Override
            public void failed(Throwable t) {
                addMessage("챗봇: 요청 실패. 네트워크를 확인하세요.", false);
            }

            @Override
            public void cancelled() {
                addMessage("챗봇: 요청이 취소되었습니다.", false);
            }
        });
    }

    // ChatResponse 클래스 정의
    private static class ChatResponse {
        public String response;
    }

    // showHelpDialog() 메서드 수정
    private void showHelpDialog() {
        // 커스텀 스타일을 사용하여 Dialog 생성
        Dialog helpDialog = new Dialog("도움말", skin, "custom-dialog") {
            @Override
            public void result(Object obj) {
                this.hide();
            }
        };
        helpDialog.text("조이스틱을 사용하여 캐릭터를 이동하세요.\n장애물을 피하고 지도를 탐험하세요!");

        // "확인" 버튼의 배경 색상을 검은색으로 설정하기 위해 TextButton을 생성 (수정된 부분 시작)
        TextButton.TextButtonStyle confirmButtonStyle = new TextButton.TextButtonStyle();
        confirmButtonStyle.up = new TextureRegionDrawable(new TextureRegion(blackTexture)); // 검은색 배경
        confirmButtonStyle.down = new TextureRegionDrawable(new TextureRegion(blackTexture)); // 검은색 배경
        confirmButtonStyle.font = koreanFont; // 폰트 설정
        confirmButtonStyle.fontColor = Color.WHITE; // 텍스트 색상 설정 (필요 시 변경 가능)

        TextButton confirmButton = new TextButton("확인", confirmButtonStyle); // 검은색 버튼 생성
        helpDialog.button(confirmButton, true); // 수정된 부분 끝

        helpDialog.show(stage);

        // 다이얼로그의 텍스트 레이블 스타일 커스터마이징
        for (Actor actor : helpDialog.getChildren()) {
            if (actor instanceof Label) {
                Label label = (Label) actor;
                label.setStyle(new Label.LabelStyle(koreanFont, Color.WHITE)); // 텍스트 색상 흰색
                label.setWrap(true); // **수정된 부분: 다이얼로그 텍스트 줄 바꿈 활성화**
                label.setAlignment(Align.center); // **수정된 부분: 다이얼로그 텍스트 중앙 정렬**
            }
        }
    }

    /**
     * 조사하기 버튼을 클릭했을 때 호출되는 메서드
     * 다이얼로그 대신 텍스트 메시지를 화면에 표시합니다.
     */
    private void checkInvestigation() {
        Vector2 playerCurrentPosition = playerBody.getPosition();

        if (playerCurrentPosition.dst(investigationPosition) < investigationRadius) {
            // 돌 조사 메시지 표시
            showInvestigationDialog("돌에 낡은 피가 묻어 있다. 그 옆에 14이란 숫자가 보인다.");
        } else if (playerCurrentPosition.dst(treePosition) < treeInvestigationRadius) {
            // 나무 조사 메시지 표시
            showInvestigationDialog("나무에 오래된 상처가 남아있다. 숫자 27이 희미하게 보인다.");
        }
        // 문 조사
        else if (playerCurrentPosition.dst(doorPosition) < doorInvestigationRadius && !doorInvestigated) {
            game.setScreen(new LockScreen(game));  // 자물쇠 화면으로 넘어가기
            doorInvestigated = true; // 한 번만 조사할 수 있도록 설정
        } else {
            Gdx.app.error("Investigation", "조사할 수 있는 위치가 아닙니다.");
        }
    }

    /**
     * 조사 메시지를 다이얼로그 상자로 표시하는 메서드
     * @param message 표시할 메시지
     */
    private void showInvestigationDialog(String message) {
        Dialog investigationDialog = new Dialog("조사 결과", skin, "custom-dialog") {
            @Override
            public void result(Object obj) {
                this.hide();
            }
        };
        investigationDialog.text(message);

        // "닫기" 버튼의 스타일 설정
        TextButton.TextButtonStyle closeButtonStyle = new TextButton.TextButtonStyle();
        closeButtonStyle.up = new TextureRegionDrawable(new TextureRegion(blackTexture)); // 검은색 배경
        closeButtonStyle.down = new TextureRegionDrawable(new TextureRegion(blackTexture)); // 검은색 배경
        closeButtonStyle.font = koreanFont; // 폰트 설정
        closeButtonStyle.fontColor = Color.WHITE; // 텍스트 색상 설정

        TextButton closeButton = new TextButton("닫기", closeButtonStyle); // 검은색 버튼 생성
        investigationDialog.button(closeButton, true);

        investigationDialog.show(stage);

        // 다이얼로그의 텍스트 레이블 스타일 커스터마이징
        for (Actor actor : investigationDialog.getChildren()) {
            if (actor instanceof Label) {
                Label label = (Label) actor;
                label.setStyle(new Label.LabelStyle(koreanFont, Color.WHITE)); // 텍스트 색상 흰색
                label.setWrap(true); // 줄 바꿈 활성화
                label.setAlignment(Align.center); // 중앙 정렬
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
        return new Animation<>(frameDuration, frames);
    }

    private Animation<TextureRegion> loadIdleAnimation(String fileName) {
        Texture idleTexture = new Texture(Gdx.files.internal(fileName));
        TextureRegion idleRegion = new TextureRegion(idleTexture);
        return new Animation<>(0.1f, idleRegion);
    }

    private void createObstacleBodiesFromTiles() {
        for (MapLayer layer : map.getLayers()) {
            if (layer instanceof TiledMapTileLayer && layer.getName().equals("obstacle")) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                int layerWidth = tileLayer.getWidth();
                int layerHeight = tileLayer.getHeight();
                float tileWidth = tileLayer.getTileWidth() / PPM;
                float tileHeight = tileLayer.getTileHeight() / PPM;

                for (int x = 0; x < layerWidth; x++) {
                    for (int y = 0; y < layerHeight; y++) {
                        TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                        if (cell != null && cell.getTile() != null && cell.getTile().getId() != 0) {
                            createStaticBodyForTile(x, y, tileWidth, tileHeight);
                        }
                    }
                }
            }
        }
    }

    private void createStaticBodyForTile(int x, int y, float tileWidth, float tileHeight) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set((x + 0.5f) * tileWidth, (y + 0.5f) * tileHeight);

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

        if (!isTransitioning) {
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

            int tileWidth = map.getProperties().get("tilewidth", Integer.class);
            int tileHeight = map.getProperties().get("height", Integer.class);
            int playerTileX = (int) (playerPosition.x * PPM / tileWidth);
            int playerTileY = map.getProperties().get("height", Integer.class) - 1 - (int) (playerPosition.y * PPM / tileHeight);

            if (playerTileX == 12 && playerTileY == 19 && !isTransitioning) {
                isTransitioning = true;
                fadeOutElapsed = 0f;
                playerBody.setLinearVelocity(0, 0);
                playerBody.setActive(false);

                if (transitionSound != null) {
                    transitionSound.play();
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

        float drawWidth = currentFrame.getRegionWidth() / PPM * playerScale;
        float drawHeight = currentFrame.getRegionHeight() / PPM * playerScale;
        batch.draw(currentFrame, playerPosition.x - drawWidth / 2, playerPosition.y - drawHeight / 2, drawWidth, drawHeight);

        batch.end();

        rayHandler.setCombinedMatrix(camera);
        rayHandler.updateAndRender();

        stage.act(deltaTime);
        stage.draw();

        if (isTransitioning) {
            fadeOutElapsed += deltaTime;
            float alpha = Math.min(fadeOutElapsed / fadeOutTime, 1f);

            Gdx.gl.glEnable(GL20.GL_BLEND);
            batch.begin();
            batch.setColor(1, 1, 1, alpha);
            batch.draw(blackTexture, camera.position.x - viewport.getWorldWidth() / 2, camera.position.y - viewport.getWorldHeight() / 2, viewport.getWorldWidth(), viewport.getWorldHeight());
            batch.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            batch.setColor(1, 1, 1, 1);

            if (fadeOutElapsed >= fadeOutTime) {
                if (backgroundMusic != null) {
                    backgroundMusic.stop();
                    backgroundMusic.dispose();
                }
                if (transitionSound != null) {
                    transitionSound.dispose();
                }
                game.setScreen(new LockScreen(game));
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
        if (joystick.baseTexture != null) joystick.baseTexture.dispose();
        if (joystick.knobTexture != null) joystick.knobTexture.dispose();
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

        if (skin != null) {
            skin.dispose();
            skin = null; // 다시 dispose되지 않도록 null로 설정
        }
        if (koreanFont != null) {
            koreanFont.dispose();
            koreanFont = null; // 다시 dispose되지 않도록 null로 설정
        }

        // "조사하기" 버튼 리소스 해제
        if (investigateButtonTexture != null) {
            investigateButtonTexture.dispose();
        }

        // "도움말" 버튼 스타일에서 사용한 Textures 해제
        if (helpButtonStyle != null) {
            if (helpButtonStyle.up instanceof TextureRegionDrawable) {
                TextureRegionDrawable upDrawable = (TextureRegionDrawable) helpButtonStyle.up;
                if (upDrawable.getRegion() != null && upDrawable.getRegion().getTexture() != null) {
                    upDrawable.getRegion().getTexture().dispose();
                }
            }
            if (helpButtonStyle.down instanceof TextureRegionDrawable) {
                TextureRegionDrawable downDrawable = (TextureRegionDrawable) helpButtonStyle.down;
                if (downDrawable.getRegion() != null && downDrawable.getRegion().getTexture() != null) {
                    downDrawable.getRegion().getTexture().dispose();
                }
            }
            if (helpButtonStyle.over instanceof TextureRegionDrawable) {
                TextureRegionDrawable overDrawable = (TextureRegionDrawable) helpButtonStyle.over;
                if (overDrawable.getRegion() != null && overDrawable.getRegion().getTexture() != null) {
                    overDrawable.getRegion().getTexture().dispose();
                }
            }
        }

        // "챗봇" ImageButton에서 사용한 Textures 해제
        if (mailButtonTexture != null) {
            mailButtonTexture.dispose();
        }
        // mail_down.png 및 mail_over.png 해제 (별도로 로드한 경우)
        /*
        if (mailButtonDownTexture != null) {
            mailButtonDownTexture.dispose();
        }
        if (mailButtonOverTexture != null) {
            mailButtonOverTexture.dispose();
        }
        */
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
}

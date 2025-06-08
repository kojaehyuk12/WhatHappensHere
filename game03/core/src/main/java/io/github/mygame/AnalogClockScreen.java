package io.github.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound; // 필요한 경우 추가
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap; // Pixmap 추가
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator; // 폰트 생성기 추가
import com.badlogic.gdx.math.MathUtils; // MathUtils 추가
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label; // Label 추가
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align; // Align 추가
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

public class AnalogClockScreen implements Screen {
    private Game game;
    private Vector2 spawnPoint;

    private float centerX, centerY, clockRadius;

    private float hourHandAngle = -90; // 시침 각도 (기본 12시 방향)
    private float minuteHandAngle = -90; // 분침 각도 (기본 12시 방향)
    private boolean isDraggingHour = false; // 시침을 드래그 중인지
    private boolean isDraggingMinute = false; // 분침을 드래그 중인지

    private final Vector2 touchPosition = new Vector2();

    private SpriteBatch batch;
    private Texture backgroundTexture; // 배경 텍스처
    private Texture hourHandTexture; // 시침 이미지
    private Texture minuteHandTexture; // 분침 이미지
    private BitmapFont font; // 시계 숫자 표시를 위한 폰트
    private GlyphLayout layout; // 텍스트 크기 측정을 위한 GlyphLayout
    private Stage stage;
    private Skin skin;
    private boolean hasReceivedItem = false; // 아이템 획득 여부

    private TextButton.TextButtonStyle customButtonStyle; // 커스텀 버튼 스타일

    // **추가된 부분: 커스텀 폰트**
    private BitmapFont creepyFont;

    public AnalogClockScreen(Game game, Vector2 spawnPoint) {
        this.game = game;
        this.spawnPoint = spawnPoint;
    }

    @Override
    public void show() {
        // 화면 중앙과 시계 반지름 설정
        centerX = Gdx.graphics.getWidth() / 2f;
        centerY = Gdx.graphics.getHeight() / 2f;
        clockRadius = Math.min(centerX, centerY) * 0.8f;

        batch = new SpriteBatch();
        font = new BitmapFont(); // 기본 폰트 사용
        layout = new GlyphLayout(); // GlyphLayout 초기화

        // 배경 및 시침, 분침 텍스처 로드
        backgroundTexture = new Texture(Gdx.files.internal("Clockbackground.png"));
        hourHandTexture = new Texture(Gdx.files.internal("hour_hand.png")); // 시침 이미지
        minuteHandTexture = new Texture(Gdx.files.internal("minute_hand.png")); // 분침 이미지

        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // 커스텀 폰트 생성
        createCustomFont();

        // 커스텀 버튼 스타일 생성
        createCustomButtonStyle();

        // 커스텀 다이얼로그 스타일 생성
        createCustomDialogStyle();

        // "나가기" 버튼 생성 및 배치
        TextButton exitButton = new TextButton("나가기", customButtonStyle); // 커스텀 스타일 적용
        exitButton.setPosition(20, Gdx.graphics.getHeight() - exitButton.getHeight() - 20); // 왼쪽 상단에 위치
        stage.addActor(exitButton);

        // "나가기" 버튼에 ClickListener 추가
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // FifthScreen으로 전환
                Gdx.app.postRunnable(() -> {
                    game.setScreen(new FifthScreen(game, spawnPoint));
                });
            }
        });

        // 입력 처리기 설정
        InputAdapter inputAdapter = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                touchPosition.set(screenX, Gdx.graphics.getHeight() - screenY); // Y축 반전

                float dx = touchPosition.x - centerX;
                float dy = touchPosition.y - centerY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance > clockRadius * 0.5f && distance < clockRadius * 1.0f) {
                    // 분침 범위에 터치
                    isDraggingMinute = true;
                    isDraggingHour = false;
                } else if (distance < clockRadius * 0.5f) {
                    // 시침 범위에 터치
                    isDraggingHour = true;
                    isDraggingMinute = false;
                } else {
                    isDraggingHour = false;
                    isDraggingMinute = false;
                }
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                touchPosition.set(screenX, Gdx.graphics.getHeight() - screenY); // Y축 반전

                float dx = touchPosition.x - centerX;
                float dy = touchPosition.y - centerY;
                float angle = (float) Math.atan2(dy, dx) * MathUtils.radiansToDegrees;

                // 각도를 [0, 360) 범위로 조정
                if (angle < 0) {
                    angle += 360;
                }

                if (isDraggingHour) {
                    // 시침 각도 설정
                    hourHandAngle = angle - 90; // 시계 방향 보정을 위해 90도 뺌
                    checkCompletion();
                } else if (isDraggingMinute) {
                    // 분침 각도 설정
                    minuteHandAngle = angle - 90;
                    checkCompletion();
                }

                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                isDraggingHour = false;
                isDraggingMinute = false;
                return true;
            }
        };

        // stage와 InputAdapter 모두 입력을 처리하도록 설정
        InputMultiplexer multiplexer = new InputMultiplexer(stage, inputAdapter);
        Gdx.input.setInputProcessor(multiplexer);
    }

    private void createCustomFont() {
        if (!Gdx.files.internal("fonts/creepy_font.ttf").exists()) {
            Gdx.app.error("AnalogClockScreen", "Font file fonts/creepy_font.ttf not found!");
            // 폰트 파일이 없을 경우 기본 폰트 사용
            creepyFont = skin.getFont("default-font");
        } else {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/creepy_font.ttf")); // 커스텀 폰트 파일 경로
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 24; // 폰트 크기 조정
            parameter.color = Color.RED; // 폰트 색상 설정
            parameter.shadowOffsetX = 1;
            parameter.shadowOffsetY = 1;
            parameter.shadowColor = Color.DARK_GRAY;
            parameter.borderWidth = 1;
            parameter.borderColor = Color.BLACK;
            parameter.flip = false;
            // 모든 한글 음절을 포함하도록 수정 (필요 시)
            StringBuilder characters = new StringBuilder(FreeTypeFontGenerator.DEFAULT_CHARS + "한글을지원합니다.");
            for (char c = '\uAC00'; c <= '\uD7A3'; c++) {
                characters.append(c);
            }
            parameter.characters = characters.toString();
            creepyFont = generator.generateFont(parameter);
            generator.dispose();
        }
    }

    private void createCustomButtonStyle() {
        // 그라데이션 배경 생성 (어두운 빨간색에서 더 어두운 빨간색으로)
        TextureRegionDrawable buttonUpDrawable = createGradientDrawable(new Color(0.3f, 0f, 0f, 1f), new Color(0.5f, 0f, 0f, 1f), 50);
        TextureRegionDrawable buttonDownDrawable = createGradientDrawable(new Color(0.6f, 0f, 0f, 1f), new Color(0.6f, 0f, 0f, 1f), 50);
        TextureRegionDrawable buttonOverDrawable = createGradientDrawable(new Color(0.8f, 0f, 0f, 1f), new Color(0.8f, 0f, 0f, 1f), 50);

        // 커스텀 TextButtonStyle 생성
        customButtonStyle = new TextButton.TextButtonStyle();
        customButtonStyle.up = buttonUpDrawable;
        customButtonStyle.down = buttonDownDrawable;
        customButtonStyle.over = buttonOverDrawable;
        customButtonStyle.font = creepyFont; // 커스텀 폰트 사용
        customButtonStyle.fontColor = Color.WHITE;
    }

    private void createCustomDialogStyle() {
        // 그라데이션 배경 생성 (어두운 빨간색에서 검은색으로)
        Drawable customDialogBackground = createGradientDrawable(
            new Color(0.2f, 0f, 0f, 0.9f), // 시작 색상 (어두운 빨간색)
            new Color(0f, 0f, 0f, 0.9f),   // 끝 색상 (검은색)
            100 // 그라데이션 높이
        );

        // 커스텀 DialogStyle 생성
        Window.WindowStyle customDialogStyle = new Window.WindowStyle();
        customDialogStyle.background = customDialogBackground;
        customDialogStyle.titleFont = creepyFont;
        customDialogStyle.titleFontColor = Color.RED;

        // Skin에 커스텀 DialogStyle 추가
        skin.add("custom-dialog", customDialogStyle);
    }

    private TextureRegionDrawable createGradientDrawable(Color startColor, Color endColor, int height) {
        Pixmap pixmap = new Pixmap(1, height, Pixmap.Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            float ratio = (float) y / height;
            Color color = new Color(
                startColor.r + (endColor.r - startColor.r) * ratio,
                startColor.g + (endColor.g - startColor.g) * ratio,
                startColor.b + (endColor.b - startColor.b) * ratio,
                1f // 알파값을 1로 고정
            );
            pixmap.setColor(color);
            pixmap.drawPixel(0, y);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture)); // TextureRegionDrawable 반환
    }

    private void handleTouch(int screenX, int screenY) {
        // 터치 이벤트는 touchDown, touchDragged에서 처리하므로 필요 없음
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1); // 흰색 배경
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        // 배경 그리기
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // 시침 그리기 (길이 스케일 50%)
        drawHand(hourHandTexture, hourHandAngle, 0.3f);

        // 분침 그리기 (길이 스케일 80%)
        drawHand(minuteHandTexture, minuteHandAngle, 0.5f);

        batch.end();

        // Stage 업데이트 및 그리기 (다이얼로그 포함)
        stage.act(delta);
        stage.draw();
    }

    private void drawHand(Texture handTexture, float angle, float lengthScale) {
        float handWidth = handTexture.getWidth();
        float handHeight = handTexture.getHeight() * lengthScale; // 길이 스케일 적용
        float originX = handWidth / 2f;
        float originY = 0; // 기준점을 밑부분으로 설정
        float x = centerX - originX;
        float y = centerY - originY;

        batch.draw(
            handTexture,
            x, y, // 위치
            originX, originY, // 회전 기준점
            handWidth, handHeight, // 크기
            1, 1, // 스케일
            angle, // 회전 각도
            0, 0,
            handTexture.getWidth(), handTexture.getHeight(),
            false, false
        );
    }

    private void checkCompletion() {
        if (hasReceivedItem) return; // 이미 아이템을 받았다면 중복 방지

        // 시침과 분침 각도를 통해 시간 계산
        float adjustedHourAngle = (hourHandAngle + 90) % 360;
        float adjustedMinuteAngle = (minuteHandAngle + 90) % 360;

        // 정답 시간 설정 (예: 4시 0분)
        boolean isHourCorrect = Math.abs(adjustedHourAngle - 120) < 5; // 4시: 120도 ±5도
        boolean isMinuteCorrect = Math.abs(adjustedMinuteAngle - 180) < 5; // 6시: 180도 ±5도

        if (isHourCorrect && isMinuteCorrect) {
            hasReceivedItem = true;
            Inventory.getInstance().addItem("깃펜");
            showSuccessMessage();
        }
    }

    private void showSuccessMessage() {
        if (hasReceivedItem) {
            Dialog successDialog = new Dialog("퍼즐 완료", skin, "custom-dialog") { // "custom-dialog" 스타일 사용
                {
                    // 커스텀 레이블 스타일 생성
                    Label.LabelStyle labelStyle = new Label.LabelStyle(creepyFont, Color.WHITE);
                    Label dialogLabel = new Label("시계 뒤에서 깃펜이 떨어졌다.", labelStyle);
                    dialogLabel.setWrap(true);
                    dialogLabel.setAlignment(Align.center);

                    getContentTable().add(dialogLabel).width(Gdx.graphics.getWidth() * 0.4f).pad(20);

                    // 커스텀 버튼 스타일 적용
                    TextButton confirmButton = new TextButton("확인", customButtonStyle);
                    button(confirmButton, true);
                }

                @Override
                protected void result(Object object) {
                    Gdx.app.postRunnable(() -> {
                        game.setScreen(new FifthScreen(game, spawnPoint));
                    });
                }
            };
            successDialog.show(stage);
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        // 필요 시 구현
    }

    @Override
    public void resume() {
        // 필요 시 구현
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        if (creepyFont != null) {
            creepyFont.dispose(); // 커스텀 폰트 해제
        }
        backgroundTexture.dispose();
        hourHandTexture.dispose();
        minuteHandTexture.dispose();
        stage.dispose();
        skin.dispose();
    }
}

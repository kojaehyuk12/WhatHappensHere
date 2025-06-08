package io.github.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class LockScreen implements Screen {
    private Game game;
    private Stage stage;
    private SpriteBatch batch; // 별도의 SpriteBatch 선언
    private Texture blackTexture;
    private float fadeElapsed = 0f; // 페이드 효과의 시간을 추적하는 변수
    private float fadeDuration = 1f; // 페이드 시간 (1초)

    // Sound 객체 선언
    private Sound transitionSound;

    // 화면 전환 상태 및 타이머
    private boolean isTransitioning = false;
    private float transitionTimer = 0f;
    private float transitionDelay = 1f; // 소리 재생 시간 (초 단위)

    // 그라데이션 배경을 저장하기 위한 Drawable
    private Drawable buttonUpDrawable;
    private Drawable buttonDownDrawable;
    private Drawable buttonOverDrawable;

    private Drawable passwordFieldBackground;

    // 커스텀 폰트
    private BitmapFont horrorFont;

    public LockScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        // Stage 객체 초기화
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // 별도의 SpriteBatch 초기화
        batch = new SpriteBatch();

        // 검은색 텍스처 생성 (페이드 효과용)
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 1); // 검은색
        pixmap.fill();
        blackTexture = new Texture(pixmap);
        pixmap.dispose();

        // UI 스킨 로드
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        // FreeTypeFontGenerator를 사용하여 공포 컨셉의 폰트 생성 (필요 시)
        // 폰트 파일이 assets/fonts/creepy_font.ttf 에 존재하는지 확인하세요.
        if (!Gdx.files.internal("fonts/creepy_font.ttf").exists()) {
            Gdx.app.error("LockScreen", "Font file fonts/creepy_font.ttf not found!");
            // 폰트 파일이 없을 경우 기본 폰트 사용 또는 예외 처리
            horrorFont = skin.getFont("default-font");
        } else {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/creepy_font.ttf")); // 공포 컨셉의 폰트 파일 경로
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 28; // 원하는 폰트 크기 (조금 더 크게)
            parameter.color = Color.RED; // 공포스러운 색상
            parameter.shadowOffsetX = 2;
            parameter.shadowOffsetY = 2;
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
            horrorFont = generator.generateFont(parameter);
            generator.dispose();
        }

        // 그라데이션 배경 생성 메서드 사용
        buttonUpDrawable = createGradientDrawable(
            new Color(0.3f, 0f, 0f, 1f), // 시작 색상 (어두운 빨간색)
            new Color(0.5f, 0f, 0f, 1f), // 끝 색상 (더 어두운 빨간색)
            50 // 그라데이션 높이
        );

        buttonDownDrawable = createGradientDrawable(
            new Color(0.6f, 0f, 0f, 1f), // 클릭 시 색상 변경
            new Color(0.6f, 0f, 0f, 1f),
            50
        );

        buttonOverDrawable = createGradientDrawable(
            new Color(0.8f, 0f, 0f, 1f), // 마우스 올렸을 때 색상 변경
            new Color(0.8f, 0f, 0f, 1f),
            50
        );

        passwordFieldBackground = createGradientDrawable(
            new Color(0.0f, 0f, 0.0f, 0.8f), // 시작 색상 (투명한 검은색)
            new Color(0.2f, 0f, 0.0f, 0.8f), // 끝 색상 (더 진한 검은색)
            100 // 그라데이션 높이
        );

        // 커스텀 TextButtonStyle 생성 (확인 버튼과 취소 버튼 모두 동일한 스타일 사용)
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonUpDrawable;
        buttonStyle.down = buttonDownDrawable;
        buttonStyle.over = buttonOverDrawable;
        buttonStyle.font = horrorFont;
        buttonStyle.fontColor = Color.WHITE;

        // 커스텀 TextFieldStyle 생성
        TextField.TextFieldStyle passwordFieldStyle = new TextField.TextFieldStyle();
        passwordFieldStyle.font = horrorFont;
        passwordFieldStyle.fontColor = Color.WHITE;
        passwordFieldStyle.background = passwordFieldBackground;
        passwordFieldStyle.cursor = skin.newDrawable("white", Color.RED); // 커서 색상
        passwordFieldStyle.selection = skin.newDrawable("white", Color.DARK_GRAY); // 선택 색상

        // 비밀번호 입력 필드 생성 및 스타일 적용
        TextField passwordField = new TextField("", passwordFieldStyle);
        passwordField.setMessageText("비밀번호 입력");
        passwordField.setAlignment(Align.center);
        passwordField.setPasswordMode(true); // 비밀번호 입력 모드 활성화
        passwordField.setPasswordCharacter('*'); // 비밀번호 표시 문자

        // 확인 버튼 생성 및 스타일 적용
        TextButton confirmButton = new TextButton("확인", buttonStyle);
        confirmButton.setSize(100, 50);

        // 취소 버튼 생성 및 스타일 적용
        TextButton cancelButton = new TextButton("취소", buttonStyle);
        cancelButton.setSize(100, 50);

        // transition_sound.mp3 로드
        if (!Gdx.files.internal("transition_sound.mp3").exists()) {
            Gdx.app.error("LockScreen", "Sound file transition_sound.mp3 not found!");
            // 소리 파일이 없을 경우 예외 처리 또는 대체 조치
        } else {
            transitionSound = Gdx.audio.newSound(Gdx.files.internal("transition_sound.mp3"));
        }

        // 비밀번호가 맞을 때의 소리 재생 시간 설정 (초 단위)
        transitionDelay = 1f; // 예시: 1초

        // 버튼 클릭 이벤트 처리
        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // 비밀번호가 맞으면 4번째 화면으로 넘어감
                if (passwordField.getText().equals("1427")) {
                    // 비밀번호가 맞을 때 소리 재생
                    if (transitionSound != null) {
                        transitionSound.play();
                    }

                    // 화면 전환 상태로 설정하고 타이머 초기화
                    isTransitioning = true;
                    transitionTimer = 0f;
                } else {
                    // 비밀번호가 틀리면 메시지 출력
                    Dialog wrongPasswordDialog = new Dialog("비밀번호 오류", skin) {
                        public void result(Object obj) {
                            this.hide();
                        }
                    };
                    wrongPasswordDialog.text("비밀번호가 틀립니다. 다시 시도해보세요.");
                    wrongPasswordDialog.button("확인", true);
                    wrongPasswordDialog.show(stage);

                    // 다이얼로그의 텍스트 레이블 스타일 커스터마이징
                    for (Actor actor : wrongPasswordDialog.getChildren()) {
                        if (actor instanceof Label) {
                            Label label = (Label) actor;
                            label.setStyle(new Label.LabelStyle(horrorFont, Color.RED)); // 텍스트 색상 빨간색
                        }
                    }
                }
            }
        });

        // 취소 버튼 클릭 이벤트 처리
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // 취소 버튼 클릭 시, 해당 화면을 닫고 이전 화면으로 돌아감
                game.setScreen(new ThirdScreen(game)); // 이전 화면(3번째 화면)으로 돌아가기
            }
        });

        // Table 레이아웃을 사용하여 UI 요소들을 중앙에 배치하고 간격 조정
        Table table = new Table();
        table.setFillParent(true);
        table.center();
        table.pad(20); // 전체 테이블 패딩 설정

        // 비밀번호 입력 필드 추가
        table.add(passwordField).width(400).height(60).padBottom(30); // 패스워드 필드 크기 조정 및 하단 패딩 추가
        table.row();

        // 버튼들을 가로로 추가하고 간격을 일정하게 설정
        table.add(confirmButton).width(120).height(60).padBottom(20); // 확인 버튼 크기 조정 및 하단 패딩 추가
        table.row();
        table.add(cancelButton).width(120).height(60); // 취소 버튼 크기 조정

        // Table을 Stage에 추가
        stage.addActor(table);
    }

    /**
     * 그라데이션을 생성하는 메서드
     *
     * @param startColor 그라데이션 시작 색상 (위쪽)
     * @param endColor 그라데이션 끝 색상 (아래쪽)
     * @param height 그라데이션 이미지의 높이
     * @return 생성된 Drawable
     */
    private Drawable createGradientDrawable(Color startColor, Color endColor, int height) {
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

    @Override
    public void render(float delta) {
        // 화면을 깨끗하게 지우고 배경을 검은색으로 덮기
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 페이드 효과 시간 업데이트
        fadeElapsed += delta;
        float alpha = Math.min(fadeElapsed / fadeDuration, 1f); // 0에서 1까지 증가

        // 별도의 SpriteBatch를 사용하여 페이드 효과 적용
        batch.begin();
        batch.setColor(0, 0, 0, alpha); // 검은색과 알파값 설정
        batch.draw(blackTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // Stage 업데이트 및 렌더링
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();

        // 소리 재생 후 화면 전환 처리
        if (isTransitioning) {
            transitionTimer += delta;
            if (transitionTimer >= transitionDelay) {
                // FourthScreen 클래스가 프로젝트에 존재하는지 확인하세요.
                game.setScreen(new FourthScreen(game));
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        // 화면 크기 조정
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void pause() {
        // 게임이 멈출 때 필요한 코드
    }

    @Override
    public void resume() {
        // 게임이 다시 시작될 때 필요한 코드
    }

    @Override
    public void dispose() {
        // 자원 해제
        stage.dispose();
        blackTexture.dispose();
        batch.dispose(); // SpriteBatch 해제

        // Sound 객체 해제
        if (transitionSound != null) {
            transitionSound.dispose();
        }

        // 폰트 해제
        if (horrorFont != null) {
            horrorFont.dispose();
        }

        // 그라데이션 배경 텍스처 해제
        if (buttonUpDrawable instanceof TextureRegionDrawable) {
            ((TextureRegionDrawable) buttonUpDrawable).getRegion().getTexture().dispose();
        }
        if (buttonDownDrawable instanceof TextureRegionDrawable) {
            ((TextureRegionDrawable) buttonDownDrawable).getRegion().getTexture().dispose();
        }
        if (buttonOverDrawable instanceof TextureRegionDrawable) {
            ((TextureRegionDrawable) buttonOverDrawable).getRegion().getTexture().dispose();
        }

        if (passwordFieldBackground instanceof TextureRegionDrawable) {
            ((TextureRegionDrawable) passwordFieldBackground).getRegion().getTexture().dispose();
        }
    }
}

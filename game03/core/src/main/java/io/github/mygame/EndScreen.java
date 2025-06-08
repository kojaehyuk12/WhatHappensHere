package io.github.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

public class EndScreen implements Screen {
    private Game game;
    private SpriteBatch batch;
    private BitmapFont font;
    private Texture backgroundTexture;
    private Stage stage;
    private Skin skin;
    private Music endingMusic;

    // 전환 제어 변수
    private float animationTime = 0f;
    private float transitionTime = 2f; // 전환에 걸리는 시간 (초)
    private float firstTextDuration = 7f; // 첫 번째 텍스트가 표시되는 시간 (초)
    private float currentTransitionTime = 0f;
    private float fadeAlpha = 0f; // 두 번째 텍스트 전환 시 페이드 알파
    private boolean isTransitioning = false;
    private boolean isSecondTextDisplayed = false;

    // 초기 페이드 인 관련 변수 추가
    private float fadeInAlpha = 1f; // 시작 시 완전히 검정 화면
    private boolean fadeIn = true;
    private float fadeInDuration = 1f; // 초기 페이드 인 지속 시간

    // 화면 크기
    private int screenWidth;
    private int screenHeight;

    // 커스텀 폰트 (선택 사항)
    private BitmapFont horrorFont;

    // 그라데이션 버튼 스타일 변수
    private TextButton.TextButtonStyle gradientButtonStyle;
    private Drawable buttonUpDrawable;
    private Drawable buttonDownDrawable;
    private Drawable buttonOverDrawable;

    // 단색 텍스처 (재사용)
    private Texture blackTexture;

    // 라벨 변수
    private Label firstDialogueLabel;
    private Label secondDialogueLabel;

    // 상태 관리
    private enum State {
        DISPLAY_FIRST_TEXT,
        TRANSITION_TO_BLACK,
        DISPLAY_SECOND_TEXT
    }

    private State currentState = State.DISPLAY_FIRST_TEXT;

    public EndScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        Gdx.app.log("EndScreen", "show() called");

        batch = new SpriteBatch();
        loadCustomFont();

        font = (horrorFont != null) ? horrorFont : new BitmapFont();
        font.getData().setScale(1.5f);

        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        try {
            backgroundTexture = new Texture(Gdx.files.internal("SPrit_converted.png"));
        } catch (GdxRuntimeException e) {
            Gdx.app.error("EndScreen", "SPrit_converted.png 파일을 로드할 수 없습니다.", e);
            backgroundTexture = null;
        }

        try {
            endingMusic = Gdx.audio.newMusic(Gdx.files.internal("ending_music.mp3"));
            endingMusic.setLooping(false);
            endingMusic.play();
        } catch (GdxRuntimeException e) {
            Gdx.app.error("EndScreen", "ending_music.mp3 파일을 로드할 수 없습니다.", e);
            endingMusic = null;
        }

        createGradientButtonStyle();
        blackTexture = createColoredTexture(Color.BLACK);
        if (blackTexture == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.BLACK);
            pixmap.fill();
            blackTexture = new Texture(pixmap);
            pixmap.dispose();
        }

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        stage.addActor(table);

        firstDialogueLabel = new Label(
            "정신을 차려보니 저택에서 나와 있었다.\n\n" +
                "몸은 여전히 무거웠지만, 더 이상 차가운 기운은 느껴지지 않았다.\n\n" +
                "저택의 어둠과 저주는 끝이 났다.\n\n" +
                "이제, 모든 것이 평화로워진 것 같았다.",
            skin
        );
        firstDialogueLabel.setFontScale(1.5f);
        firstDialogueLabel.setColor(Color.WHITE);
        firstDialogueLabel.setAlignment(Align.center);
        firstDialogueLabel.setWrap(true);
        table.add(firstDialogueLabel)
            .width(screenWidth * 0.8f)
            .padTop(screenHeight * 0f)
            .align(Align.center)
            .row();

        secondDialogueLabel = new Label(
            "하지만 모든 것이 끝난 걸까?\n\n" +
                "저택은 여전히 그 자리에 서 있었고,\n" +
                "희미한 바람소리 속에서 누군가의 목소리가 들리는 듯했다.\n\n" +
                "**은 카메라를 꼭 쥐고 조용히 뒤돌아섰다.\n\n" +
                "이제 다시는 돌아오고 싶지 않은 곳이었다.",
            skin
        );
        secondDialogueLabel.setFontScale(1.5f);
        secondDialogueLabel.setColor(Color.WHITE);
        secondDialogueLabel.setAlignment(Align.center);
        secondDialogueLabel.setWrap(true);
        secondDialogueLabel.setWidth(screenWidth * 0.8f);
        secondDialogueLabel.setVisible(false);

        currentState = State.DISPLAY_FIRST_TEXT;
        animationTime = 0f;
        currentTransitionTime = 0f;
        isTransitioning = false;
        isSecondTextDisplayed = false;

        // 초기 페이드 인 설정
        fadeInAlpha = 1f;
        fadeIn = true;
    }

    private void loadCustomFont() {
        if (!Gdx.files.internal("fonts/horror_font.ttf").exists()) {
            Gdx.app.error("EndScreen", "Font file fonts/horror_font.ttf not found!");
            horrorFont = null;
            return;
        }

        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/horror_font.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 28;
            parameter.color = Color.WHITE;
            parameter.shadowOffsetX = 2;
            parameter.shadowOffsetY = 2;
            parameter.shadowColor = Color.DARK_GRAY;
            parameter.borderWidth = 1;
            parameter.borderColor = Color.BLACK;
            parameter.flip = false;
            StringBuilder characters = new StringBuilder(FreeTypeFontGenerator.DEFAULT_CHARS + "한글을지원합니다.");
            for (char c = '\uAC00'; c <= '\uD7A3'; c++) {
                characters.append(c);
            }
            parameter.characters = characters.toString();
            horrorFont = generator.generateFont(parameter);
            generator.dispose();
        } catch (Exception e) {
            Gdx.app.error("EndScreen", "폰트 생성 중 오류 발생", e);
            horrorFont = null;
        }
    }

    private void createGradientButtonStyle() {
        buttonUpDrawable = createGradientDrawable(
            new Color(0.3f, 0f, 0f, 1f),
            new Color(0.5f, 0f, 0f, 1f),
            50
        );

        buttonDownDrawable = createGradientDrawable(
            new Color(0.6f, 0f, 0f, 1f),
            new Color(0.6f, 0f, 0f, 1f),
            50
        );

        buttonOverDrawable = createGradientDrawable(
            new Color(0.8f, 0f, 0f, 1f),
            new Color(0.8f, 0f, 0f, 1f),
            50
        );

        gradientButtonStyle = new TextButton.TextButtonStyle();
        gradientButtonStyle.up = buttonUpDrawable;
        gradientButtonStyle.down = buttonDownDrawable;
        gradientButtonStyle.over = buttonOverDrawable;
        gradientButtonStyle.font = (horrorFont != null) ? horrorFont : skin.getFont("default-font");
        gradientButtonStyle.fontColor = Color.WHITE;
    }

    private Drawable createGradientDrawable(Color startColor, Color endColor, int height) {
        Pixmap pixmap = new Pixmap(1, height, Pixmap.Format.RGBA8888);
        try {
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
            return new TextureRegionDrawable(new TextureRegion(texture));
        } catch (Exception e) {
            Gdx.app.error("EndScreen", "그라데이션 Drawable 생성 중 오류 발생", e);
            return null;
        } finally {
            pixmap.dispose();
        }
    }

    private Texture createColoredTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try {
            pixmap.setColor(color);
            pixmap.fill();
            Texture texture = new Texture(pixmap);
            return texture;
        } catch (Exception e) {
            Gdx.app.error("EndScreen", "단색 텍스처 생성 중 오류 발생", e);
            return null;
        } finally {
            pixmap.dispose();
        }
    }

    @Override
    public void render(float delta) {
        animationTime += delta;

        // 초기 페이드 인 처리
        if (fadeIn) {
            float progress = animationTime / fadeInDuration;
            if (progress > 1f) {
                progress = 1f;
                fadeIn = false;
            }
            // fadeInAlpha: 1에서 시작해서 0으로 감소
            fadeInAlpha = 1f - progress;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        if (currentState != State.DISPLAY_SECOND_TEXT) {
            if (backgroundTexture != null) {
                batch.draw(backgroundTexture, 0, 0, screenWidth, screenHeight);
            }
        } else {
            // 두 번째 텍스트 상태에서는 배경 대신 검은 화면
            batch.draw(blackTexture, 0, 0, screenWidth, screenHeight);
        }
        batch.end();

        stage.act(delta);
        stage.draw();

        switch (currentState) {
            case DISPLAY_FIRST_TEXT:
                if (animationTime > firstTextDuration) {
                    currentState = State.TRANSITION_TO_BLACK;
                    animationTime = 0f;
                    currentTransitionTime = 0f;
                    Gdx.app.log("EndScreen", "Transition to black started.");
                }
                break;

            case TRANSITION_TO_BLACK:
                currentTransitionTime += delta;
                fadeAlpha = currentTransitionTime / transitionTime;
                if (fadeAlpha > 1f) fadeAlpha = 1f;

                // 페이드 아웃 (투명 -> 검정)
                batch.begin();
                batch.setColor(0, 0, 0, fadeAlpha);
                batch.draw(blackTexture, 0, 0, screenWidth, screenHeight);
                batch.setColor(Color.WHITE);
                batch.end();

                if (currentTransitionTime >= transitionTime) {
                    currentState = State.DISPLAY_SECOND_TEXT;
                    showSecondText();
                    Gdx.app.log("EndScreen", "Transition to black completed.");
                }
                break;

            case DISPLAY_SECOND_TEXT:
                // 두 번째 텍스트 상태에서는 별도 처리 없음
                break;
        }

        // 페이드 인 오버레이 (초기 페이드 인)
        // fadeInAlpha가 0보다 크면 여전히 초기 페이드 인 중
        if (fadeInAlpha > 0) {
            batch.begin();
            batch.setColor(0,0,0, fadeInAlpha);
            batch.draw(blackTexture, 0,0, screenWidth, screenHeight);
            batch.setColor(Color.WHITE); // 상태 복원
            batch.end();
        }
    }

    private void showSecondText() {
        stage.clear();

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        stage.addActor(table);

        secondDialogueLabel.setVisible(true);
        table.add(secondDialogueLabel).width(screenWidth * 0.8f).padBottom(40).row();

        TextButton exitButton = new TextButton("게임 종료", gradientButtonStyle);
        exitButton.setSize(200, 80);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        table.add(exitButton).size(200, 80).padBottom(20).row();

        TextButton mainMenuButton = new TextButton("메인 메뉴", gradientButtonStyle);
        mainMenuButton.setSize(200, 80);
        mainMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new FirstScreen(game));
                dispose();
            }
        });
        table.add(mainMenuButton).size(200, 80).padBottom(20).row();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        screenWidth = width;
        screenHeight = height;
    }

    @Override
    public void pause() {
        if (endingMusic != null) {
            endingMusic.pause();
        }
    }

    @Override
    public void resume() {
        if (endingMusic != null) {
            endingMusic.play();
        }
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        Gdx.app.log("EndScreen", "dispose() called");

        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (horrorFont != null) {
            horrorFont.dispose();
            horrorFont = null;
        }
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
        if (blackTexture != null) {
            blackTexture.dispose();
            blackTexture = null;
        }
        if (skin != null) {
            skin.dispose();
            skin = null;
        }
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        if (endingMusic != null) {
            endingMusic.dispose();
            endingMusic = null;
        }
        if (buttonUpDrawable instanceof TextureRegionDrawable) {
            TextureRegionDrawable drawable = (TextureRegionDrawable) buttonUpDrawable;
            if (drawable.getRegion().getTexture() != null) {
                drawable.getRegion().getTexture().dispose();
            }
        }
        if (buttonDownDrawable instanceof TextureRegionDrawable) {
            TextureRegionDrawable drawable = (TextureRegionDrawable) buttonDownDrawable;
            if (drawable.getRegion().getTexture() != null) {
                drawable.getRegion().getTexture().dispose();
            }
        }
        if (buttonOverDrawable instanceof TextureRegionDrawable) {
            TextureRegionDrawable drawable = (TextureRegionDrawable) buttonOverDrawable;
            if (drawable.getRegion().getTexture() != null) {
                drawable.getRegion().getTexture().dispose();
            }
        }
    }
}

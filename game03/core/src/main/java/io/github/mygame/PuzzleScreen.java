package io.github.mygame;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class PuzzleScreen implements Screen {
    private Game game;
    private Stage stage;
    private BitmapFont font;
    private String targetCombination;
    private String correctAnswer;
    private float totalTimeRemaining = 20f; // 총 20초 타이머
    private boolean puzzleFailed = false;
    private int puzzleLevel = 1; // 현재 퍼즐 레벨

    private TextField inputField;
    private SpriteBatch batch;
    private Texture backgroundTexture; // 배경 이미지 텍스처
    private Texture textFieldBackgroundTexture; // TextField 배경 텍스처
    private Texture cursorTexture; // TextField 커서 텍스처
    private Texture selectionTexture; // TextField 선택 텍스처
    private Music bgm; // BGM

    // 페이드 관련 변수
    private Texture blackTexture;
    private float fadeElapsed = 0f;
    private float fadeDuration = 1f;
    private boolean fadeIn = true;   // 초기 진입 시 페이드 인
    private boolean fadeOut = false; // 다른 화면 전환 시 페이드 아웃
    private Screen nextScreen = null;

    public PuzzleScreen(Game game) {
        this.game = game;
        setNextPuzzle();
    }

    private void setNextPuzzle() {
        switch (puzzleLevel) {
            case 1:
                int num1 = MathUtils.random(1, 9);
                int num2 = MathUtils.random(1, 9);
                targetCombination = num1 + " + " + num2 + " = ?";
                correctAnswer = Integer.toString(num1 + num2);
                break;
            case 2:
                int num3 = MathUtils.random(10, 99);
                int num4 = MathUtils.random(10, 99);
                targetCombination = num3 + " + " + num4 + " = ?";
                correctAnswer = Integer.toString(num3 + num4);
                break;
            case 3:
                targetCombination = "x³ + 4x - 17.2 = 0";
                correctAnswer = ""; // 실제 정답 없음 (예시)
                break;
            default:
                targetCombination = "";
                correctAnswer = "";
                break;
        }
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        batch = new SpriteBatch();
        backgroundTexture = new Texture(Gdx.files.internal("horror_background.png")); // 배경 이미지

        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
        font = new BitmapFont(Gdx.files.internal("sdss.fnt")); // 폰트 로드

        // 검은색 페이드용 텍스처
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        blackTexture = new Texture(pixmap);
        pixmap.dispose();

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);

        Label puzzleLabel = new Label("Solve: " + targetCombination, labelStyle);
        puzzleLabel.setAlignment(Align.center);
        puzzleLabel.setSize(Gdx.graphics.getWidth(), 100);
        puzzleLabel.setPosition(0, Gdx.graphics.getHeight() / 2 + 100);

        // TextField 스타일 설정
        TextField.TextFieldStyle passwordStyle = new TextField.TextFieldStyle();
        passwordStyle.font = font;
        passwordStyle.fontColor = Color.WHITE;

        // TextField 배경
        Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(new Color(0, 0, 0, 0.5f));
        bgPixmap.fill();
        textFieldBackgroundTexture = new Texture(bgPixmap);
        bgPixmap.dispose();
        passwordStyle.background = new TextureRegionDrawable(new TextureRegion(textFieldBackgroundTexture));

        // 커서
        Pixmap cursorPixmap = new Pixmap(2, 20, Pixmap.Format.RGBA8888);
        cursorPixmap.setColor(Color.WHITE);
        cursorPixmap.fill();
        cursorTexture = new Texture(cursorPixmap);
        cursorPixmap.dispose();
        passwordStyle.cursor = new TextureRegionDrawable(new TextureRegion(cursorTexture));

        // 셀렉션
        Pixmap selectionPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        selectionPixmap.setColor(new Color(1, 1, 1, 0.3f));
        selectionPixmap.fill();
        selectionTexture = new Texture(selectionPixmap);
        selectionPixmap.dispose();
        passwordStyle.selection = new TextureRegionDrawable(new TextureRegion(selectionTexture));

        inputField = new TextField("", passwordStyle);
        inputField.setMessageText("Enter your answer");
        inputField.setSize(500, 100);
        inputField.setPosition(Gdx.graphics.getWidth() / 2 - 250, Gdx.graphics.getHeight() / 2 - 50);
        inputField.setAlignment(Align.center);
        inputField.setPasswordCharacter('*');
        inputField.setPasswordMode(true);
        inputField.setTextFieldListener((textField, c) -> {
            if (c == '\n' || c == '\r') {
                String playerInput = textField.getText();
                checkAnswer(playerInput);
                textField.setText("");
            }
        });

        stage.addActor(puzzleLabel);
        stage.addActor(inputField);

        // BGM 로드 및 재생
        bgm = Gdx.audio.newMusic(Gdx.files.internal("compuzzle.mp3"));
        bgm.setLooping(true);
        bgm.play();
    }

    @Override
    public void render(float delta) {
        if (!puzzleFailed && puzzleLevel <= 3 && !fadeOut) {
            totalTimeRemaining -= delta;
            if (totalTimeRemaining <= 0) {
                onPuzzleFail();
            }
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 페이드 인/아웃 alpha 계산
        fadeElapsed += delta;
        float alpha;
        if (fadeIn && !fadeOut) {
            alpha = 1f - Math.min(fadeElapsed / fadeDuration, 1f); // 페이드 인: 1→0
        } else if (fadeOut) {
            alpha = Math.min(fadeElapsed / fadeDuration, 1f);      // 페이드 아웃: 0→1
        } else {
            alpha = 0f;
        }

        // 배경 그리기
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        stage.act(delta);
        stage.draw();

        // 페이드 오버레이(검은색)
        batch.begin();
        batch.setColor(0, 0, 0, alpha);
        batch.draw(blackTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // 페이드 아웃 완료 시 다음 프레임에 화면 전환
        if (fadeOut && alpha >= 1f && nextScreen != null) {
            // 다음 프레임에 화면 전환을 위한 Runnable 등록
            Gdx.app.postRunnable(() -> {
                game.setScreen(nextScreen);
            });
        }

        // 시간, 퍼즐 레벨 표시
        batch.begin();
        batch.setColor(1,1,1,1);
        font.draw(batch, "Puzzle Level: " + puzzleLevel, 50, Gdx.graphics.getHeight() - 50);
        font.draw(batch, "Time Left: " + (int) totalTimeRemaining + " seconds", Gdx.graphics.getWidth() - 400, Gdx.graphics.getHeight() - 50);
        batch.end();

        if (puzzleFailed && !fadeOut) {
            startFadeOut(new SurvivalScreen(game));
        }
    }

    private void checkAnswer(String playerInput) {
        if (puzzleLevel < 3 && playerInput.equals(correctAnswer)) {
            onPuzzleSuccess();
        } else if (puzzleLevel < 3 && !playerInput.equals(correctAnswer) && playerInput.length() > 0) {
            onPuzzleFail();
        } else if (puzzleLevel == 3) {
            // 3단계는 정답 없음. 임의로 성공 처리
            onPuzzleSuccess();
        }
    }

    private void onPuzzleFail() {
        puzzleFailed = true;
        System.out.println("Puzzle Failed! Transitioning to Survival Screen.");
    }

    private void onPuzzleSuccess() {
        System.out.println("Puzzle Solved! Proceeding to next level.");
        puzzleLevel++;
        if (puzzleLevel <= 3) {
            setNextPuzzle();
            updatePuzzle();
            totalTimeRemaining = 20f;
        } else {
            // 모든 퍼즐 완료 후 EndScreen으로 전환
            startFadeOut(new EndScreen(game));
        }
    }

    private void updatePuzzle() {
        for (Actor actor : stage.getActors()) {
            if (actor instanceof Label) {
                ((Label) actor).setText("Solve: " + targetCombination);
            }
        }
    }

    private void startFadeOut(Screen targetScreen) {
        fadeOut = true;
        fadeIn = false;
        fadeElapsed = 0f;
        nextScreen = targetScreen;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}
    @Override
    public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        batch.dispose();
        backgroundTexture.dispose();
        textFieldBackgroundTexture.dispose();
        cursorTexture.dispose();
        selectionTexture.dispose();
        if (bgm != null) {
            bgm.stop();
            bgm.dispose();
        }
        if (blackTexture != null) {
            blackTexture.dispose();
        }
        stage.dispose();
        font.dispose();
    }
}

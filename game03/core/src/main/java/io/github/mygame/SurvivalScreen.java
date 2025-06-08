package io.github.mygame;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SurvivalScreen implements Screen {
    private final Game game;
    private SpriteBatch batch;
    private BitmapFont font;

    private Vector2 playerPosition;
    private Vector2 virusPosition;
    private float survivalTime = 5f;
    private float virusSpeed = 100f;

    private Texture backgroundTexture;
    private Texture virusTexture;
    private Texture ghostTexture;

    private Music backgroundMusic;
    private List<Vector2> ghosts;
    private float ghostSpawnTimer = 0f;
    private float ghostSpawnInterval = 1f;

    private int screenWidth;
    private int screenHeight;
    private Stage stage;
    private boolean isGameOver = false;

    private Joystick joystick;

    private Animation<TextureRegion> walkLeftAnimation;
    private Animation<TextureRegion> walkRightAnimation;
    private Animation<TextureRegion> walkDownAnimation;
    private Animation<TextureRegion> walkUpAnimation;
    private Animation<TextureRegion> idleAnimation;
    private TextureRegion currentFrame;
    private float stateTime;
    private float playerSpeed = 200f;

    private enum Direction {
        UP, DOWN, LEFT, RIGHT, IDLE
    }

    private Direction currentDirection = Direction.IDLE;

    // 페이드 관련 변수
    private Texture blackTexture;
    private float fadeElapsed = 0f;
    private float fadeDuration = 2f;
    private boolean fadeIn = true;   // 처음 진입 시 검정에서 서서히 밝아지기
    private boolean fadeOut = false; // 화면 전환 시 서서히 어두워지기
    private Screen nextScreen = null;

    public SurvivalScreen(Game game) {
        this.game = game;
        ghosts = new ArrayList<>();
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont(Gdx.files.internal("sdss.fnt"));

        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        backgroundTexture = new Texture(Gdx.files.internal("survivalbackground.png"));
        virusTexture = new Texture(Gdx.files.internal("virus.png"));
        ghostTexture = new Texture(Gdx.files.internal("virus.png"));

        walkLeftAnimation = createAnimation(new Texture(Gdx.files.internal("player_walk_left.png")), 4, 1, 0.2f);
        walkRightAnimation = createAnimation(new Texture(Gdx.files.internal("player_walk_right.png")), 4, 1, 0.2f);
        walkDownAnimation = createAnimation(new Texture(Gdx.files.internal("player_walk_down.png")), 4, 1, 0.2f);
        walkUpAnimation = createAnimation(new Texture(Gdx.files.internal("player_walk_up.png")), 4, 1, 0.2f);
        idleAnimation = createAnimation(new Texture(Gdx.files.internal("player_idle.png")), 1, 1, 0.2f);

        playerPosition = new Vector2(screenWidth / 2f, screenHeight / 2f);
        virusPosition = new Vector2(MathUtils.random(0, screenWidth), MathUtils.random(0, screenHeight));

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("survival.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.play();

        Texture joystickBase = new Texture("joystick_base.png");
        Texture joystickKnob = new Texture("joystick_knob.png");
        joystick = new Joystick(joystickBase, joystickKnob, 50, 50);
        stage.addActor(joystick);

        // 검은색 페이드용 텍스처
        Pixmap pixmap = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        blackTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    @Override
    public void render(float delta) {
        if (isGameOver && !fadeOut) {
            // 게임 오버 상태에서 아직 페이드 아웃 시작 안했다면, 그냥 스테이지만 표시
            Gdx.gl.glClearColor(0,0,0,1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            stage.act(delta);
            stage.draw();
            return;
        }

        // 페이드 인/아웃 알파 계산
        fadeElapsed += delta;
        float alpha;
        if (fadeIn && !fadeOut) {
            alpha = 1f - Math.min(fadeElapsed / fadeDuration, 1f);
        } else if (fadeOut) {
            alpha = Math.min(fadeElapsed / fadeDuration, 1f);
        } else {
            alpha = 0f;
        }

        if (!isGameOver && !fadeOut) {
            // 생존 타이머 감소
            survivalTime -= delta;

            // 조이스틱 이동
            if (joystick.isEnabled()) {
                Vector2 direction = joystick.getDirection();
                Vector2 velocity = direction.cpy().scl(playerSpeed * delta);
                playerPosition.add(velocity);

                playerPosition.x = MathUtils.clamp(playerPosition.x, 0, screenWidth - 78);
                playerPosition.y = MathUtils.clamp(playerPosition.y, 0, screenHeight - 78);

                updateDirection(direction);
            }

            // 바이러스 추적
            Vector2 virusDirection = playerPosition.cpy().sub(virusPosition).nor();
            virusPosition.add(virusDirection.scl(virusSpeed * delta));

            // 귀신 스폰
            ghostSpawnTimer += delta;
            if (ghostSpawnTimer >= ghostSpawnInterval) {
                spawnGhost();
                ghostSpawnTimer = 0f;
            }

            // 귀신 추적
            Iterator<Vector2> iterator = ghosts.iterator();
            while (iterator.hasNext()) {
                Vector2 ghost = iterator.next();
                Vector2 ghostDir = playerPosition.cpy().sub(ghost).nor();
                ghost.add(ghostDir.scl(virusSpeed * delta));

                if (playerPosition.dst(ghost) < 24) {
                    onSurvivalFail();
                    break;
                }
            }

            // 바이러스 충돌
            if (playerPosition.dst(virusPosition) < 24) {
                onSurvivalFail();
            }

            // 애니메이션 업데이트
            stateTime += delta;
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
                default:
                    currentFrame = idleAnimation.getKeyFrame(stateTime, true);
                    break;
            }

            // 생존 시간 완료 시 성공
            if (survivalTime <= 0) {
                onSurvivalSuccess();
            }
        }

        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 배경 및 게임 요소 그리기
        batch.begin();
        batch.setColor(1,1,1,1); // 배치 색상 초기화
        batch.draw(backgroundTexture, 0, 0, screenWidth, screenHeight);
        font.draw(batch, "Survival Time: " + (int)survivalTime + " seconds", 50, screenHeight - 30);

        if (currentFrame != null) {
            batch.draw(currentFrame, playerPosition.x, playerPosition.y, 96, 96);
        }
        batch.draw(virusTexture, virusPosition.x, virusPosition.y, 96, 96);

        for (Vector2 ghost : ghosts) {
            batch.draw(ghostTexture, ghost.x, ghost.y, 96, 96);
        }
        batch.end();

        stage.act(Math.min(delta, 1/30f));
        stage.draw();

        // 페이드 오버레이를 마지막에 그리기
        batch.begin();
        batch.setColor(1,1,1,1); // 먼저 배치 색을 초기화한 뒤
        batch.setColor(0,0,0,alpha); // 페이드용 색상 적용
        batch.draw(blackTexture, 0, 0, screenWidth, screenHeight);
        batch.setColor(1,1,1,1); // 다시 흰색으로 복원
        batch.end();

        // 페이드 아웃 완료 시 다음 프레임에 화면 전환
        if (fadeOut && alpha >= 1f && nextScreen != null) {
            Gdx.app.postRunnable(() -> {
                game.setScreen(nextScreen);
            });
        }
    }

    private void updateDirection(Vector2 direction) {
        if (direction.len() > 0.1f) {
            if (Math.abs(direction.x) > Math.abs(direction.y)) {
                currentDirection = direction.x > 0 ? Direction.RIGHT : Direction.LEFT;
            } else {
                currentDirection = direction.y > 0 ? Direction.UP : Direction.DOWN;
            }
        } else {
            currentDirection = Direction.IDLE;
        }
    }

    private void spawnGhost() {
        float spawnRadius = 1000f;
        float angle = MathUtils.random(0, MathUtils.PI2);
        float distance = MathUtils.random(100, spawnRadius);
        float spawnX = playerPosition.x + MathUtils.cos(angle) * distance;
        float spawnY = playerPosition.y + MathUtils.sin(angle) * distance;

        spawnX = MathUtils.clamp(spawnX, 0, screenWidth - 48);
        spawnY = MathUtils.clamp(spawnY, 0, screenHeight - 48);

        ghosts.add(new Vector2(spawnX, spawnY));
    }

    private void onSurvivalFail() {
        backgroundMusic.stop();
        isGameOver = true;
        joystick.setEnabled(false);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;

        TextButton retryButton = new TextButton("RESTART", buttonStyle);
        retryButton.setSize(200, 80);
        retryButton.setPosition(screenWidth / 2f - 100, screenHeight / 2f - 40);

        retryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startFadeOut(new SurvivalScreen(game));
            }
        });

        stage.addActor(retryButton);
    }

    private void onSurvivalSuccess() {
        backgroundMusic.stop();
        startFadeOut(new EndScreen(game));
    }

    private void startFadeOut(Screen targetScreen) {
        fadeOut = true;
        fadeIn = false;
        fadeElapsed = 0f;
        nextScreen = targetScreen;
    }

    private Animation<TextureRegion> createAnimation(Texture sheet, int frameCols, int frameRows, float frameDuration) {
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

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        backgroundMusic.pause();
    }

    @Override
    public void resume() {
        backgroundMusic.play();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        backgroundTexture.dispose();
        virusTexture.dispose();
        ghostTexture.dispose();
        backgroundMusic.dispose();
        stage.dispose();
        joystick.dispose();
        if (blackTexture != null) {
            blackTexture.dispose();
        }
    }
}

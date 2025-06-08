package io.github.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class DrawingPuzzleScreen implements Screen {
    private Game game;
    private Vector2 spawnPoint;

    private SpriteBatch batch;
    private Texture backgroundTexture; // 배경 텍스처 추가
    private Texture faintImage; // 플레이어가 따라 그릴 희미한 그림
    private Texture horrificImage; // 그리기 진행에 따라 드러날 끔찍한 형상
    private ShapeRenderer shapeRenderer;

    private Stage stage;
    private Skin skin;

    private Music bgmStart; // 화면 열릴 때 BGM 추가
    private Music bgmPuzzleComplete; // 퍼즐 완료 시 BGM 추가
    private Music drawingSound; // 드로잉 중 재생할 효과음

    private boolean puzzleCompleted = false;
    private float horrificAlpha = 0f; // 끔찍한 형상의 투명도

    // quiz4 위치 정의 (좌표는 필요에 따라 조정)
    private final Vector2 quiz4Position = new Vector2(160f / 100f, 820f / 100f); // 예시 좌표 (PPM 고려)

    private boolean hasReceivedItem = false;
    private boolean isTransitioning = false;

    public DrawingPuzzleScreen(Game game, Vector2 spawnPoint) {
        this.game = game;
        this.spawnPoint = spawnPoint;
    }

    @Override
    public void show() {
        Gdx.app.log("DrawingPuzzleScreen", "Screen가 표시되었습니다.");

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // 배경 및 희미한 그림과 끔찍한 형상 이미지 로드
        backgroundTexture = new Texture(Gdx.files.internal("drawingbackground.png")); // 배경 이미지 경로
        faintImage = new Texture(Gdx.files.internal("faint_image.png")); // 희미한 그림 이미지 경로
        horrificImage = new Texture(Gdx.files.internal("horrific_image.png")); // 끔찍한 형상 이미지 경로

        // BGM 로드
        bgmStart = Gdx.audio.newMusic(Gdx.files.internal("ghostbgm.mp3")); // 화면 열릴 때 BGM 파일 경로
        bgmPuzzleComplete = Gdx.audio.newMusic(Gdx.files.internal("ghostbgm.mp3")); // 퍼즐 완료 BGM 파일 경로

        // 드로잉 효과음 로드
        drawingSound = Gdx.audio.newMusic(Gdx.files.internal("drawing_sound.mp3")); // 드로잉 효과음 파일 경로
        drawingSound.setLooping(true); // 효과음을 반복 재생
        drawingSound.setVolume(0.5f);

        // 화면 열릴 때 BGM 설정 및 재생
        bgmStart.setLooping(true);
        bgmStart.setVolume(0.5f);
        bgmStart.play();

        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // 입력 처리기 설정
        Gdx.input.setInputProcessor(new InputAdapter() {
            private Vector2 lastPoint = null;
            private boolean isDrawing = false; // 드로잉 중인지 여부

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (isTransitioning) return false;
                lastPoint = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);
                isDrawing = true;

                // 드로잉 시작 시 효과음 재생
                if (!drawingSound.isPlaying()) {
                    drawingSound.play();
                }

                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isTransitioning) return false;
                Vector2 currentPoint = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);
                if (lastPoint != null) {
                    // 선 그리기
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                    shapeRenderer.setColor(1, 0, 0, 1); // 빨간색
                    shapeRenderer.rectLine(lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y, 5); // 선의 두께
                    shapeRenderer.end();

                    // 그리기 진행도 확인
                    updateHorrificImage();
                }
                lastPoint = currentPoint;
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (isTransitioning) return false;
                lastPoint = null;
                isDrawing = false;

                // 드로잉 종료 시 효과음 정지
                if (drawingSound.isPlaying()) {
                    drawingSound.stop();
                }

                return true;
            }
        });
    }

    private void updateHorrificImage() {
        // 그리기 진행도에 따라 끔찍한 형상의 투명도 증가
        horrificAlpha += 0.005f;
        if (horrificAlpha >= 1f && !hasReceivedItem && !isTransitioning) {
            horrificAlpha = 1f;
            puzzleCompleted = true;
            hasReceivedItem = true;
            isTransitioning = true;

            // 기존 BGM 중지 및 퍼즐 완료 BGM 재생
            bgmStart.stop();
            bgmPuzzleComplete.setLooping(false);
            bgmPuzzleComplete.setVolume(0.5f);
            bgmPuzzleComplete.play();

            // 드로잉 효과음 정지
            if (drawingSound.isPlaying()) {
                drawingSound.stop();
            }

            // "3층 열쇠"를 인벤토리에 추가
            Inventory.getInstance().addItem("3층 열쇠");
            Gdx.app.log("Inventory", "3층 열쇠가 인벤토리에 추가되었습니다.");

            // FifthScreen으로 전환
            transitionToFifthScreen();
        }
    }

    private void transitionToFifthScreen() {
        Gdx.app.log("DrawingPuzzleScreen", "FifthScreen으로 전환합니다.");
        Gdx.app.postRunnable(() -> {
            FifthScreen fifthScreen = new FifthScreen(game, quiz4Position);
            game.setScreen(fifthScreen);
            dispose();
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1); // 검은색 배경
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        // 배경 그리기
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // 희미한 그림 그리기
        batch.draw(faintImage, centerX() - faintImage.getWidth() / 2f, centerY() - faintImage.getHeight() / 2f);
        // 끔찍한 형상 그리기 (투명도에 따라 점점 나타남)
        batch.setColor(1, 1, 1, horrificAlpha);
        batch.draw(horrificImage, centerX() - horrificImage.getWidth() / 2f, centerY() - horrificImage.getHeight() / 2f);
        batch.setColor(1, 1, 1, 1); // 색상 초기화
        batch.end();

        // Stage 업데이트 및 그리기
        stage.act(delta);
        stage.draw();
    }

    private float centerX() {
        return Gdx.graphics.getWidth() / 2f;
    }

    private float centerY() {
        return Gdx.graphics.getHeight() / 2f;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        Gdx.app.log("DrawingPuzzleScreen", "Screen 리소스가 해제되었습니다.");
        try {
            if (shapeRenderer != null) shapeRenderer.dispose();
            if (batch != null) batch.dispose();
            if (backgroundTexture != null) backgroundTexture.dispose();
            if (faintImage != null) faintImage.dispose();
            if (horrificImage != null) horrificImage.dispose();
            if (bgmStart != null) bgmStart.dispose();
            if (bgmPuzzleComplete != null) bgmPuzzleComplete.dispose();
            if (drawingSound != null) drawingSound.dispose(); // 효과음 자원 해제
            if (stage != null) stage.dispose();
            if (skin != null) skin.dispose();
        } catch (Exception e) {
            Gdx.app.error("DrawingPuzzleScreen", "Dispose 중 오류 발생: " + e.getMessage());
        }
    }
}

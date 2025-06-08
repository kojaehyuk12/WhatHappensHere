package io.github.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SlidePuzzleScreen 클래스는 게임의 슬라이드 퍼즐 화면을 담당합니다.
 * 퍼즐 완료 시 커스텀 디자인의 다이얼로그를 표시합니다.
 */
public class SlidePuzzleScreen implements Screen {
    private Game game;
    private Stage stage;
    private FourthScreen previousScreen;
    private Skin skin;
    private AssetManager assetManager;
    private List<PuzzleTile> tiles;
    private int emptyIndex;

    private static final int GRID_SIZE = 3;
    private static final int TILE_COUNT = GRID_SIZE * GRID_SIZE - 1;

    // **추가된 부분: 커스텀 폰트**
    private BitmapFont creepyFont;

    // **추가된 부분: 커스텀 버튼 스타일**
    private TextButton.TextButtonStyle customButtonStyle;

    public SlidePuzzleScreen(Game game, FourthScreen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        this.skin = new Skin(Gdx.files.internal("uiskin.json"));
        this.assetManager = new AssetManager();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 퍼즐 타일 이미지 로드
        for (int i = 1; i <= TILE_COUNT; i++) {
            String fileName = "haunting_portrait_" + i + ".jpg";
            assetManager.load(fileName, Texture.class);
        }
        // 버튼 이미지 로드
        assetManager.load("inventory_button.png", Texture.class);
        assetManager.load("investigate_button.png", Texture.class);
        assetManager.load("ok.png", Texture.class);

        assetManager.finishLoading();
    }

    /**
     * **추가된 부분: 커스텀 폰트 생성 메서드**
     */
    private void createCustomFont() {
        if (!Gdx.files.internal("fonts/creepy_font.ttf").exists()) {
            Gdx.app.error("SlidePuzzleScreen", "Font file fonts/creepy_font.ttf not found!");
            // 폰트 파일이 없을 경우 기본 폰트 사용
            creepyFont = skin.getFont("default-font");
        } else {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/creepy_font.ttf")); // 커스텀 폰트 파일 경로
            FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 24; // 폰트 크기 조정 (줄임)
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

    /**
     * **추가된 부분: 커스텀 다이얼로그 스타일 생성 및 스킨에 추가 메서드**
     */
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
        customDialogStyle.titleFontColor = Color.RED; // 제목 글자색을 붉게 설정

        // Skin에 커스텀 DialogStyle 추가
        skin.add("custom-dialog", customDialogStyle);
        Gdx.app.log("Skin", "custom-dialog style added to skin");
    }

    // 그라데이션을 생성하는 메서드 (기존과 동일)
    private Drawable createGradientDrawable(Color startColor, Color endColor, int height) {
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
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    /**
     * **추가된 부분: 커스텀 버튼 스타일 생성 메서드**
     */
    private void createCustomButtonStyle() {
        // 그라데이션 배경 생성 (어두운 빨간색에서 더 어두운 빨간색으로)
        Drawable buttonUpDrawable = createGradientDrawable(new Color(0.3f, 0f, 0f, 1f), new Color(0.5f, 0f, 0f, 1f), 50);
        Drawable buttonDownDrawable = createGradientDrawable(new Color(0.6f, 0f, 0f, 1f), new Color(0.6f, 0f, 0f, 1f), 50);
        Drawable buttonOverDrawable = createGradientDrawable(new Color(0.8f, 0f, 0f, 1f), new Color(0.8f, 0f, 0f, 1f), 50);

        // 커스텀 TextButtonStyle 생성
        customButtonStyle = new TextButton.TextButtonStyle();
        customButtonStyle.up = buttonUpDrawable;    // 직접 할당
        customButtonStyle.down = buttonDownDrawable;
        customButtonStyle.over = buttonOverDrawable;
        customButtonStyle.font = creepyFont;         // 커스텀 폰트 사용
        customButtonStyle.fontColor = Color.WHITE;   // 텍스트 색상 흰색
    }

    @Override
    public void show() {
        // 초기화는 생성자에서 하지 않고 show()에서 수행
        // 커스텀 폰트와 스타일 생성
        createCustomFont();
        createCustomDialogStyle();
        createCustomButtonStyle();

        setupSlidePuzzle();
        addButtons();
    }

    private void setupSlidePuzzle() {
        if (tiles != null) {
            for (PuzzleTile tile : tiles) {
                tile.remove();
            }
            tiles.clear();
        } else {
            tiles = new ArrayList<>();
        }

        for (int i = 1; i <= TILE_COUNT; i++) {
            String fileName = "haunting_portrait_" + i + ".jpg";
            Texture texture = assetManager.get(fileName, Texture.class);
            PuzzleTile tile = new PuzzleTile(texture, i);
            tiles.add(tile);
        }

        List<Integer> tileNumbers = new ArrayList<>();
        for (int i = 1; i <= TILE_COUNT; i++) {
            tileNumbers.add(i);
        }
        tileNumbers.add(0); // 빈 칸 표시

        do {
            Collections.shuffle(tileNumbers.subList(0, TILE_COUNT));
        } while (!isSolvable(tileNumbers) || isSolved(tileNumbers));

        tileNumbers.set(TILE_COUNT, 0); // 마지막에 빈 칸 위치

        float tileSize = Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) / (GRID_SIZE + 1);

        for (int i = 0; i < tileNumbers.size(); i++) {
            int number = tileNumbers.get(i);
            if (number != 0) {
                PuzzleTile tile = tiles.get(number - 1);
                float x = (i % GRID_SIZE) * tileSize + (Gdx.graphics.getWidth() - (GRID_SIZE * tileSize)) / 2;
                float y = ((GRID_SIZE - 1) - (i / GRID_SIZE)) * tileSize + (Gdx.graphics.getHeight() - (GRID_SIZE * tileSize)) / 2;
                tile.setSize(tileSize, tileSize);
                tile.setPosition(x, y);
                tile.setIndex(i);
                tile.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        moveTile(tile);
                    }
                });
                stage.addActor(tile);
            } else {
                emptyIndex = i;
            }
        }
    }

    private void addButtons() {
        // "나가기" 버튼 생성 및 설정
        TextButton exitButton = new TextButton("나가기", customButtonStyle); // 커스텀 스타일 적용
        exitButton.setSize(200, 75); // 버튼 크기 200x75로 변경
        exitButton.setPosition(20, 20); // 화면 왼쪽 하단에 배치
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("SlidePuzzleScreen", "Exit button clicked");
                game.setScreen(previousScreen);
            }
        });
        stage.addActor(exitButton);

        // "재시작" 버튼 생성 및 설정
        TextButton restartButton = new TextButton("재시작", customButtonStyle); // 커스텀 스타일 적용
        restartButton.setSize(200, 75); // 버튼 크기 200x75로 변경
        restartButton.setPosition(240, 20); // "나가기" 버튼과의 간격을 띄움 (x 위치 조정)
        restartButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("SlidePuzzleScreen", "Restart button clicked");
                resetPuzzle(); // 퍼즐 재설정 메서드 호출
            }
        });
        stage.addActor(restartButton);
    }

    /**
     * 퍼즐을 재설정하고 다시 섞는 메서드
     */
    private void resetPuzzle() {
        // 기존 퍼즐 타일 제거
        for (PuzzleTile tile : tiles) {
            tile.remove();
        }
        tiles.clear();

        // 퍼즐 설정 재호출
        setupSlidePuzzle();

        Gdx.app.log("SlidePuzzleScreen", "Puzzle reset");
    }

    private boolean isSolvable(List<Integer> tileNumbers) {
        int inversions = 0;
        for (int i = 0; i < TILE_COUNT; i++) {
            for (int j = i + 1; j < TILE_COUNT; j++) {
                if (tileNumbers.get(i) > tileNumbers.get(j)) {
                    inversions++;
                }
            }
        }
        return inversions % 2 == 0;
    }

    private boolean isSolved(List<Integer> tileNumbers) {
        for (int i = 0; i < TILE_COUNT; i++) {
            if (tileNumbers.get(i) != i + 1) {
                return false;
            }
        }
        return tileNumbers.get(TILE_COUNT) == 0;
    }

    private void moveTile(PuzzleTile tile) {
        int tileIndex = tile.getIndex();
        if (isAdjacent(tileIndex, emptyIndex)) {
            float emptyX = (emptyIndex % GRID_SIZE) * tile.getWidth() + (Gdx.graphics.getWidth() - (GRID_SIZE * tile.getWidth())) / 2;
            float emptyY = ((GRID_SIZE - 1) - (emptyIndex / GRID_SIZE)) * tile.getHeight() + (Gdx.graphics.getHeight() - (GRID_SIZE * tile.getHeight())) / 2;

            tile.setPosition(emptyX, emptyY);

            tile.setIndex(emptyIndex);
            emptyIndex = tileIndex;

            if (isPuzzleCompleted()) {
                onPuzzleComplete();
            }
        }
    }

    private boolean isAdjacent(int index1, int index2) {
        int row1 = index1 / GRID_SIZE;
        int col1 = index1 % GRID_SIZE;
        int row2 = index2 / GRID_SIZE;
        int col2 = index2 % GRID_SIZE;

        return (Math.abs(row1 - row2) + Math.abs(col1 - col2)) == 1;
    }

    private boolean isPuzzleCompleted() {
        for (PuzzleTile tile : tiles) {
            if (!tile.isInCorrectPosition()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // GL20 사용

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        dispose();
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
    public void dispose() {
        stage.dispose();
        skin.dispose();
        assetManager.dispose();
        if (creepyFont != null) {
            creepyFont.dispose(); // 커스텀 폰트 해제
        }

        // 그라데이션 드로어블의 텍스처 해제
        // 여기서는 TextureRegionDrawable의 Texture를 따로 관리하지 않았으므로 생략
    }

    /**
     * **수정된 부분: 퍼즐 완료 시 커스텀 디자인의 다이얼로그 표시**
     */
    public void onPuzzleComplete() {
        Dialog dialog = new Dialog("퍼즐 완료", skin, "custom-dialog") { // "custom-dialog" 스타일 사용
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    Inventory inventory = Inventory.getInstance();
                    if (!inventory.hasItem("2층 열쇠")) {
                        inventory.addItem("2층 열쇠");
                    }
                    game.setScreen(previousScreen);
                }
            }
        };

        // 다이얼로그의 텍스트 레이블 스타일 커스터마이징
        Label dialogLabel = new Label("액자 뒤에서 열쇠가 떨어졌다.", new Label.LabelStyle(creepyFont, Color.WHITE));
        dialogLabel.setWrap(true); // 줄 바꿈 활성화
        dialogLabel.setAlignment(Align.center); // 중앙 정렬
        // dialogLabel.setFontScale(0.8f); // 텍스트 크기 줄이기 (폰트 크기 조정으로 대체)

        dialog.getContentTable().clear(); // 기본 텍스트 제거
        dialog.getContentTable().add(dialogLabel).width(Gdx.graphics.getWidth() * 0.4f).pad(20); // Label의 너비 설정 및 패딩 추가

        // **추가된 부분: 커스텀 버튼 스타일 생성 및 적용**
        TextButton confirmButton = new TextButton("확인", customButtonStyle);
        confirmButton.setSize(100, 50);
        dialog.button(confirmButton, true);

        dialog.show(stage);

        // 다이얼로그의 텍스트 레이블 스타일 커스터마이징
        for (Actor actor : dialog.getChildren()) {
            if (actor instanceof Label) {
                Label label = (Label) actor;
                label.setStyle(new Label.LabelStyle(creepyFont, Color.WHITE)); // 텍스트 색상 흰색
                label.setWrap(true); // 줄 바꿈 활성화
                label.setAlignment(Align.center); // 중앙 정렬
                // label.setFontScale(0.8f); // 텍스트 크기 줄이기 (폰트 크기 조정으로 대체)
            }
        }
    }

    /**
     * 퍼즐 타일을 나타내는 내부 클래스
     */
    private class PuzzleTile extends ImageButton {
        private int number;
        private int index;

        public PuzzleTile(Texture texture, int number) {
            super(new ImageButtonStyle());
            this.number = number;

            ImageButtonStyle style = new ImageButtonStyle();
            style.imageUp = new TextureRegionDrawable(new TextureRegion(texture));
            this.setStyle(style);
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public boolean isInCorrectPosition() {
            int correctRow = (number - 1) / GRID_SIZE;
            int correctCol = (number - 1) % GRID_SIZE;

            int currentRow = index / GRID_SIZE;
            int currentCol = index % GRID_SIZE;

            return currentRow == correctRow && currentCol == correctCol;
        }
    }
}

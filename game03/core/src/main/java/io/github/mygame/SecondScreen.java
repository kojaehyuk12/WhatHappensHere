package io.github.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

public class SecondScreen implements Screen {
    private Game game; // Game 인스턴스 추가

    private SpriteBatch batch;
    private Texture img;
    private BitmapFont font;
    private Stage stage;
    private Skin skin;
    private Music typingMusic;

    private String fullText = "어두운 밤, 깊은 산속에 위치한 오래된 폐가.\n" +
        "유튜버 고스트헌터인 도훈은 구독자로부터\n" +
        "이곳에 대한 기이한 제보를 받는다.\n\n" +
        "몇 년 전부터 아무도 살지 않는 이 집은,\n" +
        "방문하는 사람마다 불운이 따르는 저주받은 곳으로\n" +
        "소문이 자자하다.\n\n" +
        "도훈은 이 미스터리를 파헤치기 위해\n" +
        "카메라를 들고 폐가로 향한다.\n" +
        "문을 열고 들어서는 순간, 그는 알 수 없는 기운이\n" +
        "감도는 이곳에서 이상한 소리를 듣기 시작하는데...\n\n" +
        "이곳에서 무슨 일이 일어났던걸까?";

    private StringBuilder displayedText = new StringBuilder();  // 타이핑된 텍스트를 저장
    private int currentCharIndex = 0;
    private float delay = 0.01f;  // 각 글자 출력 간격 0.05

    private Label textLabel;
    private ScrollPane scrollPane;
    private TextButton hiddenButton;

    // 생성자 추가
    public SecondScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        img = new Texture(Gdx.files.internal("img.jpg"));

        font = new BitmapFont(Gdx.files.internal("sdss.fnt"), false);

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // 배경 음악 설정
        typingMusic = Gdx.audio.newMusic(Gdx.files.internal("typing12.mp3"));
        typingMusic.setLooping(true);
        typingMusic.setVolume(0.5f);
        typingMusic.play();

        // 텍스트를 표시하기 위한 Label 생성
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;

        textLabel = new Label("", labelStyle);
        textLabel.setAlignment(Align.center);  // 텍스트 중앙 정렬
        textLabel.setWrap(true); // 텍스트 줄바꿈 허용

        // ScrollPane 생성
        scrollPane = new ScrollPane(textLabel, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // 가로 스크롤 비활성화, 세로 스크롤 활성화

        // ScrollPane의 배경 제거
        ScrollPaneStyle scrollPaneStyle = new ScrollPaneStyle(scrollPane.getStyle());
        scrollPaneStyle.background = null;      // 배경 제거
        scrollPaneStyle.hScroll = null;         // 가로 스크롤바 배경 제거
        scrollPaneStyle.hScrollKnob = null;     // 가로 스크롤바 손잡이 제거
        scrollPaneStyle.vScroll = null;         // 세로 스크롤바 배경 제거
        scrollPaneStyle.vScrollKnob = null;     // 세로 스크롤바 손잡이 제거
        scrollPane.setStyle(scrollPaneStyle);

        // 스크롤바를 완전히 숨김
        scrollPane.setScrollbarsVisible(false);

        // ScrollPane의 크기와 위치 설정
        float scrollPaneWidth = Gdx.graphics.getWidth() * 0.8f;
        float scrollPaneHeight = Gdx.graphics.getHeight() * 0.6f;
        scrollPane.setSize(scrollPaneWidth, scrollPaneHeight);
        scrollPane.setPosition((Gdx.graphics.getWidth() - scrollPaneWidth) / 2,
            (Gdx.graphics.getHeight() - scrollPaneHeight) / 2);

        // 히든 버튼 생성
        hiddenButton = new TextButton("Start", skin, "transparent");
        hiddenButton.getLabel().setFontScale(2);
        hiddenButton.getStyle().fontColor = Color.RED;

        // 히든 버튼의 크기 설정
        hiddenButton.setSize(200, 100);

        // 히든 버튼의 위치를 ScrollPane의 오른쪽 중간에 고정
        float buttonX = scrollPane.getX() + scrollPane.getWidth() - hiddenButton.getWidth() - 20;
        float buttonY = scrollPane.getY() + (scrollPane.getHeight() - hiddenButton.getHeight()) / 2;
        hiddenButton.setPosition(buttonX, buttonY);
        hiddenButton.setVisible(false); // 초기에는 버튼을 숨김

        hiddenButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                // 배경 음악 정지
                if (typingMusic != null) {
                    typingMusic.stop();
                    typingMusic.dispose();
                }
                // ThirdScreen으로 화면 전환
                game.setScreen(new ThirdScreen(game));
            }
        });

        stage.addActor(scrollPane);
        stage.addActor(hiddenButton);

        // 타이핑 효과를 위한 타이머 설정
        Timer.schedule(new Task() {
            @Override
            public void run() {
                if (currentCharIndex < fullText.length()) {
                    // 한 글자 추가
                    displayedText.append(fullText.charAt(currentCharIndex));
                    currentCharIndex++;

                    // Label의 텍스트 업데이트
                    textLabel.setText(displayedText.toString());

                    // 새로운 텍스트가 추가될 때마다 ScrollPane의 스크롤을 가장 아래로 설정
                    scrollPane.layout();
                    scrollPane.scrollTo(0, 0, 0, 0);

                } else {
                    // 모든 텍스트가 출력되면 타이머 정지
                    cancel();
                    hiddenButton.setVisible(true); // 버튼 보이기
                    typingMusic.stop();
                }
            }
        }, 0, delay);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);  // 배경을 검은색으로 설정
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 배경 이미지 출력
        batch.begin();
        batch.draw(img, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // Stage 업데이트 및 렌더링
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // 뷰포트 업데이트
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        if (typingMusic != null && typingMusic.isPlaying()) {
            typingMusic.pause();
        }
    }

    @Override
    public void resume() {
        if (typingMusic != null && !typingMusic.isPlaying()) {
            typingMusic.play();
        }
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        batch.dispose();
        img.dispose();
        font.dispose();
        stage.dispose();
        skin.dispose();
        if (typingMusic != null) {
            typingMusic.dispose();
        }
    }
}

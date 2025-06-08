package io.github.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

public class VoiceScreen implements Screen {
    private Game game;
    private Stage stage;
    private Skin skin;

    private Texture backgroundTexture; // 배경 텍스처
    private BitmapFont textFont; // `sdss.fnt` 폰트

    private Vector2 quiz5SpawnPosition; // Quiz5 영역으로 돌아가기 위한 스폰 위치

    private Music voiceOver; // 음성 재생을 위한 변수 추가

    public VoiceScreen(Game game, Vector2 quiz5SpawnPosition) {
        this.game = game;
        this.quiz5SpawnPosition = quiz5SpawnPosition;
    }

    @Override
    public void show() {
        // Stage 및 Skin 초기화
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // `sdss.fnt` 폰트 로드
        textFont = new BitmapFont(Gdx.files.internal("sdss.fnt"));

        // 배경 텍스처 로드
        backgroundTexture = new Texture(Gdx.files.internal("voice_screen_background.png"));
        TextureRegionDrawable backgroundDrawable = new TextureRegionDrawable(new TextureRegion(backgroundTexture));

        // UI 테이블 생성
        Table table = new Table();
        table.setFillParent(true);
        table.setBackground(backgroundDrawable); // 배경 설정
        stage.addActor(table);

        // 메시지 라벨 생성
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = textFont;
        labelStyle.fontColor = new Color(0.6f, 0f, 0f, 1f); // 버튼보다 어두운 붉은색
        Label messageLabel = new Label("    이 저택의 비밀을 알고 싶다면,\n\n                   나를 찾아라.", labelStyle);
        messageLabel.setWrap(true);
        table.add(messageLabel).width(600).pad(20).row();

        // "돌아가기" 버튼 스타일 생성
        TextButton.TextButtonStyle buttonStyle = createButtonStyle();

        // "돌아가기" 버튼 생성
        TextButton backButton = new TextButton("돌아가기", buttonStyle);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // SixthScreen으로 돌아가기
                game.setScreen(new SixthScreen(game, quiz5SpawnPosition));
            }
        });
        table.add(backButton).width(200).height(75).pad(10).center();

        // 음성 파일 로드 및 재생
        voiceOver = Gdx.audio.newMusic(Gdx.files.internal("voice_over.mp3"));
        voiceOver.setVolume(1.0f); // 볼륨 설정 (0.0f ~ 1.0f)
        voiceOver.setLooping(false); // 반복 재생 여부 설정
        voiceOver.play(); // 재생 시작
    }

    /**
     * 그라데이션을 생성하는 메서드
     * @param startColor 시작 색상
     * @param endColor 끝 색상
     * @param height 텍스처의 높이
     * @return TextureRegionDrawable 객체
     */
    private TextureRegionDrawable createGradientDrawable(Color startColor, Color endColor, int height) {
        Pixmap pixmap = new Pixmap(1, height, Pixmap.Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            float ratio = (float) y / (float) height;
            Color blendedColor = new Color(
                startColor.r + ratio * (endColor.r - startColor.r),
                startColor.g + ratio * (endColor.g - startColor.g),
                startColor.b + ratio * (endColor.b - startColor.b),
                startColor.a + ratio * (endColor.a - startColor.a)
            );
            pixmap.setColor(blendedColor);
            pixmap.drawPixel(0, y);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    /**
     * 버튼 스타일을 생성합니다.
     * @return TextButtonStyle 객체
     */
    private TextButton.TextButtonStyle createButtonStyle() {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();

        style.up = createGradientDrawable(new Color(0.3f, 0, 0, 1), new Color(0.5f, 0, 0, 1), 50);
        style.down = createGradientDrawable(new Color(0.6f, 0, 0, 1), new Color(0.6f, 0, 0, 1), 50);
        style.over = createGradientDrawable(new Color(0.8f, 0, 0, 1), new Color(0.8f, 0, 0, 1), 50);

        style.font = textFont;
        style.fontColor = Color.WHITE;
        return style;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Stage 렌더링
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        if (voiceOver != null && voiceOver.isPlaying()) {
            voiceOver.pause();
        }
    }

    @Override
    public void resume() {
        if (voiceOver != null && !voiceOver.isPlaying()) {
            voiceOver.play();
        }
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (voiceOver != null) {
            voiceOver.stop();
            voiceOver.dispose();
        }
        if (textFont != null) textFont.dispose();
    }
}

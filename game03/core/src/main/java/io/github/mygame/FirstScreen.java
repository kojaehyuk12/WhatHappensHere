package io.github.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class FirstScreen implements Screen {
    private Game game; // Game 인스턴스 추가

    private SpriteBatch batch;
    private Texture img;
    private Stage stage;
    private TextButton startButton;
    private TextButton exitButton;
    private TextButton communityButton;
    private Skin skin;
    private Music backgroundMusic;
    private BitmapFont font;

    // 생성자 추가
    public FirstScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        try {
            img = new Texture(Gdx.files.internal("first_back.jpg"));
            if (!Gdx.files.internal("first_back.jpg").exists()) {
                Gdx.app.error("Error", "Image file 'first_back.jpg' does not exist in assets directory");
            } else {
                Gdx.app.log("Info", "Image file 'first_back.jpg' loaded successfully");
            }
        } catch (Exception e) {
            Gdx.app.error("Error", "Image file not found or cannot be loaded", e);
        }

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        font = new BitmapFont(Gdx.files.internal("sdss.fnt"), false);

        // 투명한 버튼 스타일 생성
        TextButton.TextButtonStyle transparentButtonStyle = createTransparentTextButtonStyle(font, 1, 1, 1, 1);

        // Start Button
        startButton = new TextButton("Start", transparentButtonStyle);
        startButton.setSize(startButton.getWidth(), startButton.getHeight());
        startButton.setPosition(Gdx.graphics.getWidth() / 2 - startButton.getWidth() / 2 - 240,
            Gdx.graphics.getHeight() / 2 - 380);

        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // SecondScreen으로 화면 전환
                game.setScreen(new SecondScreen(game));
            }
        });

        // Community Button
        communityButton = new TextButton("Community", transparentButtonStyle);
        communityButton.setSize(communityButton.getWidth(), communityButton.getHeight());
        communityButton.setPosition(Gdx.graphics.getWidth() / 2 - communityButton.getWidth() / 2,
            Gdx.graphics.getHeight() / 2 - 380);

        communityButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // CommunityScreen으로 화면 전환 (필요 시 구현)
                // game.setScreen(new CommunityScreen(game));
            }
        });

        // Exit Button
        exitButton = new TextButton("Exit", transparentButtonStyle);
        exitButton.setSize(exitButton.getWidth(), exitButton.getHeight());
        exitButton.setPosition(Gdx.graphics.getWidth() / 2 - exitButton.getWidth() / 2 + 240,
            Gdx.graphics.getHeight() / 2 - 380);

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Add buttons to the stage
        stage.addActor(startButton);
        stage.addActor(communityButton);
        stage.addActor(exitButton);

        // Load and play background music
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("main_background.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();
    }

    private TextButton.TextButtonStyle createTransparentTextButtonStyle(BitmapFont font, float r, float g, float b,
                                                                        float a) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();

        // Create a transparent drawable
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0); // Fully transparent
        pixmap.fill();
        TextureRegionDrawable transparentDrawable = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose(); // Dispose pixmap to free resources

        style.up = transparentDrawable;
        style.down = transparentDrawable;
        style.font = font;
        style.fontColor = new Color(r, g, b, a);

        return style;
    }

    @Override
    public void render(float delta) {
        // Clear the screen
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        // Draw the background image
        if (batch != null && img != null) {
            float screenWidth = Gdx.graphics.getWidth();
            float screenHeight = Gdx.graphics.getHeight();

            batch.draw(img, 0, 0, screenWidth, screenHeight);
        }

        batch.end();

        // Update and draw the stage
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // Update the viewport
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        // Pause the music
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    @Override
    public void resume() {
        // Resume the music
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
        if (batch != null)
            batch.dispose();
        if (img != null)
            img.dispose();
        if (stage != null)
            stage.dispose();
        if (skin != null)
            skin.dispose();
        if (backgroundMusic != null)
            backgroundMusic.dispose();
        if (font != null)
            font.dispose();
    }
}

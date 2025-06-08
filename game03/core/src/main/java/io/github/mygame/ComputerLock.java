package io.github.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

public class ComputerLock implements Screen {
    private Game game;
    private FourthScreen fourthScreen;
    private Stage stage;
    private SpriteBatch batch;
    private Texture blackTexture;

    private float fadeElapsed = 0f;
    private float fadeDuration = 1f;

    // 페이드 상태 플래그
    private boolean fadeIn = false;      // 화면 진입 시 페이드 인
    private boolean fadeOut = false;    // 다음 화면 전환 시 페이드 아웃
    private Screen nextScreen = null;   // 페이드 아웃 완료 후 전환할 스크린

    private Drawable buttonUpDrawable;
    private Drawable buttonDownDrawable;
    private Drawable buttonOverDrawable;
    private Drawable passwordFieldBackground;
    private BitmapFont horrorFont;

    public ComputerLock(Game game, FourthScreen fourthScreen) {
        this.game = game;
        this.fourthScreen = fourthScreen;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        batch = new SpriteBatch();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 1);
        pixmap.fill();
        blackTexture = new Texture(pixmap);
        pixmap.dispose();

        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        // 폰트 설정
        if (!Gdx.files.internal("fonts/creepy_font.ttf").exists()) {
            Gdx.app.error("ComputerLock", "Font file fonts/creepy_font.ttf not found!");
            horrorFont = skin.getFont("default-font");
        } else {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/creepy_font.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 28;
            parameter.color = Color.RED;
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
        }

        buttonUpDrawable = createGradientDrawable(new Color(0.3f, 0f, 0f, 1f), new Color(0.5f, 0f, 0f, 1f), 50);
        buttonDownDrawable = createGradientDrawable(new Color(0.6f, 0f, 0f, 1f), new Color(0.6f, 0f, 0f, 1f), 50);
        buttonOverDrawable = createGradientDrawable(new Color(0.8f, 0f, 0f, 1f), new Color(0.8f, 0f, 0f, 1f), 50);

        passwordFieldBackground = createGradientDrawable(new Color(0.0f, 0f, 0.0f, 0.8f), new Color(0.2f, 0f, 0.0f, 0.8f), 100);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonUpDrawable;
        buttonStyle.down = buttonDownDrawable;
        buttonStyle.over = buttonOverDrawable;
        buttonStyle.font = horrorFont;
        buttonStyle.fontColor = Color.WHITE;

        TextField.TextFieldStyle passwordFieldStyle = new TextField.TextFieldStyle();
        passwordFieldStyle.font = horrorFont;
        passwordFieldStyle.fontColor = Color.WHITE;
        passwordFieldStyle.background = passwordFieldBackground;
        passwordFieldStyle.cursor = skin.newDrawable("white", Color.RED);
        passwordFieldStyle.selection = skin.newDrawable("white", Color.DARK_GRAY);

        TextField passwordField = new TextField("", passwordFieldStyle);
        passwordField.setMessageText("비밀번호 입력");
        passwordField.setAlignment(Align.center);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');

        TextButton confirmButton = new TextButton("확인", buttonStyle);
        confirmButton.setSize(200, 75);

        TextButton cancelButton = new TextButton("취소", buttonStyle);
        cancelButton.setSize(200, 75);

        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (passwordField.getText().equals("0414")) {
                    // 비밀번호 성공 시 페이드 아웃 시작
                    fadeOut = true;
                    fadeIn = false;
                    fadeElapsed = 0f;
                    nextScreen = new PuzzleScreen(game);
                } else {
                    showWrongPasswordDialog(skin);
                }
            }
        });

        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (fourthScreen != null) {
                    fadeOut = true;
                    fadeIn = false;
                    fadeElapsed = 0f;
                    nextScreen = fourthScreen;
                } else {
                    Gdx.app.error("ComputerLock", "FourthScreen is null!");
                }
            }
        });

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        table.pad(20);

        table.add(passwordField).width(400).height(60).padBottom(30);
        table.row();
        table.add(confirmButton).width(200).height(75).padBottom(20);
        table.row();
        table.add(cancelButton).width(200).height(75);

        stage.addActor(table);
    }

    private void showWrongPasswordDialog(Skin skin) {
        Window.WindowStyle dialogStyle = new Window.WindowStyle();
        dialogStyle.titleFont = horrorFont;
        dialogStyle.titleFontColor = Color.RED;
        dialogStyle.background = createGradientDrawable(
            new Color(0.2f, 0f, 0f, 0.9f),
            new Color(0f, 0f, 0f, 0.9f),
            100
        );

        Dialog dialog = new Dialog("비밀번호 오류", dialogStyle) {
            @Override
            protected void result(Object obj) {
                this.hide();
            }
        };

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = horrorFont;
        labelStyle.fontColor = Color.WHITE;

        Label dialogLabel = new Label("비밀번호가 틀립니다. 다시 시도해보세요.", labelStyle);
        dialogLabel.setWrap(true);
        dialogLabel.setAlignment(Align.center);

        dialog.getContentTable().clear();
        dialog.getContentTable().add(dialogLabel).width(Gdx.graphics.getWidth() * 0.5f).pad(20);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonUpDrawable;
        buttonStyle.down = buttonDownDrawable;
        buttonStyle.over = buttonOverDrawable;
        buttonStyle.font = horrorFont;

        TextButton confirmButton = new TextButton("확인", buttonStyle);
        dialog.button(confirmButton, true);

        dialog.show(stage);
    }

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
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 페이드 인/아웃 alpha 계산
        fadeElapsed += delta;
        float alpha;
        if (fadeIn && !fadeOut) {
            // 페이드 인: 처음 화면 진입 시 검은 화면에서 점차 밝아짐
            alpha = 1f - Math.min(fadeElapsed / fadeDuration, 1f);
        } else if (fadeOut) {
            // 페이드 아웃: 다음 화면으로 전환 시 점차 어두워짐
            alpha = Math.min(fadeElapsed / fadeDuration, 1f);
        } else {
            // 페이드 완료 상태
            alpha = 0f;
        }

        // Stage 먼저 그리기
        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();

        // 그 후 오버레이(검은 텍스처) 그리기
        batch.begin();
        batch.setColor(0, 0, 0, alpha);
        batch.draw(blackTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // 페이드 아웃 완료 시 다음 화면으로 전환
        if (fadeOut && alpha >= 1f && nextScreen != null) {
            game.setScreen(nextScreen);
        }
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
    public void pause() {}
    @Override
    public void resume() {}

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        blackTexture.dispose();

        if (horrorFont != null) {
            horrorFont.dispose();
        }

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

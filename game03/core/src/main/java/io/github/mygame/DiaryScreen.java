package io.github.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound; // Sound 클래스 임포트
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class DiaryScreen implements Screen {
    private Game game;
    private Stage stage;
    private Skin skin;
    private SpriteBatch batch;

    // 일기 텍스트 목록
    private String[] diaryTexts = {
        "20XX년 11월 XX일\n\n오늘 시장에서 버려진 물건들을 살펴보다가 이상한 컴퓨터를 발견했다. \n\n너무 오래된 모델인데도 불구하고 이상하게 나를 끌어당겼다.\n\n주인은 보이지 않았지만, 이건 그냥 버려진 물건일 뿐일 것이다.\n\n이상하게 생겼지만, 집에 가져가 보기로 했다.",
        "20XX년 11월 XX일\n\n컴퓨터를 집에 들여놓은 이후로 묘한 느낌이 든다.\n\n켜지는지도 모르겠지만, 켜진다면 흥미로운 것이 나올지도 모르겠다. \n\n이상하게 컴퓨터를 바라보고 있으면 시간이 빠르게 지나가는 기분이다.",
        "20XX년 11월 XX일\n\n드디어 컴퓨터를 켰다.\n\n화면에 무언가 깨진 듯한 글씨가 나타났고, '포기할 수 없는 것을 증명하라'는 메시지가 떴다.\n\n그게 무슨 의미인지 알 수는 없지만, 뭔가 기묘하고 불길한 기분이 든다.\n\n밤마다 이 메시지가 머릿속에서 떠나질 않는다.",
        "20XX년 12월 XX일\n\n잠을 잘 수가 없다.\n\n이상한 꿈을 꾸기 시작했는데, 저택 안에서 누군가가 날 쫓아오는 꿈이다.\n\n꿈에서 도망치려 애써도 매번 같은 장소로 돌아오고 만다.\n\n컴퓨터를 없애야겠다는 생각도 들었지만, 마치 나를 붙잡고 놓아주지 않는 느낌이다.",
        "20XX년 12월 XX일\n\n악몽이 점점 더 끔찍해지고 있다.\n\n꿈속에서 들리는 속삭임은 이제는 분명하게 내 이름을 부르고 있다.\n\n그 속삭임은 마치 컴퓨터에서 들리는 것 같다.\n\n컴퓨터를 치우려고 해도 손이 떨리고, 가까이 다가가면 심장이 터질 것 같은 공포가 밀려온다.",
        "20XX년 12월 XX일\n\n이제는 꿈과 현실의 구분이 점점 흐려지고 있다.\n\n문득 컴퓨터 앞에 앉아 있는 나 자신을 발견한다.\n\n이틀이나 밥을 먹지 못했지만 배고픔조차 느껴지지 않는다.\n\n컴퓨터가 나를 삼켜버린 것 같다.",
        "20XX년 1월 XX일\n\n나는 더 이상 나 자신이 아니다.\n\n오늘 거울을 보는데 내 눈이 아닌 무언가가 나를 바라보고 있었다.\n\n컴퓨터는 나를 지배하고 있다.\n\n이 저택의 모든 것이 나를 붙잡고 있다.\n\n이 글을 보는 누군가가 있다면, 제발 이 저택에서 도망쳐라.",
        "20XX년 1월 XX일\n\n나는 이제 이름도 기억나지 않는다.\n\n다만 나의 몸을 통해 끊임없는 분노와 공포가 퍼져나간다.\n\n나는 이제 저주가 되었고, 이 저택의 주인이 되었다.\n\n그 누구도 나를 막을 수 없다.",
        "20XX년 4월 14일\n\n그로부터 며칠이 지났는지 모르겠다. 하지만 오늘은 무언가 특별한 날임은 틀림없다.\n\n누군가가 이 저주를 풀어줄 것 같은 느낌이 든다. 더 깊은 어둠으로 잠식되기 전에 구원받고 싶다.\n\n제발, 누군가... 이 글을 읽는 네가 나를 도와줬으면 좋겠다.\n\n하지만 조심해라.\n\n이 컴퓨터가, 이 저택이 너를 삼키지 않도록."
    };

    private int currentPage = 0; // 현재 일기 페이지
    private Vector2 quiz5SpawnPosition; // Quiz5 영역으로 돌아가기 위한 스폰 위치

    private Texture backgroundTexture; // 배경 텍스처
    private BitmapFont textFont; // 텍스트 폰트

    private Label diaryLabel; // 일기 텍스트 라벨

    private Sound pageTurnSound; // 페이지 전환 효과음

    // 페이지 전환 애니메이션 변수
    private boolean isTransitioning = false;
    private float transitionAlpha = 1f;
    private boolean isTransitioningForward = true; // true: 다음 페이지로, false: 이전 페이지로

    public DiaryScreen(Game game, Vector2 quiz5SpawnPosition) {
        this.game = game;
        this.quiz5SpawnPosition = quiz5SpawnPosition;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        // Stage 및 Skin 초기화
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // 배경 텍스처 로드
        backgroundTexture = new Texture(Gdx.files.internal("voice_screen_background.png"));
        TextureRegionDrawable backgroundDrawable = new TextureRegionDrawable(new TextureRegion(backgroundTexture));

        // `sdss.fnt` 폰트 로드
        textFont = new BitmapFont(Gdx.files.internal("sdss.fnt"));

        // 효과음 로드
        pageTurnSound = Gdx.audio.newSound(Gdx.files.internal("page_flip.wav"));

        // UI 테이블 생성
        Table table = new Table();
        table.setFillParent(true);
        table.setBackground(backgroundDrawable); // 배경 설정
        stage.addActor(table);

        // 일기 텍스트 라벨 생성
        final Label.LabelStyle labelStyle = new Label.LabelStyle(textFont, new Color(0.5f, 0f, 0f, 1f)); // 어두운 붉은색 설정
        diaryLabel = new Label(diaryTexts[currentPage], labelStyle);
        diaryLabel.setWrap(true); // 텍스트 줄 바꿈 활성화
        table.add(diaryLabel).width(600).pad(20).row();

        // "이전" 버튼 생성
        TextButton.TextButtonStyle buttonStyle = createButtonStyle();
        TextButton prevButton = new TextButton("이전", buttonStyle);
        prevButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (currentPage > 0 && !isTransitioning) {
                    isTransitioning = true;
                    isTransitioningForward = false;
                    currentPage--;

                    // 페이지 변경 후 알파 값 초기화
                    diaryLabel.setText(diaryTexts[currentPage]);
                    transitionAlpha = 0f;

                    // 효과음 재생
                    pageTurnSound.play();
                }
            }
        });

        // "다음" 버튼 생성
        TextButton nextButton = new TextButton("다음", buttonStyle);
        nextButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (currentPage < diaryTexts.length - 1 && !isTransitioning) {
                    isTransitioning = true;
                    isTransitioningForward = true;
                    currentPage++;

                    // 페이지 변경 후 알파 값 초기화
                    diaryLabel.setText(diaryTexts[currentPage]);
                    transitionAlpha = 0f;

                    // 효과음 재생
                    pageTurnSound.play();
                } else if (currentPage == diaryTexts.length - 1) {
                    // 마지막 페이지를 넘기면 VoiceScreen으로 전환
                    game.setScreen(new VoiceScreen(game, quiz5SpawnPosition));
                }
            }
        });

        // 버튼 테이블 생성
        Table buttonTable = new Table();
        buttonTable.add(prevButton).width(200).height(75).pad(10).left();
        buttonTable.add(nextButton).width(200).height(75).pad(10).right();
        table.add(buttonTable).expandX().padTop(10);
    }

    private TextButton.TextButtonStyle createButtonStyle() {
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = textFont;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.overFontColor = Color.RED;
        buttonStyle.up = skin.newDrawable("white", new Color(0.5f, 0f, 0f, 1f)); // 버튼 배경색
        buttonStyle.down = skin.newDrawable("white", new Color(0.3f, 0f, 0f, 1f));
        buttonStyle.over = skin.newDrawable("white", new Color(0.8f, 0f, 0f, 1f));
        return buttonStyle;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (isTransitioning) {
            transitionAlpha += delta * 2;
            if (transitionAlpha >= 1f) {
                transitionAlpha = 1f;
                isTransitioning = false;
            }
            diaryLabel.getColor().a = transitionAlpha;
        } else {
            diaryLabel.getColor().a = 1f;
        }

        // Stage 렌더링
        stage.act(delta);
        stage.draw();
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
        stage.dispose();
        skin.dispose();
        batch.dispose();
        backgroundTexture.dispose();
        textFont.dispose();
        pageTurnSound.dispose(); // 효과음 자원 해제
    }
}

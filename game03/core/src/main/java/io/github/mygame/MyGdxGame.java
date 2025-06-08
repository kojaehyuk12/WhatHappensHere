// core/src/io/github/mygame/core/MyGdxGame.java
package io.github.mygame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.physics.box2d.*;

public class MyGdxGame extends ApplicationAdapter {
    private SpriteBatch batch;
    private Stage stage;
    private Joystick joystick;
    private Vector2 playerPosition;
    private float playerSpeed;

    private Texture playerTexture;

    // Box2D
    private World world;
    private Body playerBody;

    // 애니메이션
    private Animation<TextureRegion> walkUpAnimation;
    private Animation<TextureRegion> walkDownAnimation;
    private Animation<TextureRegion> walkLeftAnimation;
    private Animation<TextureRegion> walkRightAnimation;

    private Animation<TextureRegion> idleUpAnimation;
    private Animation<TextureRegion> idleDownAnimation;
    private Animation<TextureRegion> idleLeftAnimation;
    private Animation<TextureRegion> idleRightAnimation;

    private float stateTime;
    private Direction currentDirection;
    private boolean isMoving;

    // 방향을 나타내는 열거형(Enum) 정의
    private enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        IDLE_UP,
        IDLE_DOWN,
        IDLE_LEFT,
        IDLE_RIGHT
    }

    @Override
    public void create () {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 조이스틱 텍스처 로드
        Texture joystickBase = new Texture("joystick_base.png");
        Texture joystickKnob = new Texture("joystick_knob.png");

        // 조이스틱 생성 및 위치 설정 (좌측 하단)
        joystick = new Joystick(joystickBase, joystickKnob, 50, 50);
        stage.addActor(joystick);

        // 플레이어 초기 위치
        playerPosition = new Vector2(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
        playerSpeed = 200f; // 초당 이동 거리

        // 플레이어 텍스처 로드 (대기용)
        playerTexture = new Texture("player_idle_down.png"); // 초기 대기용 이미지

        // 애니메이션 초기화
        stateTime = 0f;
        isMoving = false;
        currentDirection = Direction.IDLE_DOWN; // 초기 방향 설정

        // 걷기 애니메이션 로드
        walkUpAnimation = loadAnimation("player_walk_up.png", 4, 1, 0.1f);
        walkDownAnimation = loadAnimation("player_walk_down.png", 4, 1, 0.1f);
        walkLeftAnimation = loadAnimation("player_walk_left.png", 4, 1, 0.1f);
        walkRightAnimation = loadAnimation("player_walk_right.png", 4, 1, 0.1f);

        // 대기 애니메이션 로드 (각 방향의 첫 프레임 사용)
        idleUpAnimation = loadIdleAnimation("player_idle_up.png");
        idleDownAnimation = loadIdleAnimation("player_idle_down.png");
        idleLeftAnimation = loadIdleAnimation("player_idle_left.png");
        idleRightAnimation = loadIdleAnimation("player_idle_right.png");

        // Box2D 월드 생성
        world = new World(new Vector2(0, 0), true); // 중력 없음

        // 플레이어 바디 생성
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(playerPosition.x, playerPosition.y);

        playerBody = world.createBody(bodyDef);

        CircleShape shape = new CircleShape();
        shape.setRadius(playerTexture.getWidth() / 2);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0.3f;

        playerBody.createFixture(fixtureDef);
        shape.dispose();
    }

    // 애니메이션 로드 헬퍼 메서드
    private Animation<TextureRegion> loadAnimation(String fileName, int frameCols, int frameRows, float frameDuration) {
        Texture sheet = new Texture(Gdx.files.internal(fileName));
        TextureRegion[][] tmp = TextureRegion.split(sheet,
            sheet.getWidth() / frameCols,
            sheet.getHeight() / frameRows);

        TextureRegion[] frames = new TextureRegion[frameCols * frameRows];
        int index = 0;
        for (int i = 0; i < frameRows; i++) {
            for (int j = 0; j < frameCols; j++) {
                frames[index++] = tmp[i][j];
            }
        }

        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames);
        animation.setPlayMode(Animation.PlayMode.LOOP);
        return animation;
    }

    // 대기 애니메이션 로드 헬퍼 메서드
    private Animation<TextureRegion> loadIdleAnimation(String fileName) {
        Texture idleTexture = new Texture(Gdx.files.internal(fileName));
        TextureRegion idleRegion = new TextureRegion(idleTexture);
        Animation<TextureRegion> idleAnimation = new Animation<>(0.1f, idleRegion);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP);
        return idleAnimation;
    }

    @Override
    public void render () {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // 물리 엔진 업데이트
        world.step(deltaTime, 6, 2);

        // 조이스틱 방향에 따라 힘 적용
        Vector2 direction = joystick.getDirection();
        Vector2 force = direction.cpy().scl(playerSpeed);
        playerBody.setLinearVelocity(force);

        // 캐릭터 이동 여부 설정
        isMoving = direction.len() > 0;

        // 캐릭터의 현재 방향 설정
        if (isMoving) {
            if (Math.abs(direction.x) > Math.abs(direction.y)) {
                if (direction.x > 0) {
                    currentDirection = Direction.RIGHT;
                } else {
                    currentDirection = Direction.LEFT;
                }
            } else {
                if (direction.y > 0) {
                    currentDirection = Direction.UP;
                } else {
                    currentDirection = Direction.DOWN;
                }
            }
        } else {
            // 이동하지 않을 때는 마지막 방향의 대기 상태로 설정
            switch (currentDirection) {
                case UP:
                    currentDirection = Direction.IDLE_UP;
                    break;
                case DOWN:
                    currentDirection = Direction.IDLE_DOWN;
                    break;
                case LEFT:
                    currentDirection = Direction.IDLE_LEFT;
                    break;
                case RIGHT:
                    currentDirection = Direction.IDLE_RIGHT;
                    break;
                default:
                    currentDirection = Direction.IDLE_DOWN;
                    break;
            }
        }

        // 애니메이션 시간 업데이트
        stateTime += deltaTime;

        // 플레이어 위치 업데이트
        playerPosition.set(playerBody.getPosition());

        // 화면 클리어
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 렌더링
        batch.begin();
        TextureRegion currentFrame;
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
            case IDLE_UP:
                currentFrame = idleUpAnimation.getKeyFrame(stateTime, true);
                break;
            case IDLE_DOWN:
                currentFrame = idleDownAnimation.getKeyFrame(stateTime, true);
                break;
            case IDLE_LEFT:
                currentFrame = idleLeftAnimation.getKeyFrame(stateTime, true);
                break;
            case IDLE_RIGHT:
                currentFrame = idleRightAnimation.getKeyFrame(stateTime, true);
                break;
            default:
                currentFrame = idleDownAnimation.getKeyFrame(stateTime, true);
                break;
        }
        batch.draw(currentFrame, playerPosition.x - currentFrame.getRegionWidth() / 2,
            playerPosition.y - currentFrame.getRegionHeight() / 2);
        batch.end();

        // Stage 렌더링 (조이스틱)
        stage.act(deltaTime);
        stage.draw();
    }

    @Override
    public void dispose () {
        batch.dispose();
        stage.dispose();
        joystick.baseTexture.dispose();
        joystick.knobTexture.dispose();
        playerTexture.dispose();
        world.dispose();

        // 애니메이션 텍스처 정리
        disposeAnimation(walkUpAnimation);
        disposeAnimation(walkDownAnimation);
        disposeAnimation(walkLeftAnimation);
        disposeAnimation(walkRightAnimation);

        disposeAnimation(idleUpAnimation);
        disposeAnimation(idleDownAnimation);
        disposeAnimation(idleLeftAnimation);
        disposeAnimation(idleRightAnimation);
    }

    // 애니메이션 텍스처 정리 헬퍼 메서드
    private void disposeAnimation(Animation<TextureRegion> animation) {
        for (TextureRegion frame : animation.getKeyFrames()) {
            frame.getTexture().dispose();
        }
    }
}
